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
    AUTH_ACCESS_DENIED("auth.access_denied");

    private final String messageKey;

    @Override
    public String getCode() {
        return this.name();
    }
}
