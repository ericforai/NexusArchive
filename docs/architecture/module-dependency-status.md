# 模块依赖现状

> **目的**：非侵入式记录系统中的跨模块依赖，作为架构演进的参考文档。
>
> **原则**：记录现状，评估风险，渐进改善。避免为了"架构纯洁性"进行高风险重构。

---

## 一、已确认的跨模块依赖

### 1. 预归档 → 正式归档

| 属性 | 详情 |
|------|------|
| **位置** | `PreArchiveSubmitService.java:84-161` |
| **依赖方式** | 直接注入 `ArchiveMapper`，绕过 `ArchiveService` |
| **代码证据** | `Archive existingArchive = archiveMapper.selectById(fileId);` |
| **方法** | `submitForArchival()`, `completeArchival()` |

#### 存在原因

```
1. ERP 同步场景：当文件来自 ERP 同步时，Archive 记录已存在（ID 相同）
   - 需要更新而非创建 Archive
   - 需要生成正式档号替换临时档号

2. 状态转换需要：
   - ArcFileContent.preArchiveStatus: READY_TO_ARCHIVE → SUBMITTED → COMPLETED
   - Archive.status: PENDING → archived
   - 两个实体的状态需要原子性协调
```

#### 潜在风险

| 风险 | 影响 | 概率 |
|------|------|------|
| 档号生成逻辑分散 | 档号冲突或重复 | 低 |
| 状态转换不一致 | 预归档与正式归档状态不同步 | 中 |
| Archive 业务规则绕过 | 权限、校验逻辑被跳过 | 低 |

#### 缓解措施

- [x] `@Transactional(propagation = REQUIRES_NEW)` 保证事务隔离
- [x] 状态枚举统一管理 `PreArchiveStatus`
- [ ] 需要补充集成测试覆盖状态转换路径

---

### 2. 销毁 → Archive

| 属性 | 详情 |
|------|------|
| **位置** | `DestructionServiceImpl.java`, `DestructionApprovalServiceImpl.java` |
| **依赖方式** | 直接注入 `ArchiveMapper`，直接操作 `Archive` 实体 |
| **代码证据** | `archiveMapper.selectBatchIds(archiveIds)` |
| **方法** | `createDestruction()`, `executeDestruction()`, `updateArchiveStatus()` |

#### 存在原因

```
1. 需要读取 Archive.destructionHold 标志判断是否可销毁
2. 需要执行 Archive 的逻辑删除（@TableLogic）
3. 需要更新 Archive.destructionStatus 状态
```

#### 潜在风险

| 风险 | 影响 | 概率 |
|------|------|------|
| Archive 状态变更绕过 ArchiveService | 业务规则不一致 | 中 |
| 冻结档案逻辑分散 | 非预期的销毁 | 低 |
| 双人复核逻辑复杂化 | 状态机混乱 | 低 |

#### 缓解措施

- [x] `DestructionHold` 标志在 Archive 实体上强制校验
- [x] 双人复核通过状态机约束（PENDING → FIRST_APPROVED → DESTRUCTION_APPROVED）
- [x] 逻辑删除使用 MyBatis-Plus `@TableLogic`
- [ ] 需要补充集成测试覆盖销毁流程

---

### 3. 审批流程 → 预归档

| 属性 | 详情 |
|------|------|
| **位置** | `ArchiveApprovalServiceImpl.java:104` |
| **依赖方式** | 通过 `@Lazy` 注入 `PreArchiveSubmitService` |
| **代码证据** | `preArchiveSubmitService.completeArchival(archive.getId());` |
| **方法** | `approveArchive()` 调用 `completeArchival()` |

#### 存在原因

```
1. 归档审批通过后，需要触发：
   - OFD 文件签名
   - 文件锁定
   - 状态变更为 archived

2. 这些逻辑在 PreArchiveSubmitService.completeArchival() 中已实现
3. 使用 @Lazy 避免循环依赖
```

#### 潜在风险

| 风险 | 影响 | 概率 |
|------|------|------|
| 循环依赖风险 | 启动失败 | 低（已用 @Lazy 缓解） |
| 职责不清晰 | 难以定位问题 | 中 |

#### 缓解措施

- [x] 使用 `@Lazy` 注解避免循环依赖
- [ ] 考虑将 `completeArchival()` 提取为独立的事件监听器

---

## 二、核心服务职责分析

### ArchiveService

| 指标 | 值 | 评估 |
|------|-----|------|
| 代码行数 | 473 行 | 偏高，但功能完整 |
| 公开方法数 | 15 个 | 偏多 |
| 职责类型 | 查询、创建、更新、删除、权限、验证、关联 | 上帝类倾向 |

#### 为什么暂不拆分？

```
1. 稳定性：当前代码运行稳定，无频繁变更
2. 测试覆盖：已有充分的单元和集成测试
3. 拆分成本：需要重构所有调用方，风险极高
4. 收益不确定：拆分后未必提升可维护性
```

