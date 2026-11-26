package com.mewrix.core.common.service

import com.mewrix.core.common.http.ErrorCode
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Service

@Service
class I18nService(
    private val messageSource: MessageSource
) {
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