package com.mewrix.flowloom.engine.agent.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 创建 Agent 注册后的响应。
 *
 * @param registrationToken Base64URL（无 padding）编码的 {@code registrationId || psk}，明文仅返回一次
 * @param expiresAt         该挂起注册的过期时间（Asia/Shanghai）
 */
@Schema(description = "Agent 注册凭证")
public record AgentRegistrationResponse(
    @Schema(
        description = "Base64URL 编码的注册凭证（registrationId + PSK）",
        example = "YWJjZGVmZ2g_MTIzNDU2Nzg5MGFiY2RlZg"
    )
    String registrationToken,

    @Schema(description = "注册凭证过期时间（Asia/Shanghai）", example = "2026-11-20T12:00:00") LocalDateTime expiresAt
) {}
