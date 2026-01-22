# Bug 修复报告：仪表盘统计与列表数据不一致

## 1. 现场还原 (The Crime Scene)
* **症状素描**：在“财务报告库”等特定门类页面中，仪表盘卡片（如“待检测”）显示有数据（2条），但下方的列表提示“暂无数据”。
* **业务影响**：用户无法准确获知当前门类下的实际待处理任务量，造成视觉误导。

## 2. 致命一击：定位“零号病人” (Patient Zero)
* **万恶之源**：`src/components/pool-dashboard/PoolDashboard.tsx`
* **根因分析**：
    1. `PoolDashboard` 组件接收了 `categoryFilter`（门类筛选），但调用 `usePoolDashboard()` 挂钩时未传递该参数。
    2. `usePoolDashboard` 内部调用 `usePoolKanban()` 时同样未传递门类筛选。
    3. 导致仪表盘统计的是**全量数据**（跨门类），而列表组件（`ArchiveListPage`）执行了正确的门类筛选。
    4. 此外，`usePoolKanban` 原本在前端进行门类过滤，未利用后端 API 能力。

## 3. 手术式修复 (The Fix)

### 3.1 核心 Hook 升级：`usePoolKanban`
将门类筛选下沉到 API 层，并集成了**全宗上下文 (Fonds Context)** 监听。通过将 `fondsCode` 引入 `queryKey`，确保用户切换顶部全宗时，仪表盘数据能立即刷新。

```diff:src/hooks/usePoolKanban.ts
+  const currentFonds = useFondsStore(state => state.currentFonds);
-    queryKey: ['pool', 'kanban', options.categoryFilter || 'all'],
+    queryKey: ['pool', 'kanban', currentFonds?.fondsCode || 'default', options.categoryFilter || 'all'],
     queryFn: async () => {
-      return await poolApi.getList();
+      return await poolApi.getList(options.categoryFilter);
     },
```

### 3.2 全局全宗支持 (Global Fonds Support)
*   **API 层**：`src/api/client.ts` 已配置拦截器，自动从 `useFondsStore` 获取当前全宗号并注入 `X-Fonds-No` 请求头，确保后端返回对应全宗的数据。
*   **列表联动**：`ArchiveListPage` 及其 `useArchiveDataLoader` 模块已具备完善的状态监听能力，全宗切换会触发 API 重新加载。

### 3.3 标题栏统计对齐 (Header Count Alignment)
修复了标题栏（如“财务报告库 / 共 2 条”）显示全量统计的问题。通过升级 `useArchivePool` 及其对应的 API 调用，确保统计接口也挂载了当前的门类筛选参数。

```diff:src/features/archives/controllers/useArchivePool.ts
-            const response = await client.get('/pool/stats/status');
+            const url = categoryFilter 
+                ? `/pool/stats/status?category=${categoryFilter}`
+                : '/pool/stats/status';
+            const response = await client.get(url);
```

### 3.4 统计逻辑对齐：`usePoolDashboard`
使统计 Hook 能够感知当前的门类筛选环境。

```diff:src/hooks/usePoolDashboard.ts
-export function usePoolDashboard(): {
+export function usePoolDashboard(options: { categoryFilter?: string | null } = {}): {
   // ...
-  const { cards } = usePoolKanban();
+  const { cards } = usePoolKanban({ categoryFilter: options.categoryFilter });
```

### 3.3 UI 连通：`PoolDashboard`
完成最后的参数传递闭环。

```diff:src/components/pool-dashboard/PoolDashboard.tsx
-  const { stats, totalCount: _totalCount } = usePoolDashboard();
+  const { stats, totalCount: _totalCount } = usePoolDashboard({ categoryFilter });
```

## 4. 验证结果 (Verification)
* **预期行为**：进入“财务报告库”后，仪表盘各卡片统计数值应仅包含“财务报告”门类下的数据。若该门类无数据，统计数值应显示为 0。
* **副作用评估**：
    * **性能提升**：通过后端过滤减少了前端加载的数据量。
    * **一致性**：所有预归档相关页面（凭证池、账簿库、报告库）现在共享统一的统计筛选逻辑。

---
**根因猎手** (Root Cause Hunter) 报告完毕。 🎯
