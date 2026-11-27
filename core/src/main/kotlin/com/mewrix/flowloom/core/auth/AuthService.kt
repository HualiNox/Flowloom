package com.mewrix.flowloom.core.auth

import com.mewrix.flowloom.core.auth.dto.request.ResetAdminAccountRequest
import com.mewrix.flowloom.core.bootstrap.BootstrapErrorCode
import com.mewrix.flowloom.core.bootstrap.BootstrapService
import com.mewrix.flowloom.core.common.Result
import com.mewrix.flowloom.core.common.http.CommonErrorCode
import com.mewrix.flowloom.core.user.UserService
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userService: UserService,
    private val sessionService: SessionService,
    private val bootstrapService: BootstrapService
) {
    // 登录与两种凭据更新共用同一把锁，避免旧密码通过校验后签发新会话。
    @Synchronized
    fun login(username: String, password: String, previousSessionId: String?): String? {
        if (!userService.authenticate(username, password)) {
            return null
        }
        previousSessionId?.let(sessionService::removeSession)
        return sessionService.createSession()
    }

    @Synchronized
    fun configureAdminAccount(request: ResetAdminAccountRequest): Result<Unit> {
        // 请求可能在初始化完成前已通过过滤器，进入更新时需重新检查。
        if (bootstrapService.isInitialized()) {
            return Result.error(BootstrapErrorCode.INSTANCE_ALREADY_INITIALIZED)
        }
        val result = updateAdminCredentials(request)
        if (result.errorCode == CommonErrorCode.OK) {
            bootstrapService.refreshInstanceState()
        }
        return result
    }

    @Synchronized
    fun resetAdminAccount(request: ResetAdminAccountRequest): Result<Unit> =
        updateAdminCredentials(request)

    private fun updateAdminCredentials(request: ResetAdminAccountRequest): Result<Unit> {
        val result = userService.resetAdminAccount(request)
        if (result.errorCode == CommonErrorCode.OK) {
            sessionService.removeAllSessions()
        }
        return result
    }
}
