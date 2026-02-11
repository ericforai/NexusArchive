// Input: Java 并发容器、Spring
// Output: InMemoryNonceReplayGuard
// Pos: SSO 服务层实现

package com.nexusarchive.service.sso.impl;

import com.nexusarchive.service.sso.NonceReplayGuard;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Primary
public class InMemoryNonceReplayGuard implements NonceReplayGuard {

    private final Map<String, Long> nonceExpireMap = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(String clientId, String nonce, long ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        nonceExpireMap.entrySet().removeIf(entry -> entry.getValue() <= now);
        String key = clientId + ":" + nonce;
        Long existing = nonceExpireMap.putIfAbsent(key, now + ttlSeconds);
        return existing == null;
    }
}
