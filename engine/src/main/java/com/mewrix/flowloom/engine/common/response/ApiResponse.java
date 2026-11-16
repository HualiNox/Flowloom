package com.mewrix.flowloom.engine.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API 统一响应。
 *
 * @param code    业务码
 * @param message 响应消息
 * @param data    响应数据，可以为空
 * @param <T>     数据类型
 */
@Schema(description = "统一 API 响应")
public record ApiResponse<T>(
        @Schema(description = "业务码，成功时为 OK", example = "OK")
        String code,

        @Schema(description = "本地化消息", example = "success")
        String message,

        @Schema(description = "业务数据，可为 null")
        T data) {
}
