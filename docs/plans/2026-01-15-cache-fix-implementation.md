# 缓存失效问题修复实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 修复 Redis 缓存失效问题，让所有 `@Cacheable` 注解正常工作，提升菜单加载响应速度

**Architecture:**
1. 增强 `RedisConfig.errorHandler()` 添加异常日志，诊断缓存失效原因
2. 根据日志修复具体问题（序列化/连接/代理）
3. 验证缓存生效，响应时间从 1-3 秒降低到 <300ms

**Tech Stack:** Spring Boot 3.1.6, Spring Cache, Redis (lettuce), SLF4J

---

## Task 1: 增强缓存异常日志（诊断）

**Files:**
- Modify: `nexusarchive-java/src/main/java/com/nexusarchive/config/RedisConfig.java:196-221`

**Step 1: 修改 RedisConfig 添加 logger 和异常日志**

在 `RedisConfig.java` 类中添加静态 logger：

```java
package com.nexusarchive.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ... 其他 import 保持不变

@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);  // ← 添加这行

    // ... 其他代码保持不变
```

**Step 2: 修改 errorHandler() 方法，添加详细日志**

替换 `errorHandler()` 方法（第 196-221 行）：

```java
    /**
     * 缓存异常处理器
     * 缓存异常时记录详细日志，便于诊断问题
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error("[CACHE_ERROR] Get failed - cache: {}, key: {}, error: {}",
                    cache != null ? cache.getName() : "null",
                    key,
                    exception.getMessage(),
                    exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
                log.error("[CACHE_ERROR] Put failed - cache: {}, key: {}, value type: {}, error: {}",
                    cache != null ? cache.getName() : "null",
                    key,
                    value != null ? value.getClass().getSimpleName() : "null",
                    exception.getMessage(),
                    exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
                log.error("[CACHE_ERROR] Evict failed - cache: {}, key: {}, error: {}",
                    cache != null ? cache.getName() : "null",
                    key,
                    exception.getMessage(),
                    exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
                log.error("[CACHE_ERROR] Clear failed - cache: {}, error: {}",
                    cache != null ? cache.getName() : "null",
                    exception.getMessage(),
                    exception);
            }
        };
    }
```

**Step 3: 编译验证**

```bash
cd nexusarchive-java
mvn compile -q
```

Expected: 无编译错误

**Step 4: 提交修改**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/config/RedisConfig.java
git commit -m "feat(cache): 添加缓存异常日志，便于诊断缓存失效问题"
```

---

## Task 2: 重启应用并收集诊断日志

**Step 1: 清空 Redis（可选，便于观察）**

```bash
docker exec nexus-redis redis-cli -p 6379 FLUSHDB
docker exec nexus-redis redis-cli -p 6379 DBSIZE
```

Expected: `0`

**Step 2: 停止当前运行的应用**

在运行 `mvn spring-boot:run` 的终端按 `Ctrl+C`

**Step 3: 重新启动应用**

```bash
cd nexusarchive-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

等待看到：`Started NexusArchiveBackendApplication in X.XXX seconds`

**Step 4: 触发几次页面加载**

打开浏览器，访问以下页面（需要先登录）：
- http://localhost:15175/system/menu
- http://localhost:15175/archives/list
- http://localhost:15175/dashboard

或者使用 curl：

```bash
# 获取 token（替换为实际 token）
TOKEN="your_actual_token"

# 触发菜单请求
curl -X GET "http://localhost:19090/api/system/menu" \
  -H "Authorization: Bearer $TOKEN"

# 触发档案列表请求
curl -X GET "http://localhost:19090/api/archives?page=1&limit=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Step 5: 检查日志中的 CACHE_ERROR**

```bash
# 在 Maven 控制台查找 CACHE_ERROR
# 或者如果日志输出到文件
grep -i "CACHE_ERROR" nexusarchive-java/logs/backend.log
```

Expected: 两种可能
- **有 ERROR 日志** → 记录错误信息，用于下一步修复
- **无 ERROR 日志** → 说明缓存正常写入，问题在其他地方

**Step 6: 检查 Redis keys**

```bash
docker exec nexus-redis redis-cli -p 6379 KEYS "*"
docker exec nexus-redis redis-cli -p 6379 DBSIZE
```

Expected: keys 数量 > 0

---

## Task 3: 根据日志修复具体问题

### 场景 A：序列化错误

**日志示例**：
```
CACHE_ERROR ... SerializationException
```

**Step A1: 检查缓存对象的序列化**

查看日志中 `value type` 指向的类，确保它实现了 `Serializable`：

```java
import java.io.Serializable;

public class SomeDto implements Serializable {
    private static final long serialVersionUID = 1L;
    // ...
}
```

**Step A2: 如果问题持续，修改序列化器**

在 `RedisConfig.java` 的 `defaultCacheConfig()` 方法中：

```java
private RedisCacheConfiguration defaultCacheConfig() {
    return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
}
```

确保类有无参构造函数（Jackson 反序列化需要）。

---

### 场景 B：Redis 连接超时

**日志示例**：
```
CACHE_ERROR ... RedisCommandTimeoutException
```

**Step B1: 修改 application.yml**

编辑 `nexusarchive-java/src/main/resources/application.yml`：

```yaml
spring.data.redis:
  timeout: 10000  # 从 3000ms 改为 10000ms
