package com.mewrix.flowloom.core.auth

import com.mewrix.flowloom.core.auth.dto.request.LoginRequest
import com.mewrix.flowloom.core.auth.dto.request.ResetAdminAccountRequest
import com.mewrix.flowloom.core.bootstrap.BootstrapService
import com.mewrix.flowloom.core.common.http.ApiResponse
import com.mewrix.flowloom.core.common.http.ApiResponseFactory
import com.mewrix.flowloom.core.common.http.CommonErrorCode
import com.mewrix.flowloom.core.user.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val userService: UserService,
    private val bootstrapService: BootstrapService,
    private val sessionService: SessionService,
    private val sessionCookieManager: SessionCookieManager,
    private val apiResponseFactory: ApiResponseFactory
) {

    // 与账户重置串行，避免旧密码刚通过校验就在会话作废后签发新会话。
    @Synchronized
    @PostMapping("/login")
    fun login(
        @RequestBody body: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing>> {
        if (!userService.authenticate(body.username, body.password)) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(apiResponseFactory.error(AuthErrorCode.INVALID_CREDENTIALS))
        }

        // 如果请求里带着旧会话，先作废，登录后一定使用全新的会话 ID（防会话固定）
        removeCurrentSession(request)

        sessionCookieManager.write(response, sessionService.createSession())
        return ResponseEntity.ok(apiResponseFactory.success(null))
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ApiResponse<Nothing> {
        removeCurrentSession(request)
        sessionCookieManager.clear(response)
        return apiResponseFactory.success(null)
    }

    private fun removeCurrentSession(request: HttpServletRequest) {
        sessionCookieManager.read(request)?.let(sessionService::removeSession)
    }

    /**
     * 更新已登录管理员的凭据。成功后撤销该管理员在所有客户端的会话，
     * 并清除当前客户端的会话 Cookie，使其使用新凭据重新登录。
     */
    @Synchronized
    @PostMapping("/account/reset")
    fun resetAdminAccount(
        @RequestBody body: ResetAdminAccountRequest,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Unit>> {
        val result = userService.resetAdminAccount(body)
        if (result.errorCode != CommonErrorCode.OK) {
            return ResponseEntity.badRequest().body(apiResponseFactory.fromResult(result))
        }

        sessionService.removeAllSessions()
        sessionCookieManager.clear(response)
        bootstrapService.refreshInstanceState()
        return ResponseEntity.ok(apiResponseFactory.fromResult(result))
    }
}