#### 潜在风险

| 风险 | 缓解 |
|------|------|
| 上帝类难以维护 | 充分的单元测试，清晰的注释 |
| 修改影响范围广 | 任何修改都需要充分测试 |
| 新人理解成本高 | 完善 Javadoc，提供使用示例 |

---

## 三、模块化标杆（已实现的 DDD 结构）

### Borrowing 模块

```
modules/borrowing/
├── api/dto/          ← 对外契约
├── api/BorrowingController.java
├── app/              ← 应用服务
│   ├── BorrowingFacade.java
│   └── BorrowingApplicationService.java
├── domain/           ← 领域模型
│   ├── Borrowing.java
│   └── BorrowingStatus.java
└── infra/            ← 基础设施
    ├── mapper/BorrowingMapper.java
    └── ...
```

**特点**：
- 对外只暴露 `BorrowingFacade` 和 `api.dto`
- 依赖 `ArchiveService`（只读接口）和 `DataScopeService`
- 状态机内聚于 `BorrowingStatus` 枚举
- 完整的单元测试覆盖

### 新功能应采用的模板

对于**新开发的功能**，使用以下模板：

```
modules/<feature-name>/
├── api/dto/          ← 对外 DTO
├── app/              ← 应用服务 / Facade
├── domain/           ← 领域模型
└── infra/            ← 基础设施
```

**旧功能保持现状，不强制重构。**

---

## 四、风险缓解措施

### 1. 测试覆盖

| 场景 | 测试类型 | 文件 | 状态 |
|------|----------|------|------|
| 预归档 → 正式归档状态转换 | 集成测试 | `PreArchiveToArchiveBoundaryTest.java` | ✅ 已补充 |
| 销毁执行流程 | 集成测试 | `DestructionToArchiveBoundaryTest.java` | ✅ 已补充 |
| 审批 → 预归档回调 | 集成测试 | `ApprovalToPreArchiveBoundaryTest.java` | ✅ 已补充 |
| ArchiveService CRUD | 单元测试 | 已有测试 | ✅ 已有 |
| 借阅状态机 | 单元测试 | 已有测试 | ✅ 已有 |
| **依赖契约验证** | **契约测试** | **`ModuleDependencyContractTest.java`** | **✅ 已补充** |

#### 契约测试说明

`ModuleDependencyContractTest.java` 验证文档中记录的跨模块依赖关系在代码中保持一致：

| 验证项 | 说明 |
|--------|------|
| ARCHITECTURE-NOTE 注释 | 确保文档化的依赖关系有对应代码注释 |
| MAPPER_DEPENDENCY | 验证已接受的 Mapper 直接依赖存在 |
| LAZY_DEPENDENCY | 验证 @Lazy 注解用于避免循环依赖 |
| 文件存在性 | 确保契约文件未被移动/删除 |

**当契约测试失败时**：
1. 检查代码重构是否改变了依赖关系
2. 如果是，更新 `module-dependency-status.md`
3. 如果不是，恢复被误删的注释或依赖

**运行方式**：
```bash
# 单独运行契约测试
mvn test -Dtest=ModuleDependencyContractTest

# 运行所有架构测试（含契约测试）
mvn test -Dgroups=architecture
```

### 2. 文档同步

当以下变更发生时，需要更新本文档：

- [ ] 新增跨模块依赖
- [ ] 修改现有依赖方式
- [ ] 模块结构重组

### 3. 代码审查清单

在 Code Review 时关注：

- [ ] 是否引入了新的跨模块直接依赖？
- [ ] 如果是，是否考虑过通过 Facade/Service 解耦？
- [ ] 是否更新了本文档？

---

## 五、演进路线图

### 短期（1-2 周）

- [x] 创建本文档
- [x] 补充边界场景的集成测试（3 个文件，18 个场景）
- [x] 在关键依赖点添加 `// ARCHITECTURE-NOTE:` 注释
- [x] 创建依赖契约测试，自动检测文档与代码不一致

### 中期（1-2 月）

- [ ] 新功能采用 `modules/xxx` DDD 结构
- [ ] 旧功能保持现状，只修 bug
- [ ] 评估是否有必要创建 `ArchiveFacade` 统一对外接口

### 长期（按需）

- [ ] 如果某个模块频繁变更导致问题，考虑局部重构
- [ ] **不要为了"架构纯洁性"而重构**
- [ ] 每个重构决策都需要成本效益分析

---

## 六、相关文档

- [Module Boundaries](module-boundaries.md) - 模块边界规则
- [Data Ownership Map](data-ownership-map.md) - 数据主权清单
- [Contract Catalog](contract-catalog.md) - 对外契约清单

---

**更新日期**: 2026-03-14
**更新者**: Software Architect Agent
**下次审查**: 2026-04-14
