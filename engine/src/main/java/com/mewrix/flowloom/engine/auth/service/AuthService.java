package com.mewrix.flowloom.engine.auth.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.mewrix.flowloom.engine.auth.dto.response.AuthLoginResponse;
import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.common.response.BusinessCode;
import com.mewrix.flowloom.engine.common.result.BusinessResult;

import lombok.RequiredArgsConstructor;

/** 处理登录：校验用户名密码并签发自建会话凭证。 */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final SessionService sessionService;

    /**
     * 校验凭据并为用户创建会话。
     *
     * @param usernameOrEmail 用户名
     * @param password        明文密码
     * @return 成功时包含会话 token；凭据错误时返回失败业务码
     */
    public BusinessResult<AuthLoginResponse> login(String usernameOrEmail, String password) {
        final Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(usernameOrEmail, password));
        } catch (BadCredentialsException ex) {
            return BusinessResult.fail(BusinessCode.AUTH_INVALID_CREDENTIALS);
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = sessionService.createSession(principal.userId())
                .orElseThrow(() -> new IllegalStateException(
                        "Failed to create session for user: " + principal.userId()));

        return BusinessResult.success(new AuthLoginResponse(token));
    }

    /** 吊销当前会话凭证；对无效或已吊销 token 保持幂等成功。 */
    public void logout(String token) {
        sessionService.revokeSession(token);
    }
}
