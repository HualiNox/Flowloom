package com.mewrix.flowloom.engine.mail;

import com.mewrix.flowloom.engine.common.response.BusinessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 邮件模块业务码。 */
@Getter
@RequiredArgsConstructor
public enum MailBusinessCode implements BusinessCode {
    MAIL_DISABLED("mail.disabled"),
    MAIL_SEND_FAILED("mail.send_failed");

    private final String messageKey;

    @Override
    public String getCode() {
        return name();
    }
}
