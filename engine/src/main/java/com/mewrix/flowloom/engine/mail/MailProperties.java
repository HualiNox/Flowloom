package com.mewrix.flowloom.engine.mail;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 邮件相关配置，对应 {@code flowloom.mail.*}。
 *
 * @param enable       是否启用业务邮件发送
 * @param verification 邮箱验证码相关配置
 */
@Validated
@ConfigurationProperties(prefix = "flowloom.mail")
public record MailProperties(
        boolean enable,
        @NotNull @Valid Verification verification) {

    /**
     * 邮箱验证码配置。
     *
     * @param codeTtl         验证码有效期
     * @param resendInterval  同一用户两次发信的最小间隔（按 userId 限流）
     * @param maxAttempts     同一验证码最大错误尝试次数
     */
    public record Verification(
            @NotNull @DurationMin(nanos = 1) Duration codeTtl,
            @NotNull @DurationMin(nanos = 1) Duration resendInterval,
            @Min(1) @Max(20) int maxAttempts) {
    }
}
