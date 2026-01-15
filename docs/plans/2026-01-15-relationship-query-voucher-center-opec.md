# 穿透联查功能 - 以记账凭证为中心 OPEC 提案

**日期**: 2026-01-15  
**状态**: 📋 OPEC 提案  
**优先级**: P0（致命问题）  
**类型**: 业务逻辑修复 + 架构优化  
**关联文档**:
- `docs/plans/2026-01-15-relationship-query-three-column-layout-spec.md`
- `docs/planning/expert-group-rules.md`
- `docs/planning/expert-group-workflow.md`

---

## 📋 问题描述（Problem）

### 核心问题

当前穿透联查功能允许**任意档案类型**作为中心节点进行穿透查询，这违反了**电子会计档案管理的核心业务逻辑**。

### 问题影响

1. **合规风险**（🛑 致命）
   - 违反《会计档案管理办法》（财政部、国家档案局令第 79 号）
   - 不符合 DA/T 94-2022《电子会计档案管理规范》
   - 记账凭证是会计业务的核心，应该作为穿透查询的中心节点

2. **用户体验问题**（高风险）
   - 用户输入发票（FP-2025-01-001）时，系统以发票为中心展示
   - 但发票只是原始凭证，不是会计业务中心
   - 导致业务关系展示不完整、不符合会计实务

3. **业务逻辑混乱**（高风险）
   - 发票、合同、报销单等都是**原始凭证**或**业务单据**
   - 记账凭证（JZ-/PZ-）才是**会计档案的核心**
   - 当前实现无法体现会计业务链路的完整性

### 证据与数据

**当前实现**：
- `RelationController.java:89-139`：直接以输入的 `archiveId` 作为中心节点
- 前端 `RelationshipQueryView.tsx`：接收任意档号，展示以该档号为中心的关系图谱

**业务关系数据**（从 `V102__seed_relationship_demo_data.sql`）：
```sql
-- 完整业务链路示例：
发票（FP-） → 报销单（BX-） → 付款单（FK-） → 记账凭证（JZ-） → 报表（BB-）
合同（HT-） → 发票（FP-） → 记账凭证（JZ-） → 付款单（FK-） → 银行回单（HD-）
```

**会计业务逻辑**：
- 所有原始凭证（发票、合同、报销单等）最终都归集到**记账凭证**
- 记账凭证是会计账务处理的核心节点
- 记账凭证是后续归档、报表生成的基础

### 问题复现

**场景**：
1. 用户输入发票档号：`FP-2025-01-001`
2. 系统以发票为中心展示关系
3. 发票作为中心节点，下游显示报销单（BX-2025-01-001）
4. **问题**：发票不是会计业务中心，无法看到完整的业务链路（如记账凭证、报表等）

**期望**：
1. 用户输入发票档号：`FP-2025-01-001`
2. 系统自动查找关联的记账凭证（JZ-2025-01-001）
3. 以记账凭证为中心展示完整业务链路
4. 同时高亮显示原始查询的发票，提示用户查询路径

---

## 🎯 方案设计（Option）

### 方案 1：自动查找关联凭证作为中心（推荐⭐）

#### 设计思路

- **核心原则**：记账凭证（JZ-/PZ-开头）作为穿透查询的中心节点
- **自动转换**：如果输入非凭证档案，自动查找关联的记账凭证
- **向后兼容**：如果没有关联凭证，仍以输入档案为中心（保留当前行为）

#### 实现逻辑

```
用户输入档号 → 判断档案类型
    ├─ 是记账凭证（JZ-/PZ-）→ 直接作为中心节点 ✅
    └─ 非记账凭证 → 查找关联的记账凭证
        ├─ 找到关联凭证 → 以凭证为中心，高亮原始档案 ✅
        └─ 未找到凭证 → 以原档案为中心（向后兼容）⚠️
```

#### 技术实现

**后端修改**（`RelationController.java`）：

