# Integration Settings UI Components

> **一旦我所属的文件夹有所变化，请更新我。**

## 模块职责

本目录包含集成设置模块的所有 UI 展示组件。每个组件专注于单一视觉呈现，遵循 React 函数式组件模式。

---

## 完整文件清单

### v2.2 新增组件 (三层架构)

| 文件 | 行数 | 角色 | 能力描述 |
|------|------|------|----------|
| `ScenarioDrawer.tsx` | ~100 | **Detail View Layer** | 右侧抽屉详情页，显示完整场景列表，480px 宽度，支持逐个同步 |
| `ScenarioSummaryCard.tsx` | ~50 | **Summary Widget** | 场景统计卡片，显示总数/运行中/失败数量，带动画状态指示器 |
| `ConnectionHealthBadge.tsx` | ~60 | **Health Indicator** | 健康状态徽章，显示状态（健康/警告/异常）+ 相对时间格式 |

### 核心展示组件

| 文件 | 行数 | 角色 | 能力描述 |
|------|------|------|----------|
| `ErpConfigCard.tsx` | ~230 | **Summary Card** | 连接器摘要卡片，固定高度。显示 SAP 接口类型标签，点击"配置中心"打开 ConnectorForm |
| `SapInterfaceTypes.tsx` | ~247 | **Interface Types** | SAP 四种接口类型展示组件（OData 已实现，其他预留） |
| `SapInterfaceConfigForm.tsx` | ~220 | **SAP Config Form** | SAP 接口动态配置表单（OData/RFC/IDoc/Gateway） |

**Note:** "配置中心" button delegates to useConnectorModal hook → ConnectorForm modal

| `ErpConfigList.tsx` | ~67 | **Grid Layout** | 连接器列表网格布局（响应式：移动 1 列，桌面 3 列），空状态提示 |
| `ConnectorForm.tsx` | ~260 | **Form Modal** | 连接器配置表单模态框（支持 SAP 接口类型选择和配置） |
| `DiagnosisPanel.tsx` | ~89 | **Diagnosis UI** | 诊断结果面板，显示健康状态和详细检查项 |
| `ParamsEditor.tsx` | ~106 | **Params Modal** | 同步参数编辑模态框（日期范围、分页大小） |
| `ScenarioCard.tsx` | ~145 | **Scenario Item** | 场景卡片组件（v2.1 使用，v2.2 由 Drawer 替代） |

### 测试文件

| 文件 | 测试数 | 覆盖内容 |
|------|--------|----------|
| `__tests__/ScenarioDrawer.test.tsx` | 7 | 抽屉开关、场景渲染、同步回调、状态徽章、禁用状态 |
| `__tests__/ScenarioSummaryCard.test.tsx` | 5 | 统计显示、运行中/失败状态、空值处理 |
| `__tests__/ConnectionHealthBadge.test.tsx` | 5 | 健康状态、警告状态、异常状态、时间格式化 |
| `__tests__/ErpConfigCard.test.tsx` | 6 | 摘要视图、按钮操作、编辑模式、删除确认 |
| `__tests__/ErpConfigList.test.tsx` | 4 | 网格布局、空状态、配置渲染 |
| `__tests__/SapInterfaceConfigForm.test.tsx` | 6 | OData 配置表单、预留类型提示、禁用状态 |
| `__tests__/IntegrationSettingsPage.test.tsx` | 8 | 页面组合、抽屉状态、数据流 |

---

## SAP 集成接口类型支持

### 接口类型

| 类型 | 状态 | 描述 |
|------|------|------|
| **OData** | 已实现 | SAP S/4HANA OData V4 REST 风格集成 |
| **RFC/BAPI** | 预留 | 传统 SAP 集成方式，需要 SAP JCo |
| **IDoc** | 预留 | 异步批量数据交换，类似 EDI 格式 |
| **SAP Gateway** | 预留 | 自定义 OData 服务构建 |

### OData 配置项

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `serverUrl` | string | 是 | OData 服务端点 URL |
| `authType` | select | 是 | 固定为 "Basic" |
| `username` | string | 是 | 技术用户名 |
| `password` | password | 是 | 密码 |
| `clientNumber` | string | 否 | on-premise S/4HANA 客户端编号 |
| `testService` | string | 否 | 测试连接服务名 |

---

## 三层信息架构 (v2.2)

```
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: Summary Card (ErpConfigCard)                        │
│ - Fixed height, summary info only                            │
│ - Entry: "View Details" button → opens Drawer                │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Layer 2: Detail Drawer (ScenarioDrawer)                      │
│ - 480px width, slides from right                             │
│ - Full scenario list with per-item actions                   │
│ - Entry: Click scenario → (future) Layer 3                   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: Management Page (Future)                            │
│ - Dedicated page for scenario management                     │
│ - Advanced features: history, logs, field mapping            │
└─────────────────────────────────────────────────────────────┘
```

---

## 组件依赖关系

```
IntegrationSettingsPage (Compositor)
│
├── ErpConfigList
│   └── ErpConfigCard
│       ├── ScenarioSummaryCard
│       ├── ConnectionHealthBadge
│       └── [Action Buttons]
│
├── ScenarioDrawer (条件渲染)
│   └── [Scenario List Items]
│
├── ConnectorForm (Modal)
│   ├── SapInterfaceTypesCard (ERP=SAP 时显示)
│   └── SapInterfaceConfigForm (选中接口类型后显示)
│
├── DiagnosisPanel (Modal)
└── ParamsEditor (Modal)
```

---

## 设计模式

| 模式 | 应用场景 | 说明 |
|------|----------|------|
| **Compositor Pattern** | IntegrationSettingsPage | 主页面作为轻量级组合器，协调 Hooks 和 Components |
| **Container/Presenter** | 所有组件 | 逻辑在 Hooks，组件只负责渲染 |
| **Controlled Component** | Form 相关组件 | 状态由父组件/Hooks 管理 |
| **Conditional Rendering** | Drawer/Modal | 根据状态条件渲染浮层组件 |

---

## 样式规范

- **Tailwind CSS** 工具类优先
- **Ant Design** 组件用于 Modal、Drawer、Form
- **固定高度** 卡片（ErpConfigCard）保证布局一致性
- **响应式网格** 1 列（移动）→ 2 列（平板）→ 3 列（桌面）

---

## 维护说明

修改组件后，请务必更新：

1. **文件头注释**（三行式：Input/Output/Pos）
2. **本文档**的文件清单和能力描述
3. **对应的单元测试**（保持测试覆盖率 100%）

---

**父目录**: `src/components/settings/integration/`
**模块所有者**: team-platform
**最后更新**: 2026-01-09 (v2.3 - SAP Interface Config Support)
