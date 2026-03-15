# 系统设计审查报告：NexusArchive 电子会计档案管理系统

> **审查日期**: 2026-03-15
> **审查版本**: PRD v1.0 + Architecture v1.0.0
> **状态**: ✅ 审查完成（已按虚拟专家组标准重构）

---

## Step 0：假设与审查边界声明

### 已知前提
1. 系统采用模块化单体架构（Modular Monolith）。
2. 核心业务实体为 `Archive`，位于 `modules/archivecore`。
3. 存在预归档、销毁、审批等业务模块。

### 合理假设
1. 系统的长期目标是实现完全的领域驱动设计（DDD）隔离。
2. `ArchiveMapper` 代表了对核心数据的直接持久化访问。
3. 私有化部署环境要求极高的审计完整性及数据一致性。

### 审查边界（Out of Scope）
1. 本次审查不涵盖具体的 UI/UX 细节。
2. 不涵盖第三方 ERP 系统的内部逻辑，仅关注其与本系统的集成接口。

---

## 🛑 阻断点 (Showstoppers)

### 1. 多写入方违反单一数据所有者原则（架构师风险）
*   **问题**: `Archive` 实体存在 4 个写入方（`PreArchiveSubmitService`, `DestructionServiceImpl`, `DestructionApprovalServiceImpl`, `ArchiveApprovalServiceImpl`），绕过了其 Owner 模块及其核心 Service，直接通过 `ArchiveMapper` 写入。
*   **关联规范**: 模块化单体架构原则、系统一致性要求。
*   **风险**: 并发更新冲突、状态转换逻辑散乱、审计日志无法统一捕获、技术债务累积。
*   **证据来源**: `com.nexusarchive.service.impl.DestructionServiceImpl.java` L114 等多处代码路径。

---

## 🏛 架构优化方案

| 模块 | 现状 | 优化建议 | 难度 |
|------|------|----------|------|
| ArchiveCore | 写入路径分散 | **统一 ArchiveStateTransitionFacade**: 建立单一写入入口，所有状态变更必须通过此 Facade。 | 中 |
| 预归档/销毁/审批 | 模块边界模糊 | **完成 DDD 拆分**: 迁移逻辑到各自的 domain 层，通过 Facade 与核心模块交互。 | 高 |
| 前端 Feature | 存在 re-export | **清理 re-export**: 直接引用组件，增强打包工具的 Tree-shaking 能力。 | 低 |

---

## 📦 部署与交付策略

*   **安装包设计**: 需确保 `fonds_no` 与客户环境初始化脚本强绑定，防止数据越权风险。
*   **环境检测**: 离线环境下需预置所有依赖包，特别是与国密算法相关的安全库。
*   **数据迁移**: 考虑到 Archive 实体的复杂性，迁移工具必须支持状态机状态的完整映射和哈希链验证。

---

## ⚖️ 专家联合建议

### 电子会计档案与合规审计专家 (Compliance Authority)
> 必须确保所有涉及 Archive 状态变更的操作都能生成符合 **DA/T 94** 要求的元数据审计日志。目前的跨模块写入可能导致审计点缺失，存在审计不通过的法律风险。

### 高安全系统架构师 (Xinchuang Architect)
> **单一写路径（Single Writer Principle）**是保障高安全系统一致性的底座。应立即启动 P0 任务：重构写入逻辑，通过 `ArchiveStateTransitionFacade` 统一验证「四性」和状态转换。

### 企业级私有化交付与 DevOps 专家 (Delivery Strategist)
> 模块边界的模糊会增加补丁升级的复杂度。建议在完成 DDD 拆分后，为每个模块建立独立的集成测试，确保私有化环境下升级的稳健性。

---

## 变更记录

| 日期 | 版本 | 变更内容 | 操作人 |
|------|------|----------|--------|
| 2026-03-15 | 1.1.0 | 按虚拟专家组规范重构并迁移 | Antigravity |
| 2026-03-15 | 1.0.0 | 初始审查报告 | Claude (system-design-review) |
