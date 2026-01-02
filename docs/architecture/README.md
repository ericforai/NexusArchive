本目录存放系统架构相关文档。
用于描述整体设计与模块关系。

## 最新更新 (2026-01-02)

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

## 历史更新

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
| `modularization-refactoring-2025-12-31.md` | 文档 | 模块化重构记录 |
| **其他** | | |
| `data-ownership-map.md` | 文档 | 数据主权清单 |
| `contract-catalog.md` | 文档 | 对外契约清单 |
| `variability-registry.md` | 文档 | 变体登记表 |
| `self-review-sop.md` | 文档 | 自我审查清单 |
| `group_version_assessment_20251217.md` | 文档 | group_version 评估 |
| `page_migration_assessment_20251226.md` | 文档 | page 迁移评估 |
