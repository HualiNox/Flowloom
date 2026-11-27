package com.mewrix.flowloom.core.bootstrap

import com.mewrix.flowloom.core.bootstrap.dto.response.GetStatusResponse
import com.mewrix.flowloom.core.bootstrap.instance.InstanceService
import com.mewrix.flowloom.core.bootstrap.state.SetupState
import org.springframework.stereotype.Service

@Service
class BootstrapService(
    private val instanceService: InstanceService
) {

    fun instanceStatus(): GetStatusResponse =
        GetStatusResponse(
            instanceId = instanceService.instanceId,
            setupState = instanceService.setupState
        )

    /**
     * 判断实例是否已初始化。
     *
     * @return [Boolean] 如果实例已初始化返回 true，否则返回 false。
     */
    fun isInitialized(): Boolean =
        instanceService.setupState == SetupState.INITIALIZED
}
