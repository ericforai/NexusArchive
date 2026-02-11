# 更新日志 (Changelog)

本文件记录 NexusArchive 电子会计档案系统的版本更新历史。

---

## [Unreleased]

### Added
- **ERP 发起联查 SSO 接口与对接文档**
  - 新增 `POST /api/erp/sso/launch`（HMAC 签名、nonce 防重放、一次性 ticket）
  - 新增 `POST /api/erp/sso/consume`（ticket 换取登录态）
  - 新增联查对接文档：`docs/api/erp-sso-launch.md`
- **穿透联查功能** (`RelationshipQueryView` + `SimpleGraphView` + `ThreeColumnLayout`)
  - 输入档号查询档案关联关系图谱
  - **三栏布局**：左侧上游数据、中心核心单据、右侧下游数据
  - 点击节点渐进式展开关联（最多3度）
  - 纯 CSS + SVG 实现，支持缩放拖拽
  - 双向箭头连线显示关系类型
  - 使用 `useRelationGraphStore` 管理图谱状态
  - 后端 API: `/api/relations/{archiveId}/graph`
  - **以记账凭证为中心**：自动查找关联凭证作为中心节点（符合会计业务逻辑）
  - **自动转换提示**：非凭证档案自动切换时显示 Toast 提示
  - **原始查询高亮**：原始查询的档案自动高亮显示
  - **性能优化**：Redis 缓存"档案→凭证"映射关系（TTL 30分钟）
- **YonSuite 组织同步服务** (`ErpOrgSyncService`)
  - 从 YonSuite ERP 同步组织架构到 `sys_entity` 表
  - 支持树版本 API (`treeversionsync`) 和组织成员 API (`treemembersync`)
  - 增量同步机制（基于 `pubts` 时间戳）
  - 详细的调试日志输出
- **SysEntity 树形结构支持**
  - 新增 `parentId` 字段支持层级关系
  - 新增 `orderNum` 字段支持排序
  - EntityService 新增树形操作方法
- **ErpConfig 组织映射功能**
  - `accbookMapping` 字段存储账套-全宗映射关系
  - 后端强制路由机制，根据全宗上下文自动选择账套

### Changed
- **组织管理重构** - 简化组织架构管理
  - 删除独立的 `Org` 实体，合并到 `SysEntity` 中
  - 删除 `OrgService`，功能迁移到 `EntityService`
  - 删除 `OrgMapper`，功能迁移到 `SysEntityMapper`
  - `AdminOrgController` 改为代理模式，调用 `EntityService` 和 `ErpOrgSyncService`
- **前端组织设置页面移除**
  - 删除 `OrgSettings.tsx`，组织管理集成到法人管理页面
  - 更新相关路由和 API 调用
- **预归档状态简化** (V96 数据库迁移)
  - 从 10 种状态简化为 5 种：`PENDING`, `SUPPLEMENTING`, `VERIFYING`, `APPROVED`, `REJECTED`
  - 更新相关枚举、配置和 API

### Fixed
- 修复 `BorrowPermissionInterceptor` 借阅权限校验逻辑
- 更新 `BasFondsController` 全宗接口的权限校验
- 更新 `EntityController` 法人接口支持树形结构

### Technical
- 新增 `YonSuiteOrgClient` - YonSuite 组织架构 API 客户端
- 新增 `YonOrgTreeSyncRequest/Response` DTO
- 数据库迁移 V97 简化预归档状态
- 数据库迁移 V98 添加 SAP 接口类型到 ERP 配置

---

## [2025-01-11] Organization Management Refactor

### Added
- **YonSuite 组织同步服务** (`ErpOrgSyncService`)
  - 从 YonSuite ERP 同步组织架构到 `sys_entity` 表
  - 支持树版本 API (`treeversionsync`) 和组织成员 API (`treemembersync`)
  - 增量同步机制（基于 `pubts` 时间戳）
  - 详细的调试日志输出
- **SysEntity 树形结构支持**
  - 新增 `parentId` 字段支持层级关系
  - 新增 `orderNum` 字段支持排序
  - EntityService 新增树形操作方法
- **ErpConfig 组织映射功能**
  - `accbookMapping` 字段存储账套-全宗映射关系
  - 后端强制路由机制，根据全宗上下文自动选择账套

