# NexusArchive 性能优化设计文档

**版本**: v1.1
**编制日期**: 2026-01-09
**更新日期**: 2026-01-09
**适用版本**: NexusArchive v1.0+
**实际工时**: 91.5 小时 (~12 个工作日)
**执行策略**: 渐进式 - 从简单到复杂，每日交付
**执行状态**: ✅ 规划完成

---

## 执行状态摘要

| 维度 | 计划任务 | 实际完成状态 | 备注 |
|------|----------|-------------|------|
| 前端性能 | 9 类 | 📋 待执行 | 设计方案已确认 |
| 后端数据库 | 6 类 | 📋 待执行 | SQL 脚本已准备 |
| API 安全 | 9 类 | 📋 待执行 | 优先级 P0 任务已识别 |
| **合计** | **24 类** | **📋 待执行** | 本文档为设计阶段输出 |

> **说明**: 本文档是性能优化的**设计阶段输出**，详细记录了优化方案设计。实际执行请参考总结报告: `/docs/reports/2026-01-09-performance-optimization-summary.md`

---

## 文档概述

本文档是 NexusArchive 电子会计档案管理系统的**全面性能优化设计**，涵盖前端、后端数据库、API 安全三个维度共 **24 类问题**的完整解决方案。

### 优化范围

| 维度 | 问题数量 | 预估工时 | 优先级 |
|------|----------|----------|--------|
| 前端性能 | 9 类 | 32h | 高 |
| 后端数据库 | 6 类 | 26.5h | 高 |
| API 安全 | 9 类 | 33h | 高 |
| **合计** | **24 类** | **91.5h** | - |

### 执行原则

1. **渐进式交付**: 每天完成 1-2 个优化，持续 2-3 周
2. **难易递进**: 从简单修复到复杂重构，保持成就感
3. **全面验证**: 自动化测试 + 性能基准 + 人工验证
4. **文档驱动**: 单一设计文档 + GitHub Issues 追踪

---

## 一、问题总览

### 1.1 前端性能问题（9 类）

| # | 问题 | 严重程度 | 影响文件 | 工时 |
|---|------|----------|----------|------|
| 1 | 定时器未清理 | 🔴 高 | WatermarkOverlay.tsx | 1h |
| 2 | 大表格无虚拟化 | 🔴 高 | BatchTable.tsx (642行) | 4h |
| 3 | 组件过大需拆分 | 🟠 中 | LegacyImportPage.tsx (822行) | 8h |
| 4 | 缺少 React.memo | 🟡 中 | 多个组件 | 3h |
| 5 | 内联函数作为 props | 🟡 中 | 多个组件 | 4h |
| 6 | 重复计算未用 useMemo | 🟡 中 | 多个组件 | 2h |
| 7 | useEffect 依赖不当 | 🟡 中 | 多个组件 | 2h |
| 8 | 配置对象重复创建 | 🟢 低 | 多个组件 | 2h |
| 9 | 状态管理过度 | 🟢 低 | 多个页面 | 6h |

### 1.2 后端数据库问题（6 类）

| # | 问题 | 严重程度 | 工时 |
|---|------|----------|------|
| 1 | N+1 查询问题 | 🔴 高 | 3.5h |
| 2 | 缺少关键索引 | 🔴 高 | 4h |
| 3 | 未分页列表查询 | 🟠 高 | 5h |
| 4 | SELECT * 查询大字段 | 🟠 中 | 3h |
| 5 | 大字段未分离 | 🟡 中 | 8h |
| 6 | JSONB 查询缺少索引 | 🟡 中 | 3.5h |

### 1.3 API 安全问题（9 类）

| # | 问题 | 严重程度 | 工时 |
|---|------|----------|------|
| 1 | **DebugController 无权限校验** | 🔴 严重 | 1h |
| 2 | 缓存优化缺失 | 🟠 高 | 4h |
| 3 | SQL 注入风险 | 🟠 高 | 1h |
| 4 | 缺少分页 | 🟠 高 | 3h |
| 5 | 直接返回 Entity | 🟠 高 | 8h |
| 6 | 异步任务未异步化 | 🟡 中 | 6h |
| 7 | 错误处理不当 | 🟡 中 | 2h |
| 8 | 硬编码数据 | 🟢 低 | 2h |
| 9 | API 文档不完整 | 🟢 低 | 4h |

---

## 二、执行计划

### 2.1 渐进式交付时间表（12 天）

