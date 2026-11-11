package com.mewrix.flowloom.engine.common.response;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import com.mewrix.flowloom.engine.common.result.BusinessResult;

import lombok.RequiredArgsConstructor;

/** 将业务结果转换为包含本地化消息的 API 响应。 */
@Component
@RequiredArgsConstructor
public class ApiResponseFactory {

    private final MessageSource messageSource;

    /**
     * 根据当前语言环境解析业务码对应的消息。
     *
     * @param result 业务处理结果
     * @param <T> 结果数据类型
     * @return 包含业务码、消息和数据的响应
     */
    public <T> ApiResponse<T> from(BusinessResult<T> result) {
        Locale locale = LocaleContextHolder.getLocale();

        String message = messageSource.getMessage(
                result.code().getMessageKey(),
                result.messageArgs() != null ? result.messageArgs().toArray() : null,
                locale);

        return new ApiResponse<>(
                result.code().getCode(),
                message,
                result.data());
    }
}
