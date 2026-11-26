package com.mewrix.flowloom.core.auth.dto.request

class LoginRequest(
    val username: String,
    val password: String
) {
    // 避免密码被意外打进日志
    override fun toString(): String = "LoginRequest(username=$username, password=***)"
}