```
第 1 天: 安全加固（Critical）
├── DebugController 权限校验
├── SQL 注入修复
└── 全局异常处理增强

第 2 天: 前端基础优化
├── 定时器未清理修复
├── React.memo 包裹组件
└── 配置对象提取

第 3 天: 数据库索引
├── 添加关键索引
├── N+1 查询修复（IngestServiceImpl）
└── JSONB 表达式索引

第 4 天: 前端进阶优化
├── useCallback 优化内联函数
└── useMemo 优化重复计算

第 5 天: 分页支持
├── ErpScenarioController 分页
├── GlobalSearchController 分页
└── StatsController 优化

第 6 天: 虚拟化改造
├── BatchTable 虚拟化
└── 性能测试验证

第 7 天: 组件拆分
├── LegacyImportPage 拆分（上）
└── 自定义 Hooks 提取

第 8 天: 组件拆分
├── LegacyImportPage 拆分（下）
└── 回归测试

第 9 天: 缓存实现
├── Redis 配置
├── Service 层缓存注解
└── 缓存验证

第 10 天: DTO 转换
├── Entity-DTO 映射
├── Controller 修改
└── API 测试

第 11 天: 异步任务
├── 异步配置
├── 四性检测异步化
└── ERP 同步异步化

第 12 天: 文档与收尾
├── API 文档完善
├── 性能基准测试
└── 总结与复盘
```

### 2.2 每日验证清单

- [ ] 自动化测试通过 (`mvn test` + `npm run test:run`)
- [ ] 架构检查通过 (`npm run check:arch`)
- [ ] 类型检查通过 (`npm run typecheck`)
- [ ] 性能基准记录（响应时间、查询时间）
- [ ] 人工验证核心功能
- [ ] 更新 `.test-status` 文件

---

## 三、技术方案详细设计

### 3.1 前端优化方案

#### 3.1.1 定时器未清理修复（P0 - 1h）

**问题**: `WatermarkOverlay.tsx` 中定时器引用未使用 `useRef` 存储，可能导致内存泄漏。

**修复代码**:
```tsx
const refreshTimerRef = useRef<NodeJS.Timeout | null>(null);

useEffect(() => {
    // ...
    refreshTimerRef.current = setInterval(drawWatermark, 5000);

    return () => {
        if (refreshTimerRef.current) {
            clearInterval(refreshTimerRef.current);
            refreshTimerRef.current = null;
        }
    };
}, [drawWatermark]);
```

#### 3.1.2 虚拟化表格改造（P0 - 4h）

**依赖安装**:
```bash
npm install @tanstack/react-virtual
```

**核心实现**:
```tsx
import { useVirtualizer } from '@tanstack/react-virtual';

const rowVirtualizer = useVirtualizer({
    count: batches.length,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 56,
    overscan: 5,
});
```

#### 3.1.3 组件拆分方案（P1 - 8h）

**目标结构**:
```
LegacyImportPage/
├── index.tsx
├── components/
│   ├── FileUploader.tsx
│   ├── ImportTab.tsx
│   └── HistoryTab.tsx
└── hooks/
    ├── useFileUpload.ts
    └── useImportHistory.ts
```

**自定义 Hook 示例**:
```tsx
export function useFileUpload() {
    const [file, setFile] = useState<File | null>(null);
    const [previewResult, setPreviewResult] = useState<ImportPreviewResult | null>(null);

    const handlePreview = useCallback(async () => {
        // 预览逻辑
    }, [file]);

    return { file, previewResult, handlePreview, setFile };
}
```

---

### 3.2 后端数据库优化方案

#### 3.2.1 索引创建 SQL（P0 - 4h）

```sql
-- V2026010901__add_performance_indexes.sql

-- acc_archive 表补充索引
CREATE INDEX IF NOT EXISTS idx_acc_archive_department_id
ON acc_archive(department_id)
WHERE department_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_acc_archive_fonds_status_year
ON acc_archive(fonds_no, status, fiscal_year)
WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_acc_archive_fiscal_period_year
ON acc_archive(fiscal_period, fiscal_year)
WHERE deleted = 0;

-- arc_file_content 表补充索引
CREATE INDEX IF NOT EXISTS idx_arc_file_content_batch_status
ON arc_file_content(batch_id, pre_archive_status)
WHERE batch_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_arc_file_content_fonds_status
ON arc_file_content(fonds_code, pre_archive_status)
WHERE fonds_code IS NOT NULL;
```

#### 3.2.2 N+1 查询修复（P0 - 3.5h）

**IngestServiceImpl 修复**:
```java
// 修复前
for (String id : poolItemIds) {
    ArcFileContent file = arcFileContentMapper.selectById(id);  // N+1
    // ...
}

// 修复后
List<ArcFileContent> files = arcFileContentMapper.selectList(
    new LambdaQueryWrapper<ArcFileContent>()
        .in(ArcFileContent::getId, poolItemIds)
        .select(ArcFileContent::getId, ArcFileContent::getPreArchiveStatus)
);
Map<String, ArcFileContent> fileMap = files.stream()
    .collect(Collectors.toMap(ArcFileContent::getId, Function.identity()));
```

