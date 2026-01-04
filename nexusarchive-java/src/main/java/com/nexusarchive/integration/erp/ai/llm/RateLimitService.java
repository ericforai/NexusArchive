// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/llm/RateLimitService.java
package com.nexusarchive.integration.erp.ai.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.currentTimeMillis;

@Slf4j
@Service
public class RateLimitService {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final int MINUTE_IN_MS = 60000;

    private final AtomicInteger requestCount = new AtomicInteger(0);
    private long windowStartTimestamp = currentTimeMillis();

    /**
     * 检查是否允许发起请求
     */
    public synchronized boolean tryAcquire() {
        long now = currentTimeMillis();

        // 重置时间窗口
        if (now - windowStartTimestamp >= MINUTE_IN_MS) {
            windowStartTimestamp = now;
            requestCount.set(0);
        }

        // 检查限制
        if (requestCount.get() >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded: {} requests/min", requestCount.get());
            return false;
        }

        requestCount.incrementAndGet();
        return true;
    }

    /**
     * 获取当前可用请求数
     */
    public synchronized int getAvailablePermits() {
        return MAX_REQUESTS_PER_MINUTE - requestCount.get();
    }
}
