package com.mewrix.core.bootstrap.instance

import com.mewrix.core.bootstrap.state.SetupState
import com.mewrix.core.storage.file.FileService
import com.mewrix.core.storage.file.loadFromJson
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * 管理实例信息（instance/instance.json）：实例 ID 和初始化状态。
 *
 * 首次启动时生成实例 ID 并落盘；之后每次启动都沿用文件里的内容。
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

    /** 更新初始化状态并立即落盘 */
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
