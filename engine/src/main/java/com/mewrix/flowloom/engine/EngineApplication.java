package com.mewrix.flowloom.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;

/**
 * 排除默认 Thymeleaf Web 自动配置；邮件 HTML 使用 {@code MailTemplateConfig} 独立引擎。
 */
@SpringBootApplication(exclude = ThymeleafAutoConfiguration.class)
@ConfigurationPropertiesScan
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }

}
