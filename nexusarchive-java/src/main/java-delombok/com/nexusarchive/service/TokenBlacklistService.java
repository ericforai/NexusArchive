// Input: SLF4J、Spring Framework、Java 标准库
// Output: TokenBlacklistService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final boolean failOpen;

    public TokenBlacklistService(
            StringRedisTemplate redisTemplate,
            @Value("${token.blacklist.fail-open:false}") boolean failOpen) {
        this.redisTemplate = redisTemplate;
        this.failOpen = failOpen;
        log.info("TokenBlacklistService initialized with fail-open={}", failOpen);
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
     * 生产环境（fail-open=false）：Redis 不可用时采取 fail-closed（视为黑名单命中）
     * 开发环境（fail-open=true）：Redis 不可用时采取 fail-open（允许令牌通过）
     */
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(getBlacklistKey(token)));
        } catch (Exception e) {
            if (failOpen) {
                log.warn("Redis unavailable for blacklist check, allowing token (dev mode): {}", e.getMessage());
                return false;  // 开发环境：允许令牌通过
            } else {
                log.error("Redis unavailable for blacklist check, rejecting token for safety: {}", e.getMessage());
                return true;   // 生产环境：拒绝令牌（安全优先）
            }
        }
    }

    private String getBlacklistKey(String token) {
        return "auth:blacklist:" + token;
    }
}
