package com.mewrix.flowloom.core.bootstrap

import com.mewrix.flowloom.core.bootstrap.dto.response.GetStatusResponse
import com.mewrix.flowloom.core.bootstrap.instance.InstanceService
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
}
