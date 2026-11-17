package com.mewrix.flowloom.engine.auth.response;

import com.mewrix.flowloom.engine.common.response.BusinessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 认证模块业务码。 */
@Getter
@RequiredArgsConstructor
public enum AuthBusinessCode implements BusinessCode {
    // 用户名或密码错误
    AUTH_INVALID_CREDENTIALS("auth.invalid_credentials"),

    // 认证凭证无效或已过期
    AUTH_INVALID_OR_EXPIRED_CREDENTIALS("auth.invalid_or_expired_credentials"),

    // 已认证但无权访问
    AUTH_ACCESS_DENIED("auth.access_denied"),

    // Redis 会话 key 缺失或已过期（Lua 返回 -2 / null）
    AUTH_GET_KEY_FAILED("auth.get_key_failed"),

    // Redis 会话 key 无 TTL 的异常状态（Lua 返回 -1，脚本侧已 DEL）
    AUTH_KEY_TTL_EXCEPTION("auth.key_ttl_exception");

    private final String messageKey;

    @Override
    public String getCode() {
        return this.name();
    }
}
