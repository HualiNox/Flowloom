package com.mewrix.flowloom.core.user

data class User(
    val username: String,
    /** 持久化时保存包含盐和参数的密码哈希。 */
    val password: String,
    /** 账户文件缺少重置标记时，管理员配置仍待完成。 */
    val credentialsReset: Boolean = false
)
