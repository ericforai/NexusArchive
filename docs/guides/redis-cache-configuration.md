# Redis 缓存配置指南

## 概述

NexusArchive 使用 Redis 作为分布式缓存，支持多命名空间与不同 TTL（过期时间）配置。

## 配置文件

### 1. Java 配置类

**位置**: `/nexusarchive-java/src/main/java/com/nexusarchive/config/RedisConfig.java`

```java
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {
    // RedisTemplate 配置
    // CacheManager 配置（多命名空间）
    // KeyGenerator 配置
    // CacheErrorHandler 配置
}
```

### 2. application.yml 配置

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:16379}
      password: ${REDIS_PASSWORD:}
      database: ${REDIS_DB:0}
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0
```

## 缓存命名空间

| 命名空间 | TTL | 用途 | 示例 |
|----------|-----|------|------|
| `permissions` | 1 小时 | 用户权限缓存 | `@Cacheable(value = "permissions", key = "#userId")` |
| `roles` | 1 小时 | 角色数据缓存 | `@Cacheable(value = "roles", key = "#roleId")` |
| `stats` | 5 分钟 | 统计数据缓存 | `@Cacheable(value = "stats", key = "'dashboard'")` |
| `fonds` | 30 分钟 | 全宗数据缓存 | `@Cacheable(value = "fonds", key = "#fondsCode")` |
| `users` | 15 分钟 | 用户信息缓存 | `@Cacheable(value = "users", key = "#userId")` |
| `loginAttempts` | 15 分钟 | 登录尝试记录 | `@Cacheable(value = "loginAttempts", key = "#username")` |
| `rateLimit` | 1 分钟 | 请求限流计数 | `@Cacheable(value = "rateLimit", key = "#ip")` |
| `archiveStats` | 10 分钟 | 归档统计 | `@Cacheable(value = "archiveStats", key = "#fondsId")` |
| `menuCache` | 1 小时 | 菜单数据 | `@Cacheable(value = "menuCache", key = "#userId")` |
| `dictCache` | 2 小时 | 数据字典 | `@Cacheable(value = "dictCache", key = "#dictType")` |
| `default` | 30 分钟 | 默认缓存 | 未指定命名空间时使用 |

## 使用方法

### 基本注解

```java
// 读取缓存
@Cacheable(value = "users", key = "#userId")
public User getUserById(Long userId) {
    return userMapper.selectById(userId);
}

// 更新缓存
@CachePut(value = "users", key = "#user.id")
public User updateUser(User user) {
    userMapper.updateById(user);
    return user;
}

// 删除缓存
@CacheEvict(value = "users", key = "#userId")
public void deleteUser(Long userId) {
    userMapper.deleteById(userId);
}

// 删除全部缓存
@CacheEvict(value = "users", allEntries = true)
public void clearAllUsers() {
    // ...
}
```

### 条件缓存

```java
// 仅当结果非空时缓存
@Cacheable(value = "users", key = "#userId", unless = "#result == null")
public User findUserById(Long userId) {
    return userMapper.selectById(userId);
}

// 仅当条件满足时缓存
@Cacheable(value = "users", key = "#userId", condition = "#userId > 0")
public User findUserById(Long userId) {
    return userMapper.selectById(userId);
}
```

### 直接使用 RedisTemplate

```java
@Autowired
private RedisTemplate<String, Object> redisTemplate;

// 设置值
redisTemplate.opsForValue().set("key", "value", Duration.ofMinutes(10));

// 获取值
Object value = redisTemplate.opsForValue().get("key");

// 删除值
redisTemplate.delete("key");
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `REDIS_HOST` | localhost | Redis 服务器地址 |
| `REDIS_PORT` | 16379 | Redis 端口（Docker 映射端口） |
| `REDIS_PASSWORD` | (空) | Redis 密码 |
| `REDIS_DB` | 0 | Redis 数据库编号 |

## Docker 启动 Redis

```bash
# 使用项目 docker-compose 启动
docker-compose -f docker-compose.infra.yml up -d redis

# 验证 Redis 运行
docker ps | grep redis
redis-cli -h localhost -p 16379 ping
```

## 验证命令

### 1. 编译验证

```bash
cd nexusarchive-java
mvn compile
```

### 2. 测试验证

```bash
mvn test -Dtest=RedisConfig*
```

### 3. Redis 连接测试

```bash
redis-cli -h localhost -p 16379
> PING
PONG
> KEYS *
> GET "stats::dashboard:StatsServiceImpl"
```

### 4. 清空所有缓存

```bash
redis-cli -h localhost -p 16379 FLUSHDB
```

## 异常处理

缓存异常不会影响业务流程。当 Redis 不可用时，系统会降级到直接查询数据库。

```java
@Bean
@Override
public CacheErrorHandler errorHandler() {
    return new SimpleCacheErrorHandler() {
        // 缓存异常时忽略，不影响业务
    };
}
```

## 序列化配置

- **Key 序列化**: StringRedisSerializer
- **Value 序列化**: GenericJackson2JsonRedisSerializer（JSON 格式）

## 注意事项

1. **缓存空值**: 默认禁用 (`disableCachingNullValues()`)，避免缓存穿透
2. **事务感知**: 启用 `transactionAware()`，确保事务一致性
3. **命名规范**: 建议使用 `cacheName::key` 格式（Spring 自动添加双冒号）
4. **缓存更新**: 数据变更时务必使用 `@CacheEvict` 清除相关缓存
