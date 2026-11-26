package com.mewrix.flowloom.core.common

import com.mewrix.flowloom.core.common.http.CommonErrorCode
import com.mewrix.flowloom.core.common.http.ErrorCode

/**
 * 业务操作结果，由响应层将业务码和消息参数转换为本地化的 API 响应。
 *
 * @property args 按消息模板中的占位符顺序传入的参数。
 */
data class Result<T>(
    val errorCode: ErrorCode,
    val data: T? = null,
    val args: Array<out Any> = emptyArray(),
) {
    companion object {
        fun <T> success(data: T? = null): Result<T> {
            return Result(
                errorCode = CommonErrorCode.OK,
                data = data
            )
        }

        fun <T> error(errorCode: ErrorCode, vararg args: Any = emptyArray(), data: T? = null): Result<T> {
            return Result(
                errorCode = errorCode,
                data = data,
                args = args
            )
        }
    }
}