```

**Step B2: 重启应用验证**

重复 Task 2 的步骤

---

### 场景 C：无错误但缓存仍不生效

**可能原因**：内部方法调用绕过 AOP 代理

**Step C1: 检查缓存方法的调用方式**

搜索带有 `@Cacheable` 注解的方法：

```bash
grep -r "@Cacheable" nexusarchive-java/src/main/java --include="*.java" -A 3
```

**Step C2: 确保通过代理调用**

如果发现类似问题：

```java
// ❌ 错误：内部调用
@Service
public class MenuService {
    @Cacheable("menuCache")
    public List<Menu> getMenus() { ... }

    public void refreshMenus() {
        getMenus();  // 缓存不生效！
    }
}

// ✅ 修复：自注入
@Service
public class MenuService {
    @Autowired
    private MenuService self;  // Spring 会处理循环依赖

    @Cacheable("menuCache")
    public List<Menu> getMenus() { ... }

    public void refreshMenus() {
        self.getMenus();  // 缓存生效！
    }
}
```

---

## Task 4: 验证缓存生效

**Step 1: 清空 Redis**

```bash
docker exec nexus-redis redis-cli -p 6379 FLUSHDB
```

**Step 2: 触发第一次请求（记录时间）**

```bash
time curl -X GET "http://localhost:19090/api/system/menu" \
  -H "Authorization: Bearer $TOKEN" \
  -o /dev/null
```

记录响应时间（预期：1-3秒）

**Step 3: 检查 Redis keys**

```bash
docker exec nexus-redis redis-cli -p 6379 KEYS "menuCache*"
docker exec nexus-redis redis-cli -p 6379 DBSIZE
```

Expected: 至少有 1 个 `menuCache::` 开头的 key

**Step 4: 触发第二次请求（记录时间）**

```bash
time curl -X GET "http://localhost:19090/api/system/menu" \
  -H "Authorization: Bearer $TOKEN" \
  -o /dev/null
```

Expected: 响应时间 < 300ms

**Step 5: 检查缓存命中率**

```bash
docker exec nexus-redis redis-cli -p 6379 INFO stats | grep -E "keyspace_hits|keyspace_misses"
```

Expected: keyspace_hits 有明显增加

**Step 6: 验证成功标准**

| 指标 | 目标值 | 实际值 |
|:-----|:-------|:-------|
| Redis keys | > 0 | ___ |
| keyspace_hits | > 0 | ___ |
| 二次请求响应 | < 300ms | ___ |
| CACHE_ERROR 日志 | 无 | ___ |

---

## Task 5: 添加缓存监控（可选，P1）

**Files:**
- Create: `nexusarchive-java/src/main/java/com/nexusarchive/config/CacheMetricsConfig.java`

**Step 1: 创建缓存指标配置**

```java
package com.nexusarchive.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CacheMeterBinder;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CacheMetricsConfig {

    @Bean
    public List<CacheMeterBinder> cacheMeterBinders(CacheManager cacheManager, MeterRegistry meterRegistry) {
        return cacheManager.getCacheNames().stream()
            .map(cacheName -> new CacheMeterBinder(
                cacheManager.getCache(cacheName),
                cacheName,
                List.of("cache", cacheName)
            ) {
                @Override
                protected Long size() {
                    return null; // Redis 不支持 size()
                }

                @Override
                protected Long hitCount() {
                    return null; // 从 Redis INFO stats 获取
                }

                @Override
                protected Long missCount() {
                    return null; // 从 Redis INFO stats 获取
                }

                @Override
                protected Long evictionCount() {
                    return null; // Redis 没有这个概念
                }

                @Override
                protected Long putCount() {
                    return null;
                }

                @Override
                protected void bindImplementationSpecificMetrics(MeterRegistry registry) {
                    // 绑定 Redis 特定指标
                }
            })
            .toList();
    }
}
```

**Step 2: 提交监控配置**

```bash
git add nexusarchive-java/src/main/java/com/nexusarchive/config/CacheMetricsConfig.java
git commit -m "feat(cache): 添加缓存指标监控配置"
```

---

## 验收标准

### 功能验收

- [ ] 缓存异常时有清晰的错误日志
- [ ] 第二次打开菜单响应时间 < 300ms
- [ ] Redis 中有业务缓存 keys（不是只有 JWT 黑名单）
- [ ] 缓存命中率 > 70%

### 性能验收

| 操作 | 修复前 | 修复后 |
|:-----|:-------|:-------|
| 打开菜单（首次） | 1-3秒 | 1-3秒（无变化） |
| 打开菜单（二次） | 1-3秒 | <300ms |
| Redis keys | 1 | >10 |

---

## 回滚方案

如果修复后问题更严重：

```bash
# 回滚到修复前
git log --oneline -5  # 找到修复前的 commit
git revert <commit-hash>
# 或者
git reset --hard <commit-before-fix>

# 恢复 RedisConfig
git checkout HEAD~1 -- nexusarchive-java/src/main/java/com/nexusarchive/config/RedisConfig.java

# 重启应用
mvn spring-boot:run
```

---

## 相关文档

- 设计文档：`docs/plans/2026-01-15-cache-fix-design.md`
- 缓存策略：`docs/architecture/cache-strategy.md`
- 性能分析：`docs/plans/2026-01-15-performance-bottleneck-analysis-opec.md`

---

**计划创建时间**: 2026-01-15
**预计总工时**: 2-3 小时
**风险等级**: 低（仅添加日志，不改变业务逻辑）