```java
@GetMapping("/{archiveId}/graph")
public Result<RelationGraphDto> getRelationGraph(@PathVariable String archiveId) {
    String currentFonds = FondsContext.getCurrentFondsNo();
    Archive inputArchive = archiveService.getArchiveById(archiveId);
    
    // Step 1: 判断输入的档案类型
    String centerArchiveId;
    String originalQueryId = null;
    
    if (isVoucher(inputArchive.getArchiveCode())) {
        // 输入的就是记账凭证，直接作为中心
        centerArchiveId = inputArchive.getId();
    } else {
        // 输入的是非凭证档案，查找关联的记账凭证
        originalQueryId = inputArchive.getId(); // 记录原始查询档案
        centerArchiveId = findRelatedVoucher(inputArchive.getId());
        
        if (centerArchiveId == null) {
            // 未找到关联凭证，以原档案为中心（向后兼容）
            centerArchiveId = inputArchive.getId();
            originalQueryId = null;
            log.warn("[RelationController] No related voucher found for archive: {}", archiveId);
        } else {
            log.info("[RelationController] Found related voucher {} for input archive: {}", 
                centerArchiveId, archiveId);
        }
    }
    
    // Step 2: 以 centerArchiveId 为中心查询关系
    Archive center = archiveService.getArchiveById(centerArchiveId);
    List<ArchiveRelation> relations = archiveRelationService.list(
        new LambdaQueryWrapper<ArchiveRelation>()
            .eq(ArchiveRelation::getSourceId, centerArchiveId)
            .or()
            .eq(ArchiveRelation::getTargetId, centerArchiveId)
    );
    
    // Step 3: 构建返回数据
    RelationGraphDto graph = buildRelationGraph(center, relations);
    
    // Step 4: 标记原始查询档案（如果有）
    if (originalQueryId != null) {
        graph.setOriginalQueryId(originalQueryId);
        graph.setAutoRedirected(true); // 标记为自动转换
    }
    
    return Result.success(graph);
}

/**
 * 判断是否为记账凭证
 */
private boolean isVoucher(String archiveCode) {
    if (archiveCode == null) return false;
    String prefix = archiveCode.toUpperCase().substring(0, Math.min(2, archiveCode.length()));
    return prefix.equals("JZ") || prefix.equals("PZ");
}

/**
 * 查找关联的记账凭证
 * 查找逻辑：沿着关系链查找，优先查找 ORIGINAL_VOUCHER 关系的目标节点
 */
private String findRelatedVoucher(String archiveId) {
    // 策略1: 查找以该档案为源、关系类型为 ORIGINAL_VOUCHER 的目标节点
    List<ArchiveRelation> relations = archiveRelationService.list(
        new LambdaQueryWrapper<ArchiveRelation>()
            .eq(ArchiveRelation::getSourceId, archiveId)
            .eq(ArchiveRelation::getRelationType, "ORIGINAL_VOUCHER")
            .last("LIMIT 1")
    );
    
    if (!relations.isEmpty()) {
        Archive target = archiveService.getArchiveById(relations.get(0).getTargetId());
        if (target != null && isVoucher(target.getArchiveCode())) {
            return target.getId();
        }
    }
    
    // 策略2: 查找所有关联关系，递归查找凭证
    Set<String> visited = new HashSet<>();
    return findVoucherInRelationChain(archiveId, visited, 0, 3); // 最多查找3度
}

/**
 * 递归查找关系链中的记账凭证
 */
private String findVoucherInRelationChain(String archiveId, Set<String> visited, int depth, int maxDepth) {
    if (depth > maxDepth || visited.contains(archiveId)) {
        return null;
    }
    visited.add(archiveId);
    
    Archive archive = archiveService.getArchiveById(archiveId);
    if (archive != null && isVoucher(archive.getArchiveCode())) {
        return archiveId;
    }
    
    // 查找所有关联关系
    List<ArchiveRelation> relations = archiveRelationService.list(
        new LambdaQueryWrapper<ArchiveRelation>()
            .eq(ArchiveRelation::getSourceId, archiveId)
            .or()
            .eq(ArchiveRelation::getTargetId, archiveId)
    );
    
    for (ArchiveRelation relation : relations) {
        String nextId = relation.getSourceId().equals(archiveId) 
            ? relation.getTargetId() 
            : relation.getSourceId();
        
        String voucherId = findVoucherInRelationChain(nextId, visited, depth + 1, maxDepth);
        if (voucherId != null) {
            return voucherId;
        }
    }
    
    return null;
}
```

