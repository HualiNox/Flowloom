package com.mewrix.flowloom.engine.auth.provider;

import java.util.List;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.user.entity.User;
import com.mewrix.flowloom.engine.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

/**
 * 基于数据库用户的用户名密码认证。 校验时读取 password_hash，成功后仅将 {@link UserPrincipal} 写入
 * Authentication。
 */
@Component
@RequiredArgsConstructor
public class DatabaseAuthenticationProvider implements AuthenticationProvider {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String usernameOrEmail = authentication.getName();
        Object credentials = authentication.getCredentials();
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || credentials == null) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String rawPassword = credentials.toString();
        User user = userMapper.findByUsername(usernameOrEmail);
        if (user == null) {
            user = userMapper.findByEmail(usernameOrEmail);
        }
        if (user == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            // 用户不存在与密码错误统一为 BadCredentials，避免泄露用户是否存在
            throw new BadCredentialsException("Invalid credentials");
        }

        // 登录认证不因邮箱未验证失败；验证状态写入 principal，由过滤器限制后续业务范围
        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getEmailVerifiedAt() != null);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
