package com.mewrix.flowloom.engine.agent;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

/**
 * Agent 注册阶段二进制编解码。
 *
 * <p>
 * 客户端凭证 {@link RegistrationCredential} 为 {@code registrationId (8) || psk (16)}。
 * Redis 中的 {@link RegistrationRecord} 为
 * {@code userId (16) || psk (16)}，注册键的后缀为注册 ID 的 SHA-256 十六进制摘要。
 * </p>
 */
public final class AgentRegistrationCodec {

    private AgentRegistrationCodec() {
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int UUID_SIZE = 16;

    /** 预共享密钥长度（字节）。 */
    public static final int PSK_SIZE = 16;

    /** 注册 ID 长度（字节）。 */
    public static final int REGISTRATION_ID_SIZE = 8;

    /** 为一次挂起注册生成随机预共享密钥。 */
    public static byte[] generatePsk() {
        return generateRandomBytes(PSK_SIZE);
    }

    /** 生成注册 ID，唯一性由 Redis 中的挂起注册键校验。 */
    public static byte[] generateRegistrationId() {
        return generateRandomBytes(REGISTRATION_ID_SIZE);
    }

    private static byte[] generateRandomBytes(int size) {
        byte[] data = new byte[size];
        SECURE_RANDOM.nextBytes(data);
        return data;
    }

    /**
     * 下发给前端 / Agent 的注册凭证：{@code registrationId || psk}。
     *
     * @param registrationId 注册会话 ID
     * @param psk            预共享密钥
     */
    public record RegistrationCredential(
            byte[] registrationId,
            byte[] psk) {
        /** 按注册 ID、PSK 的顺序拼接两个定长字段。 */
        public byte[] toBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(REGISTRATION_ID_SIZE + PSK_SIZE);
            buffer.put(registrationId);
            buffer.put(psk);
            return buffer.array();
        }

        /**
         * 从 Base64URL 解码后的原始字节恢复注册凭证。
         *
         * @param data 注册凭证的二进制数据
         * @return 注册 ID 与 PSK
         * @throws IllegalArgumentException 字节数与凭证格式不符
         */
        public static RegistrationCredential fromBytes(byte[] data) {
            if (data.length != REGISTRATION_ID_SIZE + PSK_SIZE) {
                throw new IllegalArgumentException("Invalid data length for RegistrationCredential");
            }
            byte[] registrationId = Arrays.copyOfRange(data, 0, REGISTRATION_ID_SIZE);
            byte[] psk = Arrays.copyOfRange(data, REGISTRATION_ID_SIZE, data.length);
            return new RegistrationCredential(registrationId, psk);
        }
    }

    /**
     * Redis 中挂起的注册记录：归属用户与 PSK（registrationId 仅出现在 key 的哈希中）。
     *
     * @param userId 发起注册的用户
     * @param psk    与客户端共享的预共享密钥
     */
    public record RegistrationRecord(
            UUID userId,
            byte[] psk) {
        /** 先以大端序写入 UUID 的高、低各 64 位，再写入 PSK。 */
        public byte[] toBytes() {
            ByteBuffer buffer = ByteBuffer.allocate(UUID_SIZE + PSK_SIZE);
            buffer.putLong(userId.getMostSignificantBits());
            buffer.putLong(userId.getLeastSignificantBits());
            buffer.put(psk);
            return buffer.array();
        }

        /**
         * 从 Redis 值恢复挂起注册记录。
         *
         * @param data Redis 中的二进制记录
         * @return 注册发起用户与 PSK
         * @throws IllegalArgumentException 字节数与记录格式不符
         */
        public static RegistrationRecord fromBytes(byte[] data) {
            if (data.length != UUID_SIZE + PSK_SIZE) {
                throw new IllegalArgumentException("Invalid data length for RegistrationRecord");
            }
            ByteBuffer buffer = ByteBuffer.wrap(data);
            UUID userId = new UUID(buffer.getLong(), buffer.getLong());
            byte[] psk = new byte[PSK_SIZE];
            buffer.get(psk);
            return new RegistrationRecord(userId, psk);
        }
    }
}
