一旦我所属的文件夹有所变化，请更新我。

# Integration Settings Module

## Overview
Card-based layout for ERP connector configuration, optimized for finance users.

## Key Components

### ErpConfigCard
- **Purpose**: Display single connector summary card
- **Features**:
  - Summary view with connection status
  - Scenario count badge
  - Health check button (slate-700 + white text)
  - Actions: config, test, diagnose, reconcile
  - "View Details" button opens scenario drawer
  - Fixed height for consistent layout

### ErpConfigList
- **Purpose**: Grid layout for connector cards
- **Layout**: Responsive grid (1 col mobile, 3 cols desktop)
- **Empty State**: Helpful message with call-to-action

### IntegrationSettingsPage
- **Purpose**: Main page compositor
- **Pattern**: Compositor pattern combining specialized hooks

## Architecture (v2.2 - Summary + Drawer)

### Three-Layer Information Architecture

**Layer 1: Summary Card (ErpConfigCard)**
- Displays summary information only
- Fixed height for consistent layout
- Shows: connection status, health, scenario counts
- Actions: config, test, diagnose, reconcile
- Entry point: "View Details" button

**Layer 2: Detail Drawer (ScenarioDrawer)**
- Slides in from right (480px width)
- Shows full scenario list
- Per-scenario actions: sync, view history
- Dismissible with X button or click outside

**Layer 3: Management Page (Future)**
- Dedicated page for scenario management
- Advanced features: history, logs, mapping
- Reached by clicking individual scenario

### Component Structure

```
IntegrationSettingsPage
├── ErpConfigList (grid layout)
│   └── ErpConfigCard (summary view)
│       ├── ScenarioSummaryCard (counts)
│       ├── ConnectionHealthBadge (status)
│       └── [Action Buttons]
└── ScenarioDrawer (detail view)
    └── [Scenario List]
```

### Visual Improvements

- Health check button: slate-700 background + white text (better contrast)
- Card height: Fixed for consistent layout
- Spacing: Increased padding and gaps for breathing room
- Status indicators: Color-coded with icons

## Hooks

- **useErpConfigManager**: Config CRUD operations
- **useScenarioSyncManager**: Scenario sync management
- **useConnectorModal**: Config form modal
- **useParamsEditor**: Scenario parameter editing
- **useIntegrationDiagnosis**: Health check functionality
- **useAiAdapterHandler**: AI adapter generation

## Usage

```typescript
import { IntegrationSettingsPage } from '@/components/settings/integration';

<IntegrationSettingsPage erpApi={erpApi} />
```

## Testing

```bash
npm run test -- IntegrationSettingsPage
npm run test -- ErpConfigCard
npm run test -- ErpConfigList
```

---

**层级位置**: `src/components/settings/integration/`
**模块职责**: ERP 集成设置 - 管理外部 ERP 系统连接器、业务场景同步、AI 适配器等功能
**维护策略**: 遵循模块化架构原则，每个 Hook/组件职责单一，修改时更新对应文件的头部注释和本文档

---

## 模块概述

本模块实现 ERP 集成设置功能，从原始 1,709 行巨型组件重构为 161 行组合器 + 8 个专用 Hook + 5 个 UI 组件，代码减少 **91.1%**。

**重构日期**: 2026-01-05
**最新更新**: 2026-01-07 (v2.6 - 关账检查模式)
**设计模式**: Compositor 组合器模式 + React Hooks
**测试覆盖**: 44 个测试用例，100% 通过率

---

## 目录结构

```
src/components/settings/integration/
├── IntegrationSettingsPage.tsx    # 161 行 - 主页面组合器
├── types.ts                        # 类型定义
├── index.ts                        # 公共 API 导出
├── hooks/                          # 业务逻辑 Hooks
│   ├── useErpConfigManager.ts      # 130 行 - ERP 配置管理
│   ├── useScenarioSyncManager.ts   # 133 行 - 场景同步管理
│   ├── useConnectorModal.ts        # 167 行 - 连接器模态框
│   ├── useIntegrationDiagnosis.ts  # 53 行  - 诊断功能
│   ├── useParamsEditor.ts          # 68 行  - 参数编辑器
│   ├── useAiAdapterHandler.ts      # 109 行 - AI 适配器处理
│   └── __tests__/                  # Hook 单元测试
│       ├── useErpConfigManager.test.ts
│       ├── useScenarioSyncManager.test.ts
│       ├── useConnectorModal.test.ts
│       ├── useIntegrationDiagnosis.test.ts
│       ├── useParamsEditor.test.ts
│       ├── useAiAdapterHandler.test.ts
│       └── ...
├── components/                     # UI 组件
│   ├── ErpConfigList.tsx           # 79 行  - 配置列表
│   ├── ScenarioCard.tsx            # 145 行 - 场景卡片
│   ├── ConnectorForm.tsx           # 156 行 - 连接器表单
│   ├── DiagnosisPanel.tsx          # 89 行  - 诊断面板
│   ├── ParamsEditor.tsx            # 106 行 - 参数编辑器
│   └── __tests__/                  # 组件单元测试
└── README.md                       # 本文档
```

