package com.mewrix.flowloom.engine.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/** 提供摘要、安全随机数和凭证比较工具。 */
public final class CryptoUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtil() {
    }

    /** 计算 SHA-256 摘要并返回小写十六进制字符串。 */
    public static String sha256Hex(byte[] input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    /** 生成指定长度的安全随机字节。 */
    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /** 生成大于等于零且小于指定上限的均匀随机整数。 */
    public static int randomInt(int bound) {
        return SECURE_RANDOM.nextInt(bound);
    }

    /** 按 UTF-8 字节比较凭证，避免在首个不同字节处提前返回。 */
    public static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
