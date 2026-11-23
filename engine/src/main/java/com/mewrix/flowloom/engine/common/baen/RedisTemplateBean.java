package com.mewrix.flowloom.engine.common.baen;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

/** 提供使用字符串键与二进制值的 Redis 模板。 */
@Component
public class RedisTemplateBean {

    /**
     * 将 Redis 键编码为 UTF-8，按原样传递值与 Lua 脚本参数的字节。
     *
     * @param factory Redis 连接工厂
     * @return 使用 UTF-8 字符串键和原始字节值的模板
     */
    @Bean
    public RedisTemplate<String, byte[]> binaryRedisTemplate(
            RedisConnectionFactory factory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();

        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.byteArray());

        template.afterPropertiesSet();

        return template;
    }

}
