package com.mewrix.flowloom.engine.user.dto.request;

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
public record UserRegisterRequest(
        @NotBlank
        @Size(min = 4, max = 32)
        String username,

        @NotBlank
        @Email
        @Size(max = 254)
        String email,

        @NotBlank
        @Size(min = 8, max = 128)
        String password,

        @NotBlank
        @Size(max = 64)
        String displayName) {
}
