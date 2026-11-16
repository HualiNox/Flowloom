package com.mewrix.flowloom.engine.common.util;

import java.util.Optional;

import org.springframework.http.HttpHeaders;

import jakarta.servlet.http.HttpServletRequest;

public class SessionUtil {
    private SessionUtil() {
        // 私有构造函数，防止实例化
    }

    private static final String BEARER_PREFIX = "Bearer ";

    public static Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(token);
    }

}
