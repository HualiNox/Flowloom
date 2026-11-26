package com.mewrix.core.storage.file

import com.mewrix.core.bootstrap.init.HomeDirectoryInitializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonTypeRef
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

inline fun <reified T> FileService.loadFromJson(path: String): T? = loadFromJson(path, jacksonTypeRef<T>())

@Service
class FileService(
    homeDirectoryInitializer: HomeDirectoryInitializer,
    private val objectMapper: ObjectMapper
) {
    private val log: Logger = LoggerFactory.getLogger(FileService::class.java)
    private val rootDir: Path = homeDirectoryInitializer.homeDir
    // 同一进程内按规范化后的目标路径协调读写和删除，锁的范围是单个文件。
    private val pathLocks = ConcurrentHashMap<Path, ReentrantLock>()

    /**
     * 把相对于 home 的路径转成规范化的绝对路径。
     * 检查只针对路径文本，不解析符号链接，调用方应传入受信任的内部路径。
     */
    fun getAbsolutePath(path: String): Path = resolveSafePath(path)

    /**
     * 读取 JSON 文件并反序列化为对象。
     * 文件不存在时返回 null。
     */
    fun <T> loadFromJson(path: String, typeRef: TypeReference<T>): T? {
        val target = resolveSafePath(path)
        if (!Files.exists(target)) {
            return null
        }

        val lock = lockFor(target)
        return lock.withLock {
            if (!Files.exists(target)) {
                // 等待锁期间，另一个线程可能已删除文件。
                return@withLock null
            }
            Files.newBufferedReader(target, Charsets.UTF_8).use { reader ->
                objectMapper.readValue(reader, typeRef)
            }
        }
    }

    /**
     * 把对象序列化为 JSON 并写入文件，覆盖原有内容。
     * 写入失败时不会删除原文件。
     */
    fun <T> saveToJson(path: String, data: T) {
        writeViaTempFile(resolveSafePath(path)) { tempFile ->
            objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(tempFile.toFile(), data)
        }
    }

    /**
     * 把字符串原样写入文本文件（UTF-8）。
     *
     * @param ownerOnlyAccess 为 true 时，文件权限设为 600，所在目录（home 本身除外）设为 700，
     *                        只有当前系统用户能访问；Windows 等不支持 POSIX 权限的系统会跳过这一步
     */
    fun saveToText(path: String, content: String, ownerOnlyAccess: Boolean = false) {
        writeViaTempFile(resolveSafePath(path), restrictParentDirectory = ownerOnlyAccess) { tempFile ->
            Files.writeString(tempFile, content, Charsets.UTF_8)
            if (ownerOnlyAccess) {
                setOwnerOnlyPermissions(tempFile, isDirectory = false)
            }
        }
    }

    /**
     * 先把内容写进同目录下的临时文件，写完后再改名覆盖目标文件。
     * 写入临时文件失败时不会开始替换。替换优先使用原子移动，
     * 文件系统不支持原子移动时退回普通移动。
     *
     * @param writeContent 接收临时文件路径，负责把内容写进去
     */
    private fun writeViaTempFile(
        target: Path,
        restrictParentDirectory: Boolean = false,
        writeContent: (tempFile: Path) -> Unit
    ) {
        lockFor(target).withLock {
            val parentDir = target.parent ?: rootDir
            Files.createDirectories(parentDir)
            if (restrictParentDirectory && parentDir != rootDir) {
                setOwnerOnlyPermissions(parentDir, isDirectory = true)
            }

            val tempFile = Files.createTempFile(parentDir, ".${target.fileName}.", ".tmp")
            try {
                writeContent(tempFile)
                moveReplacingTarget(tempFile, target)
            } finally {
                Files.deleteIfExists(tempFile)
            }
        }
    }

    /**
     * 目录设为 rwx------（700），文件设为 rw-------（600）
     */
    private fun setOwnerOnlyPermissions(path: Path, isDirectory: Boolean) {
        try {
            Files.setPosixFilePermissions(
                path,
                PosixFilePermissions.fromString(if (isDirectory) "rwx------" else "rw-------")
            )
        } catch (ex: UnsupportedOperationException) {
            log.debug("File system does not support POSIX permissions, skip setting owner-only access on {}", path)
        }
    }

    /**
     * 检查文件是否存在。
     *
     * @param path 相对于 home 的路径
     * @return 文件存在返回 true，否则返回 false
     */
    fun exists(path: String): Boolean {
        val target = resolveSafePath(path)
        return Files.exists(target)
    }

    /**
     * 删除文件或空目录。
     *
     * @param path 相对于 home 的路径
     * @return 文件存在并删除成功返回 true，否则返回 false
     */
    fun delete(path: String): Boolean {
        val target = resolveSafePath(path)
        val lock = lockFor(target)
        return lock.withLock {
            Files.deleteIfExists(target)
        }
    }

    /**
     * 把相对于 home 的路径转成规范化的绝对路径。
     *
     * @param relativePath 相对于 home 的路径
     * @return 规范化的绝对路径
     */
    private fun resolveSafePath(relativePath: String): Path {
        val cleaned = relativePath.trim()
        require(cleaned.isNotEmpty()) {
            "Relative path must not be blank"
        }

        val candidate = Path.of(cleaned)
        require(!candidate.isAbsolute) {
            "Path must be relative to homeDir: $relativePath"
        }

        val resolved = rootDir.resolve(candidate).normalize()
        require(resolved.startsWith(rootDir) && resolved != rootDir) {
            "Path escapes homeDir directory: $relativePath"
        }

        return resolved
    }

    /**
     * 获取指定路径的锁对象，确保同一进程内对同一文件的读写和删除操作串行化。
     *
     * @param target 目标文件的绝对路径
     * @return 锁对象
     */
    private fun lockFor(target: Path): ReentrantLock =
        pathLocks.computeIfAbsent(target) {
            ReentrantLock()
        }

    /**
     * 尝试原子移动文件，失败时退回普通移动。
     *
     * @param source 源文件路径
     * @param target 目标文件路径
     */
    private fun moveReplacingTarget(source: Path, target: Path) {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (ex: AtomicMoveNotSupportedException) {
            log.warn("Atomic move not supported, falling back to non-atomic move for $source -> $target", ex)
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
