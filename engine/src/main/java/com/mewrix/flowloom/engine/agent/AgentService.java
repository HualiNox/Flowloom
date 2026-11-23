package com.mewrix.flowloom.engine.agent;

import com.mewrix.flowloom.engine.agent.AgentRegistrationCodec.RegistrationCredential;
import com.mewrix.flowloom.engine.agent.AgentRegistrationCodec.RegistrationRecord;
import com.mewrix.flowloom.engine.agent.dto.response.AgentRegistrationResponse;
import com.mewrix.flowloom.engine.common.util.CryptoUtil;
import com.mewrix.flowloom.engine.common.util.TimeUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Agent 添加流程中的挂起注册管理。
 *
 * <p>
 * 前端调用创建接口后获得 {@code registrationId || psk}。Redis 保存以下三项：
 * </p>
 * <ul>
 * <li>{@code flowloom:agent:registration:{hex(sha256(registrationId))}} →
 * {@link RegistrationRecord}</li>
 * <li>{@code flowloom:agent:registration:user:{userId}} → 注册记录键</li>
 * <li>{@code flowloom:agent:registration:owner:{hex(sha256(registrationId))}} →
 * 用户索引键</li>
 * </ul>
 * 同一用户同一时刻只保留一个挂起的 registrationId；再次创建会原子作废旧注册。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final RedisTemplate<String, byte[]> redisTemplate;

    private static final String REGISTRATION_KEY_PREFIX = "flowloom:agent:registration:";

    private static final String REGISTRATION_USER_KEY_PREFIX = "flowloom:agent:registration:user:";

    private static final String REGISTRATION_OWNER_KEY_PREFIX = "flowloom:agent:registration:owner:";

    private static final String REGISTRATION_CHALLENGE_KEY_PREFIX = "flowloom:agent:registration:challenge:";

    static final int CHALLENGE_BYTES = 32;
    private static final int GCM_NONCE_BYTES = 12;
    private static final byte SERVER_PROOF_AAD_TYPE = 0x01;
    private static final byte[] S2C_KEY_INFO = "flowloom/registration/s2c/v1".getBytes(StandardCharsets.US_ASCII);

    /** 挂起注册有效期（秒）。 */
    private static final long REGISTRATION_TTL_SECONDS = 300;

    private static final RedisScript<Long> CREATE_REGISTRATION_SCRIPT = RedisScript.of(
            new ClassPathResource("redis/create-agent-registration.lua"),
            Long.class);

    private static final RedisScript<Long> DELETE_AGENT_REGISTRATION_SCRIPT = RedisScript.of(
            new ClassPathResource("redis/delete-agent-registration.lua"),
            Long.class);

    private static final RedisScript<byte[]> SAVE_AGENT_CHALLENGE_SCRIPT = RedisScript.of(
            new ClassPathResource("redis/save-agent-challenge.lua"),
            byte[].class);

    /**
     * 为当前用户创建挂起的 Agent 注册。
     *
     * <p>
     * 每次尝试都生成新的 {@code registrationId} 与 {@code psk}，最多尝试 3 次。
     * 成功时返回 Base64URL 编码的 {@link RegistrationCredential}（明文仅此一次下发）。
     * 会替换该用户此前未完成的注册，保证同一时间只有一个有效 registrationId。
     * </p>
     *
     * @param userId 用户 ID
     * @return 注册凭证与过期时间
     * @throws IllegalStateException 注册有效期不合法或达到尝试上限
     */
    public AgentRegistrationResponse createRegistration(UUID userId) {
        String userKey = registrationUserKey(userId);
        // Lua 脚本参数采用值序列化器，TTL 以十进制字符串的字节传入。
        byte[] ttlArg = String.valueOf(REGISTRATION_TTL_SECONDS).getBytes(StandardCharsets.UTF_8);

        for (int attempt = 0; attempt < 3; attempt++) {
            byte[] registrationId = AgentRegistrationCodec.generateRegistrationId();
            byte[] psk = AgentRegistrationCodec.generatePsk();
            String registrationHash = CryptoUtil.sha256Hex(registrationId);
            String registrationKey = REGISTRATION_KEY_PREFIX + registrationHash;
            String ownerKey = REGISTRATION_OWNER_KEY_PREFIX + registrationHash;
            byte[] registration = new RegistrationRecord(userId, psk).toBytes();

            Long result = redisTemplate.execute(
                    CREATE_REGISTRATION_SCRIPT,
                    List.of(registrationKey, userKey, ownerKey),
                    registration,
                    ttlArg);

            if (result != null && result == 1L) {
                LocalDateTime expiresAt = TimeUtil.now().plusSeconds(REGISTRATION_TTL_SECONDS);
                byte[] credential = new RegistrationCredential(registrationId, psk).toBytes();
                return new AgentRegistrationResponse(
                        Base64.getUrlEncoder().withoutPadding().encodeToString(credential),
                        expiresAt);
            }

            if (result != null && result == -2L) {
                throw new IllegalStateException("Invalid registration TTL: " + REGISTRATION_TTL_SECONDS);
            }

            // 注册 ID 碰撞或脚本未返回结果时，换一个 ID 再试。
        }

        throw new IllegalStateException("Failed to create agent registration after 3 attempts");
    }

    /**
     * 为挂起注册生成服务端证明。相同 AgentChallenge 的重试返回 Redis 中已保存的证明。
     *
     * @param registrationId 原始 8 字节注册 ID
     * @param agentChallenge Agent 生成的 32 字节挑战
     * @return {@code ServerChallenge || nonce || tag}。输入无效或挂起注册失效时返回 {@code null}
     */
    public byte[] register(byte[] registrationId, byte[] agentChallenge) {
        if (registrationId.length != AgentRegistrationCodec.REGISTRATION_ID_SIZE
                || agentChallenge.length != CHALLENGE_BYTES) {
            return null;
        }

        String registrationHash = CryptoUtil.sha256Hex(registrationId);
        String registrationKey = REGISTRATION_KEY_PREFIX + registrationHash;
        String challengeKey = REGISTRATION_CHALLENGE_KEY_PREFIX + registrationHash;
        byte[] rawRegistrationRecord = redisTemplate.opsForValue().get(registrationKey);
        if (rawRegistrationRecord == null) {
            return null;
        }

        RegistrationRecord registrationRecord;
        try {
            registrationRecord = RegistrationRecord.fromBytes(rawRegistrationRecord);
        } catch (IllegalArgumentException e) {
            String ownerKey = REGISTRATION_OWNER_KEY_PREFIX + registrationHash;
            // 将读到的损坏记录交给脚本比对，避免清理由并发请求替换的新记录。
            Long count = redisTemplate.execute(DELETE_AGENT_REGISTRATION_SCRIPT,
                    List.of(registrationKey, ownerKey, challengeKey), rawRegistrationRecord);
            log.warn("Invalid agent registration record. Deleted {} keys.", count);
            return null;
        }

        byte[] serverChallenge = CryptoUtil.randomBytes(CHALLENGE_BYTES);
        byte[] nonce = CryptoUtil.randomBytes(GCM_NONCE_BYTES);
        // 认证标签绑定报文用途、注册 ID 和双方挑战。
        byte[] aad = ByteBuffer.allocate(1 + registrationId.length + agentChallenge.length + serverChallenge.length)
                .put(SERVER_PROOF_AAD_TYPE)
                .put(registrationId)
                .put(agentChallenge)
                .put(serverChallenge)
                .array();
        byte[] s2cKey = CryptoUtil.hkdfSha256(registrationRecord.psk(), S2C_KEY_INFO, 32);
        // 空明文只产生认证标签，用于证明 Engine 持有 PSK。
        byte[] tag = CryptoUtil.encryptAesGcm(s2cKey, nonce, aad, new byte[0]);
        byte[] handshake = ByteBuffer.allocate(agentChallenge.length + serverChallenge.length + nonce.length + tag.length)
                .put(agentChallenge)
                .put(serverChallenge)
                .put(nonce)
                .put(tag)
                .array();

        // Lua 在写入前检查注册记录是否仍匹配，并沿用它的剩余有效期。
        byte[] storedHandshake = redisTemplate.execute(SAVE_AGENT_CHALLENGE_SCRIPT,
                List.of(registrationKey, challengeKey), rawRegistrationRecord, handshake);
        // Agent 已持有自己的挑战，响应只返回握手状态的后 60 字节。
        return storedHandshake == null ? null : Arrays.copyOfRange(storedHandshake, CHALLENGE_BYTES, storedHandshake.length);
    }

    /** {@code flowloom:agent:registration:user:{userId}}，用于保证每用户仅一个挂起注册。 */
    private static String registrationUserKey(UUID userId) {
        return REGISTRATION_USER_KEY_PREFIX + userId;
    }
}
