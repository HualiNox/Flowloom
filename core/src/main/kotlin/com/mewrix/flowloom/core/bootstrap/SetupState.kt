package com.mewrix.flowloom.core.bootstrap

import com.mewrix.flowloom.core.bootstrap.instance.State

/** 各配置服务当前报告的步骤状态，供状态接口返回。 */
data class SetupState(
    val adminAccount: State
) {
    fun overallState(): State =
        if (listOf(adminAccount).all { it == State.INITIALIZED }) {
            State.INITIALIZED
        } else {
            State.UNINITIALIZED
        }
}
