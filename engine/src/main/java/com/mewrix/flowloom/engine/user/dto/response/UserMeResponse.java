package com.mewrix.flowloom.engine.user.dto.response;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前登录用户信息。
 *
 * @param userId   用户 ID
 * @param username 用户名
 * @param email    用户邮箱
 */
@Schema(description = "当前登录用户信息")
public record UserMeResponse(
        @Schema(description = "用户 ID")
        UUID userId,

        @Schema(description = "用户名", example = "alice")
        String username,

        @Schema(description = "邮箱", example = "alice@example.com")
        String email) {
}
