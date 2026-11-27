package com.mewrix.flowloom.core.bootstrap

import com.mewrix.flowloom.core.auth.AuthService
import com.mewrix.flowloom.core.auth.SessionCookieManager
import com.mewrix.flowloom.core.auth.dto.request.ResetAdminAccountRequest
import com.mewrix.flowloom.core.bootstrap.dto.request.AdminAccountRequest
import com.mewrix.flowloom.core.bootstrap.dto.response.GetStatusResponse
import com.mewrix.flowloom.core.common.http.ApiResponse
import com.mewrix.flowloom.core.common.http.ApiResponseFactory
import com.mewrix.flowloom.core.common.http.CommonErrorCode
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bootstrap")
class BootstrapController(
    private val apiResponseFactory: ApiResponseFactory,
    private val bootstrapService: BootstrapService,
    private val authService: AuthService,
    private val sessionCookieManager: SessionCookieManager
) {

    /**
     * 获取 Flowloom 实例的状态。
     */
    @GetMapping("/status")
    fun getBootstrapStatus(): ApiResponse<GetStatusResponse> =
        apiResponseFactory.success(bootstrapService.instanceStatus())

    /** 在引导期间更新初始管理员凭据，成功后需用新凭据重新登录。 */
    @PostMapping("/admin/account")
    fun configureAdminAccount(
        @RequestBody body: AdminAccountRequest,
        response: HttpServletResponse
    ): ResponseEntity<ApiResponse<Unit>> {
        val result = authService.configureAdminAccount(
            ResetAdminAccountRequest(body.username, body.oldPassword, body.newPassword, body.confirmPassword)
        )
        val apiResponse = apiResponseFactory.fromResult(result)
        return when (result.errorCode) {
            CommonErrorCode.OK -> {
                sessionCookieManager.clear(response)
                ResponseEntity.ok(apiResponse)
            }
            BootstrapErrorCode.INSTANCE_ALREADY_INITIALIZED ->
                ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiResponse)
            else -> ResponseEntity.badRequest().body(apiResponse)
        }
    }
}
