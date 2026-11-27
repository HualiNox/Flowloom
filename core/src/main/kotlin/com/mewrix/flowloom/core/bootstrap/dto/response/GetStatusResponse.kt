package com.mewrix.flowloom.core.bootstrap.dto.response

import com.mewrix.flowloom.core.bootstrap.SetupState
import com.mewrix.flowloom.core.bootstrap.instance.State
import java.util.UUID

data class GetStatusResponse(
    val instanceId: UUID,
    val instanceState: State,
    val setupState: SetupState
)