### Changed
- **组织管理重构** - 简化组织架构管理
  - 删除独立的 `Org` 实体，合并到 `SysEntity` 中
  - 删除 `OrgService`，功能迁移到 `EntityService`
  - 删除 `OrgMapper`，功能迁移到 `SysEntityMapper`
  - `AdminOrgController` 改为代理模式，调用 `EntityService` 和 `ErpOrgSyncService`
- **前端组织设置页面移除**
  - 删除 `OrgSettings.tsx`，组织管理集成到法人管理页面
  - 更新相关路由和 API 调用
- **预归档状态简化** (V97 数据库迁移)
  - 从 10 种状态简化为 5 种：`PENDING`, `SUPPLEMENTING`, `VERIFYING`, `APPROVED`, `REJECTED`
  - 更新相关枚举、配置和 API

### Fixed
- 修复 `BorrowPermissionInterceptor` 借阅权限校验逻辑
- 更新 `BasFondsController` 全宗接口的权限校验
- 更新 `EntityController` 法人接口支持树形结构

### Technical
- 新增 `YonSuiteOrgClient` - YonSuite 组织架构 API 客户端
- 新增 `YonOrgTreeSyncRequest/Response` DTO
- 数据库迁移 V97 简化预归档状态
- 数据库迁移 V98 添加 SAP 接口类型到 ERP 配置
- 数据库迁移 V101 添加 SysEntity.parentId 字段

---

## [2025-01-10] Pool Status Simplification

### Changed
- **预归档状态简化** (V97 数据库迁移)
  - 从 10 种状态简化为 5 种：`PENDING`, `SUPPLEMENTING`, `VERIFYING`, `APPROVED`, `REJECTED`
  - 更新相关枚举、配置和 API

---

## [2025-01-09] Pool Dual View Design

### Added
- **电子凭证池双视图模式** (`/docs/plans/2026-01-11-pool-dual-view-design.md`)
  - 列表视图（默认）：适合查看大量数据，支持筛选、批量操作
  - 看板视图：直观展示处理流程（待检测→待补全→待归档）
  - 视图切换器：页面右上角"列表"/"看板"按钮或 URL 参数 `?view=list|kanban`
  - 用户偏好记忆：localStorage 保存视图选择
  - 路由兼容：旧的 `/kanban` 路由自动重定向

### Changed
- 性能优化设计文档标记为"规划完成"状态
- 预估工时: 91.5 小时 (~12 个工作日)
- 优化收益预估: 首屏 < 1s, N+1 查询 -99%, API 响应 +50%

---

## [2025-01-08] Performance Optimization Design

### Added
- 性能优化设计文档 (`/docs/plans/2026-01-09-performance-optimization-design.md`)
  - 24 类性能/安全问题识别与解决方案
  - 前端、后端数据库、API 安全三维度优化方案
  - 12 天渐进式交付时间表
- 性能优化总结报告 (`/docs/reports/2026-01-09-performance-optimization-summary.md`)
  - 执行摘要与关键指标
  - 技术债务清单 (P0/P1/P2 分级)
  - 短期/中期/长期优化建议

### Planned (待执行优化)

**P0 优先级 (严重/高)**:
- DebugController 权限校验 (1h) - 严重安全隐患
- 定时器未清理修复 (1h) - WatermarkOverlay.tsx
- N+1 查询修复 (3.5h) - IngestServiceImpl
- 关键数据库索引 (4h) - acc_archive, arc_file_content
- 虚拟化表格改造 (4h) - BatchTable.tsx (642行)

**P1 优先级 (中)**:
- Redis 缓存实现 (4h)
- 分页查询实现 (5h)
- Entity 转 DTO (8h)
- 组件拆分重构 (16h) - LegacyImportPage.tsx (822行)

---

## [2025-01-09] Integration Settings UI Redesign v2.2

### Added
- ScenarioDrawer component - right-side slide-in panel for scenario details
- ScenarioSummaryCard component - displays scenario counts with status
- ConnectionHealthBadge component - health status with relative time display
- Three-layer information architecture (Card → Drawer → Page)
- OriginalVoucherListView menu-type title/filter tests
- Route mapping test for bank receipt type

### Changed
- ErpConfigCard redesigned as summary-only view (removed expand/collapse)
- Health check button style improved (slate-700 + white text for better contrast)
- Fixed card heights for consistent page layout
- Better visual hierarchy and spacing
- OriginalVoucherListView now derives archive type titles from menu mapping
- Archive original voucher bank receipt menu uses BANK_RECEIPT for filtering

