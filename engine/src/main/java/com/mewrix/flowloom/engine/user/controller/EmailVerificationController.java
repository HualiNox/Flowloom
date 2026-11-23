package com.mewrix.flowloom.engine.user.controller;

import java.util.Locale;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.common.response.ApiResponse;
import com.mewrix.flowloom.engine.common.response.ApiResponseFactory;
import com.mewrix.flowloom.engine.user.dto.request.EmailVerifyRequest;
import com.mewrix.flowloom.engine.user.service.EmailVerificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 邮箱验证接口；需登录，未验证用户由 EmailVerificationFilter 放行。 */
@RestController
@RequestMapping("/api/v1/user/email/verify")
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;
    private final ApiResponseFactory apiResponseFactory;

    @PostMapping("/send")
    public ApiResponse<Void> sendVerificationCode(
            @AuthenticationPrincipal UserPrincipal principal,
            Locale locale) {
        return apiResponseFactory.from(
                emailVerificationService.sendVerificationCode(principal, locale));
    }

    @PostMapping
    public ApiResponse<Void> verifyEmail(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody EmailVerifyRequest request) {
        return apiResponseFactory.from(
                emailVerificationService.verifyEmail(principal, request.code()));
    }
}
