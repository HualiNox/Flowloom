package com.mewrix.flowloom.engine.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 邮箱验证码校验请求。
 *
 * @param code 邮件中的验证码
 */
public record EmailVerifyRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{6}$")
        String code) {
}
