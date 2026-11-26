package com.mewrix.core.user

data class User(
    val username: String,
    /** 持久化时保存包含盐和参数的密码哈希。 */
    val password: String
)
