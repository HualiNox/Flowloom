package com.mewrix.flowloom.engine.auth.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.auth.service.SessionService;
import com.mewrix.flowloom.engine.common.util.SessionUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 从 {@code Authorization: Bearer <token>} 解析自建会话，并将 {@link UserPrincipal} 写入 SecurityContext。
 * 无凭证或凭证无效时不设置认证信息，交由后续授权规则处理。
 */
@Component
@RequiredArgsConstructor
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            SessionUtil.extractBearerToken(request)
                    .flatMap(sessionService::findValidPrincipal)
                    .ifPresent(principal -> SecurityContextHolder.getContext()
                            .setAuthentication(authenticated(principal)));
        }

        filterChain.doFilter(request, response);
    }

    private static Authentication authenticated(UserPrincipal principal) {
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }
}
