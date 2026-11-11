package com.mewrix.flowloom.engine.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP 请求标识过滤器
 *
 * <p>
 * 在请求进入 Spring MVC 处理链之前生成唯一 RequestId，
 * 将其绑定到 MDC 日志上下文。
 * </p>
 *
 * <p>
 * 请求处理结束后清理 MDC 中的 RequestId，
 * 避免线程复用导致不同请求之间的上下文污染。
 * </p>
 */
@Component
public class RequestIdFilter extends OncePerRequestFilter {

    /**
     * 为当前请求建立 RequestId 上下文，并在请求处理结束后清理。
     *
     * @param request     当前 HTTP 请求
     * @param response    当前 HTTP 响应
     * @param filterChain Servlet 过滤器链
     * @throws ServletException 请求处理过程中发生 Servlet 异常
     * @throws IOException      请求处理过程中发生 I/O 异常
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase();

        try {
            MDC.put(MdcKeys.REQUEST_ID, requestId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MdcKeys.REQUEST_ID);
        }
    }
}
