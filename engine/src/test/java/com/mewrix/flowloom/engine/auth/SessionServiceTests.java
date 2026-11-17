package com.mewrix.flowloom.engine.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import com.mewrix.flowloom.engine.auth.principal.UserPrincipal;
import com.mewrix.flowloom.engine.auth.properties.SessionProperties;
import com.mewrix.flowloom.engine.auth.service.SessionService;
import com.mewrix.flowloom.engine.user.entity.User;
import com.mewrix.flowloom.engine.user.mapper.UserMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证会话创建、校验与吊销在 Redis 契约上的行为。 */
@ExtendWith(MockitoExtension.class)
class SessionServiceTests {
    private static final UUID USER_ID = UUID.fromString("80fdc667-e220-44d7-a36b-f582198659c7");
    private static final String KEY_PREFIX = "flowloom:session:";
    private static final String USER_SESSIONS_KEY = "flowloom:user:" + USER_ID + ":sessions";

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        sessionService = new SessionService(
                userMapper,
                new SessionProperties(Duration.ofHours(2), Duration.ofMinutes(30)),
                redisTemplate);
    }

    @Test
    void createSessionStoresHashedKeyAndUserIndex() {
        when(valueOperations.setIfAbsent(anyString(), eq(USER_ID.toString()), any(Duration.class)))
                .thenReturn(true);

        String token = sessionService.createSession(USER_ID);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(
                keyCaptor.capture(),
                eq(USER_ID.toString()),
                eq(Duration.ofHours(2)));
        assertEquals(sessionKey(token), keyCaptor.getValue());
        assertFalse(keyCaptor.getValue().contains(token));
        verify(setOperations).add(USER_SESSIONS_KEY, sessionKey(token));
    }

    @Test
    void createSessionFailsAfterThreeCollisions() {
        when(valueOperations.setIfAbsent(anyString(), eq(USER_ID.toString()), any(Duration.class)))
                .thenReturn(false);

        assertThrows(IllegalStateException.class, () -> sessionService.createSession(USER_ID));
        verify(valueOperations, times(3))
                .setIfAbsent(anyString(), eq(USER_ID.toString()), any(Duration.class));
        verify(setOperations, never()).add(anyString(), any());
    }

    @Test
    void findValidPrincipalReturnsUserWhenSessionActive() {
        when(valueOperations.setIfAbsent(anyString(), eq(USER_ID.toString()), any(Duration.class)))
                .thenReturn(true);
        String token = sessionService.createSession(USER_ID);

        when(valueOperations.get(sessionKey(token))).thenReturn(USER_ID.toString());
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(0L);

        User user = new User();
        user.setId(USER_ID);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        when(userMapper.findById(USER_ID)).thenReturn(user);

        Optional<UserPrincipal> principal = sessionService.findValidPrincipal(token);

        assertTrue(principal.isPresent());
        assertEquals(USER_ID, principal.get().userId());
        assertEquals("alice", principal.get().username());
        verify(redisTemplate).execute(
                any(RedisScript.class),
                anyList(),
                eq("5400"),
                eq("7200"));
    }

    @Test
    void findValidPrincipalIsEmptyAfterRevokeLookupMisses() {
        when(valueOperations.get(anyString())).thenReturn(null);

        Optional<UserPrincipal> principal = sessionService.findValidPrincipal("revoked-or-unknown");

        assertTrue(principal.isEmpty());
        verify(userMapper, never()).findById(any());
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), anyString(), anyString());
    }

    @Test
    void findValidPrincipalDeletesCorruptPayload() {
        when(valueOperations.get(anyString())).thenReturn("not-a-uuid");

        assertTrue(sessionService.findValidPrincipal("token").isEmpty());
        verify(redisTemplate).delete(anyString());
        verify(userMapper, never()).findById(any());
    }

    @Test
    void findValidPrincipalRemovesOrphanWhenUserMissing() {
        String key = sessionKey("orphan-token");
        when(valueOperations.get(key)).thenReturn(USER_ID.toString());
        when(userMapper.findById(USER_ID)).thenReturn(null);

        assertTrue(sessionService.findValidPrincipal("orphan-token").isEmpty());
        verify(redisTemplate).delete(key);
        verify(setOperations).remove(USER_SESSIONS_KEY, key);
        verify(redisTemplate, never()).execute(any(RedisScript.class), anyList(), anyString(), anyString());
    }

    @Test
    void findValidPrincipalIsEmptyWhenTouchReportsMissingKey() {
        when(valueOperations.get(anyString())).thenReturn(USER_ID.toString());
        when(userMapper.findById(USER_ID)).thenReturn(user("alice"));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(-2L);

        assertTrue(sessionService.findValidPrincipal("token").isEmpty());
    }

    @Test
    void findValidPrincipalIsEmptyWhenTouchReportsInvalidTtl() {
        when(valueOperations.get(anyString())).thenReturn(USER_ID.toString());
        when(userMapper.findById(USER_ID)).thenReturn(user("alice"));
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
                .thenReturn(-1L);

        assertTrue(sessionService.findValidPrincipal("token").isEmpty());
    }

    @Test
    void revokeSessionDeletesKeyAndIndexMember() {
        when(valueOperations.setIfAbsent(anyString(), eq(USER_ID.toString()), any(Duration.class)))
                .thenReturn(true);
        String token = sessionService.createSession(USER_ID);
        String key = sessionKey(token);

        when(valueOperations.get(key)).thenReturn(USER_ID.toString());

        sessionService.revokeSession(token);

        verify(redisTemplate).delete(key);
        verify(setOperations).remove(USER_SESSIONS_KEY, key);

        when(valueOperations.get(key)).thenReturn(null);
        assertTrue(sessionService.findValidPrincipal(token).isEmpty());
    }

    @Test
    void revokeAllUserSessionsDeletesIndexedKeys() {
        Set<String> keys = Set.of("flowloom:session:aaa", "flowloom:session:bbb");
        when(setOperations.members(USER_SESSIONS_KEY)).thenReturn(keys);

        sessionService.revokeAllUserSessions(USER_ID);

        verify(redisTemplate).delete(keys);
        verify(redisTemplate).delete(USER_SESSIONS_KEY);
    }

    private static User user(String username) {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        return user;
    }

    private static String sessionKey(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return KEY_PREFIX + java.util.HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
