# 减少懒加载优化开发模式页面切换速度

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将常用页面从懒加载改为直接导入，提升 Vite 开发模式下的页面切换速度

**Architecture:**
- 识别常用页面（Dashboard、档案列表、预归档库）并改为静态导入
- 保留大型页面的懒加载（全景视图、统计图表、报表）
- 移除这些页面的 `withSuspense` 包装

**Tech Stack:** React 19, React Router v7, Vite 6, TypeScript

---

## Task 1: 修改 Dashboard 为直接导入

**Files:**
- Modify: `src/routes/index.tsx:33`

**Step 1: 将 Dashboard 从懒加载改为直接导入**

在 `src/routes/index.tsx` 顶部添加 Dashboard 导入：

```tsx
// 在第 30 行 PoolPage 导入后添加
import { PoolPage } from '@/pages/pre-archive/PoolPage';
import { Dashboard } from '../pages/portal/Dashboard';  // ← 添加这行
```

**Step 2: 删除 Dashboard 的懒加载声明**

删除第 33 行：

```tsx
// 删除这行:
const Dashboard = lazy(() => import('../pages/portal/Dashboard'));
```

**Step 3: 修改 Dashboard 路由配置**

找到 Dashboard 路由（约第 179 行），从 `withSuspense(Dashboard)` 改为直接使用 `<Dashboard />`：

```tsx
// 修改前:
{ index: true, element: withSuspense(Dashboard) },

// 修改后:
{ index: true, element: <Dashboard /> },
```

**Step 4: 验证编译**

```bash
cd /Users/user/nexusarchive
npm run typecheck
```

Expected: 无类型错误

---

## Task 2: 修改路由配置移除 withSuspense 包装（已直接导入的页面）

**Files:**
- Modify: `src/routes/index.tsx:179-210`

**Step 1: 确认哪些页面已经是直接导入**

检查以下页面已经是静态导入，无需 `withSuspense` 包装：
- `ArchiveListPage` (line 28)
- `VoucherMatchingPage` (line 29)
- `PoolPage` (line 30)
- `Dashboard` (Task 1 修改后)

**Step 2: 移除这些页面的 withSuspense 包装**

在路由配置中找到这些页面的路由，移除 `withSuspense()` 调用：

```tsx
// ArchiveListPage 相关路由（约 191-210 行）
// 修改前:
{ path: 'pre-archive/link', element: <ArchiveListPage routeConfig="link" /> },
{ path: 'collection', element: <ArchiveListPage routeConfig="collection" /> },
{ path: 'archive', element: <ArchiveListPage routeConfig="view" /> },
// ... 其他 ArchiveListPage 路由

// 保持不变（这些已经是直接使用组件，没有 withSuspense）
```

注意：ArchiveListPage 等页面在路由中已经是直接使用 `<ArchiveListPage />` 形式，不需要修改。

**Step 3: 验证编译**

```bash
npm run typecheck
```

Expected: 无类型错误

---

## Task 3: 优化 LoadingFallback 注释

**Files:**
- Modify: `src/routes/index.tsx:134-138`

**Step 1: 更新 LoadingFallback 注释**

更新注释说明哪些页面使用懒加载：

```tsx
/**
 * 加载占位符
 *
 * 注意：常用页面（Dashboard, PoolPage, ArchiveListPage）已改为直接导入，无需 LoadingFallback
 * 大型页面（全景视图、统计图表、报表）仍使用懒加载，需要此占位符
 */
const LoadingFallback = () => (
    <div className="flex items-center justify-center h-full text-slate-600">
        <span className="animate-pulse text-sm">加载中...</span>
    </div>
);
```

**Step 4: 验证编译**

```bash
npm run typecheck
```

Expected: 无类型错误

---

## Task 4: 开发模式验证测试

**Step 1: 启动开发服务器**

```bash
cd /Users/user/nexusarchive
npm run dev
```

等待看到：`Local: http://localhost:15175/`

**Step 2: 测试常用页面切换速度**

在浏览器中访问 http://localhost:15175 并测试：

1. 登录（用户名: admin，密码: admin123）
2. 点击 Dashboard 首页
3. 点击预归档库
4. 点击档案管理
5. 返回 Dashboard

**预期结果**：
- 常用页面切换无明显延迟（< 200ms）
- 无 "加载中..." 闪烁

**Step 3: 测试大型页面懒加载仍正常**

1. 点击全景视图
2. 点击统计报表
3. 点击关系查询

**预期结果**：
- 首次打开显示 "加载中..."
- 页面正常加载

**Step 4: 检查 Network 面板**

打开浏览器 DevTools → Network，观察：
- 常用页面切换：无新的 JS 文件请求
- 大型页面：有新的 chunk 文件请求

---

## Task 5: 生产构建验证

**Step 1: 构建生产版本**

```bash
npm run build
```

Expected: 构建成功，无错误

**Step 2: 检查构建输出**

```bash
ls -lh dist/assets/*.js | grep -E "Dashboard|PoolPage|ArchiveList" | head -5
```

Expected: 常用页面的代码已打包到主 bundle 或较小的 chunk 中

**Step 3: 启动预览服务器验证**

```bash
npm run preview
```

访问 http://localhost:4173 测试页面切换速度。

---

## 验收标准

### 功能验收

- [ ] Dashboard 首次加载和切换都流畅
- [ ] 档案列表、预归档库切换无明显延迟
- [ ] 大型页面（全景、报表）懒加载仍正常
- [ ] TypeScript 类型检查通过
- [ ] 开发模式 HMR 热更新仍正常工作

### 性能验收

| 页面 | 修改前 | 修改后 |
|:-----|:-------|:-------|
| Dashboard 切换 | 有延迟 | 无延迟 |
| 档案列表切换 | 有延迟 | 无延迟 |
| 首次加载 | ~300ms | ~400ms（可接受） |

---

## 技术说明

### 修改的页面

| 页面 | 原加载方式 | 新加载方式 | 原因 |
|:-----|:-----------|:-----------|:-----|
| Dashboard | lazy() | 直接导入 | 首页，最常用 |
| PoolPage | 直接导入 | 直接导入 | 已是直接导入，保持 |
| ArchiveListPage | 直接导入 | 直接导入 | 已是直接导入，保持 |
| VoucherMatchingPage | 直接导入 | 直接导入 | 已是直接导入，保持 |

### 保留懒加载的页面

| 页面 | 原因 |
|:-----|:-----|
| ArchivalPanoramaView | 包含大型图表组件 |
| StatsView | 包含统计图表 |
| RelationshipQueryView | 图表组件较多 |
| 全部设置/管理页面 | 使用频率低 |

### 权衡说明

**优点**：
- 常用页面切换快，开发体验好
- HMR 热更新仍然工作
- 首次加载增加的 bundle 大小可控（Dashboard ~20KB）

**缺点**：
- 首次加载可能稍慢（一次性加载所有常用页面）
- 主 bundle 略微增大

**结论**：对于内部管理系统，这是合理的权衡。

---

## 相关文档

- `vite.config.ts` - Vite 配置
- `src/routes/index.tsx` - 路由配置
- `src/routes/paths.ts` - 路径常量

---

**计划创建时间**: 2026-01-16
**预计工时**: 1-2 小时
**风险等级**: 低（仅修改导入方式，不改变业务逻辑）
