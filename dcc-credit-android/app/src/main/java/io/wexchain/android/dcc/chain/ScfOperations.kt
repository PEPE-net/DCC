package io.wexchain.android.dcc.chain

import com.google.gson.reflect.TypeToken
import io.reactivex.Single
import io.reactivex.Flowable
import io.reactivex.SingleTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.wexchain.android.dcc.App
import io.wexchain.android.dcc.chain.ScfOperations.loanOrderByTx
import io.wexchain.android.dcc.domain.Passport
import io.wexchain.android.dcc.tools.MultiChainHelper
import io.wexchain.android.dcc.tools.RetryWithDelay
import io.wexchain.android.dcc.tools.pair
import io.wexchain.android.dcc.vm.domain.LoanScratch
import io.wexchain.dccchainservice.ChainGateway
import io.wexchain.dccchainservice.DccChainServiceException
import io.wexchain.dccchainservice.ScfApi
import io.wexchain.dccchainservice.domain.BusinessCodes
import io.wexchain.dccchainservice.domain.CertOrder
import io.wexchain.dccchainservice.domain.LoanChainOrder
import io.wexchain.dccchainservice.domain.Result
import io.wexchain.dccchainservice.util.ParamSignatureUtil
import io.wexchain.digitalwallet.Currencies
import okhttp3.MediaType
import java.math.BigDecimal
import java.math.BigInteger
import java.security.MessageDigest
import java.security.PrivateKey

object ScfOperations {

    val LOAN_DIGEST_VERSION_1 = BigInteger.ONE

    private const val DIGEST = "SHA256"

    @Deprecated("replaced")
    fun cancelLoan(orderId:Long,passport: Passport): Single<String> {
        val credentials = passport.credential
        val nonce = privateChainNonce(credentials.address)
        val chainGateway = App.get().chainGateway
        return Single.just(orderId)
            .observeOn(Schedulers.computation())
            .map {
                EthsFunctions.cancelLoan(orderId)
            }
            .observeOn(Schedulers.io())
            .flatMap { cancelFunc ->
                chainGateway.getLoanContractAddress()
                    .compose(Result.checked())
                    .flatMap {
                        val tx = cancelFunc.txSigned(passport.credential, it, nonce)
                        chainGateway.cancelLoanOrder(tx)
                            .compose(Result.checked())
                    }
            }
            .confirmOnChain(chainGateway)
    }

    fun cancelLoan(orderId: Long): Single<String> {
        return withScfTokenInCurrentPassport("") {
            App.get().scfApi.cancelLoan(it,orderId)
        }
    }

    fun submitLoan(loanScratch: LoanScratch, passport: Passport): Single<String> {
        val certIdData = CertOperations.getCertIdData()
        val cmLogPhoneNo = CertOperations.getCmLogPhoneNo()
        val certBankCardData = CertOperations.getCertBankCardData()
        val credentials = passport.credential
        val nonce = privateChainNonce(credentials.address)
        val chainGateway = App.get().chainGateway
        val scfApi = App.get().scfApi
        return if (certIdData != null && certBankCardData != null && cmLogPhoneNo != null) {
            Single.just(loanScratch)
                .observeOn(Schedulers.computation())
                .map {
                    val applicationDigest = loanApplicationDigest(loanScratch)
                    val idDigest = CertOperations.digestIdName(certIdData.name, certIdData.id)
                    val feeUint256 = Currencies.DCC.toIntExact(it.fee)
                    EthsFunctions.applyLoan(
                        LOAN_DIGEST_VERSION_1,
                        idDigest,
                        applicationDigest,
                        feeUint256,
                        it.beneficiaryAddress.address
                    )
                }
                .observeOn(Schedulers.io())
                .flatMap { applyFunc ->
                    Single.zip(
                        chainGateway.getTicket().compose(Result.checked()),
                        chainGateway.getLoanContractAddress().compose(Result.checked()),
                        pair()
                    )
                        .flatMap { (ticket, contractAddress) ->
                            val tx = applyFunc.txSigned(passport.credential, contractAddress, nonce)
                            chainGateway.applyLoan(ticket.ticket, tx)
                                .compose(Result.checked())
                        }
                }
                .loanOrderByTx(chainGateway)
                .flatMap {order->
                    ScfOperations.withScfTokenInCurrentPassport(allowNull = "") {
                        scfApi.applyLoanCredit(
                            it,
                            order.id,
                            loanScratch.product.id,
                            certIdData.name,
                            loanScratch.amount.toLoanFormatString(),
                            loanScratch.period.value,
                            loanScratch.period.unit.name,
                            certIdData.id,
                            cmLogPhoneNo,
                            certBankCardData.bankCardNo,
                            certBankCardData.phoneNo,
                            loanScratch.createTime
                        )
                    }
                }
        } else {
            Single.error<String>(IllegalStateException())
        }
    }

    fun Single<String>.loanOrderByTx(api: ChainGateway): Single<LoanChainOrder> {
        return this.confirmOnChain(api)
            .flatMap {
                api.getLoanOrdersByTx(it)
                    .compose(Result.checked())
                    .map {
                        it.first()
                    }
            }
    }

    fun Single<String>.confirmOnChain(api:ChainGateway):Single<String>{
        return this.flatMap { txHash ->
            api.getReceiptResult(txHash)
                .compose(Result.checked())
                .map {
                    if (!it.hasReceipt) {
                        throw DccChainServiceException("no receipt yet")
                    }
                    it
                }
                .retryWhen(RetryWithDelay.createSimple(6, 5000L))
                .map {
                    if(!it.approximatelySuccess){
                        throw DccChainServiceException()
                    }
                    txHash
                }
        }
    }

