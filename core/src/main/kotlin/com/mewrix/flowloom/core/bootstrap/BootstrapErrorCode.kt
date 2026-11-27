package com.mewrix.flowloom.core.bootstrap

import com.mewrix.flowloom.core.common.http.ErrorCode

/**
 * 初始化状态拦截响应中的业务码及对应的国际化消息键。
 */
enum class BootstrapErrorCode(
    private val messageKey: String?
) : ErrorCode {
    INSTANCE_NOT_INITIALIZED("bootstrap.instance_not_initialized"),
    INSTANCE_ALREADY_INITIALIZED("bootstrap.instance_already_initialized");

    override fun getCode(): String = this.name
    override fun getMessageKey(): String? = this.messageKey
}
