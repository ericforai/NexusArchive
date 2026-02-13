# Walkthrough: Fix "Archive loading failed" on Initial Load

## 1. 问题描述 (Problem Description)
用户反馈在初次访问 `/system/collection/online` 页面时，会出现 "Archive loading failed"（档案加载失败）的错误，刷新后正常。
经分析，这是由于 `useFondsStore` 的水合（Hydration）异步过程与页面的初始 API 请求存在竞态条件，导致 API 请求时 `X-Fonds-No` 头部未正确设置，或者前端无法获取当前全宗代码。

## 2. 解决方案 (Technical Solution)

### 2.1 全局水合屏障 (Global Hydration Barrier)
在 `[SystemLayout.tsx](file:///Users/user/nexusarchive/src/layouts/SystemLayout.tsx)` 中引入了全局水合检查：
- 导出 `useFondsStore` 中的 `_hasHydrated` 状态。
- 在 `SystemLayout` 中，若 `_hasHydrated` 为 `false`，则渲染 `LoadingSpinner` 而不渲染子路由（Outlet）。
- 确保了所有受保护路由下的组件在渲染时，`useFondsStore` 已完成本地状态加载。

### 2.2 组件级安全检查 (Component-Level Checks)
对受影响的关键页面进行了优化，确保其在 API 调用前主动检查水合状态：
- **[OnlineReceptionView.tsx](file:///Users/user/nexusarchive/src/pages/collection/OnlineReceptionView.tsx)**: 通过 `useCallback` 封装 `loadData`，并监听 `hasHydrated` 与 `fondsCode`。
- **[Dashboard.tsx](file:///Users/user/nexusarchive/src/pages/portal/Dashboard.tsx)**: 核心 `useEffect` 增加对水合状态的判定。
- **[StatsView.tsx](file:///Users/user/nexusarchive/src/pages/stats/StatsView.tsx)**: 同样的逻辑应用于数据统计页面。
- **[OriginalVoucherListView.tsx](file:///Users/user/nexusarchive/src/pages/archives/OriginalVoucherListView.tsx)**: 利用 `@tanstack/react-query` 的 `enabled` 选项，仅在水合完成后发起请求。

## 3. 验证结果 (Verification Results)

### 3.1 模拟首次登录验证
通过浏览器 Subagent 执行了以下验证步骤：
1. 访问系统并登录。
2. 在控制台清除 `localStorage` (模拟无持久化状态)。
3. 强制重载页面。
4. **结果**：页面首先显示加载状态，随后正确跳转并成功加载 "在此接收" 页面及其集成通道数据。无任何异常提示。

### 3.2 验证截图
![验证截图](file:///Users/user/nexusarchive/docs/plans/online_reception_verified.png)

### 3.3 验证录像
录制了验证过程的 WebP 动画：[verify_hydration_fix_retry.webp](file:///Users/user/nexusarchive/docs/plans/verify_hydration_fix_retry.webp)

## 4. 交付清单
- `src/layouts/SystemLayout.tsx`: 实现全局水合屏障。
- `src/pages/collection/OnlineReceptionView.tsx`: 修复 API 竞态条件并优化代码结构。
- `src/pages/portal/Dashboard.tsx`: 增加水合等待逻辑。
- `src/pages/stats/StatsView.tsx`: 增加水合等待逻辑。
- `src/pages/archives/OriginalVoucherListView.tsx`: 优化 Query enabled 逻辑。
