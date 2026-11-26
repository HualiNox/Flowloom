package com.mewrix.flowloom.core.common.service

import com.mewrix.flowloom.core.common.http.ErrorCode
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service

@Service
class I18nService(
    private val messageSource: MessageSource
) {
    /** 使用当前线程绑定的 Locale 解析消息。未配置消息键或找不到对应消息时返回 null。 */
    fun getMessage(errorCode: ErrorCode, vararg args: Any = emptyArray()): String? {
        return errorCode.getMessageKey()?.let {
            messageSource
                .getMessage(
                    it,
                    args,
                    null,
                    LocaleContextHolder.getLocale()
                )
        }
    }
}
