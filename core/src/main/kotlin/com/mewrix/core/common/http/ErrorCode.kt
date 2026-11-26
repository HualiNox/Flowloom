package com.mewrix.core.common.http

interface ErrorCode {
    fun getCode(): String
    fun getMessageKey(): String?
}

enum class CommonErrorCode(
    private val messageKey: String?
): ErrorCode {
    OK(null);

    override fun getCode(): String {
        return this.name
    }

    override fun getMessageKey(): String? {
        return messageKey
    }
}