---

## 完整文件清单

### 主页面

| 文件 | 行数 | 角色 | 能力描述 |
|------|------|------|----------|
| `IntegrationSettingsPage.tsx` | 161 | **Compositor** | 组合器组件，协调所有 Hooks 和 Components，处理数据流和用户交互 |

### 类型定义

| 文件 | 行数 | 角色 | 能力描述 |
|------|------|------|----------|
| `types.ts` | ~265 | **Type Definitions** | 定义所有 State/Actions 接口、ErpConfig/ErpScenario 相关类型（含 accbookMapping 账套-全宗映射、requireClosedPeriod 关账检查模式）|

### Hooks (业务逻辑层)

| 文件 | 行数 | 角色 | 能力描述 |
|------|------|------|----------|
| `useErpConfigManager.ts` | 130 | **ERP Config Manager** | ERP 配置 CRUD、加载、类型展开、连接测试 |
| `useScenarioSyncManager.ts` | 133 | **Scenario Sync Manager** | 场景加载、子接口管理、同步历史记录、批量同步 |
| `useConnectorModal.ts` | 167 | **Connector Modal** | 连接器创建/编辑模态框状态、表单管理、账套-全宗映射配置、ERP 类型自动检测、关账检查模式配置 |
| `useIntegrationDiagnosis.ts` | 53 | **Diagnosis Handler** | 集成诊断执行、结果展示、健康检查 |
| `useParamsEditor.ts` | 68 | **Params Editor** | 同步参数编辑（日期范围、分页大小） |
| `useAiAdapterHandler.ts` | 109 | **AI Adapter Handler** | AI 文件上传、预览生成、配置适配 |

### Components (展示层)

| 文件 | 行数 | 角色 | 能力描述 |
|------|------|------|----------|
| `ErpConfigList.tsx` | 67 | **Config List UI** | 网格布局展示连接器卡片，空状态提示，响应式（1/3 列） |
| `ErpConfigCard.tsx` | ~176 | **Summary Card UI** | 连接器摘要卡片，固定高度。显示账套-全宗映射关系，"配置中心"按钮打开 ConnectorForm 模态框（完整6字段+映射）|
| `ScenarioDrawer.tsx` | ~100 | **Detail Drawer UI** | 右侧抽屉详情页（480px），显示完整场景列表 |
| `ScenarioSummaryCard.tsx` | ~50 | **Summary Widget UI** | 场景统计卡片（总数/运行中/失败），带动画状态 |
| `ConnectionHealthBadge.tsx` | ~60 | **Health Badge UI** | 健康状态徽章，显示状态 + 相对时间 |
| `ConnectorForm.tsx` | ~293 | **Connector Form UI** | 连接器配置表单（名称、类型、URL、密钥、账套-全宗映射配置、YonSuite 关账检查模式开关）|
| `DiagnosisPanel.tsx` | 89 | **Diagnosis Panel UI** | 诊断结果面板，显示健康状态和详细检查项 |
| `ParamsEditor.tsx` | 106 | **Params Editor UI** | 同步参数编辑模态框（日期范围、分页大小） |
| `ScenarioCard.tsx` | 145 | **Scenario Card UI** | 场景卡片（v2.1，v2.2 由 Drawer 替代） |

### 测试文件

| 文件 | 测试数 | 覆盖内容 |
|------|--------|----------|
| `hooks/__tests__/useErpConfigManager.test.ts` | 10 | 配置加载、创建、删除、状态管理 |
| `hooks/__tests__/useScenarioSyncManager.test.ts` | 3 | 场景加载、子接口、同步 |
| `hooks/__tests__/useConnectorModal.test.ts` | 6 | 模态框开关、表单更新、账套-全宗映射管理 |
| `hooks/__tests__/useIntegrationDiagnosis.test.ts` | 7 | 诊断执行、状态管理、错误处理 |
| `hooks/__tests__/useParamsEditor.test.ts` | 8 | 编辑器开关、表单更新、同步提交 |
| `hooks/__tests__/useAiAdapterHandler.test.ts` | 10 | 文件上传、预览生成、配置适配 |
| **总计** | **44** | **100% 测试通过** |

---

## 数据流图

```
IntegrationSettingsPage (Compositor)
│
├── useErpConfigManager
│   └── 加载 ERP 配置 → ErpConfigList 渲染
│
├── useScenarioSyncManager
│   └── 加载场景 → ScenarioCard 渲染
│       ├── 场景展开 → loadSubInterfaces
│       └── 同步/历史 → syncScenario / toggleHistoryView
│
├── useConnectorModal
│   └── 打开模态框 → ConnectorForm 渲染
│       ├── ERP 类型检测 → detectErpType
│       └── 保存 → createConfig / updateConfig
│
├── useIntegrationDiagnosis
│   └── 开始诊断 → DiagnosisPanel 渲染
│
├── useParamsEditor
│   └── 打开编辑器 → ParamsEditor 渲染
│       └── 提交同步 → syncScenario
│
└── useAiAdapterHandler
    └── 打开适配器 → 文件上传/预览/适配
```

