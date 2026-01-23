// Input: Spring Framework, Lombok, StringRedisTemplate, Jackson, Caffeine
// Output: ScanSessionServiceImpl 类 - 扫描会话服务实现
// Pos: Service Implementation Layer

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nexusarchive.service.ScanSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * 扫描会话服务实现
 *
 * <p>使用 Redis 存储会话信息，支持分布式部署。</p>
 * <p>Redis 不可用时降级到内存存储（Caffeine 带过期时间）。</p>
 *
 * <p>Redis Key 格式：scan:session:{sessionId}</p>
 * <p>Redis Value 格式：JSON {"userId":"xxx","createdAt":"2025-01-23T10:00:00Z"}</p>
 * <p>TTL：30 分钟</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScanSessionServiceImpl implements ScanSessionService {

    private final StringRedisTemplate redisTemplate;
    private static final String SESSION_PREFIX = "scan:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    /**
     * JSON 序列化器（线程安全）
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 降级用的内存存储（使用 Caffeine 带自动过期）
     */
    private final Cache<String, SessionData> fallbackSessions = Caffeine.newBuilder()
            .expireAfterWrite(SESSION_TTL)
            .maximumSize(1000)
            .build();

    @Override
    public String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        String key = SESSION_PREFIX + sessionId;

        // 构建会话数据
        SessionData sessionData = new SessionData(userId, Instant.now());

        try {
            // 存储到 Redis
            String json = sessionData.toJson();
            redisTemplate.opsForValue().set(key, json, SESSION_TTL);
            log.debug("创建扫描会话: sessionId={}, userId={}", sessionId, maskUserId(userId));
        } catch (Exception e) {
            log.warn("Redis 不可用，使用内存降级存储会话: {}", e.getMessage());
            fallbackSessions.put(sessionId, sessionData);
        }

        return sessionId;
    }

    @Override
    public boolean validateSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }

        String key = SESSION_PREFIX + sessionId;

        try {
            Boolean exists = redisTemplate.hasKey(key);
            if (exists != null && exists) {
                log.debug("会话验证通过: sessionId={}", sessionId);
                return true;
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，使用内存降级验证会话: {}", e.getMessage());
            return validateSessionFallback(sessionId);
        }

        log.debug("会话无效或已过期: sessionId={}", sessionId);
        return false;
    }

    @Override
    public String getSessionUserId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        String key = SESSION_PREFIX + sessionId;

        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return SessionData.fromJson(json).userId();
            }
        } catch (Exception e) {
            log.warn("Redis 不可用，使用内存降级获取用户ID: {}", e.getMessage());
            SessionData data = fallbackSessions.getIfPresent(sessionId);
            if (data != null) {
                return data.userId();
            }
        }

        return null;
    }

    @Override
    public void deleteSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        String key = SESSION_PREFIX + sessionId;

        try {
            redisTemplate.delete(key);
            log.debug("删除扫描会话: sessionId={}", sessionId);
        } catch (Exception e) {
            log.warn("Redis 不可用，使用内存降级删除会话: {}", e.getMessage());
            fallbackSessions.invalidate(sessionId);
        }
    }

    // ==================== 降级方法 ====================

    private boolean validateSessionFallback(String sessionId) {
        SessionData data = fallbackSessions.getIfPresent(sessionId);
        return data != null;
    }

    // ==================== 工具方法 ====================

    /**
     * 脱敏用户ID（用于日志）
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() <= 4) {
            return "***";
        }
        return userId.substring(0, 2) + "***" + userId.substring(userId.length() - 2);
    }

    // ==================== 内部数据类 ====================

    /**
     * 会话数据
     *
     * @param userId    用户ID
     * @param createdAt 创建时间
     */
    private record SessionData(String userId, Instant createdAt) {

        String toJson() {
            try {
                return OBJECT_MAPPER.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                // 降级到简单格式
                return String.format("{\"userId\":\"%s\",\"createdAt\":\"%s\"}", userId, createdAt);
            }
        }

        static SessionData fromJson(String json) {
            try {
                JsonNode node = OBJECT_MAPPER.readTree(json);
                String userId = node.get("userId").asText();
                Instant createdAt = Instant.parse(node.get("createdAt").asText());
                return new SessionData(userId, createdAt);
            } catch (Exception e) {
                log.error("解析会话JSON失败: {}", json, e);
                throw new IllegalStateException("会话数据格式错误: " + e.getMessage());
            }
        }
    }
}
