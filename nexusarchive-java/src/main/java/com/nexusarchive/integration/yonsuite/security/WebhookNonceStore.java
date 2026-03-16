// Input: Spring Framework、Spring Data Redis、Java 标准库
// Output: WebhookNonceStore 类
// Pos: 安全模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 使用 Redis 做 webhook nonce 防重放，避免进程重启后保护失效。
 */
@Component
public class WebhookNonceStore {
    private static final Logger log = LoggerFactory.getLogger(WebhookNonceStore.class);
    private static final String KEY_PREFIX = "integration:yonsuite:webhook:nonce:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public WebhookNonceStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public enum RegisterResult {
        ACCEPTED,
        DUPLICATE,
        UNAVAILABLE
    }

    /**
     * @return ACCEPTED 表示首次出现，DUPLICATE 表示重复，UNAVAILABLE 表示存储不可用
     */
    public RegisterResult registerIfNew(String nonce, long timestampSeconds) {
        if (nonce == null || nonce.isBlank()) {
            return RegisterResult.DUPLICATE;
        }

        try {
            Boolean inserted = redisTemplate.opsForValue().setIfAbsent(buildKey(nonce), "1", TTL);
            return Boolean.TRUE.equals(inserted) ? RegisterResult.ACCEPTED : RegisterResult.DUPLICATE;
        } catch (Exception ex) {
            log.error("Failed to persist webhook nonce to Redis, rejecting request for safety: nonce={}, timestamp={}",
                    nonce, timestampSeconds, ex);
            return RegisterResult.UNAVAILABLE;
        }
    }

    private String buildKey(String nonce) {
        return KEY_PREFIX + nonce;
    }

    /** 保留测试兼容接口；Redis 模式下不做进程内清理。 */
    public void clear() {
        // no-op
    }
}
