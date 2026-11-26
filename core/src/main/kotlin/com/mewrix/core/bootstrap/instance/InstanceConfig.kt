package com.mewrix.core.bootstrap.instance

import com.mewrix.core.bootstrap.state.SetupState
import java.util.UUID

data class InstanceConfig(
    val instanceId: UUID,
    val setupState: SetupState
)
