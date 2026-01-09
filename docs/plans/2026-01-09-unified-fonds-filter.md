# 全宗筛选统一实施方案

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 移除页面内的全宗筛选器，让档案列表数据自动跟随顶部全局全宗切换器

**架构：** 利用现有的 `useFondsStore` 作为单一数据源，数据加载层直接从 Store 获取当前全宗号，实现顶部切换器 → Store → API 过滤的自动联动。

**技术栈：** Zustand (useFondsStore), React Hooks, TypeScript

---

## 背景说明

### 问题
- 顶部有全局全宗切换器 (FondsSwitcher)
- 页面筛选弹窗内也有全宗下拉框
- 两者功能重复，用户困惑

### 解决方案
- 移除页面内的全宗筛选器
- 数据加载时直接从 `useFondsStore` 获取当前全宗号
- 用户切换顶部全宗 → 列表自动刷新过滤

### 涉及文件
| 文件 | 操作 | 说明 |
|------|------|------|
| `src/features/archives/controllers/types.ts` | 修改 | 移除 fondsFilter 相关类型 |
| `src/features/archives/controllers/useArchiveQuery.ts` | 修改 | 移除 fondsFilter 状态管理 |
| `src/features/archives/controllers/useArchiveDataLoader.ts` | 修改 | 从 useFondsStore 获取 fondsNo |
| `src/pages/archives/ArchiveListView.tsx` | 修改 | 移除全宗筛选器 UI |
| `src/features/archives/controllers/README.md` | 更新 | 更新文档说明 |

---

## Task 1: 清理类型定义

**文件：**
- 修改: `src/features/archives/controllers/types.ts`

**Step 1: 移除 fondsFilter 相关类型定义**

从 `ControllerQuery` 接口中移除以下字段：
```typescript
fondsFilter: string;
setFondsFilter: (v: string) => void;
fondsOptions: { label: string; value: string }[];
```

修改后的 `ControllerQuery` 接口：
```typescript
export interface ControllerQuery {
    searchTerm: string;
    setSearchTerm: (v: string) => void;
    statusFilter: string;
    setStatusFilter: (v: string) => void;
    orgFilter: string;
    setOrgFilter: (v: string) => void;
    orgOptions: Array<{ label: string; value: string }>;
    subTypeFilter: string;
    setSubTypeFilter: (v: string) => void;
    // 移除了 fondsFilter、setFondsFilter、fondsOptions
}
```

**Step 2: 运行类型检查**

```bash
npm run typecheck
```

预期: 会有类型错误（因为其他文件还在使用这些类型），后续任务会修复。

---

## Task 2: 清理 useArchiveQuery Hook

**文件：**
- 修改: `src/features/archives/controllers/useArchiveQuery.ts`

**Step 1: 移除 fondsFilter 状态和相关逻辑**

删除以下内容：
1. `fondsFilter` 和 `fondsOptions` 状态声明
2. 加载全宗列表的 `useEffect`
3. 处理路由 state 的 `useEffect`（从集团架构跳转的逻辑）

保留的导入和状态：
```typescript
import { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { adminApi } from '../../../api/admin';
import { ControllerQuery } from './types';

export function useArchiveQuery(): ControllerQuery {
    const location = useLocation();

    const [searchTerm, setSearchTerm] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [orgFilter, setOrgFilter] = useState('');
    const [subTypeFilter, setSubTypeFilter] = useState(
        new URLSearchParams(location.search).get('type') || ''
    );
    const [orgOptions, setOrgOptions] = useState<{ label: string; value: string }[]>([]);
    // 移除了: fondsFilter, setFondsFilter, fondsOptions

    // URL 同步
    useEffect(() => {
        const params = new URLSearchParams(location.search);
        setSubTypeFilter(params.get('type') || '');
    }, [location.search]);

    // 加载组织列表
    useEffect(() => {
        const loadOrgs = async () => {
            try {
                const res = await adminApi.listOrg();
                if (res.code === 200 && res.data) {
                    setOrgOptions(
                        (res.data as any[]).map((o) => ({
                            label: o.name,
                            value: o.id
                        }))
                    );
                }
            } catch {
                // ignore
            }
        };
        loadOrgs();
    }, []);

    // 移除了: 加载全宗列表的 useEffect
    // 移除了: 处理路由 state 的 useEffect

    return {
        searchTerm,
        setSearchTerm,
        statusFilter,
        setStatusFilter,
        orgFilter,
        setOrgFilter,
        orgOptions,
        subTypeFilter,
        setSubTypeFilter,
        // 移除了: fondsFilter, setFondsFilter, fondsOptions
    };
}
```

