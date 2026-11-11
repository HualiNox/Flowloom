package com.mewrix.flowloom.engine.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 业务处理结果的代码及其对应的国际化消息键。 */
@Getter
@RequiredArgsConstructor
public enum BusinessCode {
    OK("common.ok"),

    // === 用户相关业务码 ===

    // 用户名太短
    USERNAME_TOO_SHORT("user.username_too_short"),

    // 用户名太长
    USERNAME_TOO_LONG("user.username_too_long"),

    // 用户名存在非法字符
    USERNAME_HAS_ILLEGAL_CHARACTERS("user.username_has_illegal_characters"),

    // 用户名应以字母开头
    USERNAME_MUST_START_WITH_LETTER("user.username_must_start_with_letter"),

    // 用户名已存在
    USERNAME_ALREADY_EXISTS("user.username_already_exists"),

    // 用户邮箱不合法
    EMAIL_INVALID("user.email_invalid"),

    // 邮箱已存在
    EMAIL_ALREADY_EXISTS("user.email_already_exists"),

    // 用户展示名不合法
    DISPLAY_NAME_INVALID("user.display_name_invalid"),

    // 用户密码太短
    PASSWORD_TOO_SHORT("user.password_too_short"),

    // 用户密码太长
    PASSWORD_TOO_LONG("user.password_too_long"),

    // 用户创建失败
    USER_CREATION_FAILED("user.creation_failed");

    // === 用户相关业务码 END ===

    // 业务码对应的消息 Key
    private final String messageKey;

    /**
     * 获取业务码的字符串表示形式
     *
     * @return 业务码的字符串表示形式
     */
    public String getCode() {
        return this.name();
    }
}
