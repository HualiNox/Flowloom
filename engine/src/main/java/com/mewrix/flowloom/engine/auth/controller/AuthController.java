package com.mewrix.flowloom.engine.auth.controller;

import java.util.Optional;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mewrix.flowloom.engine.auth.dto.request.AuthLoginRequest;
import com.mewrix.flowloom.engine.auth.dto.response.AuthLoginResponse;
import com.mewrix.flowloom.engine.auth.response.AuthBusinessCode;
import com.mewrix.flowloom.engine.auth.service.AuthService;
import com.mewrix.flowloom.engine.common.response.ApiResponse;
import com.mewrix.flowloom.engine.common.response.ApiResponseFactory;
import com.mewrix.flowloom.engine.common.result.BusinessResult;
import com.mewrix.flowloom.engine.common.util.SessionUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 提供认证相关的 HTTP 接口。 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final ApiResponseFactory apiResponseFactory;

    /**
     * 校验用户名密码并返回会话凭证。
     *
     * @param request 登录请求
     * @return 包含业务码与会话 token 的响应
     */
    @PostMapping("/login")
    public ApiResponse<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        BusinessResult<AuthLoginResponse> result = authService.login(
                request.usernameOrEmail(),
                request.password());

        return apiResponseFactory.from(result);
    }

    /**
     * 吊销当前 Bearer 会话；需已认证。
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        Optional<String> token = SessionUtil.extractBearerToken(request);
        if (token.isEmpty()) {
            return apiResponseFactory.from(BusinessResult.fail(
                    AuthBusinessCode.AUTH_INVALID_OR_EXPIRED_CREDENTIALS));
        }

        authService.logout(token.get());

        return apiResponseFactory.from(BusinessResult.success());
    }

}
