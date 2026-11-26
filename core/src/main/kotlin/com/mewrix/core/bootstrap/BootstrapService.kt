package com.mewrix.core.bootstrap

import com.mewrix.core.bootstrap.dto.response.GetStatusResponse
import com.mewrix.core.bootstrap.instance.InstanceService
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
