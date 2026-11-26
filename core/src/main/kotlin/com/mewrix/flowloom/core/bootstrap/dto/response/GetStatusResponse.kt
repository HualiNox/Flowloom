package com.mewrix.flowloom.core.bootstrap.dto.response

import com.mewrix.flowloom.core.bootstrap.state.SetupState
import java.util.UUID

data class GetStatusResponse(
    val instanceId: UUID,
    val setupState: SetupState
)
