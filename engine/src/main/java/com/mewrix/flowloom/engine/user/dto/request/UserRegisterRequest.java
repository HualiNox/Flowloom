package com.mewrix.flowloom.engine.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求，字段约束用于 HTTP 请求参数校验。
 *
 * @param username    用户名
 * @param email       邮箱地址
 * @param password    明文密码
 * @param displayName 用户展示名
 */
@Schema(description = "用户注册请求")
public record UserRegisterRequest(
        @Schema(description = "用户名，4–32 位，字母开头", example = "alice")
        @NotBlank
        @Size(min = 4, max = 32)
        String username,

        @Schema(description = "邮箱地址", example = "alice@example.com")
        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @Schema(description = "明文密码，8–128 位", example = "correct-password", format = "password")
        @NotBlank
        @Size(min = 8, max = 128)
        String password,

        @Schema(description = "展示名", example = "Alice")
        @NotBlank
        @Size(max = 64)
        String displayName) {
}
