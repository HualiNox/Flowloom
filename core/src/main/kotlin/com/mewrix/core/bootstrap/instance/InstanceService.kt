package com.mewrix.core.bootstrap.instance

import com.mewrix.core.bootstrap.state.SetupState
import com.mewrix.core.storage.file.FileService
import com.mewrix.core.storage.file.loadFromJson
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 管理实例信息
 */
@Service
class InstanceService(
    private val fileService: FileService
) {
    companion object {
        private const val INSTANCE_CONFIG_PATH = "instance/instance.json"
    }

    @Volatile
    private var instanceConfig: InstanceConfig = loadOrCreateInstanceConfig()

    val instanceId: UUID
        get() = instanceConfig.instanceId

    val setupState: SetupState
        get() = instanceConfig.setupState

    /**
     * 串行处理状态更新，写入成功后才发布新的内存快照。
     * 写入抛出异常时，读取方仍会看到上一次发布的状态。
     */
    @Synchronized
    fun updateSetupState(newState: SetupState) {
        if (instanceConfig.setupState == newState) {
            return
        }
        val updated = instanceConfig.copy(setupState = newState)
        fileService.saveToJson(INSTANCE_CONFIG_PATH, updated)
        instanceConfig = updated
    }

    private fun loadOrCreateInstanceConfig(): InstanceConfig =
        fileService.loadFromJson<InstanceConfig>(INSTANCE_CONFIG_PATH)
            ?: InstanceConfig(
                instanceId = UUID.randomUUID(),
                setupState = SetupState.UNINITIALIZED
            ).also {
                fileService.saveToJson(INSTANCE_CONFIG_PATH, it)
            }
}