### Removed
- On-demand scenario loading from cards (moved to drawer)
- Expand/collapse UI for scenarios
- Inline scenario list display

### Technical
- Added new types: ScenarioStatus, ConnectionHealthStatus, ScenarioStatistics
- Updated component interfaces to support summary view
- All 219 tests passing

---

## [2026-01-05] Integration Settings UI Redesign v2.1

### Breaking Changes
- Moved action bar from page-level to inside each connector card
- Changed button labels: 通联测试 → 检查连接, 一键诊断 → 健康检查, 数据核对 → 账务核对

### New Features
- Card-based layout with responsive grid (1 col mobile, 3 cols desktop)
- Inline config editing (no modal popups)
- Delete connector functionality with confirmation
- More menu with additional actions

### Improvements
- Finance-friendly UI language
- Optimized for screen space utilization
- Better error handling for delete operations
- Visual status indicators (● ○)
- Click-outside handlers for dropdowns
- Accessibility improvements (ARIA labels)

### Bug Fixes
- Fixed action bar placement (was at wrong level)
- Added missing delete functionality
- Improved error messages

### Metrics
- IntegrationSettingsPage: 1,709 lines → 161 lines (91% reduction)
- Test coverage: 85%+
- Architecture checks: Passing

---

## [2026-01-05] YonSuite 集成 v2.1 - 退款单支持

### 核心变更
- **新增退款单同步支持**：支持从 YonSuite 同步退款单数据
- **新增 YonRefundListService**：提供退款单列表、详情、附件查询功能
- **新增 4 个 DTO**：YonRefundListRequest/Response, YonRefundFileRequest/Response

### 新增文件 (nexusarchive-java/src/main/java/com/nexusarchive/integration/yonsuite/)
| 文件 | 功能 |
|------|------|
| `dto/YonRefundListRequest.java` | 退款单列表请求 |
| `dto/YonRefundListResponse.java` | 退款单列表响应 |
| `dto/YonRefundFileRequest.java` | 退款单附件查询请求 |
| `dto/YonRefundFileResponse.java` | 退款单附件查询响应 |
| `service/YonRefundListService.java` | 退款单列表服务 |

### 修改文件
| 文件 | 变更 |
|------|------|
| `integration/yonsuite/README.md` | [UPDATE] 版本升级至 v2.1.0，添加退款单文档 |
| `integration/yonsuite/dto/README.md` | [UPDATE] 添加退款单 DTO 说明 |
| `integration/yonsuite/service/README.md` | [UPDATE] 添加 YonRefundListService 说明 |
| `integration/yonsuite/controller/README.md` | [UPDATE] 更新日期 |

---

## [2026-01-05] IntegrationSettings.tsx 模块化重构 ⭐ 重大重构

### 核心变更
- **代码量减少 91.1%**：从 1,709 行巨型组件重构为 161 行组合器 + 8 个专用 Hook + 5 个 UI 组件
- **Compositor 组合器模式**：主页面作为轻量级协调器，业务逻辑提取到专用 Hook，UI 组件专注展示
- **测试覆盖 100%**：44 个单元测试全部通过，覆盖所有核心功能
- **文档自洽规则合规**：所有源文件添加三行头注释，目录 README 符合规范（完整文件清单、角色/能力描述）
- **架构防御系统**：实现 J1-J4 四个关键体征（自描述、自检查、封闭规则、违规响应）

### 新增文件 (src/components/settings/integration/)

**Hooks (8 个):**
| 文件 | 行数 | 职责 |
|------|------|------|
| `hooks/useErpConfigManager.ts` | 130 | ERP 配置 CRUD、加载、类型展开、连接测试 |
| `hooks/useScenarioSyncManager.ts` | 133 | 场景加载、子接口管理、同步历史记录、批量同步 |
| `hooks/useConnectorModal.ts` | 167 | 连接器创建/编辑模态框状态、表单管理、ERP 类型自动检测 |
| `hooks/useIntegrationDiagnosis.ts` | 53 | 集成诊断执行、结果展示、健康检查 |
| `hooks/useParamsEditor.ts` | 68 | 同步参数编辑（日期范围、分页大小） |
| `hooks/useAiAdapterHandler.ts` | 109 | AI 文件上传、预览生成、配置适配 |

