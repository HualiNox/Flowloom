package com.mewrix.flowloom.engine.agent;

import com.mewrix.flowloom.engine.agent.dto.response.AgentRegistrationResponse;
import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.common.response.ApiResponse;
import com.mewrix.flowloom.engine.common.response.ApiResponseFactory;
import com.mewrix.flowloom.engine.common.result.BusinessResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Agent 挂起注册接口，需已认证且邮箱已验证。 */
@Tag(name = "Agent", description = "Agent 挂起注册")
@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final ApiResponseFactory apiResponseFactory;

    /**
     * 开始添加 Agent：为当前用户签发挂起的注册凭证。
     *
     * <p>
     * 同一用户同一时刻只能有一个有效 registrationId；重复请求会作废旧凭证。
     * </p>
     *
     * @param userPrincipal 当前会话用户
     * @return {@code registrationId || psk}（Base64URL）与过期时间
     */
    @Operation(summary = "创建 Agent 注册", description = "签发短时注册凭证（registrationId + PSK）。" +
            "同一用户同时仅允许一个挂起注册；再次请求会使旧凭证失效。")
    @PostMapping("/registration")
    public ApiResponse<AgentRegistrationResponse> createRegistration(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        AgentRegistrationResponse result = agentService.createRegistration(userPrincipal.userId());
        return apiResponseFactory.from(BusinessResult.success(result));
    }
}
