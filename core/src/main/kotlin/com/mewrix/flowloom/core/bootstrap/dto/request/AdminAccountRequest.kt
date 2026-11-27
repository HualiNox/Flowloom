package com.mewrix.flowloom.core.bootstrap.dto.request

/** 提交当前密码和新的管理员凭据，以完成账户配置。 */
class AdminAccountRequest(
    val username: String,
    val oldPassword: String,
    val newPassword: String,
    val confirmPassword: String
) {
    override fun toString(): String =
        "AdminAccountRequest(username=$username, oldPassword=***, newPassword=***, confirmPassword=***)"
}
