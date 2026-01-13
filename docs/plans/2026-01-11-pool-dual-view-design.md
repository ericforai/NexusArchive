# 电子凭证池双视图模式实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为电子凭证池添加列表/看板双视图切换功能，保留看板视图的同时恢复列表视图作为默认选项。

**Architecture:** 创建容器组件 `PoolPage` 作为视图切换器，复用现有的 `ArchiveListPage` (列表) 和 `PoolKanbanView` (看板)，通过 URL query 参数 `?view=list|kanban` 控制视图模式。

**Tech Stack:** React 18, React Router v7, TypeScript, Vitest, Ant Design

---

## Task 1: 创建 PoolPage 容器组件

**Files:**
- Create: `src/pages/pre-archive/PoolPage.tsx`
- Create: `src/pages/pre-archive/PoolPage.css`

**Step 1: 创建 CSS 样式文件**

```css
/* src/pages/pre-archive/PoolPage.css */
.pool-page {
  width: 100%;
  height: 100%;
}

.pool-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 0 8px;
}

.pool-page__title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pool-page__title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #0f172a;
}

.pool-page__switcher {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: #f1f5f9;
  border-radius: 8px;
}

.pool-page__switcher button {
  padding: 6px 16px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.pool-page__switcher button:hover {
  color: #334155;
  background: rgba(255, 255, 255, 0.5);
}

.pool-page__switcher button.active {
  background: white;
  color: #0f172a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.pool-page__content {
  min-height: 400px;
}
```

**Step 2: 创建 PoolPage 容器组件**

```typescript
// src/pages/pre-archive/PoolPage.tsx
// Input: URL search params, 视图模式状态
// Output: 带视图切换器的容器页面
// Pos: src/pages/pre-archive/PoolPage.tsx

import React, { useState, useEffect, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { List, Columns3 } from 'lucide-react';
import { PoolKanbanView } from '@/components/pool-kanban';
import { ArchiveListPage } from '@/pages/archives/ArchiveListPage';
import './PoolPage.css';

const VIEW_MODE_STORAGE_KEY = 'pool.viewMode';
const DEFAULT_VIEW_MODE: ViewMode = 'list';

type ViewMode = 'list' | 'kanban';

interface ViewSwitcherProps {
  currentMode: ViewMode;
  onModeChange: (mode: ViewMode) => void;
}

/**
 * 视图切换器组件
 */
function ViewSwitcher({ currentMode, onModeChange }: ViewSwitcherProps) {
  return (
    <div className="pool-page__switcher">
      <button
        className={currentMode === 'list' ? 'active' : ''}
        onClick={() => onModeChange('list')}
        title="列表视图 - 适合查看大量数据和批量操作"
      >
        <List size={16} />
        列表
      </button>
      <button
        className={currentMode === 'kanban' ? 'active' : ''}
        onClick={() => onModeChange('kanban')}
        title="看板视图 - 直观展示处理流程"
      >
        <Columns3 size={16} />
        看板
      </button>
    </div>
  );
}

/**
 * 电子凭证池页面容器
 *
 * 职责：
 * 1. 维护视图模式状态 (list/kanban)
 * 2. 同步 URL query 参数 (?view=list|kanban)
 * 3. 记忆用户的视图偏好
 * 4. 渲染视图切换器和对应的子视图
 */
export const PoolPage: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  // 从 localStorage 读取用户偏好
  const getInitialViewMode = useCallback((): ViewMode => {
    // 优先级: URL 参数 > localStorage > 默认值
    const viewParam = searchParams.get('view') as ViewMode | null;
    if (viewParam === 'list' || viewParam === 'kanban') {
      return viewParam;
    }
    const stored = localStorage.getItem(VIEW_MODE_STORAGE_KEY);
    if (stored === 'list' || stored === 'kanban') {
      return stored;
    }
    return DEFAULT_VIEW_MODE;
  }, [searchParams]);

  const [viewMode, setViewMode] = useState<ViewMode>(getInitialViewMode);

  // 同步 URL 参数变化到状态
  useEffect(() => {
    const viewParam = searchParams.get('view') as ViewMode | null;
    if (viewParam && (viewParam === 'list' || viewParam === 'kanban') && viewParam !== viewMode) {
      setViewMode(viewParam);
    }
  }, [searchParams, viewMode]);

  // 处理视图切换
  const handleViewChange = useCallback((mode: ViewMode) => {
    setViewMode(mode);
    setSearchParams({ view: mode });
    localStorage.setItem(VIEW_MODE_STORAGE_KEY, mode);
  }, [setSearchParams]);

  // 兼容旧的 /kanban 路由 - 重定向到新格式
  useEffect(() => {
    if (window.location.pathname.endsWith('/kanban')) {
      navigate(`/system/pre-archive/pool?view=kanban`, { replace: true });
    }
  }, [navigate]);

  return (
    <div className="pool-page">
      <div className="pool-page__header">
        <div className="pool-page__title-section">
          <h1 className="pool-page__title">电子凭证池</h1>
        </div>
        <ViewSwitcher currentMode={viewMode} onModeChange={handleViewChange} />
      </div>

      <div className="pool-page__content">
        {viewMode === 'kanban' ? (
          <PoolKanbanView />
        ) : (
          <ArchiveListPage routeConfig="pool" />
        )}
      </div>
    </div>
  );
};

export default PoolPage;
```

