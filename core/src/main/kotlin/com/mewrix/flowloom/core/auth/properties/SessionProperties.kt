package com.mewrix.flowloom.core.auth.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "flowloom.session")
data class SessionProperties(
    /** 会话有效期，从创建或最近一次实际续期时算起。 */
    val timeout: Duration,
    /** 距离创建或最近一次续期不足此间隔时，校验通过也沿用原过期时间。 */
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
