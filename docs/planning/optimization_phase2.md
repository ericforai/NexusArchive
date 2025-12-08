# 第二阶段：架构优化方案

## 1. 模块重构：微服务架构改造

### 1.1 微服务划分方案

根据领域驱动设计（DDD）原则，将现有单体应用拆分为以下微服务：

1. **核心档案服务 (Archive Core Service)**
   - 档案元数据管理
   - 档案生命周期管理
   - 档号生成与分配

2. **文件存储服务 (File Storage Service)**
   - 文件上传、下载、检索
   - 文件完整性校验
   - 存储策略管理

3. **四性检测服务 (Four-Nature Check Service)**
   - 真实性、完整性、可用性、安全性检测
   - 检测报告生成与管理
   - 健康巡检调度

4. **合规性服务 (Compliance Service)**
   - 标准符合性检查
   - 审计日志管理
   - 合规报告生成

5. **认证授权服务 (Authentication & Authorization Service)**
   - 用户认证
   - 权限管理
   - 三员管理

6. **API网关 (API Gateway)**
   - 路由转发
   - 认证授权统一处理
   - 限流熔断

### 1.2 微服务通信架构

```yaml
# 新建文件：nexusarchive-java/microservices/docker-compose.yml
version: '3.8'

services:
  # API网关
  api-gateway:
    image: nexusarchive/api-gateway:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_SERVER_URL=http://eureka-server:8761
    depends_on:
      - eureka-server

  # 服务注册中心
  eureka-server:
    image: nexusarchive/eureka-server:latest
    ports:
      - "8761:8761"

  # 核心档案服务
  archive-core-service:
    image: nexusarchive/archive-core-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_SERVER_URL=http://eureka-server:8761
      - DB_HOST=archive-db
      - DB_NAME=nexusarchive_archive
    depends_on:
      - eureka-server
      - archive-db

  # 文件存储服务
  file-storage-service:
    image: nexusarchive/file-storage-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_SERVER_URL=http://eureka-server:8761
      - MINIO_ENDPOINT=minio:9000
    depends_on:
      - eureka-server
      - minio

  # 四性检测服务
  four-nature-check-service:
    image: nexusarchive/four-nature-check-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_SERVER_URL=http://eureka-server:8761
    depends_on:
      - eureka-server

  # 合规性服务
  compliance-service:
    image: nexusarchive/compliance-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_SERVER_URL=http://eureka-server:8761
      - DB_HOST=compliance-db
      - DB_NAME=nexusarchive_compliance
    depends_on:
      - eureka-server
      - compliance-db

  # 认证授权服务
  auth-service:
    image: nexusarchive/auth-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - EUREKA_SERVER_URL=http://eureka-server:8761
      - DB_HOST=auth-db
      - DB_NAME=nexusarchive_auth
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - eureka-server
      - auth-db

  # 数据库实例
  archive-db:
    image: postgres:14
    environment:
      - POSTGRES_DB=nexusarchive_archive
      - POSTGRES_USER=nexusarchive
      - POSTGRES_PASSWORD=nexusarchive
    volumes:
      - archive-db-data:/var/lib/postgresql/data
      - ./init/archive-db.sql:/docker-entrypoint-initdb.d/init.sql

  compliance-db:
    image: postgres:14
    environment:
      - POSTGRES_DB=nexusarchive_compliance
      - POSTGRES_USER=nexusarchive
      - POSTGRES_PASSWORD=nexusarchive
    volumes:
      - compliance-db-data:/var/lib/postgresql/data
      - ./init/compliance-db.sql:/docker-entrypoint-initdb.d/init.sql

  auth-db:
    image: postgres:14
    environment:
      - POSTGRES_DB=nexusarchive_auth
      - POSTGRES_USER=nexusarchive
      - POSTGRES_PASSWORD=nexusarchive
    volumes:
      - auth-db-data:/var/lib/postgresql/data
      - ./init/auth-db.sql:/docker-entrypoint-initdb.d/init.sql

  # 对象存储
  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      - MINIO_ROOT_USER=minioadmin
      - MINIO_ROOT_PASSWORD=minioadmin
    volumes:
      - minio-data:/data

volumes:
  archive-db-data:
  compliance-db-data:
  auth-db-data:
  minio-data:
```

### 1.3 API网关实现

