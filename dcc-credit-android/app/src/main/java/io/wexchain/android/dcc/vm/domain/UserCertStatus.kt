package io.wexchain.android.dcc.vm.domain

enum class UserCertStatus {
    NONE,
    INCOMPLETE,
    DONE,
    ;

    fun isProcessing(): Boolean {
        return this == INCOMPLETE
    }
}