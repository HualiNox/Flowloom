package com.mewrix.flowloom.engine.auth.dto.response;

/**
 * 登录成功响应，仅返回明文会话凭证。
 *
 * @param token 会话凭证，后续请求以 Bearer Token 携带
 */
public record AuthLoginResponse(
        String token) {
}
