一旦我所属的文件夹有所变化，请更新我。

本目录存放系统架构相关文档。
用于描述整体设计与模块关系。

## 最新更新 (2026-01-08)

### DDD 模块化规范 v2.0 完成
已建立完整的后端 DDD 模块化规范：
- **模块模板**: `_template/` 四层架构模板（api/app/domain/infra）
- **ArchUnit 规则**: 从 7 条扩展到 10 条
- **模块创建 SOP**: 标准化新模块创建流程
- **交互式脚本**: `./scripts/create-module.sh <ModuleName>` 一键生成
- **前端依赖修复**: 0 violations（dependency-cruiser）

**新增后端规则**（规则 8-10）：
- 规则 8: 禁止 Service 实现类之间直接依赖
- 规则 9: Controller 不得依赖 Service 实现类（依赖倒置）
- 规则 10: Controller 不得直接依赖 Mapper

**快速创建新模块**：
```bash
./scripts/create-module.sh Payment    # 生成 payment 模块
./scripts/create-module.sh Voucher    # 生成 voucher 模块
```

详见: [后端模块创建 SOP](backend-module-creation-sop.md) | [模块清单](../nexusarchive-java/src/main/java/com/nexusarchive/modules/README.md)

### 前端依赖违规修复完成
- 修复 `DestructionApprovalPage` 导入违规
- 调整 hooks 规则，允许布局级组件使用通用 hooks
- 验证结果: **0 violations** (389 modules, 1251 dependencies)

---

## 历史更新 (2026-01-04)

### 前端模块化重构完成
已完成 `useArchiveListController.ts` 的模块化重构：
- **主控制器**: 650 行 → ~90 行 (-86%)
- **拆分结果**: 9 个专用 Hook (模式、查询、分页、选择、池、数据、加载、Toast、动作)
- **设计模式**: Compositor 组合器模式
- **编译状态**: ✅ TypeScript 通过
- **向后兼容**: ✅ 100% API 兼容
详见: [重构完成报告](../reports/useArchiveListController-refactoring-complete.md) | [模块化重构记录](./modularization-refactoring-2025-12-31.md#前端模块化重构)

### 熵减审查更新
已更新前端熵减审查报告，标记 `useArchiveListController` 为已完成。
详见: [熵减审查报告](../entropy-reduction-frontend-audit.md)

---

## 历史更新

### 2026-01-02

### 凭证预览抽屉系统
已重构档案详情查看体验：
- **UI 优化**: Modal → Drawer（响应式 50vw/70vw/100vw）
- **交互增强**: 路由变更自动关闭、展开-to-新页功能
- **状态管理**: 新增 `useDrawerStore` (Zustand)
- **组件模块**: `src/components/voucher/*` 独立组件库
详见: [凭证预览抽屉架构](./voucher-preview-drawer.md)

### 模块清单 v2.2.0
新增 4 个前端模块（FE.ARCHIVES, FE.DRAWER, FE.VOUCHER_PREVIEW, FE.DRAWER_STORE）
详见: [module-manifest.md](./module-manifest.md)

---

## 更新历史

### 2026-01-01

#### 架构防御机制 v1.0
已建立完整的架构防御体系，包括：
- **前端**: dependency-cruiser 配置，0 违规
- **后端**: ArchUnit 测试，7/7 通过
- **自动化**: 模块发现、清单验证、JSON 导出 API
详见: [架构防御实施指南](./architecture-defense-guide.md)

#### 模块清单 v2.1.0
模块清单已更新至 23 个后端模块，详见: [module-manifest.md](./module-manifest.md)

---

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| **架构防御** | | |
| `architecture-defense-guide.md` | 指南 | 架构防御机制实施指南 |
| `module-manifest.md` | **SSOT** | 模块清单与允许依赖 (v2.2.0) |
| `module-boundaries.md` | 文档 | 模块边界清单与允许依赖 |
| `module-catalog-2025-12-31.md` | 文档 | 模块目录详细说明 |
| **前端架构** | | |
| `voucher-preview-drawer.md` | 文档 | 凭证预览抽屉架构 (2026-01-02 新增) |
| `frontend-boundaries.md` | 文档 | 前端边界规则 |
| `frontend-boundaries-training.md` | 文档 | 边界训练记录 |
| `frontend-continuous-improvement.md` | 文档 | 前端架构持续改进 |
| **模块化重构** | | |
| `modularization-opportunities-2025-12-31.md` | 文档 | 模块化机会分析 |
| `modularization-refactoring-2025-12-31.md` | 文档 | 模块化重构记录（后端+前端）|
| **其他** | | |
| `data-ownership-map.md` | 文档 | 数据主权清单 |
| `contract-catalog.md` | 文档 | 对外契约清单 |
| `variability-registry.md` | 文档 | 变体登记表 |
| `self-review-sop.md` | 文档 | 自我审查清单 |
| `group_version_assessment_20251217.md` | 文档 | group_version 评估 |
| `page_migration_assessment_20251226.md` | 文档 | page 迁移评估 |
