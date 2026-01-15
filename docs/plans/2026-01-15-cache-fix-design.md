# 缓存失效问题修复设计

**日期**: 2026-01-15
**状态**: 📋 设计完成
**优先级**: P0（性能问题，影响所有菜单加载）
**类型**: Bug 修复 + 性能优化

---

## 问题描述

### 当前症状

- **所有菜单打开都需要 1-3 秒**
- **第二次打开同一菜单仍然慢**（缓存未生效）
- 用户体验：明显停顿感

### 诊断发现

| 检查项 | 预期值 | 实际值 | 结论 |
|:-------|:-------|:-------|:-----|
| Redis 容器状态 | Running | ✅ Up 22 minutes | 正常 |
| Redis 连接 | 无错误 | ✅ 无连接错误 | 正常 |
| Redis keys 数量 | 50-200 | **1** | 🔴 异常 |
| 缓存命中率 | >70% | **~50%** | 🔴 过低 |

### 根因分析

**问题 1**：Redis 中只有 1 个 key（JWT 黑名单），业务缓存完全为空

**问题 2**：`RedisConfig.errorHandler()` 把所有缓存异常都**静默吞掉了**，没有任何日志：

```java
// RedisConfig.java:201-204
@Override
public void handleCacheGetError(RuntimeException exception, ...) {
    // 缓存读取失败时，直接返回 null，不影响业务
    // 生产环境应记录日志  ← 🔴 注释说应该记录，但实际没有记录！
}
```

**问题 3**：由于异常被静默吞掉，无法确定 `@Cacheable` 失败的真实原因：
- 可能是序列化问题
- 可能是 Redis 连接超时
- 可能是 AOP 代理问题
- 可能是缓存键冲突

---

## 修复方案

### 阶段 1：诊断增强（P0 - 30分钟）

**目标**：让缓存异常"可见"，找出真实失败原因

**修改文件**：`nexusarchive-java/src/main/java/com/nexusarchive/config/RedisConfig.java`

**修改内容**：

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 在类中添加 logger
private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

// 修改 errorHandler()
@Bean
@Override
public CacheErrorHandler errorHandler() {
    return new SimpleCacheErrorHandler() {
        @Override
        public void handleCacheGetError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
            log.error("[CACHE_ERROR] Get failed - cache: {}, key: {}, error: {}",
                cache != null ? cache.getName() : "null", key, exception.getMessage(), exception);
        }

        @Override
        public void handleCachePutError(RuntimeException exception, org.springframework.cache.Cache cache, Object key, Object value) {
            log.error("[CACHE_ERROR] Put failed - cache: {}, key: {}, value type: {}, error: {}",
                cache != null ? cache.getName() : "null", key,
                value != null ? value.getClass().getSimpleName() : "null",
                exception.getMessage(), exception);
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, org.springframework.cache.Cache cache, Object key) {
            log.error("[CACHE_ERROR] Evict failed - cache: {}, key: {}, error: {}",
                cache != null ? cache.getName() : "null", key, exception.getMessage(), exception);
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, org.springframework.cache.Cache cache) {
            log.error("[CACHE_ERROR] Clear failed - cache: {}, error: {}",
                cache != null ? cache.getName() : "null", exception.getMessage(), exception);
        }
    };
}
```

**验证方法**：
1. 重启应用
2. 触发几次页面加载
3. 查看日志中是否有 `CACHE_ERROR` 关键字

---

### 阶段 2：根因修复（P0 - 根据诊断结果）

#### 可能原因 A：序列化问题

**症状**：日志中出现序列化相关错误

**修复**：如果 GenericJackson2JsonRedisSerializer 有问题，考虑：
1. 检查缓存对象是否可序列化（实现 Serializable）
2. 切换到 StringRedisSerializer + JSON 手动序列化

#### 可能原因 B：Redis 连接超时

**症状**：日志中出现连接超时错误

**修复**：调整 `application.yml`：

```yaml
spring.data.redis:
  timeout: 10000  # 从 3000ms 改为 10000ms
```

#### 可能原因 C：内部方法调用绕过 AOP

**症状**：日志中没有缓存相关错误，但缓存仍不生效

**修复**：确保 `@Cacheable` 方法通过 Spring 代理调用，而非同类内部调用

```java
// ❌ 错误：内部调用，缓存不生效
@Service
public class SomeService {
    public void methodA() {
        methodB();  // 缓存不生效
    }
    @Cacheable("xxx")
    public void methodB() { ... }
}

