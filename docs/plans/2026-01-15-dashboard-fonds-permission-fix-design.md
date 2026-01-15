# Dashboard 全宗权限修复设计文档

**创建日期**: 2026-01-15
**状态**: 设计中
**优先级**: P0 - 安全问题

---

## 一、问题概述

### 1.1 问题描述

`/system` 门户首页（Dashboard）展示的部分统计数据没有正确应用全宗权限过滤，导致：

1. **数据不准确** - 用户看到不属于其权限全宗的数据
2. **缓存泄漏风险** - 缓存 Key 不包含全宗信息，可能导致跨用户数据泄漏

### 1.2 影响范围

| 指标 | API 端点 | 问题 | 风险等级 |
|------|----------|------|----------|
| 待处理任务 | `/stats/dashboard` | 统计所有全宗的任务 | 🔴 高 |
| 待办通知 | `/notifications` | 显示所有全宗的数据 | 🔴 高 |
| 缓存 Key | 所有统计 API | 不包含全宗信息 | 🔴 高 |
| 存储占用 | `/stats/dashboard` | 全局统计（可能故意设计） | 🟡 低 |

---

## 二、根因分析

### 2.1 代码位置

| 文件 | 方法 | 问题 |
|------|------|------|
| `StatsServiceImpl.java:62-64` | `getDashboardStats()` | `pendingTasks` 没有全宗过滤 |
| `NotificationServiceImpl.java:36-60` | `listLatest()` | 直接查询，无全宗过滤 |
| `StatsServiceImpl.java:51` | `getDashboardStats()` | 缓存 Key 不包含全宗 |

### 2.2 数据流图

```
Dashboard (前端)
    │
    ├── /stats/dashboard ──→ StatsServiceImpl.getDashboardStats()
    │                              ├── totalArchives     ✅ DataScopeService
    │                              ├── storageUsed       ⚠️  全局统计
    │                              ├── pendingTasks      ❌ 无过滤
    │                              ├── todayIngest       ✅ DataScopeService
    │                              └── @Cacheable        ❌ Key 无全宗
    │
    ├── /stats/archival-trend ─→ StatsServiceImpl.getArchivalTrend()
    │                              └── ✅ DataScopeService
    │                                  └── @Cacheable    ❌ Key 无全宗
    │
    └── /notifications ──→ NotificationServiceImpl.listLatest()
                                   ├── IngestRequestStatus  ❌ 无过滤
                                   └── Archive              ❌ 无过滤
```

---

## 三、修复设计

### 3.1 修复策略

#### 修复 1: pendingTasks 添加全宗过滤

**问题**: `IngestRequestStatus` 表没有直接的 `fonds_code` 字段

**解决方案**: 通过关联 `Archive` 表进行过滤

```sql
-- IngestRequestStatus 与 Archive 的关联
SELECT COUNT(*)
FROM ingest_request_status irs
LEFT JOIN acc_archive a ON irs.archive_id = a.id
WHERE (a.fonds_code IN ('用户允许的全宗') OR a.fonds_code IS NULL)
  AND irs.status NOT IN ('COMPLETED', 'FAILED')
```

**实现**:
```java
// 新增方法：IngestRequestStatusMapper 中添加带全宗过滤的计数方法
Long countPendingByFonds(List<String> fondsCodes);
```

#### 修复 2: 通知列表添加全宗过滤

```java
@Override
public List<NotificationDto> listLatest() {
    DataScopeContext scope = dataScopeService.resolve();

    // 1) 任务通知 - 通过关联 Archive 过滤
    List<IngestRequestStatus> tasks = ingestRequestStatusMapper.selectList(
        new LambdaQueryWrapper<IngestRequestStatus>()
            .inSql(IngestRequestStatus::getArchiveId,
                "SELECT id FROM acc_archive WHERE fonds_code IN ('允许的全宗')")
            .orderByDesc(IngestRequestStatus::getUpdatedTime)
            .last("LIMIT 5")
    );

    // 2) 归档通知 - 使用 DataScopeService
    List<Archive> archives = archiveMapper.selectList(
        createScopedWrapper(scope)  // 使用全宗过滤
            .orderByDesc(Archive::getCreatedTime)
            .last("LIMIT 3")
    );

    return mergeResults(tasks, archives);
}
```

#### 修复 3: 缓存 Key 添加全宗信息

