package com.mewrix.flowloom.core.bootstrap

import com.mewrix.flowloom.core.bootstrap.dto.response.GetStatusResponse
import com.mewrix.flowloom.core.bootstrap.instance.InstanceService
import com.mewrix.flowloom.core.bootstrap.instance.State
import com.mewrix.flowloom.core.user.UserService
import org.springframework.stereotype.Service

@Service
class BootstrapService(
    private val instanceService: InstanceService,
    private val userService: UserService
) {

    fun instanceStatus(): GetStatusResponse {
        val setupState = refreshInstanceState()
        return GetStatusResponse(
            instanceId = instanceService.snapshot().instanceId,
            instanceState = setupState.overallState(),
            setupState = setupState
        )
    }

    /**
     * 判断实例是否已初始化。
     *
     * @return [Boolean] 如果实例已初始化返回 true，否则返回 false。
     */
    fun isInitialized(): Boolean =
        refreshInstanceState().overallState() == State.INITIALIZED

    /** 汇总各配置服务的状态，并同步实例文件中的整体状态。 */
    @Synchronized
    fun refreshInstanceState(): SetupState {
        val adminAccount = if (userService.adminCredentialsReset()) State.INITIALIZED else State.UNINITIALIZED
        val setupState = SetupState(adminAccount = adminAccount)
        instanceService.updateInstanceState(setupState.overallState())
        return setupState
    }
}
