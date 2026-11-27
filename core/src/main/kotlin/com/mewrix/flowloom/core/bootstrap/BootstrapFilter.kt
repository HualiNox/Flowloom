package com.mewrix.flowloom.core.bootstrap

import com.mewrix.flowloom.core.common.http.ApiResponseFactory
import com.mewrix.flowloom.core.common.http.ErrorCode
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
 * 根据实例的初始化状态限制接口访问。
 */
@Component
class BootstrapFilter(
    private val bootstrapService: BootstrapService,
    private val apiResponseFactory: ApiResponseFactory,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {
    companion object {
        /**
         * 与实例状态无关的接口。状态查询和退出仍要求管理员会话。
         */
        private val UNFILTERED_ENDPOINTS: Set<String> = setOf(
            "${HttpMethod.GET.name()} /api/v1/bootstrap/status",
            "${HttpMethod.POST.name()} /api/v1/auth/login",
            "${HttpMethod.POST.name()} /api/v1/auth/logout"
        )

        /** 仅在实例未初始化时开放的配置接口。 */
        private val BOOTSTRAP_ENDPOINTS = setOf(
            "${HttpMethod.POST.name()} /api/v1/bootstrap/admin/account"
        )
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI.removePrefix(request.contextPath)
        return "${request.method} $path" in UNFILTERED_ENDPOINTS
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI.removePrefix(request.contextPath)
        val endpoint = "${request.method} $path"
        if (bootstrapService.isInitialized()) {
            if (endpoint in BOOTSTRAP_ENDPOINTS) {
                writeForbidden(response, BootstrapErrorCode.INSTANCE_ALREADY_INITIALIZED)
                return
            }
        } else {
            if (endpoint !in BOOTSTRAP_ENDPOINTS) {
                writeForbidden(response, BootstrapErrorCode.INSTANCE_NOT_INITIALIZED)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    /** 在过滤器中直接写入带国际化错误消息的标准 API 响应。 */
    private fun writeForbidden(response: HttpServletResponse, errorCode: ErrorCode) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        objectMapper.writeValue(
            response.writer,
            apiResponseFactory.error<Unit>(errorCode)
        )
    }
}
