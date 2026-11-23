package com.mewrix.flowloom.engine.common.response;

/**
 * API 统一响应。
 *
 * @param code    业务码
 * @param message 响应消息
 * @param data    响应数据，可以为空
 * @param <T>     数据类型
 */
public record ApiResponse<T>(
        String code,
        String message,
        T data) {
}
