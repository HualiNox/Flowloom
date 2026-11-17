package com.mewrix.flowloom.engine.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.context.MessageSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.common.response.BusinessCode;
import com.mewrix.flowloom.engine.common.result.BusinessResult;
import com.mewrix.flowloom.engine.common.util.TimeUtil;
import com.mewrix.flowloom.engine.mail.MailProperties;
import com.mewrix.flowloom.engine.mail.MailService;
import com.mewrix.flowloom.engine.user.mapper.UserMapper;
import com.mewrix.flowloom.engine.user.response.UserBusinessCode;

import lombok.RequiredArgsConstructor;

/**
 * 邮箱验证：发信、校验与落库。
 *
 * <p>
 * 验证码与发信冷却均按 {@code userId} 存于 Redis，避免按 IP 限流误伤共享出口用户。
 * 登录不要求已验证；验证状态由 {@code email_verified_at} 与过滤器共同约束业务范围。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CODE_KEY_PREFIX = "flowloom:email-verify:code:";
    private static final String COOLDOWN_KEY_PREFIX = "flowloom:email-verify:cooldown:";

    private final UserMapper userMapper;
    private final MailService mailService;
    private final MailProperties mailProperties;
    private final StringRedisTemplate redisTemplate;
    private final MessageSource messageSource;

    /**
     * 向当前用户邮箱发送验证码。
     *
     * <p>
     * 已验证直接返回 {@link UserBusinessCode#EMAIL_ALREADY_VERIFIED}；
     * 冷却期内返回 {@link UserBusinessCode#EMAIL_VERIFY_TOO_FREQUENT}。
     * 限流维度为 userId，不使用 IP。
     * </p>
     */
    public BusinessResult<Void> sendVerificationCode(UserPrincipal principal, Locale locale) {
        if (principal.emailVerified()) {
            return BusinessResult.fail(UserBusinessCode.EMAIL_ALREADY_VERIFIED);
        }

        String cooldownKey = cooldownKey(principal.userId());
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                cooldownKey,
                "1",
                mailProperties.verification().resendInterval());
        if (!Boolean.TRUE.equals(acquired)) {
            return BusinessResult.fail(UserBusinessCode.EMAIL_VERIFY_TOO_FREQUENT);
        }

        String code = generateCode();
        redisTemplate.opsForValue().set(
                codeKey(principal.userId()),
                encodeStoredValue(code, 0),
                mailProperties.verification().codeTtl());

        long minutes = Math.max(1, mailProperties.verification().codeTtl().toMinutes());
        String subject = messageSource.getMessage(
                "user.email_verify.mail_subject", null, locale);
        String body = messageSource.getMessage(
                "user.email_verify.mail_body",
                new Object[] { code, minutes },
                locale);

        BusinessCode sendResult = mailService.sendEmail(
                principal.email(), subject, body, false);
        if (!sendResult.isOk()) {
            // 发信失败时清掉验证码，保留冷却，避免被刷信
            redisTemplate.delete(codeKey(principal.userId()));
            return BusinessResult.fail(sendResult);
        }

        return BusinessResult.success();
    }

    /**
     * 校验验证码并写入 {@code email_verified_at}。
     *
     * <p>
     * 成功后删除 Redis 中的验证码与冷却键；下一请求重建 principal 时即为已验证。
     * </p>
     */
    public BusinessResult<Void> verifyEmail(UserPrincipal principal, String code) {
        if (principal.emailVerified()) {
            return BusinessResult.fail(UserBusinessCode.EMAIL_ALREADY_VERIFIED);
        }

        String key = codeKey(principal.userId());
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            return BusinessResult.fail(UserBusinessCode.EMAIL_VERIFY_CODE_EXPIRED);
        }

        ParsedCode parsed = parseStoredValue(stored);
        if (parsed == null) {
            redisTemplate.delete(key);
            return BusinessResult.fail(UserBusinessCode.EMAIL_VERIFY_CODE_INVALID);
        }

        if (!constantTimeEquals(parsed.code(), code)) {
            int attempts = parsed.attempts() + 1;
            if (attempts >= mailProperties.verification().maxAttempts()) {
                redisTemplate.delete(key);
            } else {
                Duration remaining = remainingTtl(key);
                if (remaining == null || remaining.isZero() || remaining.isNegative()) {
                    redisTemplate.delete(key);
                } else {
                    redisTemplate.opsForValue().set(
                            key,
                            encodeStoredValue(parsed.code(), attempts),
                            remaining);
                }
            }
            return BusinessResult.fail(UserBusinessCode.EMAIL_VERIFY_CODE_INVALID);
        }

        int updated = userMapper.markEmailVerified(principal.userId(), TimeUtil.now());
        redisTemplate.delete(key);
        redisTemplate.delete(cooldownKey(principal.userId()));

        if (updated <= 0) {
            // 并发下可能已被其他请求验证
            return BusinessResult.fail(UserBusinessCode.EMAIL_ALREADY_VERIFIED);
        }

        return BusinessResult.success();
    }

    /** 读取 key 剩余 TTL；依赖 Redis {@code PTTL} 权限。 */
    private Duration remainingTtl(String key) {
        Long seconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (seconds == null || seconds < 0) {
            return null;
        }
        return Duration.ofSeconds(seconds);
    }

    private static String generateCode() {
        int value = SECURE_RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private static String encodeStoredValue(String code, int attempts) {
        return code + ":" + attempts;
    }

    private static ParsedCode parseStoredValue(String stored) {
        int sep = stored.lastIndexOf(':');
        if (sep <= 0 || sep >= stored.length() - 1) {
            return null;
        }
        String code = stored.substring(0, sep);
        try {
            int attempts = Integer.parseInt(stored.substring(sep + 1));
            return new ParsedCode(code, attempts);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        byte[] a = left.getBytes(StandardCharsets.UTF_8);
        byte[] b = right.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private static String codeKey(UUID userId) {
        return CODE_KEY_PREFIX + userId;
    }

    private static String cooldownKey(UUID userId) {
        return COOLDOWN_KEY_PREFIX + userId;
    }

    private record ParsedCode(String code, int attempts) {
    }
}