**Step 3: 运行类型检查**

```bash
npm run typecheck
```

Expected: 无类型错误

**Step 4: 提交**

```bash
git add src/pages/pre-archive/PoolPage.tsx src/pages/pre-archive/PoolPage.css
git commit -m "feat(pool): add PoolPage container with view switcher"
```

---

## Task 2: 更新路由配置

**Files:**
- Modify: `src/routes/index.tsx`

**Step 1: 修改路由配置**

找到 `pre-archive` 相关的路由配置，修改为：

```typescript
// ========== 预归档库 ==========
// PoolPage 作为容器，支持列表/看板双视图
{ path: 'pre-archive', element: <PoolPage /> },
{ path: 'pre-archive/pool', element: <PoolPage /> },
{ path: 'pre-archive/pool/kanban', element: <PoolPage /> },  // 兼容旧路由
{ path: 'pre-archive/doc-pool', element: withSuspense(OriginalVoucherListView, { title: '单据池', subTitle: '原始单据管理', poolMode: true }) },
{ path: 'pre-archive/ocr', element: withSuspense(OCRProcessingView) },
{ path: 'pre-archive/link', element: <ArchiveListPage routeConfig="link" /> },
{ path: 'pre-archive/abnormal', element: withSuspense(AbnormalDataView) },
```

需要添加导入：
```typescript
import { PoolPage } from '@/pages/pre-archive/PoolPage';
```

**Step 2: 运行类型检查**

```bash
npm run typecheck
```

Expected: 无类型错误

**Step 3: 提交**

```bash
git add src/routes/index.tsx
git commit -m "feat(routes): add PoolPage route configuration"
```

---

## Task 3: 更新路由路径常量说明

**Files:**
- Modify: `src/routes/paths.ts`

**Step 1: 更新注释**

修改 `PRE_ARCHIVE_POOL` 相关注释：

```typescript
/**
 * 路由路径常量
 *
 * 独立文件避免循环依赖
 */

export const ROUTE_PATHS = {
    // ...

    PRE_ARCHIVE: '/system/pre-archive',
    PRE_ARCHIVE_POOL: '/system/pre-archive/pool',  // 支持 ?view=list|kanban 参数切换视图
    // PRE_ARCHIVE_POOL_KANBAN 保留用于兼容，但推荐使用 PRE_ARCHIVE_POOL?view=kanban
    PRE_ARCHIVE_DOC_POOL: '/system/pre-archive/doc-pool',
    // ...
}
```

**Step 2: 更新 SUBITEM_TO_PATH 映射**

```typescript
export const SUBITEM_TO_PATH: Record<string, string> = {
    // 预归档库
    '电子凭证池': ROUTE_PATHS.PRE_ARCHIVE_POOL,  // 默认列表视图，支持 ?view=kanban 切换
    '单据池': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL,
    // ...
}
```

**Step 3: 提交**

```bash
git add src/routes/paths.ts
git commit -m "docs(routes): update path comments for dual view mode"
```

---

## Task 4: 添加测试用例

**Files:**
- Create: `src/pages/pre-archive/__tests__/PoolPage.test.tsx`

**Step 1: 创建测试文件**