```java
// 修改前
@Cacheable(value = "stats", key = "'dashboard:' + #root.target.getClass().getSimpleName()")

// 修改后 - 从 FondsContext 获取当前全宗
@Cacheable(value = "stats", key = "'dashboard:' + #root.target.getClass().getSimpleName() + ':' + T(com.nexusarchive.config.FondsContext).get()")

// 趋势统计同理
@Cacheable(value = "stats", key = "'trend:' + T(java.time.LocalDate).now() + ':' + T(com.nexusarchive.config.FondsContext).get()")
```

### 3.2 类图修改

```
┌─────────────────────────────────────────────────────────────┐
│                    StatsServiceImpl                          │
├─────────────────────────────────────────────────────────────┤
│ + getDashboardStats(): DashboardStatsDto                     │
│   ├─ totalArchives    ✅ (已有) DataScopeService            │
│   ├─ storageUsed      ⚠️  (保持全局)                        │
│   ├─ pendingTasks     ❌ → 修复: 添加全宗过滤               │
│   ├─ todayIngest      ✅ (已有) DataScopeService            │
│   └─ @Cacheable       ❌ → 修复: Key 添加全宗               │
│                                                             │
│ + getArchivalTrend(): List<ArchivalTrendDto>                │
│   ├─ 数据查询         ✅ (已有) DataScopeService            │
│   └─ @Cacheable       ❌ → 修复: Key 添加全宗               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                 NotificationServiceImpl                       │
├─────────────────────────────────────────────────────────────┤
│ + listLatest(): List<NotificationDto>                       │
│   ├─ 任务查询         ❌ → 修复: 添加全宗过滤               │
│   └─ 归档查询         ❌ → 修复: DataScopeService           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                 IngestRequestStatusMapper                    │
├─────────────────────────────────────────────────────────────┤
│ + countPendingByFonds(List<String> fondsCodes): Long        │
│   └─ 新增方法: 按全宗统计待处理任务                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 四、实现步骤

### Step 1: 数据库层修改

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/mapper/IngestRequestStatusMapper.java`

```java
/**
 * 统计指定全宗的待处理任务数量
 * @param fondsCodes 全宗代码列表
 * @return 待处理任务数量
 */
Long countPendingByFonds(@Param("fondsCodes") List<String> fondsCodes);
```

**XML 映射**: `nexusarchive-java/src/main/resources/mapper/IngestRequestStatusMapper.xml`

```xml
<select id="countPendingByFonds" resultType="java.lang.Long">
    SELECT COUNT(*)
    FROM ingest_request_status irs
    LEFT JOIN acc_archive a ON irs.archive_id = a.id
    WHERE (a.fonds_code IN
        <foreach collection="fondsCodes" item="code" open="(" separator="," close=")">
            #{code}
        </foreach>
        OR a.fonds_code IS NULL)
      AND irs.status NOT IN ('COMPLETED', 'FAILED')
</select>
```

### Step 2: StatsService 修改

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/StatsServiceImpl.java`

```java
@Override
@Cacheable(value = "stats", key = "'dashboard:' + #root.target.getClass().getSimpleName() + ':' + T(com.nexusarchive.config.FondsContext).get()")
public DashboardStatsDto getDashboardStats() {
    DataScopeContext scope = dataScopeService.resolve();

    // ... 其他代码保持不变 ...

    // 修复 pendingTasks - 使用全宗过滤
    List<String> fondsCodes = scope.getFondsCodes();
    long pendingTasks = ingestRequestStatusMapper.countPendingByFonds(fondsCodes);

    // ... 其他代码保持不变 ...
}
```

### Step 3: NotificationService 修改

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/NotificationServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final IngestRequestStatusMapper ingestRequestStatusMapper;
    private final ArchiveMapper archiveMapper;
    private final DataScopeService dataScopeService;  // 新增

    @Override
    public List<NotificationDto> listLatest() {
        DataScopeContext scope = dataScopeService.resolve();
        List<NotificationDto> items = new ArrayList<>();

        // 1) 任务通知 - 添加全宗过滤
        List<String> fondsCodes = scope.getFondsCodes();
        List<IngestRequestStatus> tasks = ingestRequestStatusMapper.selectList(
                new LambdaQueryWrapper<IngestRequestStatus>()
                        .inSql(IngestRequestStatus::getArchiveId,
                            "SELECT id FROM acc_archive WHERE fonds_code IN ('" +
                            String.join("','", fondsCodes) + "')")
                        .orderByDesc(IngestRequestStatus::getUpdatedTime)
                        .last("LIMIT 5"));

        // 2) 归档通知 - 使用 DataScopeService
        LambdaQueryWrapper<Archive> archiveWrapper = new LambdaQueryWrapper<>();
        dataScopeService.applyArchiveScope(archiveWrapper, scope);
        archiveWrapper.orderByDesc(Archive::getCreatedTime).last("LIMIT 3");
        List<Archive> archives = archiveMapper.selectList(archiveWrapper);

        return mergeNotifications(tasks, archives);
    }
}
```

