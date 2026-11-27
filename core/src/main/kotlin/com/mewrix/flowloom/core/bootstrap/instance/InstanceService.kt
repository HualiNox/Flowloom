package com.mewrix.flowloom.core.bootstrap.instance

import com.mewrix.flowloom.core.storage.file.FileService
import com.mewrix.flowloom.core.storage.file.loadFromJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.core.JacksonException
import java.io.IOException
import java.nio.file.Files

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

    private val log: Logger = LoggerFactory.getLogger(InstanceService::class.java)

    @Volatile
    private var instanceConfig: InstanceConfig = loadOrCreateInstanceConfig()

    fun snapshot(): InstanceConfig = instanceConfig

    /** 状态由各配置服务计算。写入失败时保留原快照，等下次计算状态时重试。 */
    @Synchronized
    fun updateInstanceState(newState: State) {
        if (instanceConfig.instanceState == newState) {
            return
        }
        val updated = instanceConfig.copy(instanceState = newState)
        try {
            fileService.saveToJson(INSTANCE_CONFIG_PATH, updated)
            instanceConfig = updated
        } catch (ex: IOException) {
            log.warn("Could not save instance state to $INSTANCE_CONFIG_PATH", ex)
        } catch (ex: JacksonException) {
            log.warn("Could not serialize instance state for $INSTANCE_CONFIG_PATH", ex)
        }
    }

    private fun loadOrCreateInstanceConfig(): InstanceConfig {
        val configPath = fileService.getAbsolutePath(INSTANCE_CONFIG_PATH)
        val config = try {
            fileService.loadFromJson<InstanceConfig>(INSTANCE_CONFIG_PATH)
        } catch (ex: JacksonException) {
            // 非预期的 JSON 结构，启动时拒绝
            log.error("Failed to read instance configuration at $configPath")

            throw IllegalStateException(
                "Failed to read instance configuration at $configPath. " +
                        "Please check the file content or delete it to let Flowloom create a new one.",
                ex
            )
        }
        if (config != null) {
            return config
        }

        check(Files.notExists(configPath)) {
            "Cannot read instance configuration at $configPath. Expected a configuration object."
        }
        return InstanceConfig.createDefault().also {
            fileService.saveToJson(INSTANCE_CONFIG_PATH, it)
        }
    }
}
