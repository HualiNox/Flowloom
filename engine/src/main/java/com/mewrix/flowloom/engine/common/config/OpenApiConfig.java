package com.mewrix.flowloom.engine.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI / Swagger 文档配置。
 *
 * <p>
 * 默认要求 Bearer 会话凭证；登录、注册等公开接口通过
 * {@code @SecurityRequirements} 清空全局安全要求。
 * </p>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flowloom Engine API")
                        .description("""
                                Flowloom 引擎 HTTP API。

                                认证方式：登录成功后取得会话 token，后续请求在
                                `Authorization` 头携带 `Bearer <token>`。
                                业务结果统一封装在 `ApiResponse` 中（`code` / `message` / `data`）。
                                """)
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH,
                                new SecurityScheme()
                                        .name(BEARER_AUTH)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("opaque")
                                        .description("登录接口返回的会话凭证（非 JWT）")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