### Step 4: 缓存失效机制

**文件**: `nexusarchive-java/src/main/java/com/nexusarchive/service/impl/StatsServiceImpl.java`

确保切换全宗时清除旧缓存：

```java
/**
 * 按全宗清除仪表盘缓存
 * 用于切换全宗时刷新统计信息
 */
@CacheEvict(value = "stats", allEntries = true)
public void evictFondsCache() {
    log.debug("Fonds stats cache evicted");
}
```

---

## 五、测试计划

### 5.1 单元测试

**测试类**: `StatsServiceTest.java`

```java
@Test
void getDashboardStats_shouldRespectFondsFilter() {
    // Given: 设置全宗上下文为 "FONDS_A"
    FondsContext.set("FONDS_A");

    // When: 获取统计数据
    DashboardStatsDto stats = statsService.getDashboardStats();

    // Then: pendingTasks 应该只统计 FONDS_A 的任务
    assertThat(stats.getPendingTasks()).isEqualTo(5); // 预期值
}

@Test
void getDashboardStats_shouldHaveDifferentCacheKeysForDifferentFonds() {
    // Given: 全宗 A
    FondsContext.set("FONDS_A");
    DashboardStatsDto statsA = statsService.getDashboardStats();

    // When: 切换到全宗 B
    FondsContext.set("FONDS_B");
    DashboardStatsDto statsB = statsService.getDashboardStats();

    // Then: 数据应该不同
    assertThat(statsA.getTotalArchives()).isNotEqualTo(statsB.getTotalArchives());
}
```

### 5.2 集成测试

| 测试场景 | 预期结果 |
|----------|----------|
| 用户只有全宗 A 权限 | 只看到全宗 A 的数据和通知 |
| 切换全宗 A → B | Dashboard 数据更新为全宗 B |
| 超级管理员（所有全宗） | 看到所有全宗的数据汇总 |
| 缓存命中后再切换全宗 | 数据正确更新，不使用旧缓存 |

### 5.3 E2E 测试

```typescript
test('Dashboard should respect fonds filter', async ({ page }) => {
  // 1. 登录并选择全宗 A
  await page.goto('/system');
  await page.selectOption('[data-testid="fonds-switcher"]', 'FONDS_A');

  // 2. 验证统计数据
  const totalArchives = await page.textContent('[data-testid="total-archives"]');
  expect(totalArchives).toBe('100'); // 全宗 A 的预期值

  // 3. 切换到全宗 B
  await page.selectOption('[data-testid="fonds-switcher"]', 'FONDS_B');
  await page.waitForTimeout(500); // 等待数据刷新

  // 4. 验证数据已更新
  const totalArchivesB = await page.textContent('[data-testid="total-archives"]');
  expect(totalArchivesB).toBe('50'); // 全宗 B 的预期值
});
```

---

## 六、风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 性能下降（SQL JOIN） | 低 | 添加索引 `archive_id` |
| 缓存 Key 冲突 | 已消除 | 添加全宗到 Key |
| 无全宗用户无法访问 | 低 | FondsContextFilter 已处理 |
| 旧缓存未清除 | 低 | 部署后清除 Redis |

---

## 七、部署计划

1. **开发阶段**: 1 天
   - Mapper 方法添加
   - Service 逻辑修改
   - 单元测试编写

2. **测试阶段**: 1 天
   - 集成测试
   - E2E 测试
   - 缓存验证

3. **部署阶段**: 0.5 天
   - 灰度发布
   - Redis 缓存清除
   - 监控观察

---

## 八、验收标准

- [ ] Dashboard 所有统计数据与当前全宗一致
- [ ] 通知列表只显示当前全宗的数据
- [ ] 切换全宗后数据正确刷新
- [ ] 缓存 Key 包含全宗信息
- [ ] 单元测试覆盖率 > 90%
- [ ] E2E 测试全部通过

---

**文档历史**:
- 2026-01-15: 初始版本
