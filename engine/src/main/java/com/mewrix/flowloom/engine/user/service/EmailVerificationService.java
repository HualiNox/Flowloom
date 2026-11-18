package com.mewrix.flowloom.engine.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
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
import com.mewrix.flowloom.engine.mail.MailTemplateService;
import com.mewrix.flowloom.engine.user.mapper.UserMapper;
import com.mewrix.flowloom.engine.user.response.UserBusinessCode;

import lombok.RequiredArgsConstructor;

/**
 * 邮箱验证：发码、校验并写入 {@code email_verified_at}。
 *
 * <p>
 * 验证码与冷却按 userId 存 Redis。HTML 使用模版 {@code email-verify}，文案由 {@code #{...}} 与 Locale 解析。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String CODE_KEY_PREFIX = "flowloom:email-verify:code:";
    private static final String COOLDOWN_KEY_PREFIX = "flowloom:email-verify:cooldown:";
    private static final String VERIFY_TEMPLATE = "email-verify";

    private final UserMapper userMapper;
    private final MailService mailService;
    private final MailTemplateService mailTemplateService;
    private final MailProperties mailProperties;
    private final StringRedisTemplate redisTemplate;
    private final MessageSource messageSource;

    /**
     * 发送验证码。已验证或冷却未结束时返回对应业务码；发信失败会删除验证码并保留冷却。
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
        String plainText = messageSource.getMessage(
                "user.email_verify.mail_body",
                new Object[] { code, minutes },
                locale);
        String htmlText = mailTemplateService.renderHtml(VERIFY_TEMPLATE, locale, Map.of(
                "username", principal.username(),
                "code", code,
                "minutes", minutes));

        BusinessCode sendResult = mailService.sendEmail(
                principal.email(), subject, plainText, htmlText);
        if (!sendResult.isOk()) {
            redisTemplate.delete(codeKey(principal.userId()));
            return BusinessResult.fail(sendResult);
        }

        return BusinessResult.success();
    }

    /**
     * 校验验证码。成功后清除 Redis 键并更新 {@code email_verified_at}；错误次数达上限则作废验证码。
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
            return BusinessResult.fail(UserBusinessCode.EMAIL_ALREADY_VERIFIED);
        }

        return BusinessResult.success();
    }

    /** 需 Redis {@code PTTL}。 */
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
