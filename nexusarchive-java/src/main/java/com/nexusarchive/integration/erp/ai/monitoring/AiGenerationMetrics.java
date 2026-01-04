// nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/ai/monitoring/AiGenerationMetrics.java
package com.nexusarchive.integration.erp.ai.monitoring;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AiGenerationMetrics {

    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicInteger totalTokensUsed = new AtomicInteger(0);
    private final AtomicLong totalResponseTimeMs = new AtomicLong(0);

    public void recordRequest(boolean success, int tokensUsed, long responseTimeMs) {
        totalRequests.incrementAndGet();
        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        totalTokensUsed.addAndGet(tokensUsed);
        totalResponseTimeMs.addAndGet(responseTimeMs);
    }

    public MetricsSnapshot getSnapshot() {
        int total = totalRequests.get();
        return new MetricsSnapshot(
            total,
            successfulRequests.get(),
            failedRequests.get(),
            totalTokensUsed.get(),
            total > 0 ? totalResponseTimeMs.get() / total : 0
        );
    }

    @Data
    public static class MetricsSnapshot {
        private final int totalRequests;
        private final int successfulRequests;
        private final int failedRequests;
        private final int totalTokensUsed;
        private final double averageResponseTimeMs;
    }
}
