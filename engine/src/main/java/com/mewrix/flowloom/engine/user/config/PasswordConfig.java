package com.mewrix.flowloom.engine.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordConfig类用于配置密码编码器，确保用户密码在存储和验证过程中具有高安全性。
 */
@Configuration
public class PasswordConfig {

    /**
     * 配置密码编码器，使用Argon2算法进行密码加密。
     *
     * <p>
     * Argon2是一种现代的密码哈希算法，具有高安全性和
     * 良好的性能。该方法返回一个配置好的Argon2PasswordEncoder实例，
     * 可用于对用户密码进行加密和验证。
     * </p>
     *
     * @return 用于生成和验证密码哈希的编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                16, // Salt length (bytes)
                32, // Hash length (bytes)
                1, // Parallelism
                19 * 1024, // Memory (KB)
                2 // Iterations
        );
    }
}
