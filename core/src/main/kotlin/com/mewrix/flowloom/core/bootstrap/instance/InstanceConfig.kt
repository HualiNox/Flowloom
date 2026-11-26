package com.mewrix.flowloom.core.bootstrap.instance

import com.mewrix.flowloom.core.bootstrap.state.SetupState
import java.util.UUID

data class InstanceConfig(
    val instanceId: UUID,
    val setupState: SetupState
)
