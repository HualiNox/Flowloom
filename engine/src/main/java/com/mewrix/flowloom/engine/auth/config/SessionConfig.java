package com.mewrix.flowloom.engine.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.mewrix.flowloom.engine.auth.properties.SessionProperties;

/** 启用 {@link SessionProperties} 配置绑定。 */
@Configuration
@EnableConfigurationProperties(SessionProperties.class)
public class SessionConfig {
}
