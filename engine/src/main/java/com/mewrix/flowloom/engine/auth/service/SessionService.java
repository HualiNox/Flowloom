package com.mewrix.flowloom.engine.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.mewrix.flowloom.engine.auth.entity.Session;
import com.mewrix.flowloom.engine.auth.mapper.SessionMapper;
import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.auth.properties.SessionProperties;
import com.mewrix.flowloom.engine.common.util.TimeUtil;
import com.mewrix.flowloom.engine.user.entity.User;
import com.mewrix.flowloom.engine.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

/**
 * 会话创建、校验与滑动续期。
 * 凭证仅以哈希形式入库，明文 token 只在创建时返回一次。
 */
@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionMapper sessionMapper;
    private final UserMapper userMapper;
    private final SessionProperties sessionProperties;

    /**
     * 用于生成会话凭证的安全随机源。
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 凭证入库前使用的哈希算法。
     */
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * 为指定用户创建会话。
     *
     * @param userId 用户 ID
     * @return 明文会话凭证；调用方负责下发，服务端不再保存明文
     */
    public Optional<String> createSession(UUID userId) {
        Session session = new Session();
        session.setUserId(userId);

        LocalDateTime now = TimeUtil.now();

        String token = generateToken();
        byte[] tokenHash = hashToken(token);
        session.setTokenHash(tokenHash);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plus(sessionProperties.timeout()));

        int rowsAffected = sessionMapper.insert(session);
        if (rowsAffected != 1) {
            throw new IllegalStateException("Failed to create session for user: " + userId);
        }

        return Optional.of(token);
    }

    /**
     * 查找有效会话对应的用户身份；有效时按配置尝试滑动续期。
     *
     * @param token 明文会话凭证
     * @return 用户身份；凭证无效、已过期或已吊销时为空
     */
    public Optional<UserPrincipal> findValidPrincipal(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime now = TimeUtil.now();
        byte[] tokenHash = hashToken(token);
        Session session = sessionMapper.findValidSessionByHash(tokenHash, now);
        if (session == null) {
            return Optional.empty();
        }

        User user = userMapper.findById(session.getUserId());
        if (user == null) {
            return Optional.empty();
        }

        touchSession(tokenHash, now);
        return Optional.of(new UserPrincipal(user.getId(), user.getUsername(), user.getEmail()));
    }

    public int deleteExpiredSessions() {
        LocalDateTime now = TimeUtil.now();
        return sessionMapper.deleteExpiredSessions(now);
    }

    /**
     * 滑动续期：距上次续期达到 {@link SessionProperties#minRefreshInterval()} 后，
     * 将过期时间重置为 now + {@link SessionProperties#timeout()}。
     * 未到间隔则不写库，避免每个请求都更新。
     *
     * @param tokenHash 凭证哈希
     * @param now       当前业务时间（{@link TimeUtil#now()}）
     */
    private void touchSession(byte[] tokenHash, LocalDateTime now) {
        LocalDateTime newExpiresAt = now.plus(sessionProperties.timeout());
        // refreshThreshold = now + timeout - minRefreshInterval
        LocalDateTime refreshThreshold = newExpiresAt.minus(sessionProperties.minRefreshInterval());

        sessionMapper.extendExpiryIfDue(tokenHash, now, refreshThreshold, newExpiresAt);
    }

    /**
     * 吊销会话。对未知或已吊销凭证保持幂等，不向外抛错。
     *
     * @param token 明文会话凭证
     */
    public void revokeSession(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        LocalDateTime now = TimeUtil.now();
        byte[] tokenHash = hashToken(token);
        sessionMapper.revokeSession(tokenHash, now);
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

    /**
     * 对明文凭证做 SHA-256，得到入库用的哈希。
     *
     * @param token 明文凭证
     * @return 哈希值
     */
    private static byte[] hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            return digest.digest(token.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