// ✅ 正确：通过代理调用
@Service
public class SomeService {
    @Autowired
    private SomeService self;  // 注入自己
    public void methodA() {
        self.methodB();  // 缓存生效
    }
    @Cacheable("xxx")
    public void methodB() { ... }
}
```

---

### 阶段 3：验证测试（P0 - 30分钟）

**验证脚本**：

```bash
#!/bin/bash
# cache-fix-verify.sh

echo "=== 缓存修复验证 ==="

# 1. 清空 Redis
echo "📦 1. 清空 Redis..."
docker exec nexus-redis redis-cli -p 6379 FLUSHDB
docker exec nexus-redis redis-cli -p 6379 DBSIZE

# 2. 提示重启应用
echo ""
echo "🔄 2. 请重启应用:"
echo "   停止: Ctrl+C (mvn spring-boot:run)"
echo "   启动: mvn spring-boot:run"
echo ""
read -p "按 Enter 继续..."

# 3. 等待应用启动
echo "⏳ 3. 等待应用启动..."
sleep 10

# 4. 触发测试请求
echo ""
echo "📡 4. 触发测试请求..."
# 这里需要实际的 token
TOKEN="your_token_here"
curl -X GET "http://localhost:19090/api/system/menu" \
  -H "Authorization: Bearer $TOKEN" \
  -o /dev/null -w "响应时间: %{time_total}s\n"

# 5. 检查 Redis keys
echo ""
echo "📊 5. 检查 Redis keys:"
docker exec nexus-redis redis-cli -p 6379 KEYS "*"
echo "Keys 数量:"
docker exec nexus-redis redis-cli -p 6379 DBSIZE

# 6. 检查缓存命中率
echo ""
echo "📈 6. 缓存命中率:"
docker exec nexus-redis redis-cli -p 6379 INFO stats | grep -E "keyspace_hits|keyspace_misses"

# 7. 二次请求测试
echo ""
echo "📡 7. 二次请求测试 (预期 <300ms):"
curl -X GET "http://localhost:19090/api/system/menu" \
  -H "Authorization: Bearer $TOKEN" \
  -o /dev/null -w "响应时间: %{time_total}s\n"

echo ""
echo "=== 验证完成 ==="
```

**成功标准**：

| 指标 | 修复前 | 目标值 |
|:-----|:-------|:-------|
| Redis keys | 1 | > 10 |
| 缓存命中率 | ~50% | > 70% |
| 二次请求响应 | 1-3秒 | < 300ms |
| CACHE_ERROR 日志 | 无 | 无（或已修复） |

---

### 阶段 4：长期监控（P1 - 后续）

**监控指标**：

1. **缓存命中率告警**：低于 70% 触发告警
2. **缓存异常计数**：监控 `CACHE_ERROR` 日志数量
3. **Redis 连接池使用率**：确保不超过 80%
4. **Redis keys 数量**：监控缓存增长趋势

**实现方式**：使用 Spring Boot Actuator + Prometheus + Grafana

---

## 预期效果

| 指标 | 修复前 | 修复后 | 改进幅度 |
|:-----|:-------|:-------|:---------|
| Redis keys | 1 | 50-200 | +5000% |
| 缓存命中率 | 50% | 80%+ | +60% |
| 二次请求响应 | 1-3秒 | <300ms | -80% |
| 用户体验 | 明显卡顿 | 流畅 | 显著改善 |

---

## 风险与回滚

| 风险 | 概率 | 影响 | 缓解措施 |
|:-----|:-------|:-----|:---------|
| 日志过多 | 低 | 低 | 生产环境可调整为 WARN 级别 |
| 序列化不兼容 | 中 | 中 | 备份当前 RedisConfig |
| 缓存雪崩 | 低 | 高 | 设置合理的 TTL |

**回滚方案**：
```bash
git checkout HEAD -- nexusarchive-java/src/main/java/com/nexusarchive/config/RedisConfig.java
```

---

## 相关文档

- `docs/plans/2026-01-15-performance-bottleneck-analysis-opec.md` - 性能瓶颈分析
- `docs/architecture/cache-strategy.md` - 缓存策略文档
- `docs/guides/redis-cache-configuration.md` - Redis 配置指南

---

**设计完成时间**: 2026-01-15
**设计人**: AI Assistant (Claude Code)
**审核状态**: ✅ 待审核
**下一步**: 创建实施计划 → 执行修复 → 验证效果
