package com.mewrix.flowloom.engine.common.response;

/**
 * API 统一响应。
 *
 * @param code 业务码
 * @param message 响应消息
 * @param data 响应数据，可以为空
 * @param <T> 数据类型
 */
public record ApiResponse<T>(
        String code,
        String message,
        T data) {
    /** 创建带数据及默认英文消息的成功响应。 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                BusinessCode.OK.getCode(),
                "success",
                data);
    }

    /** 创建不含数据、使用默认英文消息的成功响应。 */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(
                BusinessCode.OK.getCode(),
                "success",
                null);
    }

    /** 使用指定的业务码和消息创建不含数据的响应。 */
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(
                code,
                message,
                null);
    }

    /** 使用指定的业务码、消息和数据创建响应。 */
    public static <T> ApiResponse<T> failWithData(
            String code,
            String message,
            T data) {
        return new ApiResponse<>(
                code,
                message,
                data);
    }
}
