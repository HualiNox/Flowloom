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
 * {@code flowloom.mail.*}
 *
 * @param enable       是否投递业务邮件
 * @param verification 验证码相关参数
 */
@Validated
@ConfigurationProperties(prefix = "flowloom.mail")
public record MailProperties(
        boolean enable,
        @NotNull @Valid Verification verification) {

    /**
     * @param codeTtl        验证码有效期
     * @param resendInterval 同一用户再次发信的最小间隔
     * @param maxAttempts    同一验证码错误次数上限
     */
    public record Verification(
            @NotNull @DurationMin(nanos = 1) Duration codeTtl,
            @NotNull @DurationMin(nanos = 1) Duration resendInterval,
            @Min(1) @Max(20) int maxAttempts) {
    }
}
