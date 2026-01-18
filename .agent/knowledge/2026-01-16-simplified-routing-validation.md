# 简化路由一致性校验方案说明

**文档日期**：2026-01-16
**实施状态**：✅ 已完成

---

## 一、问题背景

在多全宗（Fonds）环境下进行 ERP 凭证同步时，存在以下风险：

1. **跨全宗数据污染**：用户在"总部全宗"视角下，可能误选了"分公司账套"进行同步，导致数据存入错误的全宗。
2. **超级管理员风险**：开发阶段设置的超级管理员拥有所有全宗权限，使得隔离机制形同虚设。
3. **UI 与后端逻辑冲突**：同步弹窗允许自由勾选组织，但这与后端预设的 `accbookMapping` 可能产生矛盾。

---

## 二、方案设计

### 核心原则
- ✅ **不改变现有路由逻辑**：继续使用 `FondsContext` 作为上下文
- ✅ **只添加入口校验**：在同步开始前进行 Guard Clause 检查
- ✅ **低风险、易回滚**：校验失败仅阻止操作，不影响其他功能

### 校验逻辑
```
用户发起同步请求
    ↓
提取待同步账套列表 (accbookCodes)
    ↓
遍历每个账套，检查 accbookMapping
    ↓
若账套映射的全宗 ≠ 当前全宗 → 抛出异常，阻止同步
    ↓
若所有校验通过 → 继续执行原有同步逻辑
```

---

## 三、实施内容

### 后端修改

#### 1. [ErpConfig.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/entity/ErpConfig.java)
新增方法：
- `getFondsForAccbook(String accbookCode)` - 根据账套代码查询映射的全宗
- `isAccbookMappedToFonds(String accbookCode, String fondsCode)` - 检查账套是否映射到指定全宗

#### 2. [ErpSyncService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/erp/ErpSyncService.java)
新增方法：
- `validateFondsAccbookMapping(ErpConfig, ErpConfig DTO)` - 在 `syncScenario()` 入口处调用的校验方法

### 前端修改

#### 3. [OnlineReceptionView.tsx](file:///Users/user/nexusarchive/src/pages/collection/OnlineReceptionView.tsx)
新增错误检测逻辑：
- `executeRealSync()` catch 块 - 检测 `路由校验失败` 关键字
- `pollSyncStatus()` FAIL 分支 - 同样检测并显示友好提示

---

## 四、行为对照表

| 场景 | 行为 | 说明 |
| :--- | :--- | :--- |
| 未配置 `accbookMapping` | ✅ 允许同步 | 兼容现有配置 |
| 配置了映射且当前全宗匹配 | ✅ 允许同步 | 正常情况 |
| 配置了映射但当前全宗不匹配 | ❌ 拒绝同步 | 新增保护行为 |

---

## 五、用户体验

### 错误场景示例
- **用户操作**：在"演示全宗 (CS002)"视角下，尝试同步"泊冉集团 (BR01)"账套
- **后端配置**：`accbookMapping = {"BR01": "001"}` 表示 BR01 属于总公司全宗 (001)
- **系统响应**：
  - 后端日志：`路由校验失败：账套 BR01 配置的全宗为 001，但当前全宗为 CS002`
  - 前端提示：`⚠️ 全宗不匹配：当前全宗与选择的账套配置不一致。请切换到正确的全宗后重试。`

---

## 六、后续建议

1. **UI 优化**：可考虑在同步弹窗中，仅显示当前全宗关联的账套，从源头避免误选。
2. **超管降权**：生产环境应禁用或降级超级管理员，确保最小权限原则。
3. **审计日志**：记录所有被拒绝的同步请求，便于安全审计。

---

**方案优势**：以最小改动实现了跨全宗数据隔离，符合等保 2.0 最小权限原则，且完全向后兼容。
