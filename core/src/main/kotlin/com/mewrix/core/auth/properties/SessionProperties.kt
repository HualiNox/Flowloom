package com.mewrix.core.auth.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "flowloom.session")
data class SessionProperties(
    /** 空闲超时：超过这么久没有请求，会话失效 */
    val timeout: Duration,
    /** 滑动续期的最小间隔：距离上次续期不足这个时间的请求不会更新过期时间，减少写入 */
    val minRefreshInterval: Duration,
    /** 保存会话 ID 的 Cookie 名称 */
    val cookieName: String = "FLOWLOOM_SESSION",
    /** Cookie 是否带 Secure 属性：通过 HTTPS 访问时设为 true */
    val secureCookie: Boolean = false
) {
    init {
        require(!timeout.isNegative && !timeout.isZero) { "flowloom.session.timeout must be positive" }
        require(!minRefreshInterval.isNegative) { "flowloom.session.min-refresh-interval must not be negative" }
        require(minRefreshInterval < timeout) {
            "flowloom.session.min-refresh-interval must be shorter than flowloom.session.timeout"
        }
    }
}
