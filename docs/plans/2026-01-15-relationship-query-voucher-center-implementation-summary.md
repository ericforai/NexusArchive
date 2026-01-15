# 穿透联查 - 以记账凭证为中心 - 实施总结

**日期**: 2026-01-15  
**状态**: ✅ 已完成 Phase 1-3  
**优先级**: P0（致命问题）  
**关联文档**:
- `docs/plans/2026-01-15-relationship-query-voucher-center-opec.md` (OPEC 提案)

---

## 📋 实施概览

基于专家审核结论，已完成穿透联查功能的核心修复：**以记账凭证（会计档案）为中心进行穿透查询**。

---

## ✅ 已完成工作

### Phase 1: 后端核心逻辑（已完成）

#### 1. DTO 扩展
- ✅ **`RelationGraphDto.java`**
  - 添加 `originalQueryId` 字段（原始查询档案ID）
  - 添加 `autoRedirected` 字段（是否自动转换）
  - 添加 `redirectMessage` 字段（转换提示信息）

#### 2. Controller 核心逻辑
- ✅ **`RelationController.java`**
  - 实现 `isVoucher()` 方法：判断是否为记账凭证（JZ-/PZ-开头）
  - 实现 `findRelatedVoucher()` 方法：查找关联的记账凭证（带缓存）
  - 实现 `findVoucherInRelationChain()` 方法：递归查找关系链中的凭证（最多3度）
  - 修改 `getRelationGraph()` 方法：自动转换逻辑

**核心逻辑流程**：
```
用户输入档号 → 判断是否为凭证
    ├─ 是凭证 → 直接作为中心节点 ✅
    └─ 非凭证 → 查找关联凭证
        ├─ 找到凭证 → 以凭证为中心，标记原始查询档案 ✅
        └─ 未找到凭证 → 以原档案为中心（向后兼容）⚠️
```

**查找策略优化**：
1. 优先查找 `ORIGINAL_VOUCHER` 关系（最常见：发票→凭证）
2. 优先查找 `BASIS` 关系（合同→凭证）
3. 递归查找关系链（最多3度，防止性能问题）

---

### Phase 2: 前端优化（已完成）

#### 1. 类型定义更新
- ✅ **`src/api/autoAssociation.ts`**
  - 扩展 `RelationGraph` 接口，添加新字段

- ✅ **`src/types/relationGraph.ts`**
  - 扩展 `RelationGraphState` 接口，添加 `originalQueryId`、`redirectMessage` 字段

#### 2. 状态管理更新
- ✅ **`src/store/useRelationGraphStore.ts`**
  - 添加新字段到初始状态
  - 在 `initializeGraph()` 中处理自动转换状态
  - 更新 `resetGraph()` 方法

#### 3. UI 优化
- ✅ **`src/pages/utilization/RelationshipQueryView.tsx`**
  - 添加自动转换提示（Toast）
  - 实现原始查询档案高亮显示
  - 使用 `useEffect` 监听 store 状态变化

- ✅ **`src/components/relation-graph/ThreeColumnLayout.tsx`**
  - 添加 `highlightedArchiveId` 属性支持
  - 所有卡片（上游/下游/中心）都支持高亮显示

---

### Phase 3: 性能优化（已完成）

#### 1. 缓存机制
- ✅ **`RelationController.java`**
  - `findRelatedVoucher()` 方法添加 `@Cacheable` 注解
  - 缓存键：`archiveVoucherMapping::archive:voucher:{archiveId}`
  - 缓存 TTL：30 分钟（与 RedisConfig 配置一致）

- ✅ **`RedisConfig.java`**
  - 添加 `archiveVoucherMapping` 缓存命名空间
  - TTL：30 分钟

#### 2. 查询优化
- ✅ **递归查找优化**
  - 限制最大深度为 3 度（防止性能问题）
  - 使用 `visited` Set 防止循环查询
  - 优先查询直接关系（`ORIGINAL_VOUCHER`、`BASIS`）
  - 限制每次查询的关系数量（LIMIT 10/20）

- ✅ **批量查询优化**
  - 在递归查找前先检查当前节点是否为凭证
  - 避免不必要的递归调用

