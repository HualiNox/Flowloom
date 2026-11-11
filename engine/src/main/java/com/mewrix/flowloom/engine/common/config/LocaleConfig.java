package com.mewrix.flowloom.engine.common.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/** 配置请求消息使用的语言环境。 */
@Configuration
public class LocaleConfig {

    /**
     * 根据 Accept-Language 解析语言，支持英文和简体中文，默认使用英文。
     *
     * @return 请求语言环境解析器
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();

        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(
                Locale.ENGLISH,
                Locale.SIMPLIFIED_CHINESE));

        return resolver;
    }
}
