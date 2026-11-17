package com.mewrix.flowloom.engine.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 邮箱验证码校验请求。
 *
 * @param code 邮件中的验证码
 */
@Schema(description = "邮箱验证请求")
public record EmailVerifyRequest(
        @Schema(description = "6 位数字验证码", example = "123456")
        @NotBlank
        @Pattern(regexp = "^\\d{6}$")
        String code) {
}
