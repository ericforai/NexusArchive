# 预归档单据池空数据问题深度分析

**发生时间**: 2026-01-15  
**问题现象**: 预归档单据池页面点击具体类型后看不到单据  
**影响范围**: 单据池功能完全不可用

---

## 一、问题表象

```
用户路径：系统管理 → 预归档 → 单据池 → 点击"银行回单"
预期结果：看到银行回单列表
实际结果：空白，没有数据
```

---

## 二、根本原因分析

这是一个**多层状态不一致**导致的复合问题，涉及 6 个层面：

### 2.1 API 契约不一致（第一层）

**前端期望的参数**：
```typescript
// OriginalVoucherListView.tsx
const poolStatusFilter = poolMode ? 'ENTRY,PARSED,PARSE_FAILED' : 'ARCHIVED';

// 发送请求
getOriginalVouchers({ poolStatus: poolStatusFilter, type: 'BANK_RECEIPT' })
```

**后端实际情况**：
```java
// OriginalVoucherController.java (修复前)
public Result<Page<OriginalVoucher>> list(..., String status) {
    // 没有 poolStatus 参数！
    return Result.success(voucherService.getVouchers(..., status));
}
```

**问题**：前端发送 `poolStatus` 参数，但后端根本不接收。

---

### 2.2 状态模型语义不一致（第二层）

| 前端概念 | poolStatus 值 | 后端概念 | archiveStatus 值 |
|---------|---------------|---------|-----------------|
| 单据池（未处理） | ENTRY, PARSED, PARSE_FAILED | 草稿 | DRAFT |
| 已归档 | ARCHIVED | 已归档 | ARCHIVED |

**问题**：前端用 4 个状态表达"处理中"的不同阶段，后端只用 1 个 DRAFT 状态。

---

### 2.3 类型代码历史遗留问题（第三层）

**前端类型定义**：
```typescript
DOC_POOL_TYPES = [
  { type: 'BANK_RECEIPT', name: '银行回单' },
  { type: 'INV_VAT_E', name: '增值税电子发票' },
]
```

**数据库实际存储**：
```sql
-- 结果：BANK_SLIP, VAT_INVOICE
```

**类型配置表**（同时存在新旧代码）：
```sql
BANK_RECEIPT  | 银行回单   -- 新代码
BANK_SLIP     | 银行回单   -- 旧数据用的这个
INV_VAT_E     | 增值税电子发票
VAT_INVOICE   | 增值税专票  -- 旧数据用的这个
```

---

### 2.4 全宗代码体系混乱（第四层）

| 系统 | 字段 | 格式示例 |
|-----|------|---------|
| 单据表 | fonds_code | BR01, BRJT, default |
| 法人表 | id | ORG_BR_GROUP |
| 权限表 | fonds_no | DEMO, BR-GROUP |

**三套格式，完全无法对应！**

---

### 2.5 业务状态与数据状态错位（第五层）

**预归档单据池页面的预期**：显示"待处理"的单据  
**数据库中的实际状态**：所有单据都是 `ARCHIVED`（已归档）

```sql
SELECT archive_status, COUNT(*) FROM arc_original_voucher GROUP BY archive_status;
-- 结果：ARCHIVED | 6
```

**语义完全矛盾**：页面叫"预归档"，数据都是"已归档"。

---

### 2.6 缓存陷阱（第六层）

```java
@Cacheable(value = "fondsScope", key = "'user:' + #userId")
public List<String> getAllowedFonds(String userId) {
    return scopeMapper.findFondsNoByUserId(userId);
}
```

修改数据库后，缓存未失效，导致修改不生效。

---

## 三、为什么这个问题如此顽固？

### 3.1 问题的"洋葱模型"

```
┌─────────────────────────────────────────────────────────┐
│  第六层：缓存问题（修改数据不生效）                       │
├─────────────────────────────────────────────────────────┤
│  第五层：业务状态错位（页面名称与数据状态不符）           │
├─────────────────────────────────────────────────────────┤
│  第四层：全宗代码混乱（三套格式无法对应）                 │
├─────────────────────────────────────────────────────────┤
│  第三层：类型代码历史遗留（新旧代码并存）                 │
├─────────────────────────────────────────────────────────┤
│  第二层：状态模型语义不匹配（前后端状态定义不一致）       │
├─────────────────────────────────────────────────────────┤
│  第一层：API 契约不一致（参数名不匹配）                   │
└─────────────────────────────────────────────────────────┘
```

**修复第一层后，发现第二层问题；修复第二层后，发现第三层...**

每修好一层才暴露下一层，就像剥洋葱一样。

### 3.2 技术债务的累积效应

| 时间点 | 变更 | 产生的债务 |
|-------|------|----------|
| 初始版本 | 使用 `BANK_SLIP` 类型代码 | 类型代码命名 |
| 某次迭代 | 前端改用 `BANK_RECEIPT` | 前后端不一致 |
| 某次重构 | 法人表改用 `ORG_*` ID | 全宗代码不一致 |
| 某次优化 | 引入 `poolStatus` 概念 | 状态模型不一致 |
| 数据迁移 | 单据全宗代码未同步更新 | 数据孤儿 |