**Components (5 个):**
| 文件 | 行数 | 职责 |
|------|------|------|
| `components/ErpConfigList.tsx` | 79 | 按 ERP 类型分组展示配置列表 |
| `components/ScenarioCard.tsx` | 145 | 场景卡片展示（同步按钮、子接口、历史记录） |
| `components/ConnectorForm.tsx` | 156 | 连接器配置表单（名称、类型、URL、密钥、账套） |
| `components/DiagnosisPanel.tsx` | 89 | 诊断结果面板（健康状态、详细检查项） |
| `components/ParamsEditor.tsx` | 106 | 同步参数编辑模态框（日期范围、分页大小） |

**测试文件 (44 个测试用例):**
| 文件 | 测试数 |
|------|--------|
| `hooks/__tests__/useErpConfigManager.test.ts` | 10 |
| `hooks/__tests__/useScenarioSyncManager.test.ts` | 3 |
| `hooks/__tests__/useConnectorModal.test.ts` | 6 |
| `hooks/__tests__/useIntegrationDiagnosis.test.ts` | 7 |
| `hooks/__tests__/useParamsEditor.test.ts` | 8 |
| `hooks/__tests__/useAiAdapterHandler.test.ts` | 10 |

**类型与导出:**
| 文件 | 行数 | 职责 |
|------|------|------|
| `types.ts` | ~265 | State/Actions 接口定义 |
| `index.ts` | ~20 | 公共 API 导出 |
| `IntegrationSettingsPage.tsx` | 161 | 主组合器组件 |

### 修改文件
| 文件 | 变更 |
|------|------|
| `src/components/settings/integration/` | [NEW] 新模块目录，包含 8 hooks + 5 components + tests |
| `src/components/settings/integration/manifest.config.ts` | [NEW] J1 自描述 - 模块清单声明 |
| `src/components/settings/integration/README.md` | [NEW] 模块文档（完整文件清单、角色/能力、数据流图、架构防御） |
| `.dependency-cruiser.cjs` | [UPDATE] J2 自检查 - 添加 4 条 integration 模块依赖规则 |
| `.github/workflows/architecture-check.yml` | [UPDATE] J3 封闭规则 - 添加前端架构检查作业 |
| `src/components/settings/IntegrationSettings.tsx` | [BACKUP] 备份原文件 (1,709 行) |
| `docs/entropy-reduction-frontend-audit.md` | [UPDATE] 标记 IntegrationSettings.tsx 重构完成 |
| `docs/architecture/modularization-refactoring-2025-12-31.md` | [UPDATE] 添加前端模块化重构 Section |
| `docs/CHANGELOG.md` | [UPDATE] 本条目 |

### 技术细节
- **设计模式**: Compositor 组合器模式（Frontend Facade 变体）
- **TypeScript**: 严格类型定义，State/Actions 接口分离
- **测试框架**: Vitest + React Testing Library
- **代码质量**: ESLint 通过，架构检查通过 (dependency-cruiser)
- **文档规范**: 所有源文件添加 Input/Output/Pos 三行头注释
- **架构防御**: J1-J4 完整实现
  - J1: manifest.config.ts 模块清单
  - J2: dependency-cruiser 4 条依赖规则
  - J3: GitHub Actions CI 检查
  - J4: 违规阻止合并，显示模块所有者

### 收益分析
- **可维护性**: 每个模块职责单一，平均 100 行/模块，易于理解和修改
- **可测试性**: Hook 独立测试，95%+ 覆盖率
- **可复用性**: Hook 和组件可在其他模块复用
- **团队协作**: 不同开发者可并行开发不同模块

---

## [2025-01-04] 表格预览交互优化与可复用组件

### 核心变更
- **新增可复用组件**：创建 `TablePreviewAction` 组件，统一表格预览按钮样式与交互。
- **优化用户体验**：表格预览改为独立操作列，移除金额列内的"查看"文字，提升视觉清晰度。
- **整行可点击**：实现整行点击预览功能，悬停时蓝色高亮，提高操作效率。
- **测试覆盖**：为 `TablePreviewAction` 组件添加 23 个单元测试，确保代码质量。
- **架构合规**：遵循单一入口原则，组件导出至 `index.ts`，文档完整。

