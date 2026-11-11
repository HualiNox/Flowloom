package com.mewrix.flowloom.engine.common.result;

import java.util.List;

import com.mewrix.flowloom.engine.common.response.BusinessCode;

/**
 * BusinessResult类用于封装业务处理结果，包括业务码、消息参数和数据。
 *
 * <p>
 * 该类提供了静态方法用于创建成功或失败的业务结果实例。
 * </p>
 */
public record BusinessResult<T>(
        BusinessCode code,
        List<Object> messageArgs,
        T data) {

    /**
     * 判断业务处理是否成功
     */
    public boolean isSuccess() {
        return code == BusinessCode.OK;
    }

    // === 工厂方法 ===

    /** 创建包含数据的成功结果。 */
    public static <T> BusinessResult<T> success(T data) {
        return new BusinessResult<>(BusinessCode.OK, null, data);
    }

    /** 创建不含数据的成功结果。 */
    public static <T> BusinessResult<T> success() {
        return new BusinessResult<>(BusinessCode.OK, null, null);
    }

    /** 使用指定业务码创建不含消息参数的结果。 */
    public static <T> BusinessResult<T> fail(BusinessCode code) {
        return new BusinessResult<>(code, null, null);
    }

    /** 使用指定业务码和消息参数创建结果。 */
    public static <T> BusinessResult<T> fail(BusinessCode code, List<Object> messageArgs) {
        return new BusinessResult<>(code, messageArgs, null);
    }

    // === 工厂方法 End ===
}
