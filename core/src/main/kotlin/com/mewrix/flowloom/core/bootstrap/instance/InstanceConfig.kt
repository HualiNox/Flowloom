package com.mewrix.flowloom.core.bootstrap.instance

import java.util.UUID

enum class State {
    UNINITIALIZED,
    INITIALIZED
}

data class InstanceConfig(
    val instanceId: UUID,
    val instanceState: State
) {
    companion object {
        fun createDefault(): InstanceConfig =
            InstanceConfig(
                instanceId = UUID.randomUUID(),
                instanceState = State.UNINITIALIZED
            )
    }
}
