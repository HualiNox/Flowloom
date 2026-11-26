package com.mewrix.core.auth

import com.mewrix.core.auth.dto.LoginRequest
import com.mewrix.core.common.http.ApiResponse
import com.mewrix.core.common.http.ApiResponseFactory
import com.mewrix.core.user.UserService
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
}
