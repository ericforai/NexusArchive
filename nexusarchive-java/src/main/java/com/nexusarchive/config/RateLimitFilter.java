// Input: Jakarta EE、Lombok、Spring Framework、Java 标准库
// Output: RateLimitFilter 类
// Pos: 配置层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求频率限制过滤器
 * 安全加固 - 防止暴力破解和 DoS 攻击
 * 
 * 使用令牌桶算法实现，每个 IP 地址独立限流
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    /**
     * 每秒允许的最大请求数
     */
    @Value("${security.rate-limit.requests-per-second:20}")
    private int requestsPerSecond;

    /**
     * 令牌桶最大容量（允许短时突发）
     */
    @Value("${security.rate-limit.burst-capacity:50}")
    private int burstCapacity;

    /**
     * 是否启用限流
     */
    @Value("${security.rate-limit.enabled:true}")
    private boolean enabled;

    /**
     * 登录接口的更严格限流（每分钟最多尝试次数）
     */
    @Value("${security.rate-limit.login-attempts-per-minute:10}")
    private int loginAttemptsPerMinute;

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, LoginRateLimiter> loginLimiters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();

        // 登录接口使用更严格的限流
        if (requestUri.contains("/auth/login")) {
            if (!checkLoginRateLimit(clientIp)) {
                log.warn("🚫 登录请求被限流: IP={}", clientIp);
                sendRateLimitResponse(response, "登录尝试过于频繁，请稍后再试");
                return;
            }
        }

        // 通用请求限流
        TokenBucket bucket = buckets.computeIfAbsent(clientIp, 
            k -> new TokenBucket(requestsPerSecond, burstCapacity));
        
        if (!bucket.tryConsume()) {
            log.warn("🚫 请求被限流: IP={}, URI={}", clientIp, requestUri);
            sendRateLimitResponse(response, "请求过于频繁，请稍后再试");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 检查登录请求限流
     */
    private boolean checkLoginRateLimit(String clientIp) {
        LoginRateLimiter limiter = loginLimiters.computeIfAbsent(clientIp,
            k -> new LoginRateLimiter(loginAttemptsPerMinute));
        return limiter.tryAcquire();
    }

    /**
     * 发送限流响应
     */
    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format(
            "{\"code\":429,\"message\":\"%s\",\"success\":false}", message));
    }

    /**
     * 获取客户端真实 IP
     * 支持代理环境
     */
    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * 定期清理过期的限流器（防止内存泄漏）
     * 使用 Spring 管理的 ScheduledExecutorService 确保优雅关闭
     */
    @jakarta.annotation.PostConstruct
    public void startCleanupTask() {
        java.util.concurrent.ScheduledExecutorService scheduler = 
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "RateLimitCleanup");
                t.setDaemon(true);
                return t;
            });
        scheduler.scheduleAtFixedRate(this::cleanupExpiredBuckets, 60, 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void cleanupExpiredBuckets() {
        long now = System.currentTimeMillis();
        // 清理超过5分钟没有活动的桶
        buckets.entrySet().removeIf(entry -> 
            now - entry.getValue().getLastAccessTime() > 300000);
        loginLimiters.entrySet().removeIf(entry ->
            now - entry.getValue().getLastAccessTime() > 300000);
    }

    /**
     * 令牌桶实现
     */
    private static class TokenBucket {
        private final int refillRate;        // 每秒补充的令牌数
        private final int capacity;          // 桶容量
        private final AtomicInteger tokens;  // 当前令牌数
        private final AtomicLong lastRefillTime;
        private final AtomicLong lastAccessTime;

        public TokenBucket(int refillRate, int capacity) {
            this.refillRate = refillRate;
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = new AtomicLong(System.currentTimeMillis());
            this.lastAccessTime = new AtomicLong(System.currentTimeMillis());
        }

        public boolean tryConsume() {
            refill();
            lastAccessTime.set(System.currentTimeMillis());
            
            int currentTokens;
            do {
                currentTokens = tokens.get();
                if (currentTokens <= 0) {
                    return false;
                }
            } while (!tokens.compareAndSet(currentTokens, currentTokens - 1));
            
            return true;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long lastRefill = lastRefillTime.get();
            long elapsed = now - lastRefill;
            
            if (elapsed >= 1000) {
                int tokensToAdd = (int) (elapsed / 1000) * refillRate;
                int newTokens = Math.min(capacity, tokens.get() + tokensToAdd);
                tokens.set(newTokens);
                lastRefillTime.set(now);
            }
        }

        public long getLastAccessTime() {
            return lastAccessTime.get();
        }
    }

    /**
     * 登录请求限流器（滑动窗口）
     */
    private static class LoginRateLimiter {
        private final int maxAttempts;
        private final AtomicInteger attempts;
        private final AtomicLong windowStart;
        private final AtomicLong lastAccessTime;

        public LoginRateLimiter(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            this.attempts = new AtomicInteger(0);
            this.windowStart = new AtomicLong(System.currentTimeMillis());
            this.lastAccessTime = new AtomicLong(System.currentTimeMillis());
        }

        public boolean tryAcquire() {
            long now = System.currentTimeMillis();
            lastAccessTime.set(now);
            
            // 检查是否需要重置窗口（1分钟）
            if (now - windowStart.get() > 60000) {
                attempts.set(0);
                windowStart.set(now);
            }
            
            return attempts.incrementAndGet() <= maxAttempts;
        }

        public long getLastAccessTime() {
            return lastAccessTime.get();
        }
    }
}
