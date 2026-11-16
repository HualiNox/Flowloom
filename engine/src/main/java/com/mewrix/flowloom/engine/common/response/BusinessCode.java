package com.mewrix.flowloom.engine.common.response;

/**
 * 业务码契约：各业务模块提供自己的枚举实现。
 * 响应中的 {@code code} 取 {@link #getCode()}，消息走 {@link #getMessageKey()} 国际化。
 */
public interface BusinessCode {

    /** 业务码字符串，通常为枚举常量名。 */
    String getCode();

    /** i18n 消息键。 */
    String getMessageKey();
}