**DTO 扩展**（`RelationGraphDto.java`）：

```java
@Data
@Builder
public class RelationGraphDto {
    private String centerId;
    private List<RelationNodeDto> nodes;
    private List<RelationEdgeDto> edges;
    
    // 新增字段：原始查询档案ID（如果发生了自动转换）
    private String originalQueryId;
    
    // 新增字段：是否自动转换（true表示以原始档案为中心时自动找到凭证）
    private Boolean autoRedirected;
    
    // 新增字段：转换提示信息
    private String redirectMessage;
}
```

**前端修改**（`RelationshipQueryView.tsx`）：

```typescript
// 在获取图谱数据后，检查是否有自动转换
const graph = await autoAssociationApi.getRelationGraph(archiveId);

if (graph.autoRedirected && graph.originalQueryId) {
  // 显示提示：已自动切换到关联的记账凭证
  toast.success(
    `已自动切换到关联的记账凭证查看完整业务链路`,
    { duration: 3000 }
  );
  
  // 高亮显示原始查询的档案
  setHighlightedArchiveId(graph.originalQueryId);
}
```

#### 优点

1. ✅ **符合会计业务逻辑**：记账凭证始终作为中心节点
2. ✅ **自动转换**：无需用户手动查找凭证
3. ✅ **向后兼容**：未找到凭证时保留当前行为
4. ✅ **用户体验好**：自动处理，提供清晰提示

#### 缺点

1. ⚠️ **复杂度增加**：需要递归查找关联凭证
2. ⚠️ **性能影响**：可能增加数据库查询次数（可通过缓存优化）

---

### 方案 2：前端提示引导（临时方案）

#### 设计思路

- 保持后端逻辑不变
- 前端判断输入的档案类型
- 如果是非凭证档案，提示用户查询关联凭证

#### 实现逻辑

**前端修改**（`RelationshipQueryView.tsx`）：

```typescript
const handleSearch = useCallback(() => {
  if (!searchQuery.trim()) return;
  
  // 判断是否为记账凭证
  const isVoucher = /^(JZ|PZ)-/i.test(searchQuery);
  
  if (!isVoucher) {
    // 显示提示
    Modal.confirm({
      title: '提示',
      content: '该档案为原始凭证，建议查询关联的记账凭证以查看完整业务链路。是否查找关联凭证？',
      okText: '查找关联凭证',
      cancelText: '继续查看',
      onOk: async () => {
        // 查找关联凭证的逻辑
        const relatedVoucher = await findRelatedVoucher(searchQuery);
        if (relatedVoucher) {
          setSearchQuery(relatedVoucher);
          initializeGraph(relatedVoucher);
        } else {
          toast.error('未找到关联的记账凭证');
        }
      },
      onCancel: () => {
        // 继续以原档案为中心
        initializeGraph(searchQuery);
      }
    });
  } else {
    // 直接查询
    initializeGraph(searchQuery);
  }
}, [searchQuery, initializeGraph]);
```

#### 优点

1. ✅ **实现简单**：只修改前端代码
2. ✅ **快速上线**：可作为临时方案
3. ✅ **用户选择**：给用户选择权

#### 缺点

1. ❌ **不符合业务逻辑**：仍然允许非凭证作为中心
2. ❌ **用户体验差**：需要用户手动选择
3. ❌ **无法彻底解决问题**：只是提示，不是修复

---

### 方案 3：强制限制仅允许凭证查询（激进方案）

#### 设计思路

- 后端严格限制：只允许记账凭证作为中心节点
- 非凭证档案直接返回错误

#### 实现逻辑

```java
@GetMapping("/{archiveId}/graph")
public Result<RelationGraphDto> getRelationGraph(@PathVariable String archiveId) {
    Archive archive = archiveService.getArchiveById(archiveId);
    
    if (!isVoucher(archive.getArchiveCode())) {
        return Result.error(400, 
            "穿透联查仅支持记账凭证，请查询关联的记账凭证。档案类型: " + archive.getArchiveCode());
    }
    
    // 后续逻辑...
}
```