**Step 2: 验证类型检查**

```bash
npm run typecheck
```

预期: 错误减少（useArchiveQuery 不再返回 fondsFilter）

---

## Task 3: 修改数据加载器从 Store 获取全宗

**文件：**
- 修改: `src/features/archives/controllers/useArchiveDataLoader.ts`

**Step 1: 添加 useFondsStore 导入**

在文件顶部添加：
```typescript
import { useFondsStore } from '../../../store/useFondsStore';
```

**Step 2: 修改 loadArchiveList 函数**

找到 `loadArchiveList` 函数，修改 API 调用参数：

**原代码：**
```typescript
const result = await archivesApi.getArchives({
    page: pageNum,
    limit: page.pageInfo.pageSize,
    search: query.searchTerm || undefined,
    status: query.statusFilter || mode.defaultStatus,
    categoryCode: mode.categoryCode,
    orgId: query.orgFilter || undefined,
    subType: query.subTypeFilter || undefined,
    fondsNo: query.fondsFilter || undefined  // 从 query 获取
});
```

**修改为：**
```typescript
const result = await archivesApi.getArchives({
    page: pageNum,
    limit: page.pageInfo.pageSize,
    search: query.searchTerm || undefined,
    status: query.statusFilter || mode.defaultStatus,
    categoryCode: mode.categoryCode,
    orgId: query.orgFilter || undefined,
    subType: query.subTypeFilter || undefined,
    fondsNo: useFondsStore.getState().getCurrentFondsCode() || undefined  // 从 Store 获取
});
```

**Step 3: 修改依赖追踪**

更新 `prevDepsRef` 和 `depsChanged` 检查，移除 `fondsFilter`：

**原代码：**
```typescript
const prevDepsRef = useRef({
    subTitle: mode.subTitle,
    searchTerm: query.searchTerm,
    statusFilter: query.statusFilter,
    orgFilter: query.orgFilter,
    subTypeFilter: query.subTypeFilter,
    fondsFilter: query.fondsFilter,  // 移除这行
    poolStatusFilter,
});
```

**修改后：**
```typescript
const prevDepsRef = useRef({
    subTitle: mode.subTitle,
    searchTerm: query.searchTerm,
    statusFilter: query.statusFilter,
    orgFilter: query.orgFilter,
    subTypeFilter: query.subTypeFilter,
    // 移除了 fondsFilter
    poolStatusFilter,
});
```

**Step 4: 修改 filter 变化的 useEffect**

更新 `depsChanged` 检查：

**原代码：**
```typescript
const depsChanged =
    prevDeps.subTitle !== mode.subTitle ||
    prevDeps.searchTerm !== query.searchTerm ||
    prevDeps.statusFilter !== query.statusFilter ||
    prevDeps.orgFilter !== query.orgFilter ||
    prevDeps.subTypeFilter !== query.subTypeFilter ||
    prevDeps.fondsFilter !== query.fondsFilter;  // 移除这行
```

**修改后：**
```typescript
const depsChanged =
    prevDeps.subTitle !== mode.subTitle ||
    prevDeps.searchTerm !== query.searchTerm ||
    prevDeps.statusFilter !== query.statusFilter ||
    prevDeps.orgFilter !== query.orgFilter ||
    prevDeps.subTypeFilter !== query.subTypeFilter;
    // 移除了 fondsFilter 检查
```

同样更新 `prevDepsRef.current` 的赋值部分，移除 `fondsFilter`。

