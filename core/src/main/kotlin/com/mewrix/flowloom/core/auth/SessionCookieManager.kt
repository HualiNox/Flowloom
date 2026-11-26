package com.mewrix.flowloom.core.auth

import com.mewrix.flowloom.core.auth.properties.SessionProperties
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 会话 Cookie 的读写。目前只支持 Cookie 传递会话 ID。
 *
 * 写入的会话 Cookie 不设置 Max-Age，由浏览器管理其生命周期。
 * 服务端会话的过期与续期由 [SessionService] 管理。
 */
@Component
class SessionCookieManager(
    private val sessionProperties: SessionProperties
) {
    fun read(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == sessionProperties.cookieName }
            ?.value
            ?.takeIf { it.isNotBlank() }

    fun write(response: HttpServletResponse, sessionId: String) {
        addCookie(response, newCookie(sessionId).build())
    }

    /** 返回一个立即过期的同名 Cookie，让浏览器删除它 */
    fun clear(response: HttpServletResponse) {
        addCookie(response, newCookie("").maxAge(Duration.ZERO).build())
    }

    private fun newCookie(value: String): ResponseCookie.ResponseCookieBuilder =
        ResponseCookie.from(sessionProperties.cookieName, value)
            .httpOnly(true)
            .secure(sessionProperties.secureCookie)
            .sameSite("Lax")
            .path("/")

    private fun addCookie(response: HttpServletResponse, cookie: ResponseCookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }
}
