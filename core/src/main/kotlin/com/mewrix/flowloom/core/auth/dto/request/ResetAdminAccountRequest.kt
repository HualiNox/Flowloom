package com.mewrix.flowloom.core.auth.dto.request

/**
 * 管理员账户重置请求。
 *
 * @property username 更新后用于登录的用户名。
 * @property oldPassword 当前密码，用于确认本次操作。
 */
class ResetAdminAccountRequest(
    val username: String,
    val oldPassword: String,
    val newPassword: String,
    val confirmPassword: String
) {
    // 请求对象用于诊断输出时，隐藏当前密码、新密码和确认密码。
    override fun toString(): String =
        "ResetAdminAccountRequest(username=$username, oldPassword=***, newPassword=***, confirmPassword=***)"
}