**Step 5: 添加 Store 变化监听**

为了在用户切换顶部全宗时自动刷新列表，添加对 FondsStore 的监听：

在 `useArchiveDataLoader` 函数内，现有 `useEffect` 之后添加：

```typescript
// 监听全宗切换，自动刷新列表
const currentFondsCodeRef = useRef<string | null>(null);

useEffect(() => {
    const unsubscribe = useFondsStore.subscribe(
        (state) => state.currentFonds?.fondsCode,
        (fondsCode) => {
            // 跳过初始化时的触发
            if (currentFondsCodeRef.current === fondsCode) {
                return;
            }
            currentFondsCodeRef.current = fondsCode || null;

            // 非首次加载时，刷新列表
            if (!isInitialLoadRef.current && !isPoolView) {
                loadArchiveList(1);
            }
        }
    );

    return unsubscribe;
}, [loadArchiveList, isPoolView]);
```

**Step 6: 验证类型检查**

```bash
npm run typecheck
```

预期: 类型检查通过

---

## Task 4: 移除页面 UI 中的全宗筛选器

**文件：**
- 修改: `src/pages/archives/ArchiveListView.tsx`

**Step 1: 移除全宗筛选器 UI**

找到筛选弹窗中的全宗筛选器部分（约 620-635 行），删除：

```typescript
{/* Fonds Filter */}
<div>
    <label className="text-sm font-bold text-slate-700 mb-2 block">全宗</label>
    <select
        value={query.fondsFilter}
        onChange={(e) => query.setFondsFilter(e.target.value)}
        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
    >
        <option value="">全部</option>
        {query.fondsOptions.map((fonds) => (
            <option key={fonds.value} value={fonds.value}>
                {fonds.label}
            </option>
        ))}
    </select>
</div>
```

**Step 2: 更新重置按钮**

找到重置按钮的 `onClick` 处理，移除 `fondsFilter` 重置：

**原代码：**
```typescript
onClick={() => {
    query.setStatusFilter('');
    query.setOrgFilter('');
    query.setSubTypeFilter('');
    query.setFondsFilter('');  // 移除这行
}}
```

**修改后：**
```typescript
onClick={() => {
    query.setStatusFilter('');
    query.setOrgFilter('');
    query.setSubTypeFilter('');
}}
```

**Step 3: 验证构建**

```bash
npm run build
```

预期: 构建成功

---

## Task 5: 更新文档

**文件：**
- 修改: `src/features/archives/controllers/README.md`

**Step 1: 更新控制器架构说明**

在"Query 控制器"部分，移除 `fondsFilter` 相关说明：

**原内容：**
```markdown
### Query 控制器

负责筛选和搜索状态管理：

interface ControllerQuery {
    searchTerm: string;           // 搜索关键词
    statusFilter: string;         // 状态筛选
    orgFilter: string;            // 组织筛选
    subTypeFilter: string;        // 子类型筛选
    fondsFilter: string;          // 全宗筛选
    orgOptions: Array<...>;       // 组织选项
    fondsOptions: Array<...>;     // 全宗选项
}
```

**修改后：**
```markdown
### Query 控制器

负责筛选和搜索状态管理：

interface ControllerQuery {
    searchTerm: string;           // 搜索关键词
    statusFilter: string;         // 状态筛选
    orgFilter: string;            // 组织筛选
    subTypeFilter: string;        // 子类型筛选
    orgOptions: Array<...>;       // 组织选项
}

注意：全宗筛选通过顶部全局 FondsSwitcher 控制，
数据加载层 (useArchiveDataLoader) 自动从 useFondsStore 获取当前全宗。
```

**Step 2: 更新导航集成说明**

**原内容：**
```markdown
## 导航集成

控制器支持从其他页面接收导航参数：

useEffect(() => {
    const state = location.state as { fondsNo?: string } | null;
    if (state?.fondsNo) {
        setFondsFilter(state.fondsNo);
        window.history.replaceState({}, '', location.pathname);
    }
}, [navigation, location.pathname]);
```

