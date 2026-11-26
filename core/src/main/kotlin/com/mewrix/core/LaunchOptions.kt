package com.mewrix.core

import org.slf4j.LoggerFactory
import org.springframework.boot.DefaultApplicationArguments
import java.nio.file.Path

/**
 * 命令行启动参数
 */
class LaunchOptions(
    val homeDir: Path
) {
    companion object {
        private val log = LoggerFactory.getLogger(LaunchOptions::class.java)

        private const val HOME_DIR_OPTION = "flowloom.launch.home"
        private const val DEFAULT_HOME_DIR = "./.flowloom"

        fun parse(args: Array<String>): LaunchOptions {
            val values = DefaultApplicationArguments(*args).getOptionValues(HOME_DIR_OPTION)
            require(values == null || values.size == 1) {
                "--$HOME_DIR_OPTION requires exactly one value"
            }

            val homeDir = values?.single() ?: DEFAULT_HOME_DIR.also {
                log.warn("--{} is not set, using default: {}", HOME_DIR_OPTION, it)
            }

            return LaunchOptions(Path.of(homeDir).toAbsolutePath().normalize())
        }
    }

    /**
     * 将解析结果作为 Spring 默认属性。同名的命令行参数、环境变量或配置文件属性优先。
     */
    fun toMap(): Map<String, Any> = mapOf(HOME_DIR_OPTION to homeDir.toString())
}