```java
// 新建文件：nexusarchive-java/microservices/api-gateway/src/main/java/com/nexusarchive/gateway/config/GatewayConfig.java
package com.nexusarchive.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API网关路由配置
 */
@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // 核心档案服务路由
                .route("archive-core", r -> r.path("/api/archives/**", "/api/volumes/**", 
                        "/api/fonds/**", "/api/search/**")
                        .uri("lb://archive-core-service"))
                
                // 文件存储服务路由
                .route("file-storage", r -> r.path("/api/files/**", "/api/storage/**")
                        .filters(f -> f.filter(authGatewayFilter))
                        .uri("lb://file-storage-service"))
                
                // 四性检测服务路由
                .route("four-nature-check", r -> r.path("/api/four-nature/**", "/api/health/**")
                        .uri("lb://four-nature-check-service"))
                
                // 合规性服务路由
                .route("compliance", r -> r.path("/api/compliance/**", "/api/audit/**")
                        .uri("lb://compliance-service"))
                
                // 认证授权服务路由
                .route("auth", r -> r.path("/api/auth/**", "/api/users/**", "/api/roles/**", 
                        "/api/permissions/**", "/api/orgs/**")
                        .uri("lb://auth-service"))
                
                .build();
    }
}
```

## 2. 安全加固：财务数据加密和审计追踪

### 2.1 字段级加密实现

```java
// 新建文件：nexusarchive-java/microservices/archive-core-service/src/main/java/com/nexusarchive/core/util/FieldEncryptionUtil.java
package com.nexusarchive.core.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.Security;
import java.util.Base64;

/**
 * 敏感字段加密工具
 * 支持金额、账户等敏感信息的字段级加密
 */
@Component
public class FieldEncryptionUtil {
    
    private static final String ALGORITHM = "SM4";
    private static final String TRANSFORMATION = "SM4/ECB/PKCS5Padding";
    
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * 加密敏感字段
     * @param plaintext 明文
     * @param secretKey 加密密钥
     * @return 加密后的Base64字符串
     */
    public String encrypt(String plaintext, String secretKey) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        
        try {
            Key key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
            
        } catch (Exception e) {
            throw new RuntimeException("字段加密失败", e);
        }
    }
    
    /**
     * 解密敏感字段
     * @param ciphertext 加密后的Base64字符串
     * @param secretKey 解密密钥
     * @return 解密后的明文
     */
    public String decrypt(String ciphertext, String secretKey) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        
        try {
            Key key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key);
            
            byte[] encrypted = Base64.getDecoder().decode(ciphertext);
            byte[] decrypted = cipher.doFinal(encrypted);
            
            return new String(decrypted, StandardCharsets.UTF_8);
            
        } catch (Exception e) {
            throw new RuntimeException("字段解密失败", e);
        }
    }
    
    /**
     * 判断字符串是否已加密
     * 简单判断：如果Base64解码成功，则可能是加密的
     * @param text 待判断的字符串
     * @return 是否可能是加密后的字符串
     */
    public boolean isEncrypted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        try {
            Base64.getDecoder().decode(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
```

### 2.2 审计追踪增强