---

## 依赖关系

### 依赖项（外部）
- `react`, `react-dom` - React 框架
- `react-hot-toast` - Toast 通知
- `lucide-react` - 图标库
- `antd` - UI 组件库（Modal, Form, Button 等）

### 被依赖项（外部）
- `src/components/settings/` - 设置页面入口
- `src/api/erp.ts` - ERP API 调用
- `src/types.ts` - 全局类型定义

---

## 变更历史

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-01-07 | v2.6 | 关账检查模式：YonSuite 新增期间关账检查功能，ConnectorForm 增加"强制/提醒"模式切换开关 |
| 2026-01-07 | v2.5 | 账套-全宗映射功能：ConnectorForm 新增映射配置表格，ErpConfigCard 显示映射关系，后端强制路由 |
| 2026-01-06 | v2.2 | 三层信息架构：Summary Card + Detail Drawer + Management Page |
| 2026-01-05 | v2.1 | 卡片式布局优化，移除展开/收起功能 |
| 2025-XX-XX | v1.0 | 原始单体组件实现 |

---

**维护说明**: 修改任何 Hook 或 Component 后，请务必更新：
1. 文件头部注释（Input/Output/Pos）
2. 本文档的对应章节（文件清单、能力描述）
3. 相应的单元测试

**架构审查**: 符合 entropy-reduction 原则（单一职责、依赖倒置、接口隔离）

---

## 架构防御 (Architecture Defense)

本模块实现了完整的架构防御系统，符合 J1-J4 四个关键体征：

### J1: Self-Description (自描述)

**模块清单**: `manifest.config.ts`

```typescript
export const moduleManifest = {
  id: 'feature.integration-settings',
  owner: 'team-platform',
  publicApi: './index.ts',
  canImportFrom: [
    'react', 'antd', 'lucide-react', 'react-hot-toast',
    'src/types.ts', 'src/api/**'
  ],
  restrictions: {
    disallowDeepImport: true,
    disallowCrossComponentInternal: true,
    disallowDatabaseImports: true,
  },
  metrics: {
    linesOfCode: 161,
    totalFiles: 19,
    testCoverage: 100,
  },
  tags: [
    'entropy-reduction',
    'compositor-pattern',
    'test-driven-development',
    'document-self-consistency',
  ],
};
```

### J2: Self-Check (自检查)

**依赖规则** (`.dependency-cruiser.cjs`):

| 规则 | 说明 | 严重级别 |
|------|------|----------|
| `integration-settings-no-internal-import` | 阻止直接导入内部实现，必须使用 index.ts | error |
| `integration-settings-restrict-deps` | 只能导入 manifest 中声明的依赖 | error |
| `integration-settings-no-db` | UI 层不能导入数据库模型 | error |
| `integration-settings-test-only-in-tests` | 测试工具不能导入生产代码 | error |

**验证命令**:
```bash
npm run check:arch
```

### J3: Closed Rules (封闭规则)

**CI 集成** (`.github/workflows/architecture-check.yml`):

- ✅ 前端架构检查作业
- ✅ Manifest 验证步骤
- ✅ 依赖关系图生成（PR 时）
- ✅ 架构健康评分

**CI 阻断机制**: 任何架构违规将阻止 PR 合并。

### J4: Reflex (反射/违规响应)

**违规消息示例**:
```
✖ src/pages/SomePage.tsx → src/components/settings/integration/hooks/useErpConfigManager.ts
  ⚠ integration-settings-no-internal-import

  Integration Settings internal implementation cannot be imported directly.
  Use the public API (index.ts) instead.

  See: src/components/settings/integration/manifest.config.ts

  Module owner: team-platform
```

---

## 架构健康状态

| 检查项 | 状态 | 说明 |
|--------|------|------|
| Manifest 创建 | ✅ | `manifest.config.ts` 已创建 |
| 依赖规则 | ✅ | 4 条规则已添加到 `.dependency-cruiser.cjs` |
| CI 集成 | ✅ | GitHub Actions 工作流已更新 |
| 架构验证 | ✅ | 通过 `npm run check:arch` |
| 代码覆盖 | ✅ | 44/44 测试通过 (100%) |

---

## 运行时调试

在浏览器控制台中查看模块信息：

```javascript
// 查看所有模块清单
window.__ARCH__

// 查找 integration 模块
window.__ARCH__.find(m => m.id === 'feature.integration-settings')

// 获取模块所有者
const getOwner = (id) => window.__ARCH__.find(m => m.id === id)?.owner;
getOwner('feature.integration-settings'); // 'team-platform'
```