---

## 📊 修改文件清单

### 后端文件

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| `RelationGraphDto.java` | 添加 originalQueryId、autoRedirected、redirectMessage 字段 | ✅ |
| `RelationController.java` | 实现自动转换逻辑、查找关联凭证、缓存优化 | ✅ |
| `RedisConfig.java` | 添加 archiveVoucherMapping 缓存命名空间 | ✅ |

### 前端文件

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| `autoAssociation.ts` | 扩展 RelationGraph 接口 | ✅ |
| `relationGraph.ts` | 扩展 RelationGraphState 接口 | ✅ |
| `useRelationGraphStore.ts` | 添加新字段、处理自动转换状态 | ✅ |
| `RelationshipQueryView.tsx` | 添加提示和高亮逻辑 | ✅ |
| `ThreeColumnLayout.tsx` | 支持 highlightedArchiveId 属性 | ✅ |

---

## 🎯 功能验证清单

### 功能测试场景

| # | 测试场景 | 预期结果 | 状态 |
|---|---------|---------|------|
| 1 | 输入记账凭证（JZ-2025-01-001） | 直接以凭证为中心展示关系 | ⏳ 待测试 |
| 2 | 输入发票（FP-2025-01-001） | 自动查找关联凭证（JZ-2025-01-001），以凭证为中心，高亮发票 | ⏳ 待测试 |
| 3 | 输入合同（HT-2025-02-001） | 自动查找关联凭证，以凭证为中心 | ⏳ 待测试 |
| 4 | 输入无关联凭证的档案 | 以原档案为中心（向后兼容），显示提示信息 | ⏳ 待测试 |
| 5 | 自动转换提示显示 | Toast提示显示，原始档案高亮 | ⏳ 待测试 |
| 6 | 缓存性能测试 | 第二次查询相同档案时，从缓存读取，响应时间<100ms | ⏳ 待测试 |

### 性能测试

| # | 测试项 | 预期结果 | 状态 |
|---|-------|---------|------|
| 1 | 递归查找深度限制 | 最多3度，避免性能问题 | ✅ 已实现 |
| 2 | 缓存命中率 | 相同档案第二次查询命中缓存 | ✅ 已实现 |
| 3 | 响应时间 | 首次查询<500ms，缓存查询<100ms | ⏳ 待测试 |

---

## 🔄 后续优化建议

### Phase 3 补充（可选）

1. **缓存失效机制**
   - 在 `ArchiveRelationService` 中添加 `@CacheEvict` 注解
   - 当关系数据创建/更新/删除时，清除相关缓存

2. **批量查询优化**
   - 如果找到多个关联凭证，可以考虑返回列表供用户选择

### Phase 4: 测试与文档（待完成）

1. **单元测试**
   - 测试 `isVoucher()` 方法
   - 测试 `findRelatedVoucher()` 方法（各种场景）
   - 测试 `findVoucherInRelationChain()` 方法（循环关系、深度限制）

2. **集成测试**
   - 测试完整 API 流程
   - 测试缓存机制

3. **文档更新**
   - 更新 API 文档
   - 更新用户使用手册
   - 更新开发文档

---

## 🎉 实施成果

### 核心修复

✅ **记账凭证始终作为穿透查询的中心节点**，符合会计业务逻辑和合规要求

### 用户体验提升

✅ **自动转换**：无需用户手动查找凭证，系统自动处理  
✅ **清晰提示**：Toast 提示说明自动转换原因  
✅ **高亮显示**：原始查询档案高亮，便于识别

### 性能优化

✅ **缓存机制**：Redis 缓存"档案→凭证"映射关系（TTL 30分钟）  
✅ **查询优化**：限制递归深度、优先直接关系、防止循环查询

---

## 📝 测试建议

### ⚠️ 重要提示：全宗权限

**测试数据所在全宗**：所有 demo 数据都在 `BR-GROUP` 全宗下。

**使用前必须**：
1. **切换到正确的全宗**：在页面顶部使用全宗切换器，选择 `BR-GROUP` 全宗
2. **或确保用户有权限**：当前用户必须有 `BR-GROUP` 全宗的访问权限

