# 数据库读写分离配置指南

## 概述

NexusArchive 支持数据库读写分离功能，通过主从复制架构提升系统读取性能和可用性。

**版本**: 2.1.0
**状态**: 可选功能（默认禁用）

## 工作原理

```
                    ┌─────────────┐
                    │   应用层     │
                    └──────┬──────┘
                           │
                 ┌─────────┴─────────┐
                 │  RoutingDataSource │
                 │  (动态路由)        │
                 └─────────┬─────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
         ┌────▼────┐              ┌────▼────┐
         │  MASTER │              │  SLAVE  │
         │  (主库)  │              │  (从库)  │
         │  写操作  │              │  读操作  │
         └─────────┘              └─────────┘
              ▲                         │
              │    主从复制              │
              └─────────────────────────┘
```

## 启用配置

### 1. 环境变量配置

```bash
# 启用读写分离
export RW_SPLIT_ENABLED=true

# 主库配置
export DB_HOST=master-db.example.com
export DB_PORT=5432
export DB_NAME=nexusarchive
export DB_USER=nexus
export DB_PASSWORD=your_password

# 从库配置（可选，默认使用主库配置）
export DB_SLAVE_HOST=slave-db.example.com
export DB_SLAVE_PORT=5432
```

### 2. application.yml 配置

```yaml
rw-split:
  enabled: ${RW_SPLIT_ENABLED:false}
  master:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:nexusarchive}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
  slaves:
    - url: jdbc:postgresql://${DB_SLAVE_HOST:${DB_HOST}}:${DB_SLAVE_PORT:54322}/${DB_NAME:nexusarchive}
      username: ${DB_USER:postgres}
      password: ${DB_PASSWORD:postgres}
      hikari:
        minimum-idle: 5
        maximum-pool-size: 30
```

## 使用方式

### 1. @ReadOnly 注解（推荐）

标记只读方法，自动路由到从库：

```java
@Service
public class StatsService {

    @ReadOnly
    public DashboardStats getDashboardStats() {
        // 此查询路由到从库
        return archiveMapper.selectCount();
    }
}
```

### 2. @Transactional 注解

```java
// 只读事务 → 从库
@Transactional(readOnly = true)
public List<Archive> findAll() {
    return archiveMapper.selectList(null);
}

// 写事务 → 主库
@Transactional
public void createArchive(Archive archive) {
    archiveMapper.insert(archive);
}

// 写后立即读（强一致性）→ 主库
@Transactional
public Archive createAndGet(Archive archive) {
    archiveMapper.insert(archive);
    return archiveMapper.selectById(archive.getId());  // 从主库读取
}
```

## 路由规则

| 注解组合 | 路由目标 |
|---------|---------|
| `@ReadOnly` | SLAVE |
| `@Transactional(readOnly=true)` | SLAVE |
| `@ReadOnly` + `@Transactional(readOnly=true)` | SLAVE |
| `@Transactional(readOnly=false)` | MASTER |
| `@ReadOnly` + `@Transactional(readOnly=false)` | MASTER（写优先）|
| 无注解 | SLAVE（默认）|

## ⚠️ 重要限制

### 1. Read-Your-Writes 一致性

写操作后立即从从库读取可能获取到旧数据（复制延迟）：

```java
// ❌ 错误：可能读到旧数据
archiveService.createArchive(archive);
int count = statsService.getTotalArchives();  // @ReadOnly → 从从库读取

// ✅ 正确：使用同一事务保证一致性
@Transactional
public void createAndCount(Archive archive) {
    archiveMapper.insert(archive);
    int count = archiveMapper.selectCount(null);  // 从主库读取
}
```

### 2. @Async 不兼容

异步方法中 `@ReadOnly` 注解无效：

```java
// ❌ 错误：异步方法中 @ReadOnly 不生效
@ReadOnly
@Async
public CompletableFuture<List<Archive>> findAllAsync() {
    // ThreadLocal 上下文不会传递到异步线程
    return CompletableFuture.completedFuture(archiveMapper.selectList(null));
}

// ✅ 正确：显式使用只读事务
@Transactional(readOnly = true)
@Async
public CompletableFuture<List<Archive>> findAllAsync() {
    return CompletableFuture.completedFuture(archiveMapper.selectList(null));
}
```

### 3. 主从复制延迟

- 典型复制延迟：10-100ms
- 在高并发场景下可能达到数秒
- 对数据一致性敏感的场景应使用 `@Transactional(readOnly=true)` 从主库读取

## 性能优化建议

### 1. 合理使用连接池

```yaml
# 主库连接池（写多）
master:
  hikari:
    minimum-idle: 10
    maximum-pool-size: 20

# 从库连接池（读多）
slaves:
  - hikari:
      minimum-idle: 20
      maximum-pool-size: 50  # 从库可以配置更大的连接池
```

### 2. 使用场景

| 场景 | 推荐方式 | 原因 |
|------|---------|------|
| 统计数据、报表 | `@ReadOnly` | 容忍延迟，减轻主库压力 |
| 列表查询 | `@ReadOnly` | 数据量大，适合从库 |
| 写后立即读 | `@Transactional` | 需要强一致性 |
| 金额计算 | `@Transactional(readOnly=true)` | 精度要求高 |

## 故障处理

### 从库宕机

当前版本不支持自动故障转移。如果从库不可用，读操作会失败。建议：

1. 配置数据库健康检查
2. 使用数据库代理（如 PgBouncer）实现自动故障转移
3. 监控从库状态，及时告警

### 主从切换

在主从切换期间：

1. 暂时禁用读写分离：`RW_SPLIT_ENABLED=false`
2. 执行主从切换
3. 确认新主从架构稳定后，重新启用读写分离

## 监控指标

建议监控以下指标：

| 指标 | 说明 | 告警阈值 |
|------|------|---------|
| 主库连接数 | 当前活跃连接 | > 80% 最大连接数 |
| 从库连接数 | 当前活跃连接 | > 80% 最大连接数 |
| 复制延迟 | 主从同步延迟 | > 1s |
| 路由命中率 | 读操作路由到从库的比例 | - |

## 参考资料

- Spring `AbstractRoutingDataSource`: [官方文档](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/spring/jdbc/datasource/lookup/AbstractRoutingDataSource.html)
- PostgreSQL 主从复制: [官方文档](https://www.postgresql.org/docs/current/high-availability.html)