    /**
     * //申请数字摘要applicationDigest：sha256(utf8_to_bytes(放款机构名称+tostring(币种代码)+tostring(借款金额)+tostring(day(借款期限+'D'))+tostring(借款期数)+tostring(借款方式(XinYong))+ tostring(mills(申请时间))))
     */
    @JvmStatic
    fun loanApplicationDigest(loanScratch: LoanScratch): ByteArray {
        val lenderName = loanScratch.product.lender.name
        val currencySymbol = loanScratch.product.currency.symbol
        val amountStr = loanScratch.amount.toLoanFormatString()
        val period = "${loanScratch.period.value}${loanScratch.period.unit.name.first()}"
        val digestStr =
            "$lenderName$currencySymbol$amountStr$period${loanScratch.product.repayCyclesNo}${loanScratch.product.loanType}${loanScratch.createTime}"
        return MessageDigest.getInstance(DIGEST).digest(digestStr.toByteArray(Charsets.UTF_8))
    }

    private fun BigDecimal.toLoanFormatString(): String {
        val s4 = this.setScale(4, java.math.RoundingMode.DOWN)
        return s4.toPlainString()
    }

    fun loadHolding(): Single<BigDecimal> {
        val passport = App.get().passportRepository.getCurrentPassport()
        return if (passport != null) {
            val dccPrivate = MultiChainHelper.getDccPrivate()
            App.get().assetsRepository.getDigitalCurrencyAgent(dccPrivate)
                .getBalanceOf(passport.address)
                .map {
                    dccPrivate.toDecimalAmount(it)
                }
                .observeOn(AndroidSchedulers.mainThread())
        } else {
            Single.error<BigDecimal>(IllegalStateException())
        }
    }

    fun loadContractPdf(orderId: Long): Single<ByteArray> {
        return ScfOperations.currentToken
            .flatMap {
                App.get().scfApi
                    .loanAgreement(it, orderId)
                    .map {
                        when (it.contentType()) {
                            MediaType.parse("application/pdf") -> it.bytes()
                            MediaType.parse("application/json") -> {
                                val result =
                                    App.get().networking.networkGson.fromJson<Result<String>>(it.charStream(),
                                        object : TypeToken<Result<String>>() {}.type)
                                throw result.asError()
                            }
                            else -> throw IllegalArgumentException()
                        }
                    }
            }
            .compose(ScfOperations.withScfTokenInCurrentPassport())
    }

    /**
     * check scf access token and proceed action
     * 1. check token existence
     * 2. on token fail retry after login(get new token)
     */
    fun <T> withScfToken(address: String?, privateKey: PrivateKey?): SingleTransformer<T, T> {
        return SingleTransformer {
            val call = it
            if (App.get().scfTokenManager.scfToken == null) {
                loginWithPassport(address, privateKey)
                    .flatMap { call }
            } else {
                it.retryWhen {
                    it.flatMap {
                        if (isTokenFail(it.cause ?: it)) {
                            loginWithPassport(address, privateKey)
                        } else {
                            Single.error<T>(it)
                        }.toFlowable()
                    }
                }
            }
        }
    }

    fun <T> withScfTokenInCurrentPassport(): SingleTransformer<T, T> {
        val passport = App.get().passportRepository.getCurrentPassport()
        return withScfToken<T>(passport?.address, passport?.authKey?.getPrivateKey())
    }

    inline fun <T> withScfTokenInCurrentPassport(
        allowNull: T? = null,
        crossinline call: (String) -> Single<Result<T>>
    ): Single<T> {
        return currentToken
            .flatMap {
                val check = if (allowNull == null) {
                    Result.checked<T>()
                } else {
                    Result.checkedAllowingNull(allowNull)
                }
                call(it).compose(check)
            }
            .compose(withScfTokenInCurrentPassport())
    }

    val currentToken = Single.fromCallable { App.get().scfTokenManager.scfToken }

    private fun isTokenFail(e: Throwable): Boolean {
        return e is DccChainServiceException && e.businessCode == BusinessCodes.TOKEN_FORBIDDEN
    }

    private fun loginWithPassport(address: String?, privateKey: PrivateKey?): Single<String> {
        val app = App.get()
        val scfApi = App.get().scfApi
        if (address == null || privateKey == null) {
            return Single.error<String>(IllegalStateException())
        } else {
            return scfApi
                .getNonce()
                .compose(Result.checked())
                .flatMap {
                    scfApi
                        .login(
                            nonce = it,
                            address = address,
                            username = address,
                            password = null,
                            sign = ParamSignatureUtil.sign(
                                privateKey, mapOf(
                                    "nonce" to it,
                                    "address" to address,
                                    "username" to address,
                                    "password" to null
                                )
                            )
                        )
                        .map {
                            val body = it.body()
                            if (it.isSuccessful && body != null ) {
                                if(body.isSuccess) {
                                    it.headers()[ScfApi.HEADER_TOKEN]!!
                                }else{
                                    throw body.asError()
                                }
                            } else {
                                throw IllegalStateException()
                            }
                        }
                        .doOnSuccess {
                            app.scfTokenManager.scfToken = it
                        }
                }
        }
    }
}