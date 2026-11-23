package com.mewrix.flowloom.engine.agent.dto.response;

import java.time.LocalDateTime;

/**
 * 创建 Agent 注册后的响应。
 *
 * @param registrationToken Base64URL（无 padding）编码的 {@code registrationId || psk}，明文仅返回一次
 * @param expiresAt         该挂起注册的过期时间（Asia/Shanghai）
 */
public record AgentRegistrationResponse(
    String registrationToken,
    LocalDateTime expiresAt
) {}
