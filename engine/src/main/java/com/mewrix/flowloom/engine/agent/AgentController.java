package com.mewrix.flowloom.engine.agent;

import com.mewrix.flowloom.engine.agent.dto.response.AgentRegistrationResponse;
import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.common.response.ApiResponse;
import com.mewrix.flowloom.engine.common.response.ApiResponseFactory;
import com.mewrix.flowloom.engine.common.result.BusinessResult;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

/** Agent 挂起注册与挑战交换接口。 */
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
    @PostMapping("/registration")
    public ApiResponse<AgentRegistrationResponse> createRegistration(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        AgentRegistrationResponse result = agentService.createRegistration(userPrincipal.userId());
        return apiResponseFactory.from(BusinessResult.success(result));
    }

    /**
     * 使用挂起注册凭证交换挑战。
     *
     * @param registrationIdString Base64URL 编码的 8 字节注册 ID
     * @param requestBody          32 字节原始 AgentChallenge
     * @return 60 字节二进制服务端证明，格式为 ServerChallenge、nonce、tag 依次拼接
     * @throws IOException 请求体读取失败
     */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> register(
            @RequestHeader("X-Agent-Registration") String registrationIdString,
            InputStream requestBody) throws IOException {
        byte[] registrationId;
        try {
            registrationId = Base64.getUrlDecoder().decode(registrationIdString);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        // 多读一字节即可识别超长请求，无需缓冲整个请求体。
        byte[] serverProof = agentService.register(registrationId,
                requestBody.readNBytes(AgentService.CHALLENGE_BYTES + 1));
        return serverProof == null ? ResponseEntity.badRequest().build() : ResponseEntity.ok(serverProof);
    }

}
