package com.mewrix.flowloom.core.common.http

import com.mewrix.flowloom.core.common.Result
import com.mewrix.flowloom.core.common.service.I18nService
import org.springframework.stereotype.Component

@Component
class ApiResponseFactory(
    private val i18nService: I18nService
) {
    fun <T> success(data: T?): ApiResponse<T> {
        return ApiResponse(
            code = CommonErrorCode.OK.getCode(),
            message = i18nService.getMessage(CommonErrorCode.OK),
            data = data
        )
    }

    fun <T> error(errorCode: ErrorCode, vararg args: Any = emptyArray(), data: T? = null): ApiResponse<T> {
        return ApiResponse(
            code = errorCode.getCode(),
            message = i18nService.getMessage(errorCode, *args),
            data = data
        )
    }

    fun <T> fromResult(result: Result<T>): ApiResponse<T> {
        return ApiResponse(
            code = result.errorCode.getCode(),
            message = i18nService.getMessage(result.errorCode, *result.args),
            data = result.data
        )
    }
}