package com.mewrix.flowloom.core.common.http

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * API 的 JSON 响应体。[code] 是供客户端判断结果的业务码，
 * [message] 和 [data] 为 null 时会从 JSON 中省略。
 */
data class ApiResponse<T>(
    val code: String,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val message: String?,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val data: T?
)
