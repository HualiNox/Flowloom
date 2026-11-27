package com.mewrix.flowloom.core.bootstrap.instance

import com.mewrix.flowloom.core.storage.file.FileService
import com.mewrix.flowloom.core.storage.file.loadFromJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.core.JacksonException
import java.nio.file.Files

/**
 * 管理实例信息。
 *
 * 启动时读取 instance.json；文件不存在时实例信息只保存在内存中，
 * 确认引导时由 [persist] 写入。引导是否完成不记录在这里，以 admin.json 的重置标记为准。
 */
@Service
class InstanceService(
    private val fileService: FileService
) {
    companion object {
        private const val INSTANCE_CONFIG_PATH = "instance/instance.json"
    }

    private val log: Logger = LoggerFactory.getLogger(InstanceService::class.java)

    /** 启动时确定后不再变化：读到文件就沿用，没有文件就在内存中新建，等 [persist] 时才落盘 */
    private val instanceConfig: InstanceConfig = loadOrCreateInstanceConfig()

    fun snapshot(): InstanceConfig = instanceConfig

    /**
     * 把实例信息写入 instance.json，由确认引导时调用，可重复调用。
     * 失败时抛出 IOException 或 JacksonException。
     */
    fun persist() {
        fileService.saveToJson(INSTANCE_CONFIG_PATH, instanceConfig)
    }

    /**
     * 读取 instance.json。文件内容不是预期的 JSON 结构时拒绝启动；
     * 文件不存在时只在内存中新建，不写文件。
     */
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
        return InstanceConfig.createDefault()
    }
}