```typescript
// src/pages/pre-archive/__tests__/PoolPage.test.tsx
// Input: PoolPage component
// Output: Test suite for view switching behavior

import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { PoolPage } from '../PoolPage';

// Mock localStorage
const localStorageMock = (() => {
  let store: Record<string, string> = {};
  return {
    getItem: (key: string) => store[key] || null,
    setItem: (key: string, value: string) => { store[key] = value; },
    clear: () => { store = {}; },
  };
})();

Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock 子组件
jest.mock('@/components/pool-kanban', () => ({
  PoolKanbanView: () => <div data-testid="kanban-view">Kanban View</div>,
}));

jest.mock('@/pages/archives/ArchiveListPage', () => ({
  ArchiveListPage: () => <div data-testid="list-view">List View</div>,
}));

describe('PoolPage', () => {
  beforeEach(() => {
    localStorageMock.clear();
  });

  it('应默认显示列表视图', () => {
    render(
      <MemoryRouter initialEntries={['/system/pre-archive/pool']}>
        <Routes>
          <Route path="/system/pre-archive/pool" element={<PoolPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByTestId('list-view')).toBeInTheDocument();
    expect(screen.queryByTestId('kanban-view')).not.toBeInTheDocument();
  });

  it('应支持 URL 参数切换到看板视图', () => {
    render(
      <MemoryRouter initialEntries={['/system/pre-archive/pool?view=kanban']}>
        <Routes>
          <Route path="/system/pre-archive/pool" element={<PoolPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByTestId('kanban-view')).toBeInTheDocument();
    expect(screen.queryByTestId('list-view')).not.toBeInTheDocument();
  });

  it('应通过切换按钮改变视图', async () => {
    render(
      <MemoryRouter initialEntries={['/system/pre-archive/pool']}>
        <Routes>
          <Route path="/system/pre-archive/pool" element={<PoolPage />} />
        </Routes>
      </MemoryRouter>
    );

    // 初始是列表视图
    expect(screen.getByTestId('list-view')).toBeInTheDocument();

    // 点击看板按钮
    const kanbanButton = screen.getByTitle('看板视图 - 直观展示处理流程');
    fireEvent.click(kanbanButton);

    // 切换到看板视图
    await waitFor(() => {
      expect(screen.getByTestId('kanban-view')).toBeInTheDocument();
    });
  });

  it('应记忆用户的视图偏好', () => {
    // 设置用户偏好为看板
    localStorageMock.setItem('pool.viewMode', 'kanban');

    render(
      <MemoryRouter initialEntries={['/system/pre-archive/pool']}>
        <Routes>
          <Route path="/system/pre-archive/pool" element={<PoolPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByTestId('kanban-view')).toBeInTheDocument();
  });

  it('应包含视图切换器', () => {
    render(
      <MemoryRouter initialEntries={['/system/pre-archive/pool']}>
        <Routes>
          <Route path="/system/pre-archive/pool" element={<PoolPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByTitle('列表视图 - 适合查看大量数据和批量操作')).toBeInTheDocument();
    expect(screen.getByTitle('看板视图 - 直观展示处理流程')).toBeInTheDocument();
  });

  it('应显示正确的激活状态', () => {
    render(
      <MemoryRouter initialEntries={['/system/pre-archive/pool?view=list']}>
        <Routes>
          <Route path="/system/pre-archive/pool" element={<PoolPage />} />
        </Routes>
      </MemoryRouter>
    );

    const listButton = screen.getByTitle('列表视图 - 适合查看大量数据和批量操作');
    const kanbanButton = screen.getByTitle('看板视图 - 直观展示处理流程');

    expect(listButton).toHaveClass('active');
    expect(kanbanButton).not.toHaveClass('active');
  });
});
```

**Step 2: 运行测试**

```bash
npm run test -- PoolPage.test.tsx
```

Expected: PASS

**Step 3: 提交**

```bash
git add src/pages/pre-archive/__tests__/PoolPage.test.tsx
git commit -m "test(pool): add PoolPage test suite"
```

---

## Task 5: 更新模块清单

**Files:**
- Modify: `src/pages/pre-archive/manifest.config.ts`

**Step 1: 更新模块清单**

在 `exports` 中添加新组件：

```typescript
// src/pages/pre-archive/manifest.config.ts

export default {
  // ... 其他配置

  exports: {
    // ... 已有导出

    // Pool components
    PoolPage: {
      role: 'page',
      file: 'PoolPage.tsx',
      capabilities: ['view-switch', 'pool-list', 'pool-kanban'],
    },
  },
};
```

**Step 2: 提交**

```bash
git add src/pages/pre-archive/manifest.config.ts
git commit -m "chore(manifest): add PoolPage to module exports"
```

---

## Task 6: 手动验证

**Step 1: 启动开发服务器**

```bash
npm run dev
```

**Step 2: 访问以下 URL 验证**

| URL | 预期结果 |
|-----|----------|
| `http://localhost:15175/system/pre-archive/pool` | 默认显示列表视图 |
| `http://localhost:15175/system/pre-archive/pool?view=kanban` | 显示看板视图 |
| `http://localhost:15175/system/pre-archive/pool/kanban` | 重定向到 `?view=kanban` |

**Step 3: 验证功能**

- [ ] 点击"列表"按钮切换到列表视图
- [ ] 点击"看板"按钮切换到看板视图
- [ ] 切换后 URL 参数正确更新
- [ ] 刷新页面后视图模式保持不变（localStorage 生效）
- [ ] 两个视图都能正常加载数据

**Step 4: 运行全部测试**

```bash
npm run test:run
```

Expected: 全部通过

---

## Task 7: 更新文档

**Files:**
- Modify: `docs/guides/功能模块.md`

**Step 1: 添加功能说明**

在"预归档库"章节中添加：

```markdown
### 电子凭证池

支持双视图模式：

- **列表视图**（默认）：适合查看大量数据，支持筛选、批量操作
- **看板视图**：直观展示处理流程（待检测→待补全→待归档）

切换方式：点击页面右上角的"列表"/"看板"按钮，或使用 URL 参数 `?view=list|kanban`
```

**Step 2: 提交**

```bash
git add docs/guides/功能模块.md
git commit -m "docs: add dual view mode description to feature guide"
```

---

## 验收标准

- [ ] 默认显示列表视图
- [ ] 可以通过按钮切换列表/看板视图
- [ ] URL 参数 `?view=list|kanban` 正确控制视图
- [ ] 用户偏好被保存到 localStorage
- [ ] 旧的 `/kanban` 路由正确重定向
- [ ] 所有测试通过
- [ ] 类型检查通过
- [ ] 架构检查通过（`npm run check:arch`）
