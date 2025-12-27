# 虚拟专家组联合审查报告：src/pages 迁移与容器分离方案评估

## Step 0：假设与审查边界声明

* **已知前提**：
    1. 当前 `src/components/*View.tsx` 文件（如 `ArchiveListView.tsx`）体积庞大（>2400行），集成了 API 调用、状态管理、表格渲染、模态框逻辑及大量 UI 交互。
    2. 路由配置 `src/routes/index.tsx` 直接指向这些 View 组件。
    3. 项目已建立 ESLint 架构边界规则（禁止 components 依赖 api/store/features）。
* **合理假设**：
    1. 迁移至 `src/pages/` 的目标是建立明确的“容器组件（Container）”层。
    2. 容器组件负责：处理路由参数、调用 API/Features、读取 Store 状态、生命周期管理。
    3. 展示组件负责：纯 UI 渲染、接受 Props 并在事件发生时调用回调函数。
* **审查边界**：
    1. 本轮审查仅针对前端代码组织架构，不涉及后端 API 变更或数据库模型。
    2. 不涉及特定业务逻辑的正确性验证，仅关注其“位置”与“职责”。

---

## Step 1：法律法规与会计规范对标（ Compliance Authority）

> [!NOTE]
> **合规结论**：架构重构对电子会计档案的法律效力无直接负面影响，但在呈现层需注意数据真实性的展现。

* **DA/T 94-2022 条款对标**：会计档案的展现应确保元数据的“所见即所得”。
* **审查点**：重构过程中必须确保“四性检测”结果的 UI 展现逻辑不被破坏。在大组件拆分时，元数据（Metadata）与文件（Content）的关联渲染逻辑必须作为“原子能力”保留在 UI 组件中，避免逻辑泄露到多个展示组件导致显示不一致。
* **专家建议**：在 `ArchiveTable` 组件中，对于“凭证号”、“起止日期”等核心元数据的展示，应采用统一的分支逻辑（如 `resolveDocumentTypeLabel`），建议将这些转换函数提取至 `src/utils/archival.ts`，而非散落在各展示组件。

---

## Step 2：系统架构与安全深度审计（ Architecture & Security Expert）

> [!IMPORTANT]
> **风险判定**：当前 `ArchiveListView` 的过度集成是系统长期维护的“头号阻断点”。

### 🛑 阻断点（Showstoppers）
1. **职责过载**：`ArchiveListView` 同时作为页面容器和业务逻辑载体，导致 HMR（热更新）缓慢，且单元测试几乎无法覆盖（测试一个 2400 行的组件成本极高）。
2. **潜在的循环依赖**：由于 View 组件在 `src/components` 根目录下，很容易不经意间被跨目录引用，迁移至 `src/pages` 可从物理路径上切断非法引用。

### 🏛 架构优化方案
| 模块 | 现状 | 优化建议 | 难度 |
| --- | --- | --- | --- |
| **Page 层** | `ArchiveListView.tsx` | 建立 `src/pages/Archives/ArchiveList/index.tsx` 作为容器，仅负责数据 Fetch 和状态派发。 | 中 |
| **Component 层** | 2400行代码混杂 | 拆分为多个展示组件：`ArchiveTable` (表格), `ArchiveSearcher` (筛选), `ArchiveActions` (操作栏)。 | 高 |
| **Feature 结合** | 散在渲染函数中的业务逻辑 | 确保 `features/*` 提供的 Hook（如 `useArchiveSubmit`）被 Page 层调用，结果传给组件。 | 中 |

* **安全建议**：在 Page 层统一处理权限校验（RBAC），而不是在每个展示组件里判断 `useAuthStore`。这样可以确保“无权即不加载组件”。

---

## Step 3：部署、交付与运维可行性评估（ Delivery Strategist）

> [!TIP]
> **交付优势**：清晰的 Pages 结构有助于提升离线环境下的前端排错效率。

* **工程化收益**：
    1. **按需加载（Tree Shaking）**：通过 Page 层的明确划分，路由层可以更精准地进行 `React.lazy` 拆包，减小内网复杂网络环境下的首屏加载压力。
    2. **代码自洽性**：符合项目 README 规范中“每个文件夹都有说明”的要求，新人接入成本从“月”降至“周”。
* **运维角度**：
    1. 发生报错时，堆栈信息能更清晰定位是在“数据层（Page）”还是“展示层（Component）”。
* **风险控制**：迁移应分批次进行（Pilot Test），建议第一个试点为 `LoginView` 或 `Dashboard`，而非最复杂的 `ArchiveListView`。

---

## Step 4：专家联合建议（⚖️ Joint Consensus）

1. **合规专家**：确保“四性”渲染组件为独立库（如 `src/components/archive/ComplianceBadge`），以便在不同页面复用且逻辑一致。
2. **架构专家**：立即启动 `src/pages` 迁移。执行策略：**“新建 -> 按需迁移 -> 废弃旧文件”**。禁止在 `src/pages` 中写业务代码，它应当是一个“路由胶水层”。
3. **交付专家**：更新路由 `src/routes/index.tsx`，将所有懒加载路径指向 `../pages/...`。配合 `npm run lint` 强制执行物理层级约束。

**评审结论：✅ 建议立即执行试点迁移。**
