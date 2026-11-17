package com.mewrix.flowloom.engine.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.auth.properties.SessionProperties;
import com.mewrix.flowloom.engine.auth.response.AuthBusinessCode;
import com.mewrix.flowloom.engine.common.response.BusinessCode;
import com.mewrix.flowloom.engine.common.response.CommonBusinessCode;
import com.mewrix.flowloom.engine.user.entity.User;
import com.mewrix.flowloom.engine.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 会话创建、校验与滑动续期。
 *
 * <p>存储模型：
 * <ul>
 *   <li>{@code flowloom:session:{sha256(token)}} → {@code userId}，带 TTL</li>
 *   <li>{@code flowloom:user:{userId}:sessions} → 会话 key 集合，用于按用户批量吊销</li>
 * </ul>
 * 明文 token 只在创建时返回一次；Redis 中仅保存哈希后的 key。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    private final UserMapper userMapper;
    private final SessionProperties sessionProperties;
    private final StringRedisTemplate redisTemplate;

    /** 用于生成会话凭证的安全随机源。 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 凭证写入 Redis 前使用的哈希算法。 */
    private static final String HASH_ALGORITHM = "SHA-256";

    /** 会话 key 前缀：{@code flowloom:session:{sha256hex}}。 */
    private static final String KEY_PREFIX = "flowloom:session:";

    /** 用户会话索引前缀：{@code flowloom:user:{userId}:sessions}。 */
    private static final String USER_SESSIONS_PREFIX = "flowloom:user:";

    private static final String USER_SESSIONS_SUFFIX = ":sessions";

    /** 会话滑动续期的 Lua 脚本（原子 TTL 检查 + 条件 EXPIRE）。 */
    private static final RedisScript<Long> SESSION_TOUCH_SCRIPT = RedisScript.of(
            new ClassPathResource("redis/session-touch.lua"),
            Long.class);

    /**
     * 为指定用户创建会话。
     *
     * <p>
     * 使用 {@code SET NX} 写入会话 key，并将该 key 记入用户索引集合。
     * 哈希碰撞时最多重试 3 次；仍失败则抛出 {@link IllegalStateException}。
     * 成功时返回明文凭证，由调用方下发，服务端不再保存明文。
     * </p>
     *
     * @param userId 用户 ID
     * @return 明文会话凭证
     */
    public String createSession(UUID userId) {
        for (int attempt = 0; attempt < 3; attempt++) {
            String token = generateToken();
            String key = sessionKey(token);

            Boolean created = redisTemplate.opsForValue()
                    .setIfAbsent(
                            key,
                            userId.toString(),
                            sessionProperties.timeout());

            if (Boolean.TRUE.equals(created)) {
                // 索引无独立 TTL；成员会在会话自然过期后残留，revokeAll 时 DEL 缺失 key 即可
                redisTemplate.opsForSet().add(userSessionsKey(userId), key);
                return token;
            }
        }

        throw new IllegalStateException("Failed to create session after 3 attempts");
    }

    /**
     * 查找有效会话对应的用户身份；有效时按配置尝试滑动续期。
     *
     * <p>
     * 值损坏（非 UUID）或对应用户已不存在时，删除孤儿会话 key（及索引成员），
     * 避免脏数据继续占用 Redis。
     * </p>
     *
     * @param token 明文会话凭证
     * @return 用户身份；凭证无效、已过期或已吊销时为空
     */
    public Optional<UserPrincipal> findValidPrincipal(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }

        String key = sessionKey(token);
        String userIdString = redisTemplate.opsForValue().get(key);

        if (userIdString == null) {
            return Optional.empty();
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdString);
        } catch (IllegalArgumentException ex) {
            redisTemplate.delete(key);
            return Optional.empty();
        }

        User user = userMapper.findById(userId);
        if (user == null) {
            removeSessionKey(key, userId);
            return Optional.empty();
        }

        BusinessCode code = touchSession(key);
        if (!code.isOk()) {
            if (code == AuthBusinessCode.AUTH_KEY_TTL_EXCEPTION) {
                log.warn("Session key TTL exception: {}", key);
            }
            return Optional.empty();
        }

        return Optional.of(new UserPrincipal(user.getId(), user.getUsername(), user.getEmail()));
    }

    /**
     * 吊销单个会话。对未知或已吊销凭证保持幂等，不向外抛错。
     *
     * @param token 明文会话凭证
     */
    public void revokeSession(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        String key = sessionKey(token);
        String userIdString = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);

        if (userIdString == null) {
            return;
        }

        try {
            redisTemplate.opsForSet().remove(userSessionsKey(UUID.fromString(userIdString)), key);
        } catch (IllegalArgumentException ignored) {
            // 损坏的 userId 值已随 key 删除
        }
    }

    /**
     * 吊销指定用户的全部会话（例如改密、强制下线）。
     *
     * @param userId 用户 ID
     */
    public void revokeAllUserSessions(UUID userId) {
        if (userId == null) {
            return;
        }

        String indexKey = userSessionsKey(userId);
        Set<String> sessionKeys = redisTemplate.opsForSet().members(indexKey);
        if (sessionKeys != null && !sessionKeys.isEmpty()) {
            redisTemplate.delete(sessionKeys);
        }
        redisTemplate.delete(indexKey);
    }

    /** 删除会话 key，并从用户索引中移除。 */
    private void removeSessionKey(String key, UUID userId) {
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(userSessionsKey(userId), key);
    }

    /** 将明文 token 映射为 Redis 会话 key（SHA-256 hex）。 */
    private static String sessionKey(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 用户会话索引 key。 */
    private static String userSessionsKey(UUID userId) {
        return USER_SESSIONS_PREFIX + userId + USER_SESSIONS_SUFFIX;
    }

    /**
     * 滑动续期：剩余 TTL 不大于 {@code timeout - minRefreshInterval} 时，将 TTL 重置为
     * {@link SessionProperties#timeout()}。未到间隔则不写 Redis，避免每个请求都更新。
     *
     * <p>
     * {@link StringRedisTemplate} 使用 {@code StringRedisSerializer} 序列化脚本 ARGV，
     * 因此阈值与超时必须传 {@link String}（不能传 {@code Long}），否则会
     * {@link ClassCastException}。脚本内再 {@code tonumber(ARGV[n])}。
     * </p>
     *
     * @param key 会话 Redis key
     * @return 续期结果业务码；{@code -2}/null 表示 key 缺失，{@code -1} 表示无 TTL 异常
     */
    private BusinessCode touchSession(String key) {
        long threshold = sessionProperties.timeout().minus(
                sessionProperties.minRefreshInterval())
                .getSeconds();

        long timeout = sessionProperties.timeout().getSeconds();

        Long result = redisTemplate.execute(
                SESSION_TOUCH_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(threshold),
                String.valueOf(timeout));

        if (result == null || result == -2) {
            return AuthBusinessCode.AUTH_GET_KEY_FAILED;
        }

        if (result == -1) {
            return AuthBusinessCode.AUTH_KEY_TTL_EXCEPTION;
        }

        return CommonBusinessCode.OK;
    }

    /**
     * 生成 32 字节随机凭证，并以 URL-safe Base64（无 padding）编码。
     */
    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
