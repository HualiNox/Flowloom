package com.mewrix.core.bootstrap.dto.response

import com.mewrix.core.bootstrap.state.SetupState
import java.util.UUID

data class GetStatusResponse(
    val instanceId: UUID,
    val setupState: SetupState
)
