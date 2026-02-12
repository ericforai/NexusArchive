# Bug 修复报告：ArchiveListView 动态导入失败

## 1. Bug 类型判定

> **环境问题** — Vite 依赖预优化缓存过期

## 2. 根因分析（因果链）

```
大量未提交的前端代码改动（新增 SUBMITTED 状态、调试日志等）
  → Vite 运行中检测到依赖图变化，需要重新优化 pre-bundled 依赖
    → 对已缓存的 react-dom.js 等依赖返回 504 (Outdated Optimize Dep)
      → lazy(() => import('./ArchiveListView')) 的依赖链断裂
        → 浏览器抛出 "Failed to fetch dynamically imported module"
```

### 证据表

| 项目 | 内容 |
|:---|:---|
| ✅ 已确认事实 | TypeScript 编译通过（exit code 0）；`curl` 直接请求 `ArchiveListView.tsx` 返回 HTTP 200；控制台出现 `504 Outdated Optimize Dep` |
| ✅ 已确认事实 | `client.types.ts` 原始代码中 `fundsProvider` 声明和使用**内部一致**，不存在 ReferenceError（之前判断有误） |
| ✅ 已确认事实 | 重启 Vite dev server 后页面正常加载，6条档案数据正确显示 |

### 错误传播路径

```
Vite Dep Optimizer (504 Outdated) → react-dom.js 预打包模块过期
  → ArchiveListView.tsx 的 import 链中某个模块依赖 react-dom
    → Dynamic import() Promise rejected
      → React.lazy() 失败
        → Error Boundary 捕获并显示「出错了」
```

## 3. 修复思路说明

这不是代码 Bug，而是 Vite 开发服务器的依赖预优化缓存过期问题。当项目中有大量文件变更但 Vite 服务器未重启时，预打包的依赖与当前模块图不同步，Vite 会返回 504 状态码触发浏览器重新加载，但在 lazy-loaded 路由场景下这会导致整个模块链断裂。

**修复方式**：重启 Vite dev server（`kill` + `npm run dev`）

> ⚠️ 之前对 `client.types.ts` 的 `fundsProvider` → `fondsProvider` 修改是误判，已通过 `git checkout` 还原。

## 4. 修改后的代码

无代码修改。仅重启 Vite dev server。

`client.types.ts` 已还原为原始状态（`git checkout -- src/api/client.types.ts`）。

## 5. 潜在副作用与建议

| 检查项 | 是/否 | 说明 |
|:---|:---|:---|
| 影响其他模块？ | 否 | 仅重启开发服务器 |
| 改变系统状态机？ | 否 | — |
| 引入新的耦合？ | 否 | — |

### 预防建议

- 在编辑大量前端文件后，如果浏览器出现「Failed to fetch dynamically imported module」错误，优先尝试**硬刷新**（`Cmd+Shift+R`）或**重启 Vite dev server**
- 可在 `vite.config.ts` 中配置 `optimizeDeps.force: true`（仅开发环境）来强制每次重新预优化，但会略微增加启动时间