**修改后：**
```markdown
## 导航集成

从集团架构页面跳转时，自动切换全局全宗：

useEffect(() => {
    const state = location.state as { fondsNo?: string } | null;
    if (state?.fondsNo) {
        // 查找目标全宗并切换全局状态
        const targetFonds = useFondsStore.getState().fondsList
            .find(f => f.fondsCode === state.fondsNo);
        if (targetFonds) {
            useFondsStore.getState().setCurrentFonds(targetFonds);
        }
        window.history.replaceState({}, '', location.pathname);
    }
}, [navigation, location.pathname]);
```

---

## Task 6: 修复集团架构页面跳转逻辑

**文件：**
- 修改: `src/pages/admin/EnterpriseArchitecturePage.tsx`

**Step 1: 更新跳转处理函数**

**原代码：**
```typescript
const handleFondsClick = (fondsCode: string) => {
    navigate(ROUTE_PATHS.ARCHIVE, { state: { fondsNo: fondsCode } });
};
```

**修改为：**
```typescript
const handleFondsClick = (fondsCode: string) => {
    // 直接切换全局全宗状态，而不是通过路由 state
    import('../../../store/useFondsStore').then(({ useFondsStore }) => {
        const targetFonds = useFondsStore.getState().fondsList
            .find(f => f.fondsCode === fondsCode);
        if (targetFonds) {
            useFondsStore.getState().setCurrentFonds(targetFonds);
        }
    });
    navigate(ROUTE_PATHS.ARCHIVE);
};
```

或者更简洁的方式（如果组件已经导入了 useFondsStore）：
```typescript
import { useFondsStore } from '../../store/useFondsStore';

// 在组件内
const setCurrentFonds = useFondsStore(state => state.setCurrentFonds);
const fondsList = useFondsStore(state => state.fondsList);

const handleFondsClick = (fondsCode: string) => {
    const targetFonds = fondsList.find(f => f.fondsCode === fondsCode);
    if (targetFonds) {
        setCurrentFonds(targetFonds);
    }
    navigate(ROUTE_PATHS.ARCHIVE);
};
```

---

## 测试验证

### 手动测试步骤

1. **顶部切换器联动测试**
   - 打开档案列表页面
   - 点击顶部全宗切换器，切换到另一个全宗
   - 验证列表自动刷新，只显示新全宗的数据

2. **集团架构跳转测试**
   - 打开集团架构页面
   - 点击某个全宗节点
   - 验证跳转到档案列表，顶部全宗已切换，列表已过滤

3. **权限一致性测试**
   - 确认顶部切换器只显示用户有权限的全宗
   - 确认列表数据不超过用户权限范围

### 回归测试

```bash
# 后端编译
cd nexusarchive-java && mvn compile

# 前端类型检查
npm run typecheck

# 前端构建
npm run build
```

---

## 提交记录

```bash
# Task 1-2: 清理类型和状态管理
git add src/features/archives/controllers/types.ts
git add src/features/archives/controllers/useArchiveQuery.ts
git commit -m "refactor: remove fondsFilter from query state"

# Task 3: 数据加载器从 Store 获取全宗
git add src/features/archives/controllers/useArchiveDataLoader.ts
git commit -m "refactor: load fondsNo from useFondsStore instead of query"

# Task 4: 移除页面 UI 筛选器
git add src/pages/archives/ArchiveListView.tsx
git commit -m "refactor: remove fonds filter UI, use global switcher"

# Task 5: 更新文档
git add src/features/archives/controllers/README.md
git commit -m "docs: update controllers README for unified fonds filtering"

# Task 6: 修复集团架构跳转
git add src/pages/admin/EnterpriseArchitecturePage.tsx
git commit -m "fix: update enterprise architecture navigation to use global fonds store"
```

---

## 完成标志

- [ ] 类型检查通过 (`npm run typecheck`)
- [ ] 后端编译通过 (`mvn compile`)
- [ ] 页面内无全宗筛选器
- [ ] 顶部切换全宗后列表自动刷新
- [ ] 文档已更新
