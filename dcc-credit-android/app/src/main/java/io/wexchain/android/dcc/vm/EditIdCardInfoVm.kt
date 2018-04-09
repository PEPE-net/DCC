package io.wexchain.android.dcc.vm

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField
import io.reactivex.android.schedulers.AndroidSchedulers
import io.wexchain.android.common.SingleLiveEvent
import io.wexchain.android.dcc.App
import io.wexchain.android.idverify.IdCardEssentialData
import io.wexchain.dccchainservice.CertApi
import io.wexchain.dccchainservice.domain.IdOcrInfo
import io.wexchain.dccchainservice.domain.Result

/**
 * Created by sisel on 2018/2/27.
 */
class EditIdCardInfoVm : ViewModel() {

    val imgFront = ObservableField<ByteArray>()
    val imgBack = ObservableField<ByteArray>()

    val name = ObservableField<String>()
    val sex = ObservableField<String>()
    val race = ObservableField<String>()
    var year: Int? = null
    var month: Int? = null
    var dayOfMonth: Int? = null
    val birth = ObservableField<String>()
    val address = ObservableField<String>()
    val idNo = ObservableField<String>()
    val timeLimit = ObservableField<String>()
    val authority = ObservableField<String>()

    val ocrEvent = SingleLiveEvent<IdOcrInfo.IdCardInfo>()
    val proceedEvent = SingleLiveEvent<IdCardEssentialData>()
    val informationIncompleteEvent = SingleLiveEvent<CharSequence>()

    private fun setInfoFromIdCardInfo(info: IdOcrInfo.IdCardInfo) {
        info.let {
            it.name?.let { name.set(it) }
            it.sex?.let { sex.set(it) }
            it.nation?.let { race.set(it) }
            it.year?.let { year = it.toIntOrNull() }
            it.month?.let { month = it.toIntOrNull() }
            it.day?.let { dayOfMonth = it.toIntOrNull() }
            it.address?.let { address.set(it) }
            it.number?.let { idNo.set(it) }
            it.timelimit?.let { timeLimit.set(it) }
            it.authority?.let { authority.set(it) }
            updateBirthText()
        }
    }

    fun confirmToNext() {
        val n = name.get()
        val id = idNo.get()
        val tl = timeLimit.get()
        if (n == null) {
            informationIncompleteEvent.value = "请填写姓名"
            return
        }
        if (id == null) {
            informationIncompleteEvent.value = "请填写身份证号"
            return
        }
        if (tl == null) {
            informationIncompleteEvent.value = "请填写有效期限"
            return
        }
        IdCardEssentialData.from(
            n,
            id,
            tl,
            sex.get(),
            race.get(),
            year, month, dayOfMonth,
            address.get(),
            authority.get()
        )?.let {
            proceedEvent.value = it
        }
    }

    private fun updateBirthText() {
        if (year != null && month != null && dayOfMonth != null) {
            this.birth.set("${year}年${month}月${dayOfMonth}日")
        }
    }

    fun toIdCardInfo(): IdOcrInfo.IdCardInfo {
        return IdOcrInfo.IdCardInfo(
            address = address.get(),
            day = dayOfMonth.toString(),
            month = month.toString(),
            year = year.toString(),
            nation = race.get(),
            sex = sex.get(),
            name = name.get(),
            number = idNo.get(),
            authority = authority.get(),
            timelimit = timeLimit.get()
        )
    }

    fun doOcrFront() {
        val front:ByteArray? = imgFront.get()
        if(front!=null){
            App.get().certApi.idOcr(CertApi.uploadFilePart(front,"front.jpg","image/jpeg"))
                    .compose(Result.checked())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { info->
                        if(front === imgFront.get()){
                            if(info.side == IdOcrInfo.Side.front){

                            }else{
                                //todo
                            }
                            setInfoFromIdCardInfo(info.idCardInfo)
                            ocrEvent.value = info.idCardInfo
                        }
                    }
        }
    }

    fun doOcrBack() {
        val back:ByteArray? = imgBack.get()
        if(back!=null){
            App.get().certApi.idOcr(CertApi.uploadFilePart(back,"back.jpg","image/jpeg"))
                    .compose(Result.checked())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { info->
                        if(back === imgBack.get()){
                            if(info.side == IdOcrInfo.Side.back){

                            }else{
                                //todo
                            }
                            setInfoFromIdCardInfo(info.idCardInfo)
                            ocrEvent.value = info.idCardInfo
                        }
                    }
        }
    }

}