package com.mewrix.flowloom.engine.auth.handler;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import tools.jackson.databind.ObjectMapper;
import com.mewrix.flowloom.engine.auth.response.AuthBusinessCode;
import com.mewrix.flowloom.engine.common.response.ApiResponseFactory;
import com.mewrix.flowloom.engine.common.result.BusinessResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 未认证访问受保护资源时，返回与业务接口一致的 {@link ApiResponse} JSON， 而不是 Spring Security 默认的空
 * body。
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ApiResponseFactory apiResponseFactory;
    private final ObjectMapper objectMapper;
    private final LocaleResolver localeResolver;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        LocaleContextSupport.writeJson(
                request,
                response,
                localeResolver,
                objectMapper,
                apiResponseFactory.from(
                        BusinessResult.fail(AuthBusinessCode.AUTH_INVALID_OR_EXPIRED_CREDENTIALS)),
                HttpStatus.UNAUTHORIZED);
    }
}
