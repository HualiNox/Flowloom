package com.mewrix.flowloom.engine.common.baen;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;

/** 提供读写原始字节的 Redis 模板。 */
@Component
public class RedisTemplateBean {

    /**
     * 按原样传递 Redis 键、值和 Lua 脚本参数，保留注册键中的二进制哈希及定长记录布局。
     *
     * @param factory Redis 连接工厂
     * @return 键和值均采用字节数组序列化的模板
     */
    @Bean
    public RedisTemplate<byte[], byte[]> binaryRedisTemplate(
            RedisConnectionFactory factory) {
        RedisTemplate<byte[], byte[]> template = new RedisTemplate<>();

        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.byteArray());
        template.setValueSerializer(RedisSerializer.byteArray());

        template.afterPropertiesSet();

        return template;
    }

}
