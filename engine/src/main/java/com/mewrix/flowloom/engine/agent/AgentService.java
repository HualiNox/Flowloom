package com.mewrix.flowloom.engine.agent;

import com.mewrix.flowloom.engine.agent.AgentRegistrationCodec.RegistrationCredential;
import com.mewrix.flowloom.engine.agent.AgentRegistrationCodec.RegistrationRecord;
import com.mewrix.flowloom.engine.agent.dto.response.AgentRegistrationResponse;
import com.mewrix.flowloom.engine.common.util.TimeUtil;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Agent 添加流程中的挂起注册管理。
 *
 * <p>
 * 前端调用创建接口后获得 {@code registrationId || psk}。Redis 保存以下两项：
 * </p>
 * <ul>
 * <li>{@code flowloom:agent:registration:{sha256(registrationId)}} →
 * {@link RegistrationRecord}</li>
 * <li>{@code flowloom:agent:registration:user:{userId}} → 上述 Registration
 * KEY</li>
 * </ul>
 * 同一用户同一时刻只保留一个挂起的 registrationId；再次创建会原子作废旧注册。
 */
@Service
@RequiredArgsConstructor
public class AgentService {

    private final RedisTemplate<byte[], byte[]> redisTemplate;

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final String REGISTRATION_KEY_PREFIX = "flowloom:agent:registration:";

    private static final String REGISTRATION_USER_KEY_PREFIX = "flowloom:agent:registration:user:";

    /** 挂起注册有效期（秒）。 */
    private static final long REGISTRATION_TTL_SECONDS = 300;

    private static final RedisScript<Long> CREATE_REGISTRATION_SCRIPT = RedisScript.of(
            new ClassPathResource("redis/create-agent-registration.lua"),
            Long.class);

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
        byte[] userKey = registrationUserKey(userId);
        // Lua 通过 tonumber 读取 TTL，二进制模板需传入十进制字符串的字节。
        byte[] ttlArg = String.valueOf(REGISTRATION_TTL_SECONDS).getBytes(StandardCharsets.UTF_8);

        for (int attempt = 0; attempt < 3; attempt++) {
            byte[] registrationId = AgentRegistrationCodec.generateRegistrationId();
            byte[] psk = AgentRegistrationCodec.generatePsk();
            byte[] registrationKey = registrationKey(registrationId);
            byte[] registration = new RegistrationRecord(userId, psk).toBytes();

            Long result = redisTemplate.execute(
                    CREATE_REGISTRATION_SCRIPT,
                    List.of(registrationKey, userKey),
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
     * {@code flowloom:agent:registration:{sha256(registrationId)}}（前缀 UTF-8 +
     * 哈希二进制）。
     */
    private static byte[] registrationKey(byte[] registrationId) {
        try {
            byte[] hash = MessageDigest.getInstance(HASH_ALGORITHM).digest(registrationId);
            byte[] prefix = REGISTRATION_KEY_PREFIX.getBytes(StandardCharsets.UTF_8);
            return ByteBuffer.allocate(prefix.length + hash.length)
                    .put(prefix)
                    .put(hash)
                    .array();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash registration ID", e);
        }
    }

    /** {@code flowloom:agent:registration:user:{userId}}，用于保证每用户仅一个挂起注册。 */
    private static byte[] registrationUserKey(UUID userId) {
        return (REGISTRATION_USER_KEY_PREFIX + userId).getBytes(StandardCharsets.UTF_8);
    }
}
