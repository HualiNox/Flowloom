package com.mewrix.flowloom.engine.auth.handler;

import java.io.IOException;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.LocaleResolver;

import tools.jackson.databind.ObjectMapper;
import com.mewrix.flowloom.engine.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Security 过滤器阶段写 JSON 响应时的 locale / 序列化辅助。 */
final class LocaleContextSupport {
    private LocaleContextSupport() {
    }

    static void writeJson(
            HttpServletRequest request,
            HttpServletResponse response,
            LocaleResolver localeResolver,
            ObjectMapper objectMapper,
            ApiResponse<?> body,
            HttpStatus status) throws IOException {
        LocaleContextHolder.setLocale(localeResolver.resolveLocale(request));
        try {
            response.setStatus(status.value());
            response.setCharacterEncoding("UTF-8");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), body);
        } finally {
            LocaleContextHolder.resetLocaleContext();
        }
    }
}
