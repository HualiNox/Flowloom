package com.mewrix.flowloom.engine.user.response;

import com.mewrix.flowloom.engine.common.response.BusinessCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 用户模块业务码。 */
@Getter
@RequiredArgsConstructor
public enum UserBusinessCode implements BusinessCode {
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

    // 邮箱已验证，无需重复操作
    EMAIL_ALREADY_VERIFIED("user.email_already_verified"),

    // 验证码发送过于频繁（按 userId 冷却）
    EMAIL_VERIFY_TOO_FREQUENT("user.email_verify_too_frequent"),

    // 验证码错误或尝试次数耗尽
    EMAIL_VERIFY_CODE_INVALID("user.email_verify_code_invalid"),

    // 验证码不存在或已过期
    EMAIL_VERIFY_CODE_EXPIRED("user.email_verify_code_expired"),

    // 用户展示名不合法
    DISPLAY_NAME_INVALID("user.display_name_invalid"),

    // 用户密码太短
    PASSWORD_TOO_SHORT("user.password_too_short"),

    // 用户密码太长
    PASSWORD_TOO_LONG("user.password_too_long"),

    // 用户创建失败
    USER_CREATION_FAILED("user.creation_failed"),

    // 邮箱未验证：EmailVerificationFilter 拦截非白名单业务接口时返回
    EMAIL_NOT_VERIFIED("user.email_not_verified");

    private final String messageKey;

    @Override
    public String getCode() {
        return this.name();
    }
}