**如果遇到权限错误**：
- 错误提示：`您没有权限查看此档案的关系数据`
- 原因：当前选择的全宗与数据所在全宗不匹配，或用户没有权限
- 解决：切换到 `BR-GROUP` 全宗，或联系管理员分配权限

### 快速验证

1. **启动开发服务器**
   ```bash
   npm run dev
   ```

2. **切换全宗**
   - 访问：`http://localhost:15175/system/utilization/relationship`
   - 在页面顶部切换到 `BR-GROUP` 全宗（如果没有，需要先给用户分配权限）

3. **测试场景1：输入发票**
   - 输入：`FP-2025-01-001`
   - 预期：自动切换到 `JZ-2025-01-001`，显示 Toast 提示，发票高亮

4. **测试场景2：输入凭证**
   - 输入：`JZ-2025-01-001`
   - 预期：直接以凭证为中心，无转换提示

5. **测试场景3：验证缓存**
   - 再次输入：`FP-2025-01-001`
   - 预期：响应更快（缓存命中）

---

**实施状态**: ✅ Phase 1-3 已完成，Phase 4 待测试验证

---

## 📚 相关变更说明

### 缓存失效策略

**当前实现**：
- `findRelatedVoucher()` 方法使用 `@Cacheable` 缓存结果（TTL 30分钟）
- 缓存键格式：`archiveVoucherMapping::archive:voucher:{archiveId}`

**建议后续优化**：
- 在 `ArchiveRelationService` 中添加缓存失效逻辑
- 当关系数据创建/更新/删除时，清除相关缓存：
  ```java
  @CacheEvict(value = "archiveVoucherMapping", key = "'archive:voucher:' + #relation.sourceId")
  @CacheEvict(value = "archiveVoucherMapping", key = "'archive:voucher:' + #relation.targetId")
  ```

**当前影响**：
- 缓存失效依赖 TTL（30分钟），关系数据变更后最多30分钟生效
- 对于查询场景，30分钟 TTL 可接受（关系数据变更频率低）

---

## 🚀 部署检查清单

### 部署前检查

- [ ] 后端代码编译通过（✅ 已确认）
- [ ] 前端代码无 lint 错误（✅ 已确认）
- [ ] Redis 缓存配置正确（✅ 已添加 `archiveVoucherMapping` 命名空间）
- [ ] 数据库迁移脚本已执行（V102 demo 数据）

### 部署后验证

- [ ] 启动后端服务，检查日志无错误
- [ ] 启动前端服务，检查页面加载正常
- [ ] 测试输入发票档号，验证自动转换功能
- [ ] 测试输入凭证档号，验证直接查询功能
- [ ] 检查 Redis 缓存是否正常写入
- [ ] 检查 Toast 提示是否正常显示
- [ ] 检查原始查询档案是否正常高亮

---

## 📖 使用说明

### 用户使用流程

1. **访问穿透联查页面**
   - 路径：`/system/utilization/relationship`

2. **输入档号查询**
   - 可以输入任意档号（发票、合同、报销单、凭证等）
   - 点击"查询"按钮

3. **查看自动转换提示**
   - 如果输入的是非凭证档案，系统会自动切换到关联的记账凭证
   - 页面会显示 Toast 提示："已自动切换到关联的记账凭证查看完整业务链路"
   - 原始查询的档案会高亮显示（金色边框）

4. **查看业务关系**
   - 左侧：上游数据（依据、凭证、来源）
   - 中心：核心单据（记账凭证）
   - 右侧：下游数据（流向、归档、结果）
   - 底部：关系说明列表

5. **点击查看详情**
   - 点击任何单据卡片，右侧会弹出详情抽屉
   - 查看单据的详细信息

### 业务逻辑说明

**为什么以记账凭证为中心？**
- 记账凭证是会计业务的核心节点
- 所有原始凭证（发票、合同等）都会归集到记账凭证
- 记账凭证是后续归档、报表生成的基础
- 符合《会计档案管理办法》和 DA/T 94-2022 规范要求

**自动转换的好处**
- 无需用户手动查找关联凭证
- 自动展示完整的业务链路
- 提升查询效率和准确性
