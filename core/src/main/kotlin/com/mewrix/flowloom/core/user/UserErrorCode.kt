package com.mewrix.flowloom.core.user

import com.mewrix.flowloom.core.common.http.ErrorCode

enum class UserErrorCode(
    private val messageKey: String?
) : ErrorCode {
    /** 用户名长度不符合要求 */
    INVALID_USERNAME_LENGTH("user.invalid-username-length"),
    /** 用户名包含非法字符 */
    INVALID_USERNAME_CHARACTERS("user.invalid-username-characters"),
    /** 密码长度不符合要求 */
    INVALID_PASSWORD_LENGTH("user.invalid-password-length"),
    /** 新密码与确认密码不匹配 */
    PASSWORDS_DO_NOT_MATCH("auth.passwords-do-not-match"),
    /** 当前密码校验失败 */
    INVALID_OLD_PASSWORD("user.invalid-old-password"),
    /** 新密码与当前密码相同 */
    PASSWORD_UNCHANGED("user.password-unchanged");

    override fun getCode(): String = this.name
    override fun getMessageKey(): String? = this.messageKey
}
