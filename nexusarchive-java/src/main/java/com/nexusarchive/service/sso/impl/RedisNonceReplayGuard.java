// Input: Spring Data Redis
// Output: RedisNonceReplayGuard
// Pos: SSO 服务层实现

package com.nexusarchive.service.sso.impl;

import com.nexusarchive.service.sso.NonceReplayGuard;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component("redisNonceReplayGuard")
@ConditionalOnBean(StringRedisTemplate.class)
public class RedisNonceReplayGuard implements NonceReplayGuard {

    private final StringRedisTemplate redisTemplate;

    public RedisNonceReplayGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String clientId, String nonce, long ttlSeconds) {
        String key = "erp:sso:nonce:" + clientId + ":" + nonce;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", ttlSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }
}