#### 3.2.3 分页查询实现（P0 - 5h）

**分页 DTO**:
```java
@Data
public class PageRequest {
    @Min(1) private int pageNum = 1;
    @Min(1) @Max(100) private int pageSize = 20;
}
```

**Controller 修改**:
```java
@GetMapping("/list/{configId}")
public Result<Page<ErpScenario>> listByConfig(
    @PathVariable Long configId,
    @Valid PageRequest request
) {
    return Result.success(erpScenarioService.listScenariosByConfigIdPage(configId, request));
}
```

---

### 3.3 API 安全优化方案

#### 3.3.1 DebugController 权限校验（P0 - 1h）

**修复代码**:
```java
@RestController
@RequestMapping("/debug")
@ConditionalOnProperty(
    name = "app.debug.enabled",
    havingValue = "true",
    matchIfMissing = false  // 默认禁用
)
public class DebugController {

    @PostMapping("/unlock/{username}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")  // 仅超级管理员
    public Result<String> unlockUser(@PathVariable String username) {
        loginAttemptService.recordSuccess(username);
        return Result.success("用户 " + username + " 已解锁");
    }
}
```

**application.yml 配置**:
```yaml
app:
  debug:
    enabled: false  # 生产环境必须为 false
```

#### 3.3.2 缓存配置（P1 - 4h）

```java
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        Map<String, RedisCacheConfiguration> config = new HashMap<>();

        config.put("permissions",
            defaultCacheConfig().entryTtl(Duration.ofHours(1)));
        config.put("stats",
            defaultCacheConfig().entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultCacheConfig())
                .withInitialCacheConfigurations(config)
                .build();
    }
}
```

**Service 使用缓存**:
```java
@Cacheable(value = "roles", key = "'all'")
public List<Role> getAllRoles() {
    return roleMapper.selectList(null);
}

@CacheEvict(value = "roles", allEntries = true)
public Role createRole(Role role) {
    // ...
}
```

#### 3.3.3 Entity 到 DTO 转换（P1 - 8h）

**DTO 示例**:
```java
@Data
@Builder
public class ArchiveResponse {
    private String id;
    private String archiveCode;
    private String title;
    private String fiscalYear;
    // 不包含敏感字段
}
```

**Controller 使用**:
```java
@GetMapping("/{id}")
public Result<ArchiveResponse> get(@PathVariable String id) {
    Archive archive = archiveService.getArchiveById(id);
    return Result.success(dtoMapper.toResponse(archive));
}
```

---

## 四、验证方案

### 4.1 性能指标

| 指标 | 优化前 | 目标 | 测量方法 |
|------|--------|------|----------|
| 前端首屏渲染 | - | < 1s | Lighthouse |
| 组件重渲染 | 基准 | -30% | React Profiler |
| 数据库查询 N+1 | 101次 | 1次 | EXPLAIN ANALYZE |
| API 响应时间 | 基准 | +50% | Apache Benchmark |

### 4.2 每日检查脚本

```bash
#!/bin/bash
# daily-check.sh

echo "🔍 每日优化检查 - $(date +%Y-%m-%d)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 1. 类型检查
echo "📝 TypeScript 类型检查..."
npm run typecheck

# 2. 架构检查
echo "🏗️  架构边界检查..."
npm run check:arch

# 3. 前端测试
echo "🧪 前端测试..."
npm run test:run

# 4. 后端测试
echo "🧪 后端测试..."
cd nexusarchive-java && mvn test -q

# 5. 更新测试状态
echo "✅ 所有检查完成 - $(date +%H:%M)" > .test-status
```

---

## 五、风险与回滚

### 5.1 风险矩阵

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| 索引创建锁表 | 低 | 中 | CONCURRENTLY |
| 虚拟化滚动异常 | 中 | 中 | 充分测试 |
| 缓存数据不一致 | 低 | 高 | TTL 设置 |
| DTO 遗漏字段 | 中 | 中 | Code Review |

### 5.2 回滚方案

```sql
-- 索引回滚
DROP INDEX IF EXISTS idx_acc_archive_department_id;
```

```bash
# 代码回滚
git revert <commit-hash>
```

---

## 六、成功标准

优化完成后，系统应达到：

1. ✅ 所有 🔴 严重问题已修复
2. ✅ 前端首屏渲染 < 1s
3. ✅ N+1 查询消除
4. ✅ 所有列表接口支持分页
5. ✅ DebugController 仅超级管理员可访问
6. ✅ 自动化测试 100% 通过
7. ✅ 架构检查无 violation

---

**文档编制**: Claude Code
**审核状态**: ✅ 设计阶段完成
**执行状态**: 📋 待执行
**下一步**: 参考总结报告 `/docs/reports/2026-01-09-performance-optimization-summary.md` 开始实施