#### 优点

1. ✅ **严格符合业务逻辑**：强制以凭证为中心
2. ✅ **实现简单**：逻辑清晰

#### 缺点

1. ❌ **用户体验差**：直接拒绝非凭证查询
2. ❌ **不够灵活**：无法查看原始凭证的关系
3. ❌ **向后不兼容**：破坏现有功能

---

## 📊 方案评估（Evaluation）

### 评估维度

| 维度 | 方案1（自动转换） | 方案2（前端提示） | 方案3（强制限制） |
|------|------------------|------------------|------------------|
| **合规性** | ✅ 完全符合 | ⚠️ 部分符合 | ✅ 完全符合 |
| **业务逻辑** | ✅ 正确 | ❌ 不正确 | ✅ 正确 |
| **用户体验** | ✅ 优秀 | ⚠️ 一般 | ❌ 较差 |
| **实现复杂度** | ⚠️ 中等 | ✅ 简单 | ✅ 简单 |
| **向后兼容** | ✅ 兼容 | ✅ 兼容 | ❌ 不兼容 |
| **性能影响** | ⚠️ 中等 | ✅ 无影响 | ✅ 无影响 |
| **维护成本** | ⚠️ 中等 | ⚠️ 中等 | ✅ 低 |

### 综合评估

**推荐方案**：**方案1（自动查找关联凭证作为中心）** ⭐⭐⭐⭐⭐

**理由**：
1. **完全符合会计业务逻辑和合规要求**
2. **用户体验最佳**：自动处理，无需用户操作
3. **向后兼容**：未找到凭证时保留当前行为
4. **技术可行**：实现复杂度可控，性能可优化

---

## ✅ 方案选择（Choice）

### 最终选择：方案1（自动查找关联凭证作为中心）

### 实施策略

#### Phase 1：后端核心逻辑（2-3 天）

1. **修改 `RelationController.java`**
   - 实现 `isVoucher()` 方法
   - 实现 `findRelatedVoucher()` 方法
   - 实现 `findVoucherInRelationChain()` 递归查找方法
   - 修改 `getRelationGraph()` 方法，增加自动转换逻辑

2. **扩展 `RelationGraphDto.java`**
   - 添加 `originalQueryId` 字段
   - 添加 `autoRedirected` 字段
   - 添加 `redirectMessage` 字段

3. **单元测试**
   - 测试凭证直接查询场景
   - 测试非凭证自动转换场景
   - 测试未找到凭证的向后兼容场景

#### Phase 2：前端优化（1-2 天）

1. **修改前端类型定义**
   - 更新 `RelationGraph` 接口，添加新字段

2. **优化用户体验**
   - 显示自动转换提示（Toast）
   - 高亮显示原始查询档案
   - 在关系说明中标记原始档案

3. **交互优化**
   - 添加"查看原始档案详情"按钮（如果发生转换）
   - 优化加载状态显示

#### Phase 3：性能优化（1 天）

1. **缓存优化**
   - 缓存"档案 → 凭证"的映射关系（Redis）
   - 缓存失效策略（档案更新时清除缓存）

2. **查询优化**
   - 限制递归深度（最多3度）
   - 使用批量查询减少数据库访问

#### Phase 4：测试与文档（1 天）

1. **功能测试**
   - 各种档案类型的查询场景
   - 边界情况（无关联凭证、循环关系等）

2. **文档更新**
   - 更新 API 文档
   - 更新用户使用手册
   - 更新开发文档

---

## 📋 实施计划

### 时间线

| 阶段 | 任务 | 工期 | 责任人 |
|------|------|------|--------|
| **Phase 1** | 后端核心逻辑实现 | 2-3 天 | 后端开发 |
| **Phase 2** | 前端优化 | 1-2 天 | 前端开发 |
| **Phase 3** | 性能优化 | 1 天 | 后端开发 |
| **Phase 4** | 测试与文档 | 1 天 | QA + 技术文档 |
| **总计** | | **5-7 天** | |

