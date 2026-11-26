package com.mewrix.flowloom.core.bootstrap.init

import com.mewrix.flowloom.core.bootstrap.properties.LaunchOptionsProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

/**
 * 应用启动时准备 Flowloom 的数据目录（home）。
 *
 * 构造时依次做三件事：
 * 1. home 不存在 -> 创建目录
 * 2. home 已存在 -> 确认它是目录（不是文件）；空目录按全新安装处理，非空目录直接沿用
 * 3. 检查当前进程对 home 有读、写、进入权限，并实际创建一个临时文件验证能写入
 *
 * 任何一步失败都会抛出 IllegalStateException，应用启动终止，异常信息里写明原因和处理办法。
 * FileService 依赖本类，所以其他组件读写文件之前，这些检查一定已经完成。
 */
@Component
class HomeDirectoryInitializer(
    launchOptionsProperties: LaunchOptionsProperties
) {
    // 必须声明在 init 块之前，否则 init 里使用 log 时它还是 null
    private val log: Logger = LoggerFactory.getLogger(HomeDirectoryInitializer::class.java)

    /**
     * home 目录的绝对路径
     */
    val homeDir: Path = Path.of(launchOptionsProperties.home)
        .toAbsolutePath()
        .normalize()

    init {
        createHomeDirectoryIfMissing()
        checkHomeDirectoryPermissions()
    }

    private fun createHomeDirectoryIfMissing() {
        if (!Files.exists(homeDir)) {
            log.info("Flowloom home does not exist, creating: {}", homeDir)
            try {
                Files.createDirectories(homeDir)
            } catch (ex: IOException) {
                throw IllegalStateException(
                    "Cannot create Flowloom home $homeDir (${ex.javaClass.simpleName}: ${ex.message}). " +
                        "Make sure the parent directory exists and is writable, " +
                        "or pass --flowloom.launch.home=<another directory>.",
                    ex
                )
            }
            return
        }

        check(homeDir.isDirectory()) {
            "Flowloom home $homeDir exists but is not a directory."
        }
        if (isDirectoryEmpty(homeDir)) {
            log.info("Flowloom home is empty, treating as a fresh install: {}", homeDir)
        } else {
            log.info("Using existing Flowloom home: {}", homeDir)
        }
    }

    private fun checkHomeDirectoryPermissions() {
        val missingPermissions = buildList {
            if (!Files.isReadable(homeDir)) add("read")
            if (!Files.isWritable(homeDir)) add("write")
            if (!Files.isExecutable(homeDir)) add("execute (enter directory)")
        }
        check(missingPermissions.isEmpty()) {
            "Flowloom home $homeDir is missing permission(s): ${missingPermissions.joinToString()}. " +
                "Fix the directory permissions or pass --flowloom.launch.home=<another directory>."
        }

        // 权限位检查发现不了只读文件系统等情况，所以再真正创建一个临时文件试一次
        try {
            val testFile = Files.createTempFile(homeDir, ".write-test-", ".tmp")
            Files.deleteIfExists(testFile)
        } catch (ex: IOException) {
            throw IllegalStateException(
                "Cannot write to Flowloom home $homeDir (${ex.javaClass.simpleName}: ${ex.message}).",
                ex
            )
        }
    }

    private fun isDirectoryEmpty(dir: Path): Boolean =
        Files.newDirectoryStream(dir).use { entries ->
            !entries.iterator().hasNext()
        }
}
