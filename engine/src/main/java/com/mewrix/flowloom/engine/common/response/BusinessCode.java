package com.mewrix.flowloom.engine.common.response;

import java.util.function.Supplier;

/**
 * 业务码契约：各业务模块提供自己的枚举实现。 响应中的 {@code code} 取 {@link #getCode()}，消息走
 * {@link #getMessageKey()} 国际化。
 */
public interface BusinessCode {

    /** 业务码字符串，通常为枚举常量名。 */
    String getCode();

    /** i18n 消息键。 */
    String getMessageKey();

    /** 表示处理成功的通用业务码。 */
    static BusinessCode ok() {
        return CommonBusinessCode.OK;
    }

    /** 是否为成功业务码。 */
    default boolean isOk() {
        return this == CommonBusinessCode.OK;
    }

    /**
     * 成功时继续执行下一步校验；失败时短路，直接返回当前业务码。
     *
     * @param next 下一步校验，仅在当前为 {@link #ok()} 时调用
     * @return 下一步结果，或当前失败业务码
     */
    default BusinessCode andThen(Supplier<? extends BusinessCode> next) {
        return isOk() ? next.get() : this;
    }
}
