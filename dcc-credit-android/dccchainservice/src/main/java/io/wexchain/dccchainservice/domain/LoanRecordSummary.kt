package io.wexchain.dccchainservice.domain
import com.google.gson.annotations.SerializedName



data class LoanRecordSummary(
    @SerializedName("orderId") val orderId: Long,
    @SerializedName("status") val status: LoanStatus,
    @SerializedName("applyDate") val applyDate: Long?,
    @SerializedName("currency") val currency: LoanCurrency?,
    @SerializedName("lender") val lender: Lender?
) {
}