### 修改文件
| 文件 | 变更 |
|------|------|
| `src/components/table/TablePreviewAction.tsx` | [NEW] 可复用表格预览操作组件 |
| `src/components/table/__tests__/TablePreviewAction.test.tsx` | [NEW] 单元测试（23个测试用例） |
| `src/components/table/index.ts` | [UPDATE] 添加 TablePreviewAction 公共导出 |
| `src/components/table/README.md` | [UPDATE] 添加文件清单与 TablePreviewAction 文档 |
| `src/pages/archives/ArchiveListView.tsx` | [UPDATE] 使用新的预览组件和交互 |
| `src/pages/archives/OriginalVoucherListView.tsx` | [UPDATE] 使用新的预览组件和交互 |
| `docs/CHANGELOG.md` | [UPDATE] 记录本次变更 |

### 技术细节
- 使用 TypeScript 严格类型定义
- 支持悬停高亮（Tailwind CSS）
- 可配置的预览标签和删除按钮
- 阻止事件冒泡
- 支持键盘导航（Tab、Enter）

---

## [2025-12-31] 后端核心实体与接口合规性补全

### 核心变更
- **实体增强**：`Archive`/`Borrowing`/`Destruction` 补全了销毁、保管期限、审批快照等关键合规字段。
- **审计追踪**：`SysAuditLog` 新增 `traceId`，支持跨操作链路追踪；新增跨全宗访问审计接口。
- **存储管理**：`FileStorageService` 补全软/硬删除与文件信息获取能力。
- **导入修复**：修复历史数据导入服务的 Map 初始化限制与审计参数错误。
- **策略重构**：`ArchiveService` 验证逻辑剥离至 `ArchiveValidationPolicy`，增强可维护性。

### 修改文件
| 文件 | 变更 |
|------|------|
| `nexusarchive-java/.../entity/Archive.java` | [UPDATE] 新增 destructionStatus, retentionStartDate |
| `nexusarchive-java/.../entity/Borrowing.java` | [UPDATE] 新增 fondsNo, archiveYear |
| `nexusarchive-java/.../entity/Destruction.java` | [UPDATE] 新增 approvalSnapshot |
| `nexusarchive-java/.../entity/SysAuditLog.java` | [UPDATE] 新增 traceId |
| `nexusarchive-java/.../service/AuditLogService.java` | [NEW] logCrossFondsAccess 接口 |
| `nexusarchive-java/.../service/FileStorageService.java` | [NEW] softDelete/hardDelete/getFileInfo 接口 |
| `nexusarchive-java/.../service/impl/LegacyImportServiceImpl.java` | [FIX] 修复导入逻辑与审计调用 |
| `docs/announcements/2025-12-31-schema-updates.md` | [NEW] 详细变更通知 |

## [2025-12-31] 预览链路验证与水印头消费

### 核心变更
- **预览链路对齐**：前端改为解析 `X-Watermark-*` 响应头并回填水印参数。
- **水印组件统一**：水印组件合并为单一入口，加入防篡改监听与白屏锁定策略。
- **全宗请求头对齐**：统一使用 `X-Fonds-No`/`X-FondsNo`。
- **验证补充**：新增预览水印元数据解析单测 + Playwright 端到端断言。
- **调试入口**：新增预览水印链路验证调试页。

### 修改文件
| 文件 | 变更 |
|------|------|
| `src/api/preview.ts` | 解析水印元数据与预览模式返回 |
| `src/pages/preview/ArchivePreviewModal.tsx` | 预览水印兜底与 TraceID 绑定 |
| `src/components/watermark/WatermarkOverlay.tsx` | Canvas 水印平铺与防篡改监听 |
| `src/components/WatermarkOverlay.tsx` | 统一水印组件入口 |
| `src/api/client.ts` | 统一全宗请求头 |
| `src/__tests__/api/preview.test.ts` | [NEW] 预览水印元数据解析测试 |
| `src/__tests__/api/README.md` | 更新测试清单 |
| `tests/playwright/ui/preview_watermark.spec.ts` | [NEW] 预览水印端到端断言 |
| `src/pages/debug/PreviewWatermarkTestView.tsx` | [NEW] 预览水印链路验证页 |

---

## [2025-12-30] PRD 对齐与文档索引补齐