### 里程碑

- [ ] **M1**: 后端核心逻辑完成，单元测试通过
- [ ] **M2**: 前端集成完成，交互优化完成
- [ ] **M3**: 性能优化完成，缓存机制上线
- [ ] **M4**: 功能测试通过，文档更新完成

---

## 🧪 验收标准

### 功能验收

| # | 测试场景 | 预期结果 | 优先级 |
|---|---------|---------|--------|
| 1 | 输入记账凭证（JZ-2025-01-001） | 直接以凭证为中心展示关系 | P0 |
| 2 | 输入发票（FP-2025-01-001） | 自动查找关联凭证（JZ-2025-01-001），以凭证为中心，高亮发票 | P0 |
| 3 | 输入合同（HT-2025-02-001） | 自动查找关联凭证，以凭证为中心 | P0 |
| 4 | 输入无关联凭证的档案 | 以原档案为中心（向后兼容），显示提示信息 | P1 |
| 5 | 输入不存在的档号 | 返回404错误 | P0 |
| 6 | 自动转换提示显示 | Toast提示显示，原始档案高亮 | P1 |
| 7 | 关系说明正确性 | 关系说明中正确显示所有关系 | P0 |
| 8 | 性能测试 | 递归查找在3度内完成，响应时间<500ms | P1 |

### 合规验收

- [ ] ✅ 记账凭证始终作为穿透查询的中心节点
- [ ] ✅ 符合《会计档案管理办法》要求
- [ ] ✅ 符合 DA/T 94-2022《电子会计档案管理规范》

### 用户体验验收

- [ ] ✅ 自动转换过程流畅，用户无感知
- [ ] ✅ 提示信息清晰，用户理解转换原因
- [ ] ✅ 原始查询档案高亮显示，便于识别

---

## ⚠️ 风险评估

### 技术风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| **递归查找性能问题** | 高 | 中 | 限制递归深度（3度），添加缓存机制 |
| **循环关系导致无限递归** | 高 | 低 | 使用 `visited` Set 防止循环 |
| **数据库查询压力** | 中 | 中 | 批量查询优化，添加 Redis 缓存 |

### 业务风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| **向后兼容性问题** | 中 | 低 | 未找到凭证时保留原行为 |
| **用户不理解自动转换** | 低 | 中 | 提供清晰的提示信息和帮助文档 |

### 合规风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| **不符合会计业务逻辑** | 🛑 致命 | 0%（修复后） | 本方案正是为了解决此问题 |

---

## 📚 相关文档

- `docs/plans/2026-01-15-relationship-query-three-column-layout-spec.md` - 三栏布局设计
- `docs/planning/expert-group-rules.md` - 专家审核规则
- `docs/planning/expert-group-workflow.md` - 专家审核工作流
- `nexusarchive-java/src/main/java/com/nexusarchive/controller/RelationController.java` - 后端控制器
- `src/pages/utilization/RelationshipQueryView.tsx` - 前端页面

---

## 🔄 后续优化方向

1. **智能推荐**：如果找到多个关联凭证，提供选择界面
2. **关系路径可视化**：显示"原始档案 → 凭证"的查找路径
3. **批量查询优化**：支持一次查询多个关联凭证
4. **关系链完整性检查**：自动检测业务链路是否完整

---

## ✅ 批准与执行

**提案状态**: ✅ 已实施（Phase 1-3 已完成）  
**实施优先级**: P0（致命问题，必须修复）  
**实施版本**: v1.1.0  
**实施日期**: 2026-01-15

---

## 📋 实施状态更新

### ✅ 已完成阶段

- [x] **Phase 1**: 后端核心逻辑（已完成）
- [x] **Phase 2**: 前端优化（已完成）
- [x] **Phase 3**: 性能优化（已完成）
- [ ] **Phase 4**: 测试与文档（待手动测试验证）

### 📝 实施总结

详见：`docs/plans/2026-01-15-relationship-query-voucher-center-implementation-summary.md`

---

**生成时间**: 2026-01-15  
**提案人**: AI 助手（基于专家审核结论）
