package com.mewrix.flowloom.engine.common.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;

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

    /**
     * 使用 HKDF-SHA256 和默认全零 salt 派生密钥。
     *
     * @param inputKey 输入密钥材料
     * @param info     用于区分协议或密钥用途的上下文字节
     * @param length   输出长度（字节）
     * @return 派生密钥
     */
    public static byte[] hkdfSha256(byte[] inputKey, byte[] info, int length) {
        HKDFBytesGenerator generator = new HKDFBytesGenerator(new SHA256Digest());
        generator.init(new HKDFParameters(inputKey, null, info));
        byte[] key = new byte[length];
        generator.generateBytes(key, 0, key.length);
        return key;
    }

    /**
     * 使用 AES-GCM 加密。
     *
     * @param key       AES 密钥
     * @param nonce     调用方提供的 nonce，同一密钥下每次加密须使用不同的值
     * @param aad       附加认证数据
     * @param plaintext 待加密明文
     * @return 密文及末尾的 16 字节认证标签
     */
    public static byte[] encryptAesGcm(byte[] key, byte[] nonce, byte[] aad, byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad);
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt with AES-GCM", e);
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
