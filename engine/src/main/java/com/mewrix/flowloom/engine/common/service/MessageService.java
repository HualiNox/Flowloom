package com.mewrix.flowloom.engine.common.service;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * MessageService类用于处理与国际化消息相关的业务逻辑。
 *
 * <p>
 * 该类依赖于Spring的MessageSource对象，用于获取不同语言环境下
 * 的国际化消息。通过提供业务码和语言环境，MessageService可以返回对应的国际化消息字符串。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    /**
     * Spring的MessageSource对象，用于获取国际化消息
     */
    private final MessageSource messageSource;

    /**
     * 获取指定业务码对应的国际化消息
     *
     * @param code   业务码
     * @param locale 语言环境
     * @return 国际化消息
     */
    public String getMessage(String code, Locale locale) {
        return messageSource.getMessage(code, null, locale);
    }
}