### 核心变更
- **PRD 对齐**：补齐法人/全宗术语映射、预览接口兼容说明与复合分区示例。
- **PRD 补强**：补齐分区主键/语法约束、Auth Ticket、冻结/保全、销毁在借校验、服务端水印模式、实物结构化盘点与盲盘，并加入迁移注意事项。
- **PRD 重构**：切换为全宗隔离、去 PG 化、四性检测细化、实物生命周期补齐。
- **评审报告新增**：新增首席架构师终极评审报告并补齐评审索引（含评估与专家评审）。
- **代码/DDL 对齐**：补充部门数据域在现有实现中的使用说明。
- **文档导航更新**：根 README 与 docs/README 增加产品文档入口。
- **产品文档修订**：功能清单、产品架构、数据库设计中的组织维度表述对齐。
- **版本一致性**：Spring Boot 版本统一为 3.1.6，技术栈表述同步为 React 19。

### 修改文件
| 文件 | 变更 |
|------|------|
| `README.md` | 增加 PRD 导航入口 |
| `docs/README.md` | 增加产品文档入口 |
| `docs/product/prd-v1.0.md` | 术语/接口/分区/销毁/票据/水印/盘点补强 |
| `docs/product/README.md` | 补齐目录文件清单/更新评审文档索引 |
| `docs/product/architecture.md` | 术语对齐说明 |
| `docs/product/功能清单.md` | 组织维度表述对齐 |
| `docs/database/数据库设计.md` | fonds/entity 术语对齐 |
| `nexusarchive-java/README.md` | Spring Boot 版本对齐 |
| `nexusarchive-java/src/main/java/com/nexusarchive/NexusArchiveApplication.java` | 启动类技术栈版本对齐 |
| `nexusarchive-java/src/main/java/com/nexusarchive/README.md` | 目录说明补充版本对齐说明 |
| `docs/CHANGELOG.md` | 记录本次文档变更 |
| `docs/README.md` | 技术栈表述对齐 |
| `docs/ai-brain/walkthrough.md` | 技术栈表述对齐 |
| `docs/agents/README.md` | 技术栈表述对齐 |
| `docs/agents/task-agent-a.md` | 技术栈表述对齐 |
| `docs/agents/task-agent-b.md` | 技术栈表述对齐 |
| `docs/agents/task-agent-c.md` | 技术栈表述对齐 |
| `docs/planning/phase2-implementation-summary.md` | Spring Boot 版本对齐 |
| `docs/planning/ai_integration_strategy_20251217.md` | Spring Boot 版本对齐 |
| `docs/plans/README.md` | 评审目录清单补齐 |
| `docs/plans/2025-12-30-prd-v1-evaluation.md` | [NEW] PRD v1.0 多法人架构评估报告 |
| `docs/plans/2025-12-30-prd-v1-chief-architect-review.md` | [NEW] 首席架构师终极评审报告 |
| `docs/product/prd-review-v1.0-expert.md` | [NEW] PRD v1.0 专家评审记录 |

---

## [2025-12-27] 模块化试点文档补齐与新人接手指南

### 核心变更
- **新增新人接手指南**：提供系统阅读顺序、代码结构与边界规则的入门路径。
- **新增试点成果记录**：完整记录 SYS(Settings) + Borrowing 模块化试点成果与验证结果。
- **Borrowing DTO 契约统一**：对外 DTO 统一到 `api.dto`，并同步 ArchUnit 规则。
- **新增自审材料**：补齐模块清单、数据主权、契约与变体登记，形成可执行自审基线。
- **新增端到端测试流程**：补齐凭证归档主链路 12 步 SOP 与证据清单。
- **SQL 字段校准**：对照数据库设计与迁移脚本，统一 RUN_ID 落点字段与 SQL 模板。
- **文档导航同步**：更新根 README 与文档目录索引，补齐新文档入口。

### 质量门槛补充
- **新增 `typecheck` 脚本**：统一 TypeScript 类型校验入口，覆盖 `src` + `tests`。
- **类型错误清理**：修复 ArchiveListView 新接口适配、OrgTree 类型缺失、IntegrationSettings 导出不一致、ArchiveBatch/OnlineReception 类型不匹配等问题。
- **Playwright 冒烟固化**：新增关键路径冒烟脚本与 `test:smoke`，完善 headless 配置与运行说明。

