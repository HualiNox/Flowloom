package com.mewrix.flowloom.core.auth

import com.mewrix.flowloom.core.auth.dto.Session
import com.mewrix.flowloom.core.auth.properties.SessionProperties
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 内存会话管理服务（单用户，会话只表示“已登录”）。进程重启后所有会话失效，需要重新登录。
 *
 * - 会话 ID：32 字节 SecureRandom，Base64URL 编码，只在 Cookie 里出现
 * - 内存表 key：会话 ID 的 SHA-256，服务端不保存明文会话 ID
 * - 过期：读取时惰性判断 + 定时清理；滑动续期按 minRefreshInterval 节流
 */
@Service
class SessionService(
    private val sessionProperties: SessionProperties
) {
    private val log = LoggerFactory.getLogger(SessionService::class.java)

    private val sessionStore = ConcurrentHashMap<String, Session>()
    private val secureRandom = SecureRandom()

    companion object {
        private const val SESSION_ID_BYTES = 32

        // Base64.Encoder 无状态、线程安全，可共用
        private val base64 = Base64.getUrlEncoder().withoutPadding()
    }

    /** 创建新会话，返回明文会话 ID（只在此处出现一次，用于写入 Cookie） */
    fun createSession(): String {
        val sessionId = generateSessionId()
        sessionStore[hash(sessionId)] = Session.create(sessionProperties.timeout)
        return sessionId
    }

    /**
     * 校验会话 ID 并按需续期，返回会话是否有效。
     * 不存在或已过期返回 false（过期的顺手删除）。
     * “判断过期 + 续期”在 computeIfPresent 里原子完成，并发请求不会互相覆盖。
     */
    fun validateSession(sessionId: String): Boolean {
        val now = Instant.now()
        return sessionStore.computeIfPresent(hash(sessionId)) { _, session ->
            when {
                session.isExpired(now) -> null
                session.shouldRefresh(sessionProperties.minRefreshInterval, now) ->
                    session.refreshed(sessionProperties.timeout, now)

                else -> session
            }
        } != null
    }

    fun removeSession(sessionId: String) {
        sessionStore.remove(hash(sessionId))
    }

    /** 让所有会话失效（例如修改密码后） */
    fun removeAllSessions() {
        sessionStore.clear()
    }

    @Scheduled(fixedDelay = 10, initialDelay = 10, timeUnit = TimeUnit.MINUTES)
    fun cleanUpExpiredSessions() {
        val now = Instant.now()
        val before = sessionStore.size
        sessionStore.values.removeIf { it.isExpired(now) }
        val removed = before - sessionStore.size
        if (removed > 0) {
            log.debug("Cleaned up {} expired session(s)", removed)
        }
    }

    private fun generateSessionId(): String =
        base64.encodeToString(ByteArray(SESSION_ID_BYTES).also(secureRandom::nextBytes))

    private fun hash(sessionId: String): String =
        base64.encodeToString(
            MessageDigest.getInstance("SHA-256").digest(sessionId.toByteArray(Charsets.UTF_8))
        )
}
