# 实施计划 (修订版) - 完善账套与全宗的自动映射路由同步

## 问题背景
在之前的分析中，我们确定了需要根据 `accbook_mapping` 动态切换全宗。但目前的 UI 交互与后端逻辑存在严重的**“权力边界冲突”**：

1. **信息丢失**：适配器漏掉 `accbookCode` 字段，导致路由丢失。
2. **上下文绑架**：同步任务目前受限于 UI 顶部切换的“当前全宗”，导致跨主体的账套数据被误存。
3. **UI 逻辑冗余**：同步弹窗允许用户手动勾选组织，这与后端预设的映射关系可能产生逻辑冲突（例如：在 A 全宗下同步属于 B 全宗的账套）。
4. **ThreadLocal 泄漏**：异步线程池中上下文残留风险。

## 方案目标
建立严密的“源账套 -> 映射校验 -> 目标上下文切换 -> 独立查重 -> 附件路径计算 -> 上下文还原”闭环。

## 拟议变更

### 1. 适配器层补齐元数据
#### [MODIFY] [VoucherDTO.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/dto/VoucherDTO.java)
- 确认包含 `accbookCode` 字段（已确认存在）。
#### [MODIFY] [YonSuiteErpAdapter.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/integration/erp/adapter/YonSuiteErpAdapter.java)
- 在遍历 `accbookCodes` 同步时，强制为每个生成的 `VoucherDTO` 设置其归属的 `accbookCode`。

### 2. 同步引擎重构逻辑 (核心路由逻辑)
#### [MODIFY] [ErpSyncService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/erp/ErpSyncService.java)
- **取消“当前全宗”依赖**：同步任务不再盲目信任 API 传入的 `currentFonds` 作为归档全宗。
- **强制映射路由**：
    1. 在 `processVouchers` 循环中，针对每一条凭证，**强制**通过其 `accbookCode` 在 `accbookMapping` 中查找目标全宗。
    2. **权限校验**：校验该目标全宗是否在当前操作人的授权范围内（防止越权采集）。
    3. **动态切换**：在查重和保存前，通过 `try-finally` 切换 `FondsContext`。
    4. **异常处理**：若账套无对应映射，将该凭证标记为“路由异常”并记录，不再强行存入当前可见全宗。

### 3. 持久化层适配
#### [MODIFY] [VoucherPersistenceService.java](file:///Users/user/nexusarchive/nexusarchive-java/src/main/java/com/nexusarchive/service/erp/VoucherPersistenceService.java)
- 确保 `setStoragePath` 和 `Archive` 创建逻辑直接读取 `FondsContext` 或 `ArcFileContent` 中的全宗字段，保证物理路径的一致性。

## 验证计划

### 回归测试
- 验证单全宗同步保持原有行为。
- 验证跨全宗同步时，审计日志最后记录的 `source_fonds` 是否为任务发起全宗，而非最后一个处理的全宗。

### 破坏性测试
- 模拟映射表中缺失某个账套的情景，验证系统是否会优雅报错而非存错全宗。
