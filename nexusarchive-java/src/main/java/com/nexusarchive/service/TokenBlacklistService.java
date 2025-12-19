package com.nexusarchive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token Blacklist Service (Redis Implementation)
 * <p>
 * Replaced memory map with Redis for production readiness.
 * Gracefully degrades if Redis is unavailable.
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    
    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * blacklist
     */
    public void blacklist(String token, long expiresAt) {
        try {
            long ttl = expiresAt - System.currentTimeMillis();
            if (ttl > 0) {
                redisTemplate.opsForValue().set(getBlacklistKey(token), "blacklisted", ttl, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Redis不可用时降级处理，记录日志但不抛出异常
            log.warn("Redis unavailable for token blacklist, gracefully degrading: {}", e.getMessage());
        }
    }

    /**
     * isBlacklisted
     * 安全加固：Redis 不可用时采取 fail-closed（视为黑名单命中）
     */
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(getBlacklistKey(token)));
        } catch (Exception e) {
            // 安全加固：Redis 异常时采取 fail-closed，避免吊销失效
            log.error("Redis unavailable for blacklist check, rejecting token for safety: {}", e.getMessage());
            return true;
        }
    }

    private String getBlacklistKey(String token) {
        return "auth:blacklist:" + token;
    }
}
