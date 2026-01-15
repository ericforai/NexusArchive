// Input: Spring Framework、Java 标准库
// Output: WebhookNonceStore 类
// Pos: 安全模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存级 Nonce 去重，防止重放。
 * 真实部署建议使用 Redis 带 TTL。
 */
@Component
public class WebhookNonceStore {
    private final Map<String, Long> nonceSeen = new ConcurrentHashMap<>();
    private static final long TTL_SECONDS = 600; // 10 分钟

    /**
     * @return true 表示首次出现，false 表示重复
     */
    public boolean registerIfNew(String nonce, long timestampSeconds) {
        if (nonce == null || nonce.isBlank()) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        cleanupExpired(now);

        Long existing = nonceSeen.putIfAbsent(nonce, timestampSeconds);
        return existing == null;
    }

    private void cleanupExpired(long nowSeconds) {
        nonceSeen.entrySet().removeIf(e -> nowSeconds - e.getValue() > TTL_SECONDS);
    }

    /** 仅测试使用：清空已记录的 nonce */
    public void clear() {
        nonceSeen.clear();
    }
}
