package com.mewrix.flowloom.engine.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求。
 *
 * @param usernameOrEmail 用户名或邮箱
 * @param password        明文密码
 */
@Schema(description = "登录请求")
public record AuthLoginRequest(
        @Schema(description = "用户名或邮箱", example = "alice")
        @NotBlank
        String usernameOrEmail,

        @Schema(description = "明文密码", example = "correct-password", format = "password")
        @NotBlank
        String password) {
}
