一旦我所属的文件夹有所变化，请更新我。

// Input: UI 组件库
// Output: 极简架构说明
// Pos: src/components/README.md
// 一旦我所属的文件夹有所变化，请更新我。

# UI 组件库 (Components)

本目录存放项目的所有 UI 组件，按复用程度和业务领域进行划分。

## 目录结构

- `common/`: 高复用原子组件（按钮、输入框、模态框等），**禁止依赖 api/store/features/pages**。
- `layout/`: 页面布局组件（Sidebar, Header, Footer 等），**禁止依赖 api/store/features/pages**。
- `archive/`: 档案管理模块相关的业务组件。
- `matching/`: 凭证匹配模块相关的业务组件。
- `settings/`: 系统设置模块相关的业务组件。
- `org/`: 组织架构模块相关的业务组件。
- `panorama/`: 档案全景图相关的业务组件。
- `dev/`: 开发辅助组件（仅开发环境使用，生产环境无副作用）。

## 架构约束

1. **原子性**: `common/` 目录下的组件应保持无状态或仅持有 UI 状态。
2. **单向依赖**: `components` 下组件不得引入 `features/pages/api/store`，仅通过 props 注入业务能力。
3. **页面容器**: 页面级容器统一放在 `src/pages/`，routes 只引用 pages。
