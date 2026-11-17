package com.mewrix.flowloom.engine.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

import com.mewrix.flowloom.engine.auth.filter.EmailVerificationFilter;
import com.mewrix.flowloom.engine.auth.filter.SessionAuthenticationFilter;
import com.mewrix.flowloom.engine.auth.handler.RestAccessDeniedHandler;
import com.mewrix.flowloom.engine.auth.handler.RestAuthenticationEntryPoint;
import com.mewrix.flowloom.engine.auth.provider.DatabaseAuthenticationProvider;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security 配置：无状态 API + 自建会话。
 *
 * <p>
 * 不使用 Servlet HttpSession 持久化 {@code SecurityContext}；每个请求由
 * {@link SessionAuthenticationFilter} 根据 Bearer 凭证重建认证信息，
 * 随后 {@link EmailVerificationFilter} 限制未验证邮箱用户的业务范围。
 * </p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${springdoc.api-docs.enabled:false}")
    private boolean enableApiDocs;

    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean enableSwaggerUi;

    private final Environment environment;

    private boolean enableOpenApiDocs() {
        return (enableApiDocs || enableSwaggerUi) && !environment.matchesProfiles("prod");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            SessionAuthenticationFilter sessionAuthenticationFilter,
            EmailVerificationFilter emailVerificationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {
        // 禁用 Spring Security 默认的表单登录、HTTP Basic、CSRF、Logout 等功能
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable());

        // 配置无状态会话管理、异常处理、请求授权规则和自建会话过滤器
        httpSecurity
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context
                        .securityContextRepository(new NullSecurityContextRepository()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> {
                    // OpenAPI
                    if (enableOpenApiDocs()) {
                        auth
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs")
                                .permitAll();
                    }

                    // 放行非认证业务接口
                    auth
                            .requestMatchers(
                                    "/api/v1/user/register",
                                    "/api/v1/auth/login")
                            .permitAll();

                    // 其余接口需认证
                    auth
                            .anyRequest().authenticated();
                })
                // 先解析 Bearer 会话，再按邮箱验证状态限制业务接口
                .addFilterBefore(
                        sessionAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(
                        emailVerificationFilter,
                        SessionAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            DatabaseAuthenticationProvider databaseAuthenticationProvider) {
        return new ProviderManager(databaseAuthenticationProvider);
    }
}
