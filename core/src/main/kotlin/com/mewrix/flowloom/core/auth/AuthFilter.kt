package com.mewrix.flowloom.core.auth

import com.mewrix.flowloom.core.common.http.ApiResponseFactory
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper

/**
 * 登录校验过滤器：除登录接口外，所有请求都必须带有效会话 Cookie，否则返回 401。
 */
@Component
class AuthFilter(
    private val sessionService: SessionService,
    private val sessionCookieManager: SessionCookieManager,
    private val apiResponseFactory: ApiResponseFactory,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    companion object {
        /** 不需要登录的接口，格式为“方法 路径”，精确匹配 */
        private val PUBLIC_ENDPOINTS = setOf(
            "${HttpMethod.POST.name()} /api/v1/auth/login"
        )
    }

    /** 白名单精确匹配（方法 + 路径），匹配不上一律走校验，宁严勿松 */
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        return "${request.method} $path" in PUBLIC_ENDPOINTS
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authenticated = sessionCookieManager.read(request)
            ?.let(sessionService::validateSession)
            ?: false

        if (!authenticated) {
            writeUnauthorized(response)
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun writeUnauthorized(response: HttpServletResponse) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.writer,
            apiResponseFactory.error<Any>(AuthErrorCode.UNAUTHORIZED)
        )
    }
}