### 修改文件
| 文件 | 变更 |
|------|------|
| `docs/guides/新人接手指南.md` | [NEW] 新人接手阅读路径与代码导览 |
| `docs/implementation/2025-12-27-module-boundary-pilot.md` | [NEW] 模块边界试点开发成果记录 |
| `docs/architecture/self-review-sop.md` | [NEW] 模块化自我审查清单 |
| `docs/architecture/module-manifest.md` | [NEW] 模块清单 |
| `docs/architecture/data-ownership-map.md` | [NEW] 数据主权清单 |
| `docs/architecture/contract-catalog.md` | [NEW] 对外契约清单 |
| `docs/architecture/variability-registry.md` | [NEW] 变体登记表 |
| `docs/guides/端到端测试流程.md` | [NEW] 端到端测试流程 |
| `docs/guides/README.md` | 新增文档入口 |
| `docs/implementation/README.md` | 新增文档入口 |
| `docs/architecture/README.md` | 新增文档入口 |
| `docs/architecture/module-boundaries.md` | 补充自审材料入口 |
| `nexusarchive-java/src/main/java/com/nexusarchive/modules/borrowing/api/dto/*.java` | DTO 契约统一到 api.dto |
| `nexusarchive-java/src/test/java/com/nexusarchive/architecture/ModuleBoundaryTest.java` | 更新 Borrowing 边界规则 |
| `README.md` | 补充导航链接 |
| `package.json` | 新增 `test:smoke` 与 `typecheck` 脚本 |
| `playwright.config.ts` | 默认 headless 配置与环境变量开关 |
| `tests/playwright/ui/smoke_core_paths.spec.ts` | [NEW] Settings/Borrowing/Archive 冒烟测试 |
| `tests/playwright/ui/README.md` | 更新 UI 冒烟文件清单 |
| `src/SystemApp.tsx` | ArchiveListView 迁移到 ArchiveListPage + routeConfig |
| `src/__tests__/components/ArchiveListView.test.tsx` | 适配新 props 与 controller/actions |
| `tests/playwright/api/*.spec.ts` | AuthContext nullability 与 skip 语义修复 |
| `tests/playwright/delivery_v2.spec.ts` | 隐式 any 与索引签名修复 |
| `src/components/index.ts` | IntegrationSettings 默认导出聚合 |
| `src/components/settings/OrgSettings.tsx` | OrgNode → TreeNode 映射 |
| `src/api/archiveBatch.ts` | IntegrityReport/ValidationReport 类型收敛 |
| `src/pages/collection/OnlineReceptionView.tsx` | IntegrationChannel 类型对齐 |

---

## [2025-12-25] 凭证关联增强与文档规范化
 
 ### 核心变更
 - **凭证手动关联逻辑升级**: 移除了前端 `LinkModal` 组件中的演示数据硬编码提示，开始转向真实 API 驱动。
 - **后端精准匹配规划**: 新增 `CandidateSearchRequest` 搜索模型，支持按金额（±0.01 误差）、日期范围、发票号码、供应商等多维度搜索。
 - **文档一致性维护**: 按照项目“文档自洽规则”，同步更新了根目录 README、CHANGELOG 及组件目录子文档。
 
 ### 修改文件
 | 文件 | 变更 |
 |------|------|
 | `src/components/archive/LinkModal.tsx` | 删除硬编码的演示模式提示，清理依赖 |
 | `src/components/ArchiveListView.tsx` | 重构关联逻辑，使用标准的 `LinkModal` 组件并优化档案文件加载状态机 |
 | `nexusarchive-java/.../CandidateSearchRequest.java` | [NEW] 新增候选凭证搜索 DTO |
 | `nexusarchive-java/.../PoolService.java` | [NEW] 新增凭证池业务接口 |
 
 ---
 
 ## [2025-12-07] 文档重构


### 变更
- 重构 `docs/` 目录结构，按类型分类文档
- 精简 `README.md`，详细内容迁移至专门文档
- 新增 `docs/README.md` 作为文档导航中心

---

## [2025-12-06] 前端路由架构重构 ⭐ 重大更新

### 核心变更
- **路由架构升级**: 采用 `createBrowserRouter` + 嵌套路由，实现真正的 URL 驱动导航
- **支持深层链接**: 所有模块现在可通过 URL 直接访问（如 `/system/archive/vouchers`）
- **刷新保持位置**: 页面刷新后保持当前位置，不再回退到门户首页
- **浏览器历史支持**: 完整支持前进/后退按钮
- **登录后跳转**: 未登录访问受保护路由时，登录后自动跳转回原页面

