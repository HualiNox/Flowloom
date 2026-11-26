package com.mewrix.core.auth.dto

import java.time.Duration
import java.time.Instant

/**
 * 一条登录会话（不可变，更新时用 copy 生成新对象）。
 * 会话 ID 本身不保存在这里，内存表里的 key 是它的 SHA-256。
 */
data class Session(
    val createdAt: Instant,
    val lastAccessedAt: Instant,
    val expiredAt: Instant,
) {
    fun isExpired(now: Instant = Instant.now()): Boolean = now.isAfter(expiredAt)

    /** 距离上次续期是否已满 [minRefreshInterval]，用于节流滑动续期 */
    fun shouldRefresh(minRefreshInterval: Duration, now: Instant = Instant.now()): Boolean =
        Duration.between(lastAccessedAt, now) >= minRefreshInterval

    /** 记录一次访问，并把过期时间顺延到「现在 + timeout」 */
    fun refreshed(timeout: Duration, now: Instant = Instant.now()): Session =
        copy(lastAccessedAt = now, expiredAt = now.plus(timeout))

    companion object {
        fun create(timeout: Duration, now: Instant = Instant.now()): Session =
            Session(
                createdAt = now,
                lastAccessedAt = now,
                expiredAt = now.plus(timeout)
            )
    }
}