**每一层问题单独看都不严重，但叠加在一起就形成了"不可见数据"的现象。**

---

## 四、修复方案总结

### 4.1 API 契约统一
```java
// Controller 添加 poolStatus 参数
@RequestParam(required = false) String poolStatus

// Service 映射处理
if (poolStatus.contains("ARCHIVED")) {
    wrapper.eq(OriginalVoucher::getArchiveStatus, "ARCHIVED");
} else {
    wrapper.eq(OriginalVoucher::getArchiveStatus, "DRAFT");
}
```

### 4.2 类型代码别名映射
```java
private List<String> getTypeAliases(String typeCode) {
    return switch (typeCode) {
        case "BANK_RECEIPT" -> List.of("BANK_RECEIPT", "BANK_SLIP");
        case "INV_VAT_E" -> List.of("INV_VAT_E", "VAT_INVOICE");
        default -> List.of(typeCode);
    };
}
```

### 4.3 全宗代码统一
```sql
-- 1. 创建统一的法人实体
INSERT INTO sys_entity (id, name, status) VALUES ('DEMO', '演示全宗', 'ACTIVE');

-- 2. 更新单据的全宗代码
UPDATE arc_original_voucher SET fonds_code = 'DEMO' 
WHERE fonds_code IN ('BR01', 'BRJT', 'default');
```

### 4.4 业务状态修正
```sql
-- 预归档单据池应该显示 DRAFT 状态的单据
UPDATE arc_original_voucher SET archive_status = 'DRAFT' WHERE fonds_code = 'DEMO';
```

### 4.5 缓存清理
```bash
docker exec nexus-redis redis-cli --scan --pattern "fondsScope*" | xargs redis-cli DEL
```

---

## 五、预防措施（经验教训）

### 5.1 设计原则

1. **单一事实来源（SSOT）**
   - 枚举值应该只有一处定义
   - 前后端共享同一个类型定义文件（OpenAPI spec）

2. **向后兼容的 API 设计**
   - 添加新参数时，旧参数继续工作
   - 使用版本化的 API

3. **状态机明确化**
   - 在数据库中明确定义状态转换规则
   - 使用 CHECK 约束限制非法状态

### 5.2 代码审查检查点

当修改以下内容时，需要特别检查：
- [ ] API 参数变更 → 检查所有调用方
- [ ] 状态枚举变更 → 检查数据迁移
- [ ] 类型代码变更 → 检查类型别名
- [ ] 全宗/组织架构变更 → 检查关联表
- [ ] 缓存相关 → 检查缓存失效

### 5.3 测试策略

```java
@Test
void shouldQueryVouchersWithLegacyTypeCodes() {
    // 数据库中有 BANK_SLIP 类型的单据
    OriginalVoucher oldType = createVoucher("BANK_SLIP");

    // 前端用 BANK_RECEIPT 查询，应该能查到
    Page<OriginalVoucher> result = service.getVouchers(
        1, 10, null, null, "BANK_RECEIPT", null, null, null, null
    );

    assertThat(result.getRecords()).contains(oldType);
}
```

### 5.4 监控和告警

- 添加"查询结果为空但预期有数据"的监控
- 记录 API 请求参数与实际查询条件的日志
- 定期检查数据一致性

---

## 六、快速诊断清单

当遇到"数据查不到"类型的问题时，按以下顺序排查：

1. **API 层**：前端发送的参数 vs 后端接收的参数
2. **映射层**：参数值 vs 数据库实际存储值
3. **权限层**：用户是否有该数据的访问权限
4. **状态层**：数据的业务状态是否符合页面筛选条件
5. **缓存层**：是否使用了缓存，缓存是否过期
6. **关联层**：外键关联的实体是否存在

---

## 七、相关文件

- `OriginalVoucherController.java` - API 入口
- `OriginalVoucherService.java` - 业务逻辑和类型映射
- `FondsScopeService.java` - 全宗权限缓存
- `OriginalVoucherListView.tsx` - 前端查询组件
- `src/api/originalVoucher.ts` - 前端 API 调用

---

## 附录：完整的问题-修复映射表

| 问题 | 修复位置 | 修复方式 |
|-----|---------|---------|
| 后端不接收 poolStatus | Controller | 添加参数 |
| poolStatus 映射错误 | Service | 添加状态映射逻辑 |
| 类型代码不匹配 | Service | 添加 getTypeAliases 方法 |
| 全宗代码不存在 | Database | 创建 DEMO 法人实体 |
| 单据全宗代码错误 | Database | UPDATE 为 DEMO |
| 单据状态错误 | Database | UPDATE 为 DRAFT |
| 缓存未失效 | Redis | 清除 fondsScope 缓存 |
