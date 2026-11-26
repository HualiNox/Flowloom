package com.mewrix.core.auth

import com.mewrix.core.common.http.ErrorCode

enum class AuthErrorCode(
    private val messageKey: String?
) : ErrorCode {
    /** 没有有效会话（未登录、会话过期或已登出） */
    UNAUTHORIZED("auth.unauthorized"),

    /** 登录时用户名或密码不正确 */
    INVALID_CREDENTIALS("auth.invalid-credentials");

    override fun getCode(): String = this.name

    override fun getMessageKey(): String? = messageKey
}
