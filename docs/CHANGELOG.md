# 更新日志 (Changelog)

本文件记录 NexusArchive 电子会计档案系统的版本更新历史。

---

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
