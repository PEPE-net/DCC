package io.wexchain.android.dcc

import android.support.multidex.MultiDexApplication
import com.wexmarket.android.network.Networking
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.wexchain.android.dcc.repo.AssetsRepository
import io.wexchain.android.dcc.repo.PassportRepository
import io.wexchain.android.dcc.repo.db.PassportDatabase
import io.wexchain.android.idverify.IdVerifyHelper
import io.wexchain.auth.BuildConfig
import io.wexchain.dccchainservice.CertApi
import io.wexchain.dccchainservice.ChainGateway
import io.wexchain.dccchainservice.PrivateChainApi
import io.wexchain.dccchainservice.domain.Result
import io.wexchain.digitalwallet.Chain
import io.wexchain.digitalwallet.DigitalCurrency
import io.wexchain.digitalwallet.EthsTransaction
import io.wexchain.digitalwallet.api.*
import io.wexchain.digitalwallet.proxy.*
import java.lang.ref.WeakReference
import java.math.BigInteger

/**
 * Created by sisel on 2018/3/27.
 */
class App : MultiDexApplication() {

    //lib
    lateinit var idVerifyHelper: IdVerifyHelper

    private lateinit var networking: Networking

    //our services
    lateinit var chainGateway: ChainGateway
    lateinit var certApi: CertApi
    lateinit var chainFrontEndApi: ChainFrontEndApi
    lateinit var privateChainApi: PrivateChainApi

    //public services
    lateinit var infuraApi: InfuraApi
    lateinit var etherScanApi: EtherScanApi
    lateinit var ethplorerApi: EthplorerApi
    lateinit var publicRpc: EthsRpcAgent


    lateinit var passportRepository: PassportRepository
    lateinit var assetsRepository: AssetsRepository

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
        RxJavaPlugins.setErrorHandler { if (BuildConfig.DEBUG) it.printStackTrace() }
        initLibraries(this)
        initServices(this)
        initData(this)
    }

    private fun initData(app: App) {
        val dao = PassportDatabase.createDatabase(this).dao
        passportRepository = PassportRepository(app)
        passportRepository.load()
        assetsRepository = AssetsRepository(
                dao,
                chainFrontEndApi,
                EthereumAgent(EthsRpcAgent.Companion.by(infuraApi), EthsTxAgent.Companion.by(etherScanApi)),
                { buildAgent(it) }
        )
    }

    private fun initServices(app: App) {
        val networking = Networking(app, BuildConfig.DEBUG)
        this.networking = networking
        chainGateway = networking.createApi(ChainGateway::class.java, BuildConfig.GATEWAY_BASE_URL)
        certApi = networking.createApi(CertApi::class.java, BuildConfig.CHAIN_FUNC_URL)
        chainFrontEndApi = networking.createApi(ChainFrontEndApi::class.java, BuildConfig.CHAIN_FRONTEND_URL)
        privateChainApi = networking.createApi(PrivateChainApi::class.java,BuildConfig.CHAIN_EXPLORER_URL)

        infuraApi = networking.createApi(InfuraApi::class.java, InfuraApi.getUrl)
        etherScanApi = networking.createApi(EtherScanApi::class.java, EtherScanApi.apiUrl(Chain.publicEthChain))
        ethplorerApi = networking.createApi(EthplorerApi::class.java, EthplorerApi.API_URL)
        publicRpc = EthsRpcAgent.by(infuraApi)
    }

    private fun initLibraries(context: App) {
        idVerifyHelper = IdVerifyHelper(context)
    }

    private fun buildAgent(dc: DigitalCurrency): Erc20Agent {
        return when (dc.chain) {
            Chain.JUZIX_PRIVATE -> {
                dc.contractAddress!!
                val privateRpc = networking.createApi(EthJsonRpcApi::class.java, EthJsonRpcApi.juzixErc20RpcUrl(dc.symbol))
                        .getPrepared()
                JuzixErc20Agent(dc, EthsRpcAgent.by(privateRpc), txAgentBy(privateChainApi, dc))
            }
            Chain.publicEthChain -> {
                val contractAddress = dc.contractAddress!!
                Erc20Agent(dc, publicRpc, EthsTxAgent.Companion.by(ethplorerApi, contractAddress))
            }
            else -> throw IllegalArgumentException()
        }
    }

    private fun txAgentBy(privateChainApi: PrivateChainApi, dc: DigitalCurrency) = object : EthsTxAgent {
        override fun listTransactionsOf(address: String, start: Long, end: Long): Single<List<EthsTransaction>> {
            val contractAddress = dc.contractAddress!!
            return privateChainApi.tokenTransfer(contractAddress, address)
                    .compose(Result.checked())
                    .map {
                        it.items.map {
                            EthsTransaction(
                                    digitalCurrency = dc,
                                    txId = it.transactionHash,
                                    from = it.fromAddress,
                                    to = it.toAddress,
                                    amount = BigInteger(it.value),
                                    transactionType = EthsTransaction.TYPE_TRANSFER,
                                    time = it.blockTimestamp,
                                    blockNumber = it.blockNumber,
                                    gas = BigInteger.ZERO,
                                    gasPrice = BigInteger.ZERO,
                                    gasUsed = BigInteger.ZERO,
                                    status = EthsTransaction.Status.MINED
                            )
                        }
                    }
        }
    }


    companion object {


        private lateinit var instance: WeakReference<App>

        fun get(): App = instance.get()!!
    }

}