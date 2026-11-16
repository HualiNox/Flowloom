package com.mewrix.flowloom.engine.auth;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

import com.mewrix.flowloom.engine.auth.config.SecurityConfig;
import com.mewrix.flowloom.engine.auth.filter.SessionAuthenticationFilter;
import com.mewrix.flowloom.engine.auth.handler.RestAccessDeniedHandler;
import com.mewrix.flowloom.engine.auth.handler.RestAuthenticationEntryPoint;
import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.auth.provider.DatabaseAuthenticationProvider;
import com.mewrix.flowloom.engine.auth.service.SessionService;
import com.mewrix.flowloom.engine.user.config.PasswordConfig;
import com.mewrix.flowloom.engine.user.entity.User;
import com.mewrix.flowloom.engine.user.mapper.UserMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证真实认证组件的装配与密码校验，用户查询使用内存桩。 */
@SpringJUnitWebConfig(classes = {
        SecurityConfig.class,
        PasswordConfig.class,
        DatabaseAuthenticationProvider.class,
        SessionAuthenticationFilter.class,
        AuthenticationTests.MapperConfig.class
})
class AuthenticationTests {
    private static final UUID USER_ID = UUID.fromString("80fdc667-e220-44d7-a36b-f582198659c7");
    private static final String PASSWORD = "correct-password";

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void onlyOnePasswordEncoderIsRegistered() {
        assertEquals(1, applicationContext.getBeansOfType(PasswordEncoder.class).size());
    }

    @Test
    void validPasswordAuthenticatesWithoutExposingPasswordHash() {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("alice", PASSWORD));

        assertTrue(authentication.isAuthenticated());
        UserPrincipal principal = assertInstanceOf(UserPrincipal.class, authentication.getPrincipal());
        assertEquals(USER_ID, principal.userId());
        assertEquals("alice", principal.username());
        assertEquals("alice@example.com", principal.email());
        assertNull(authentication.getCredentials());
    }

    @Test
    void emailLoginAuthenticates() {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("alice@example.com", PASSWORD));

        UserPrincipal principal = assertInstanceOf(UserPrincipal.class, authentication.getPrincipal());
        assertEquals(USER_ID, principal.userId());
    }

    @Test
    void wrongPasswordIsRejected() {
        assertThrows(BadCredentialsException.class, () -> authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("alice", "wrong-password")));
    }

    @Test
    void unknownUserIsRejectedAsBadCredentials() {
        assertThrows(BadCredentialsException.class, () -> authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("missing", PASSWORD)));
    }

    @Configuration
    static class MapperConfig {
        @Bean
        UserMapper userMapper(PasswordEncoder passwordEncoder) {
            String passwordHash = passwordEncoder.encode(PASSWORD);
            return new UserMapper() {
                @Override
                public User findById(UUID id) {
                    if (!USER_ID.equals(id)) {
                        return null;
                    }
                    return alice(passwordHash);
                }

                @Override
                public User findByUsername(String username) {
                    if (!"alice".equals(username)) {
                        return null;
                    }
                    return alice(passwordHash);
                }

                @Override
                public User findByEmail(String email) {
                    if (!"alice@example.com".equals(email)) {
                        return null;
                    }
                    return alice(passwordHash);
                }

                @Override
                public int insertUser(User user) {
                    throw new UnsupportedOperationException();
                }

                private static User alice(String passwordHash) {
                    User user = new User();
                    user.setId(USER_ID);
                    user.setUsername("alice");
                    user.setEmail("alice@example.com");
                    user.setPasswordHash(passwordHash);
                    return user;
                }
            };
        }

        /** 认证装配测试不依赖真实会话存储。 */
        @Bean
        SessionService sessionService() {
            return Mockito.mock(SessionService.class);
        }

        @Bean
        RestAuthenticationEntryPoint restAuthenticationEntryPoint() {
            return Mockito.mock(RestAuthenticationEntryPoint.class);
        }

        @Bean
        RestAccessDeniedHandler restAccessDeniedHandler() {
            return Mockito.mock(RestAccessDeniedHandler.class);
        }
    }
}
