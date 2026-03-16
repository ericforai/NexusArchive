package com.nexusarchive.integration.yonsuite.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("unit")
class WebhookNonceStoreTest {

    @Test
    @DisplayName("should accept first nonce and persist it to Redis with TTL")
    void should_accept_first_nonce_and_persist_it_to_redis_with_ttl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("integration:yonsuite:webhook:nonce:nonce-1"), eq("1"), eq(Duration.ofHours(24))))
                .thenReturn(true);

        WebhookNonceStore store = new WebhookNonceStore(redisTemplate);

        assertEquals(WebhookNonceStore.RegisterResult.ACCEPTED, store.registerIfNew("nonce-1", 1_700_000_000L));
    }

    @Test
    @DisplayName("should reject duplicated nonce when Redis key already exists")
    void should_reject_duplicated_nonce_when_redis_key_already_exists() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq("1"), eq(Duration.ofHours(24)))).thenReturn(false);

        WebhookNonceStore store = new WebhookNonceStore(redisTemplate);

        assertEquals(WebhookNonceStore.RegisterResult.DUPLICATE, store.registerIfNew("nonce-1", 1_700_000_000L));
    }

    @Test
    @DisplayName("should fail closed when Redis is unavailable")
    void should_fail_closed_when_redis_is_unavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), eq("1"), eq(Duration.ofHours(24))))
                .thenThrow(new RuntimeException("redis down"));

        WebhookNonceStore store = new WebhookNonceStore(redisTemplate);

        assertEquals(WebhookNonceStore.RegisterResult.UNAVAILABLE, store.registerIfNew("nonce-1", 1_700_000_000L));
    }

    @Test
    @DisplayName("should reject blank nonce")
    void should_reject_blank_nonce() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        WebhookNonceStore store = new WebhookNonceStore(redisTemplate);

        assertEquals(WebhookNonceStore.RegisterResult.DUPLICATE, store.registerIfNew(" ", 1_700_000_000L));
    }
}