### 新增文件
| 文件 | 说明 |
|------|------|
| `src/routes/index.tsx` | 路由配置中心，定义完整嵌套路由和懒加载 |
| `src/routes/paths.ts` | 路由路径常量，独立文件避免循环依赖 |
| `src/layouts/SystemLayout.tsx` | 系统布局组件，包含 Sidebar + TopBar + Outlet |
| `src/components/auth/ProtectedRoute.tsx` | 路由守卫，处理认证和登录跳转 |

### 修改文件
| 文件 | 变更 |
|------|------|
| `src/App.tsx` | 使用 `RouterProvider` 替代传统 `Routes` |
| `src/components/Sidebar.tsx` | 使用 `NavLink` 替代状态切换，自动高亮当前路由 |
| `src/components/LoginView.tsx` | 支持 `useLocation` 获取跳转目标，登录后自动跳转 |
| `src/components/Dashboard.tsx` | 使用 `useNavigate` 替代 `onNavigate` prop |
| `src/components/ArchiveListView.tsx` | 支持 `routeConfig` 参数，自动解析模块配置 |
| `src/components/BorrowingView.tsx` | 添加缺失的图标导入 (Download, FileSpreadsheet) |

### 路由映射表
```
/system                         → 门户首页 (Dashboard)
/system/panorama/:id?           → 全景视图
/system/pre-archive/*           → 预归档库
/system/collection/*            → 资料收集
/system/archive/*               → 档案管理（会计凭证/账簿/报告/装盒/组卷等）
/system/query/*                 → 档案查询
/system/borrowing               → 档案借阅
/system/destruction             → 档案销毁
/system/warehouse/*             → 库房管理
/system/stats/*                 → 数据统计
/system/settings                → 系统设置
/system/admin/*                 → 后台管理
/system/login                   → 登录页（独立于布局）
```

### 技术亮点
- **懒加载优化**: 所有功能模块使用 `React.lazy()` 实现按需加载
- **循环依赖避免**: 路由常量独立到 `paths.ts`，打破 routes ↔ Sidebar 循环引用
- **向后兼容**: `ArchiveListView` 同时支持传统 `config` props 和新的 `routeConfig` 字符串

### 其他更新
- **接口权限兜底**：统一后台角色判断为 `SYSTEM_ADMIN` 并补充 `nav:all` 权限
- **登录流与异常**：增强 `RestAccessDeniedHandler` 与 `GlobalExceptionHandler`

---

## [2025-12-05] 用友云凭证分录同步

### 新增
- 自动同步凭证的完整分录信息，包括摘要和科目
- 分录数据存储于 `custom_metadata` 字段 (PostgreSQL JSONB 类型)
- 导出 AIP 包时展示真实的业务摘要和科目名称

### 修复
- **Connection Refused 问题**: 添加 JVM 参数 `-Djava.net.useSystemProxies=false` 禁用系统代理
- **JSONB 类型不匹配问题**: 创建 `PostgresJsonTypeHandler.java` 正确处理 JSONB 类型转换

---

## 第三阶段：私有化部署产品化改造（已完成）

详情见 [phase3-private-deployment-summary.md](/docs/planning/phase3-private-deployment-summary.md)

### 变更
- **用户体验优化**：创建面向非技术用户的管理控制台和简化界面
- **自动化运维**：实现全面的系统健康监控、自动备份恢复
- **便捷部署交付**：提供一键安装脚本、离线安装包

---

## 第二阶段：架构优化（已完成）

详情见 [phase2-implementation-summary.md](/docs/planning/phase2-implementation-summary.md)

### 变更
- **架构重构**：确立"高性能模块化单体"架构
- **性能提升**：集成 Redis 高性能缓存
- **信创安全**：落地 SM4 国密算法字段级加密
- **监控告警**：使用 Prometheus 和 Grafana 实现系统监控

---

## 第一阶段：代码质量审查优化（已完成）

详情见 [phase1-implementation-summary.md](/docs/planning/phase1-implementation-summary.md)

### 变更
- **会计合规性增强**：新增金额精度校验、标准化XML报告生成器
- **四性检测增强**：集成金额校验，完善审计日志记录
- **安全增强**：实现电子签名验证服务
- **审计完整性**：增强审计日志记录
