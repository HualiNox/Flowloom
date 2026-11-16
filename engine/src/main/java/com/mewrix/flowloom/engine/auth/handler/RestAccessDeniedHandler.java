package com.mewrix.flowloom.engine.auth.handler;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;

import tools.jackson.databind.ObjectMapper;
import com.mewrix.flowloom.engine.common.response.ApiResponseFactory;
import com.mewrix.flowloom.engine.common.response.BusinessCode;
import com.mewrix.flowloom.engine.common.result.BusinessResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 已认证但无权限时，返回与业务接口一致的 {@link com.mewrix.flowloom.engine.common.response.ApiResponse}。
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {
    private final ApiResponseFactory apiResponseFactory;
    private final ObjectMapper objectMapper;
    private final LocaleResolver localeResolver;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        LocaleContextSupport.writeJson(
                request,
                response,
                localeResolver,
                objectMapper,
                apiResponseFactory.from(
                        BusinessResult.fail(BusinessCode.AUTH_ACCESS_DENIED)),
                HttpStatus.FORBIDDEN);
    }
}