```java
// 新建文件：nexusarchive-java/microservices/compliance-service/src/main/java/com/nexusarchive/compliance/aspect/AuditTrailAspect.java
package com.nexusarchive.compliance.aspect;

import com.nexusarchive.compliance.entity.AuditTrailLog;
import com.nexusarchive.compliance.service.AuditTrailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计追踪切面
 * 记录所有关键操作，满足《会计档案管理办法》审计要求
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditTrailAspect {
    
    private final AuditTrailService auditTrailService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 记录档案操作审计日志
     */
    @Around("@annotation(com.nexusarchive.compliance.annotation.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String operationName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        // 获取用户信息
        String userId = getCurrentUserId();
        String userName = getCurrentUserName();
        String clientIp = getClientIp(request);
        
        // 记录操作前状态
        Object[] args = joinPoint.getArgs();
        String beforeState = captureBeforeState(args);
        
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 记录操作后状态
            String afterState = captureAfterState(result);
            
            // 计算执行时间
            long executionTime = System.currentTimeMillis() - startTime;
            
            // 创建审计日志
            AuditTrailLog auditLog = AuditTrailLog.builder()
                    .className(className)
                    .methodName(operationName)
                    .userId(userId)
                    .userName(userName)
                    .clientIp(clientIp)
                    .clientInfo(request != null ? request.getHeader("User-Agent") : null)
                    .operationTime(LocalDateTime.now())
                    .executionTime(executionTime)
                    .beforeState(beforeState)
                    .afterState(afterState)
                    .status("SUCCESS")
                    .build();
            
            // 保存审计日志
            auditTrailService.saveAuditLog(auditLog);
            
            return result;
            
        } catch (Exception e) {
            // 记录失败的操作
            AuditTrailLog auditLog = AuditTrailLog.builder()
                    .className(className)
                    .methodName(operationName)
                    .userId(userId)
                    .userName(userName)
                    .clientIp(clientIp)
                    .clientInfo(request != null ? request.getHeader("User-Agent") : null)
                    .operationTime(LocalDateTime.now())
                    .executionTime(System.currentTimeMillis() - startTime)
                    .beforeState(beforeState)
                    .afterState(null)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build();
            
            // 保存审计日志
            auditTrailService.saveAuditLog(auditLog);
            
            throw e;
        }
    }
    
    /**
     * 捕获操作前状态
     */
    private String captureBeforeState(Object[] args) {
        try {
            if (args == null || args.length == 0) {
                return null;
            }
            
            // 过滤敏感信息
            Map<String, Object> state = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                String paramName = "param" + i;
                Object value = args[i];
                
                // 敏感字段过滤
                if (value instanceof String && isSensitiveField(paramName)) {
                    state.put(paramName, "***");
                } else {
                    state.put(paramName, value);
                }
            }
            
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            log.warn("Failed to capture before state", e);
            return "ERROR: " + e.getMessage();
        }
    }
    
    /**
     * 捕获操作后状态
     */
    private String captureAfterState(Object result) {
        try {
            if (result == null) {
                return null;
            }
            
            // 过滤敏感信息
            if (result instanceof String) {
                return "***";
            }
            
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Failed to capture after state", e);
            return "ERROR: " + e.getMessage();
        }
    }
    
    /**
     * 判断是否为敏感字段
     */
    private boolean isSensitiveField(String paramName) {
        return paramName.toLowerCase().contains("password") || 
               paramName.toLowerCase().contains("secret") ||
               paramName.toLowerCase().contains("key");
    }
    
    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        // 从安全上下文获取用户ID
        try {
            return org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getName();
        } catch (Exception e) {
            return "SYSTEM";
        }
    }
    
    /**
     * 获取当前用户名
     */
    private String getCurrentUserName() {
        try {
            org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                return ((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()).getUsername();
            }
        } catch (Exception e) {
            log.debug("Failed to get username", e);
        }
        return "SYSTEM";
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
```

## 3. 性能提升：数据库查询优化和缓存机制

### 3.1 数据库查询优化

```sql
-- 新建文件：nexusarchive-java/microservices/archive-core-service/src/main/resources/db/migration/V13__optimize_archive_queries.sql

-- 为档案表添加复合索引
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_archive_fonds_year 
ON acc_archive(fonds_no, fiscal_year, status);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_archive_category_period 
ON acc_archive(category_code, fiscal_period, status);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_archive_created_by_status 
ON acc_archive(created_by, status);

-- 为文件表添加复合索引
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_file_content_item_id 
ON arc_file_content(item_id, file_type);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_file_content_hash 
ON arc_file_content(file_hash);

-- 为审计表添加分区表（按月分区）
CREATE TABLE IF NOT EXISTS audit_trail_log_y2024m01 PARTITION OF audit_trail_log
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE IF NOT EXISTS audit_trail_log_y2024m02 PARTITION OF audit_trail_log
FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- 创建物化视图，优化常用统计查询
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_archive_stats AS
SELECT 
    fonds_no,
    fiscal_year,
    category_code,
    COUNT(*) AS total_count,
    COUNT(CASE WHEN status = 'archived' THEN 1 END) AS archived_count,
    COUNT(CASE WHEN status = 'pending' THEN 1 END) AS pending_count,
    SUM(amount) AS total_amount
FROM acc_archive
WHERE deleted = 0
GROUP BY fonds_no, fiscal_year, category_code;

-- 创建唯一索引，确保物化视图可被刷新
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_archive_stats_unique 
ON mv_archive_stats(fonds_no, fiscal_year, category_code);

-- 创建定时刷新物化视图的函数
CREATE OR REPLACE FUNCTION refresh_archive_stats()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY mv_archive_stats;
END;
$$ LANGUAGE plpgsql;
```

### 3.2 Redis缓存配置

