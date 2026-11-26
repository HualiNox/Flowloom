package com.mewrix.core.common.service

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.stereotype.Service

/**
 * 使用 Argon2id 对密码做单向哈希，用于保存到 admin.json。
 *
 * 哈希结果是一个自带参数和盐的字符串，例如：
 * $argon2id$v=19$m=19456,t=2,p=1$<盐>$<哈希>
 * 校验时不需要另外保存盐和参数。
 */
@Service
class PasswordHasher {
    companion object {
        // 以下参数是 OWASP 对 Argon2id 的最低推荐值
        private const val SALT_BYTES = 16
        private const val HASH_BYTES = 32
        private const val PARALLELISM = 1
        private const val MEMORY_KIB = 19_456 // 19 MiB
        private const val ITERATIONS = 2
    }

    private val argon2Encoder = Argon2PasswordEncoder(
        SALT_BYTES,
        HASH_BYTES,
        PARALLELISM,
        MEMORY_KIB,
        ITERATIONS
    )

    /** 对明文密码做哈希，每次调用都会使用新的随机盐，所以同一个密码每次结果都不同 */
    fun hashPassword(plainPassword: String): String =
        checkNotNull(argon2Encoder.encode(plainPassword)) { "Argon2id encoder returned null" }

    /** 判断明文密码是否和保存的哈希匹配 */
    fun matchesHash(plainPassword: String, storedHash: String): Boolean =
        argon2Encoder.matches(plainPassword, storedHash)
}
