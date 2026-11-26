package com.mewrix.core.common.http

import com.fasterxml.jackson.annotation.JsonInclude

data class ApiResponse<T>(
    val code: String,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val message: String?,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    val data: T?
)
