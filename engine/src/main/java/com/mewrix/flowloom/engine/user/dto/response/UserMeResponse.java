package com.mewrix.flowloom.engine.user.dto.response;

import java.util.UUID;

/**
 * 当前登录用户信息。
 *
 * @param userId   用户 ID
 * @param username 用户名
 * @param email    用户邮箱
 */
public record UserMeResponse(
        UUID userId,
        String username,
        String email) {
}
