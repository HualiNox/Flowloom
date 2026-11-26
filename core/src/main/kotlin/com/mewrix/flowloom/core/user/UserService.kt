package com.mewrix.flowloom.core.user

import com.mewrix.flowloom.core.common.service.PasswordHasher
import com.mewrix.flowloom.core.storage.file.FileService
import com.mewrix.flowloom.core.storage.file.loadFromJson
import org.springframework.stereotype.Service
import java.nio.file.Path
import java.security.SecureRandom
import java.util.Base64

/** 刚创建好的管理员账户信息，用于在终端提示用户去哪里取密码 */
data class CreatedAdminAccount(
    val username: String,
    /** 保存明文初始密码的文件的绝对路径 */
    val passwordFile: Path
)

@Service
class UserService(
    private val fileService: FileService,
    private val passwordHasher: PasswordHasher
) {
    companion object {
        private const val ADMIN_USERNAME = "admin"
        private const val ADMIN_JSON_PATH = "security/admin.json"
        private const val INITIAL_PASSWORD_PATH = "secrets/initialAdminPassword"
        private const val INITIAL_PASSWORD_BYTES = 24
    }

    private val secureRandom = SecureRandom()

    /**
     * 如果 security/admin.json 不存在，就创建 admin 账户：
     * 1. 生成随机初始密码
     * 2. 明文写入 secrets/initialAdminPassword（在支持 POSIX 权限的文件系统上仅所有者可读写）
     * 3. 把密码的 Argon2id 哈希写入 security/admin.json（不保存明文）
     *
     * 先写密码文件、后写 admin.json：如果中途失败，admin.json 还不存在，
     * 下次启动会重新生成密码并覆盖密码文件，不会出现“账户已存在但没人知道密码”的情况。
     *
     * @return 新建的账户信息；admin.json 已存在、没有新建账户时返回 null
     */
    fun createAdminAccountIfMissing(): CreatedAdminAccount? {
        if (fileService.exists(ADMIN_JSON_PATH)) {
            return null
        }

        val initialPassword = generateInitialPassword()

        fileService.saveToText(INITIAL_PASSWORD_PATH, initialPassword + "\n", ownerOnlyAccess = true)
        fileService.saveToJson(
            ADMIN_JSON_PATH,
            User(
                username = ADMIN_USERNAME,
                password = passwordHasher.hashPassword(initialPassword)
            )
        )

        return CreatedAdminAccount(
            username = ADMIN_USERNAME,
            passwordFile = fileService.getAbsolutePath(INITIAL_PASSWORD_PATH)
        )
    }

    /**
     * 校验用户名和密码是否匹配 admin.json 里的账户。
     * 账户文件存在时，先执行 Argon2 校验，再比较用户名，避免因用户名不匹配而跳过哈希校验。
     */
    fun authenticate(username: String, password: String): Boolean {
        val admin = fileService.loadFromJson<User>(ADMIN_JSON_PATH) ?: return false
        val passwordMatches = passwordHasher.matchesHash(password, admin.password)
        return passwordMatches && username == admin.username
    }

    /**
     * 生成随机初始密码
     * 使用 Base64 URL 安全编码，去掉 padding，长度约为 32 个字符。
     *
     * @return 随机生成的初始密码
     */
    private fun generateInitialPassword(): String {
        val randomBytes = ByteArray(INITIAL_PASSWORD_BYTES)
        secureRandom.nextBytes(randomBytes)
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes)
    }
}
