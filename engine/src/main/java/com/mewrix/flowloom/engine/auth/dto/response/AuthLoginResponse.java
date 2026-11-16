package com.mewrix.flowloom.engine.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录成功响应，仅返回明文会话凭证。
 *
 * @param token 会话凭证，后续请求以 Bearer Token 携带
 */
@Schema(description = "登录成功数据")
public record AuthLoginResponse(
        @Schema(description = "会话凭证，后续请求以 Authorization: Bearer <token> 携带")
        String token) {
}
