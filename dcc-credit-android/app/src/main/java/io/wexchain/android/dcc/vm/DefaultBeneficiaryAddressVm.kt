package io.wexchain.android.dcc.vm

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import io.wexchain.android.common.map
import io.wexchain.android.dcc.App
import io.wexchain.android.dcc.repo.db.BeneficiaryAddress

class DefaultBeneficiaryAddressVm(application: Application):AndroidViewModel(application) {
    val defaultAddr = getApplication<App>().passportRepository.defaultBeneficiaryAddress

    val walletAddr = getApplication<App>().passportRepository.currAddress

    fun changeDefault(beneficiaryAddress: BeneficiaryAddress?,checked:Boolean){
        beneficiaryAddress?.let {
            if(checked && defaultAddr.value != it.address){
                App.get().passportRepository.setDefaultBeneficiaryAddress(it.address)
            }else if(!checked && defaultAddr.value == it.address){
                App.get().passportRepository.setDefaultBeneficiaryAddress(null)
            }else{
                App.get().passportRepository.setDefaultBeneficiaryAddress(defaultAddr.value)
            }
        }
    }
}