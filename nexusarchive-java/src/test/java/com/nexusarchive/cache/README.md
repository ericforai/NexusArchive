# Redis 缓存功能验证测试

## 概述

本目录包含 Redis 缓存功能的完整测试套件，验证 Spring Cache + Redis 的集成是否正常工作。

## 文件结构

```
src/test/java/com/nexusarchive/cache/
├── CacheIntegrationTest.java      # 完整的缓存集成测试（需要 Redis）
├── CacheAnnotationTest.java       # 缓存注解单元测试
├── CacheMonitor.java              # 缓存监控工具类
├── EmbeddedRedisConfig.java       # 内嵌 Redis 测试配置
├── fixture/
│   └── CachedRoleService.java    # 带缓存注解的测试服务
└── README.md                      # 本文档
```

## 测试场景

### 1. 缓存写入验证 (@Cacheable)

```java
@Cacheable(key = "'all'")
public List<Role> getAllRoles() {
    return roleMapper.selectList(null);
}
```

**验证点：**
- 首次调用查询数据库
- 第二次调用从缓存读取
- 数据库只被调用一次

### 2. 缓存命中验证

```java
// 第一次调用 - 查询数据库
List<Role> firstCall = cachedRoleService.getAllRoles();
verify(roleMapper, times(1)).selectList(any());

// 第二次调用 - 应该从缓存读取
List<Role> secondCall = cachedRoleService.getAllRoles();
verify(roleMapper, times(1)).selectList(any()); // 仍然是 1 次
```

### 3. 缓存清除验证 (@CacheEvict)

```java
@CacheEvict(allEntries = true)
public void clearAllRolesCache() {
    // 只清除缓存，不执行数据库操作
}
```

**验证点：**
- 清除后再次调用应重新查询数据库

### 4. 缓存更新验证 (@CachePut)

```java
@CachePut(value = "role", key = "#id")
public Role refreshRole(String id, Role role) {
    roleMapper.updateById(role);
    return roleMapper.selectById(id);
}
```

**验证点：**
- 更新后缓存应包含新值

### 5. TTL 过期验证

在 `RedisConfig.java` 中配置的默认 TTL：
```java
.entryTtl(Duration.ofMinutes(30))
```

### 6. 多命名空间隔离

```java
@Cacheable(value = "roles", key = "'all'")
public List<Role> getAllRoles() { ... }

@Cacheable(value = "role", key = "#id")
public Role getRoleById(String id) { ... }
```

**验证点：**
- 不同缓存命名空间互不影响

## 运行测试

### 方式 1：单元测试（无需 Redis）

```bash
mvn test -Dtest=CacheAnnotationTest
```

### 方式 2：集成测试（需要 Redis）

启动 Redis：
```bash
docker-compose -f docker-compose.infra.yml up -d
```

运行测试：
```bash
mvn test -Dtest=CacheIntegrationTest
```

### 方式 3：使用内嵌 Redis

```bash
export TEST_REDIS_EMBEDDED=true
mvn test -Dtest=CacheIntegrationTest
```

## 缓存监控工具

`CacheMonitor` 提供缓存使用统计：

```java
CacheMonitor monitor = new CacheMonitor(cacheManager);
monitor.recordAccess("roles", true);  // 记录缓存命中
monitor.recordAccess("roles", false); // 记录缓存未命中
monitor.printReport();  // 打印统计报告
```

### 统计信息

- **命中率** (Hit Rate): 缓存命中次数 / 总访问次数
- **访问次数** (Total Access): 命中次数 + 未命中次数
- **时间跨度** (Duration): 首次访问到末次访问的时间

## Redis 配置

测试环境配置（`application-test.yml`）：

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

## 注意事项

1. **macOS ARM (Apple Silicon)**: 内嵌 Redis 可能需要额外配置
2. **端口冲突**: 默认使用 6379 端口，确保 Redis 已启动
3. **TTL 测试**: 需要等待 30 分钟才能验证过期，或修改配置缩短时间

## 扩展测试

### 添加新的缓存测试

1. 在 `CachedRoleService` 中添加带缓存注解的方法
2. 在 `CacheAnnotationTest` 中添加注解验证
3. 在 `CacheIntegrationTest` 中添加行为验证

### 示例：条件缓存

```java
// 只缓存系统角色
@Cacheable(value = "systemRoles", key = "#id", condition = "#type == 'system'")
public Role getRoleByTypeIfSystem(String id, String type) {
    return roleMapper.selectById(id);
}
```

## 相关文件

- `src/main/java/com/nexusarchive/config/RedisConfig.java` - Redis 缓存配置
- `src/main/java/com/nexusarchive/service/RoleService.java` - 实际角色服务（未使用缓存）
- `pom.xml` - Maven 依赖配置（含 embedded-redis）
