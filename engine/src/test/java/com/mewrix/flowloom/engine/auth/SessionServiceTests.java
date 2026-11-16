package com.mewrix.flowloom.engine.auth;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mewrix.flowloom.engine.auth.entity.Session;
import com.mewrix.flowloom.engine.auth.mapper.SessionMapper;
import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.auth.properties.SessionProperties;
import com.mewrix.flowloom.engine.auth.service.SessionService;
import com.mewrix.flowloom.engine.user.entity.User;
import com.mewrix.flowloom.engine.user.mapper.UserMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证会话创建、校验与吊销在 Mapper 契约上的行为。 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTests {
    private static final UUID USER_ID = UUID.fromString("80fdc667-e220-44d7-a36b-f582198659c7");

    @Mock
    private SessionMapper sessionMapper;

    @Mock
    private UserMapper userMapper;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        sessionService = new SessionService(
                sessionMapper,
                userMapper,
                new SessionProperties(Duration.ofHours(2), Duration.ofMinutes(30)));
    }

    @Test
    void createSessionPersistsHashNotPlainToken() {
        when(sessionMapper.insert(any(Session.class))).thenReturn(1);

        String token = sessionService.createSession(USER_ID);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionMapper).insert(captor.capture());
        Session saved = captor.getValue();
        assertEquals(USER_ID, saved.getUserId());
        assertEquals(32, saved.getTokenHash().length);
    }

    @Test
    void findValidPrincipalReturnsUserWhenSessionActive() {
        when(sessionMapper.insert(any(Session.class))).thenReturn(1);
        String token = sessionService.createSession(USER_ID);

        Session active = new Session();
        active.setUserId(USER_ID);
        when(sessionMapper.findValidSessionByHash(any(byte[].class), any(LocalDateTime.class)))
                .thenReturn(active);

        User user = new User();
        user.setId(USER_ID);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        when(userMapper.findById(USER_ID)).thenReturn(user);

        Optional<UserPrincipal> principal = sessionService.findValidPrincipal(token);

        assertTrue(principal.isPresent());
        assertEquals(USER_ID, principal.get().userId());
        assertEquals("alice", principal.get().username());
        verify(sessionMapper).extendExpiryIfDue(any(), any(), any(), any());
    }

    @Test
    void findValidPrincipalIsEmptyAfterRevokeLookupMisses() {
        when(sessionMapper.findValidSessionByHash(any(byte[].class), any(LocalDateTime.class)))
                .thenReturn(null);

        Optional<UserPrincipal> principal = sessionService.findValidPrincipal("revoked-or-unknown");

        assertTrue(principal.isEmpty());
        verify(userMapper, never()).findById(any());
        verify(sessionMapper, never()).extendExpiryIfDue(any(), any(), any(), any());
    }

    @Test
    void revokeSessionHashesTokenBeforeUpdate() {
        when(sessionMapper.insert(any(Session.class))).thenReturn(1);
        String token = sessionService.createSession(USER_ID);

        sessionService.revokeSession(token);

        ArgumentCaptor<byte[]> hashCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(sessionMapper).revokeSession(hashCaptor.capture(), any(LocalDateTime.class));
        assertEquals(32, hashCaptor.getValue().length);

        // 再次校验时若 Mapper 按 revoked_at 过滤，应查不到
        when(sessionMapper.findValidSessionByHash(eq(hashCaptor.getValue()), any(LocalDateTime.class)))
                .thenReturn(null);
        assertTrue(sessionService.findValidPrincipal(token).isEmpty());
    }
}
