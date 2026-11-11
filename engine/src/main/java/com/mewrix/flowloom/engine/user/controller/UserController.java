package com.mewrix.flowloom.engine.user.controller;

import com.mewrix.flowloom.engine.common.response.ApiResponse;
import com.mewrix.flowloom.engine.common.response.ApiResponseFactory;
import com.mewrix.flowloom.engine.common.result.BusinessResult;
import com.mewrix.flowloom.engine.user.dto.request.UserRegisterRequest;
import com.mewrix.flowloom.engine.user.dto.response.UserRegisterResponse;
import com.mewrix.flowloom.engine.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 提供用户相关的 HTTP 接口。 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final ApiResponseFactory apiResponseFactory;

    /**
     * 校验注册请求并返回用户注册结果。
     *
     * @param request 用户注册信息
     * @param locale 当前请求的语言环境
     * @return 包含业务码和本地化消息的响应
     */
    @PostMapping("/register")
    public ApiResponse<UserRegisterResponse> register(
            @Valid @RequestBody UserRegisterRequest request,
            Locale locale) {

        BusinessResult<UserRegisterResponse> res = userService.register(
                request.displayName(),
                request.username(),
                request.email(),
                request.password());

        return apiResponseFactory.from(res);
    }
}
