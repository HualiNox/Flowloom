package com.mewrix.flowloom.engine.auth.principal;

import java.io.Serializable;
import java.util.UUID;

/**
 * 已认证用户身份，只携带后续业务需要的标识信息。密码哈希仅在认证校验时使用，不会进入本对象。
 *
 * @param userId        用户 ID
 * @param username      用户名
 * @param email         用户邮箱
 * @param emailVerified 邮箱是否已验证；由 {@code users.email_verified_at != null} 推导，
 *                      供 {@link com.mewrix.flowloom.engine.auth.filter.EmailVerificationFilter} 做访问范围限制
 */
public record UserPrincipal(
        UUID userId,
        String username,
        String email,
        boolean emailVerified) implements Serializable {
    private static final long serialVersionUID = 1L;
}
