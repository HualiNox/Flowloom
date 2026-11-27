package com.mewrix.flowloom.core.auth

import com.mewrix.flowloom.core.auth.dto.request.LoginRequest
import com.mewrix.flowloom.core.auth.dto.request.ResetAdminAccountRequest
import com.mewrix.flowloom.core.common.http.ApiResponse
import com.mewrix.flowloom.core.common.http.ApiResponseFactory
import com.mewrix.flowloom.core.common.http.CommonErrorCode
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
    private val authService: AuthService,
    private val sessionService: SessionService,
    private val sessionCookieManager: SessionCookieManager,
    private val apiResponseFactory: ApiResponseFactory
) {

    @PostMapping("/login")
    fun login(
        @RequestBody body: LoginRequest,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Nothing>> {
        val sessionId = authService.login(body.username, body.password, sessionCookieManager.read(request))
            ?: return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(apiResponseFactory.error(AuthErrorCode.INVALID_CREDENTIALS))

        sessionCookieManager.write(response, sessionId)
        return ResponseEntity.ok(apiResponseFactory.success(null))
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest, response: HttpServletResponse): ApiResponse<Nothing> {
        sessionCookieManager.read(request)?.let(sessionService::removeSession)
        sessionCookieManager.clear(response)
        return apiResponseFactory.success(null)
    }

    /**
     * 更新已登录管理员的凭据。成功后撤销该管理员在所有客户端的会话，
     * 并清除当前客户端的会话 Cookie，使其使用新凭据重新登录。
     */
    @PostMapping("/account/reset")
    fun resetAdminAccount(
        @RequestBody body: ResetAdminAccountRequest,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Unit>> {
        val result = authService.resetAdminAccount(body)
        if (result.errorCode != CommonErrorCode.OK) {
            return ResponseEntity.badRequest().body(apiResponseFactory.fromResult(result))
        }

        sessionCookieManager.clear(response)
        return ResponseEntity.ok(apiResponseFactory.fromResult(result))
    }
}
