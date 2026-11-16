package com.mewrix.flowloom.engine.auth.properties;

import java.time.Duration;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * 会话相关配置，对应 {@code flowloom.session.*}。
 *
 * @param timeout            会话有效期；创建与每次滑动续期后，过期时间为 now + timeout
 * @param minRefreshInterval 滑动续期的最小间隔；未满该间隔不写库
 */
@Validated
@ConfigurationProperties(prefix = "flowloom.session")
public record SessionProperties(
        @NotNull
        @DurationMin(nanos = 1)
        Duration timeout,

        @NotNull
        @DurationMin(nanos = 1)
        Duration minRefreshInterval) {
    @AssertTrue(message = "min-refresh-interval must be less than timeout")
    public boolean isRefreshIntervalValid() {
        return minRefreshInterval.compareTo(timeout) < 0;
    }
}
