package com.mewrix.flowloom.engine.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;

import com.mewrix.flowloom.engine.auth.filter.SessionAuthenticationFilter;
import com.mewrix.flowloom.engine.auth.handler.RestAccessDeniedHandler;
import com.mewrix.flowloom.engine.auth.handler.RestAuthenticationEntryPoint;
import com.mewrix.flowloom.engine.auth.provider.DatabaseAuthenticationProvider;

/**
 * Spring Security 配置：无状态 API + 自建会话。
 *
 * <p>
 * 不使用 Servlet HttpSession 持久化 {@code SecurityContext}；每个请求由
 * {@link SessionAuthenticationFilter} 根据 Bearer 凭证重建认证信息。
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            SessionAuthenticationFilter sessionAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {
        httpSecurity
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context
                        .securityContextRepository(new NullSecurityContextRepository()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/user/register",
                                "/api/v1/auth/login")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(
                        sessionAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            DatabaseAuthenticationProvider databaseAuthenticationProvider) {
        return new ProviderManager(databaseAuthenticationProvider);
    }
}
