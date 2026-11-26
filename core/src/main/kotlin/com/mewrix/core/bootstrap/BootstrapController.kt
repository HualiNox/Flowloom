package com.mewrix.core.bootstrap

import com.mewrix.core.bootstrap.dto.response.GetStatusResponse
import com.mewrix.core.common.http.ApiResponse
import com.mewrix.core.common.http.ApiResponseFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bootstrap")
class BootstrapController(
    private val apiResponseFactory: ApiResponseFactory,
    private val bootstrapService: BootstrapService
) {

    /**
     * 获取 Flowloom 实例的状态。
     */
    @GetMapping("/status")
    fun getBootstrapStatus(): ApiResponse<GetStatusResponse> =
        apiResponseFactory.success(bootstrapService.instanceStatus())
}