```java
// 新建文件：nexusarchive-java/microservices/archive-core-service/src/main/java/com/nexusarchive/core/config/RedisConfig.java
package com.nexusarchive.core.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis缓存配置
 */
@Configuration
@EnableCaching
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        om.registerModule(new JavaTimeModule());
        jackson2JsonRedisSerializer.setObjectMapper(om);
        
        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        template.setKeySerializer(stringRedisSerializer);
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        
        return template;
    }
    
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 配置序列化
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jackson2JsonRedisSerializer()));
        
        // 配置不同缓存项的过期时间
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 档案元数据缓存 - 1小时
        cacheConfigurations.put("archives", config.entryTtl(Duration.ofHours(1)));
        
        // 用户信息缓存 - 30分钟
        cacheConfigurations.put("users", config.entryTtl(Duration.ofMinutes(30)));
        
        // 组织结构缓存 - 6小时
        cacheConfigurations.put("orgs", config.entryTtl(Duration.ofHours(6)));
        
        // 全宗信息缓存 - 12小时
        cacheConfigurations.put("fonds", config.entryTtl(Duration.ofHours(12)));
        
        // 统计数据缓存 - 5分钟
        cacheConfigurations.put("stats", config.entryTtl(Duration.ofMinutes(5)));
        
        // 权限信息缓存 - 1小时
        cacheConfigurations.put("permissions", config.entryTtl(Duration.ofHours(1)));
        
        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
        
        return cacheManager;
    }
    
    @Bean
    public Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer() {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        om.registerModule(new JavaTimeModule());
        jackson2JsonRedisSerializer.setObjectMapper(om);
        return jackson2JsonRedisSerializer;
    }
}
```

### 3.3 缓存注解使用示例

```java
// 修改文件：nexusarchive-java/microservices/archive-core-service/src/main/java/com/nexusarchive/core/service/ArchiveService.java
package com.nexusarchive.core.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/**
 * 档案服务 - 缓存应用示例
 */
@Service
@RequiredArgsConstructor
public class ArchiveService {
    
    private final ArchiveMapper archiveMapper;
    
    /**
     * 根据ID获取档案 - 使用缓存
     */
    @Cacheable(value = "archives", key = "#id")
    public Archive getArchiveById(String id) {
        return archiveMapper.selectById(id);
    }
    
    /**
     * 根据档号获取档案 - 使用缓存
     */
    @Cacheable(value = "archives", key = "'code:' + #archiveCode")
    public Archive getArchiveByCode(String archiveCode) {
        return archiveMapper.selectOne(
            new LambdaQueryWrapper<Archive>()
                .eq(Archive::getArchiveCode, archiveCode)
        );
    }
    
    /**
     * 更新档案 - 清除缓存
     */
    @Caching(evict = {
        @CacheEvict(value = "archives", key = "#archive.id"),
        @CacheEvict(value = "archives", key = "'code:' + #archive.archiveCode"),
        @CacheEvict(value = "stats", allEntries = true)  // 清除统计数据缓存
    })
    public boolean updateArchive(Archive archive) {
        return archiveMapper.updateById(archive) > 0;
    }
    
    /**
     * 删除档案 - 清除缓存
     */
    @Caching(evict = {
        @CacheEvict(value = "archives", key = "#id"),
        @CacheEvict(value = "stats", allEntries = true)  // 清除统计数据缓存
    })
    public boolean deleteArchive(String id) {
        return archiveMapper.deleteById(id) > 0;
    }
    
    /**
     * 获取档案统计 - 使用短期缓存
     */
    @Cacheable(value = "stats", key = "'fonds:' + #fondsNo + ':year:' + #year")
    public Map<String, Object> getArchiveStats(String fondsNo, String year) {
        // 查询数据库获取统计数据
        // ...
    }
}
```

## 实施计划

1. **第1-5天**：微服务架构设计与基础设施搭建
2. **第6-10天**：核心档案服务与文件存储服务拆分
3. **第11-15天**：安全加固与字段级加密实现
4. **第16-21天**：数据库优化与缓存机制实施

## 预期效果

1. 微服务架构提高了系统扩展性和可维护性
2. 字段级加密保障敏感财务数据安全
3. 增强的审计追踪满足合规要求
4. 数据库优化和缓存机制显著提升系统性能

## 风险控制措施

1. 微服务拆分采用渐进式重构，保证系统稳定运行
2. 实施完善的监控和日志系统，及时发现和解决问题
3. 加密实现支持密钥轮换，提高安全性
4. 缓存机制提供降级策略，避免因缓存故障导致系统不可用