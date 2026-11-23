package com.mewrix.flowloom.engine.auth.filter;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import tools.jackson.databind.ObjectMapper;

import com.mewrix.flowloom.engine.auth.handler.LocaleContextSupport;
import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.common.response.ApiResponseFactory;
import com.mewrix.flowloom.engine.common.result.BusinessResult;
import com.mewrix.flowloom.engine.user.response.UserBusinessCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 限制邮箱未验证用户的业务访问范围。
 *
 * <p>
 * 认证与授权分离：登录接口仍返回 {@code OK} + token；本过滤器在
 * {@link SessionAuthenticationFilter} 之后运行，对已认证但未验证邮箱的请求做业务范围限制。
 * 未验证用户仅可访问白名单；其余接口直接写
 * {@link UserBusinessCode#EMAIL_NOT_VERIFIED}（HTTP 403），由前端跳转验证页。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class EmailVerificationFilter extends OncePerRequestFilter {
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * 未验证邮箱仍可访问的路径。
     *
     * <ul>
     * <li>验证发信 / 校验：未验证用户的业务入口</li>
     * <li>注销：允许结束受限会话</li>
     * <li>登录 / 注册：公开接口，避免请求误带 Bearer 时被本过滤器误拦</li>
     * </ul>
     */
    private static final List<String> UNVERIFIED_ALLOWED_PATTERNS = List.of(
            "/api/v1/user/email/verify/send",
            "/api/v1/user/email/verify",
            "/api/v1/auth/logout",
            "/api/v1/auth/login",
            "/api/v1/user/register");

    /** 挑战交换依赖临时注册凭证，请求附带未验证的用户会话时也允许进入接口。 */
    private static final List<String> AGENT_ALLOWED_LIST = List.of(
            "/api/v1/agent/register");

    private final ApiResponseFactory apiResponseFactory;
    private final ObjectMapper objectMapper;
    private final LocaleResolver localeResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 未建立会话认证时不处理，交由后续 authorizeHttpRequests / EntryPoint
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 已验证，或命中白名单：放行
        if (principal.emailVerified() || isAllowedForUnverified(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 已认证但未验证且非白名单：短路返回，不进入 Controller
        LocaleContextSupport.writeJson(
                request,
                response,
                localeResolver,
                objectMapper,
                apiResponseFactory.from(BusinessResult.fail(UserBusinessCode.EMAIL_NOT_VERIFIED)),
                HttpStatus.FORBIDDEN);
    }

    /** 判断请求路径是否对未验证用户开放。 */
    private static boolean isAllowedForUnverified(HttpServletRequest request) {
        String requestUri = request.getRequestURI();

        for (String pattern : AGENT_ALLOWED_LIST) {
            if (PATH_MATCHER.match(pattern, requestUri)) {
                return true;
            }
        }

        for (String pattern : UNVERIFIED_ALLOWED_PATTERNS) {
            if (PATH_MATCHER.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }
}
