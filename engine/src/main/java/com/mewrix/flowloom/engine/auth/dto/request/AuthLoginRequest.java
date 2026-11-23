package com.mewrix.flowloom.engine.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求。
 *
 * @param usernameOrEmail 用户名或邮箱
 * @param password        明文密码
 */
public record AuthLoginRequest(
        @NotBlank
        String usernameOrEmail,

        @NotBlank
        String password) {
}
