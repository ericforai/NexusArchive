// src/pages/pre-archive/__tests__/PoolPage.test.tsx
// Input: PoolPage component
// Output: Test suite for view switching behavior
// Pos: src/pages/pre-archive/__tests__/PoolPage.test.tsx

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';

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

// Mock 子组件 (must be before import)
vi.mock('@/components/pool-kanban', () => ({
  PoolKanbanView: () => <div data-testid="kanban-view">Kanban View</div>,
}));

vi.mock('@/pages/archives/ArchiveListPage', () => ({
  ArchiveListPage: ({ routeConfig }: { routeConfig?: string }) => (
    <div data-testid="list-view" data-route-config={routeConfig}>List View</div>
  ),
}));

// Mock navigate function for redirect test
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual as object,
    useNavigate: () => mockNavigate,
  };
});

import { PoolPage } from '../PoolPage';

describe('PoolPage', () => {
  beforeEach(() => {
    localStorageMock.clear();
    mockNavigate.mockClear();
    vi.clearAllMocks();
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

  it('看板视图激活状态应正确显示', () => {
    render(
      <MemoryRouter initialEntries={['/system/pre-archive/pool?view=kanban']}>
        <Routes>
          <Route path="/system/pre-archive/pool" element={<PoolPage />} />
        </Routes>
      </MemoryRouter>
    );

    const listButton = screen.getByTitle('列表视图 - 适合查看大量数据和批量操作');
    const kanbanButton = screen.getByTitle('看板视图 - 直观展示处理流程');

    expect(kanbanButton).toHaveClass('active');
    expect(listButton).not.toHaveClass('active');
  });

  it('应显示页面标题', () => {
    render(
      <MemoryRouter initialEntries={['/system/pre-archive/pool']}>
        <Routes>
          <Route path="/system/pre-archive/pool" element={<PoolPage />} />
        </Routes>
      </MemoryRouter>
    );

    expect(screen.getByText('电子凭证池')).toBeInTheDocument();
  });

  it('应将 routeConfig 传递给 ArchiveListPage', () => {
    render(
      <MemoryRouter initialEntries={['/system/pre-archive/pool']}>
        <Routes>
          <Route path="/system/pre-archive/pool" element={<PoolPage />} />
        </Routes>
      </MemoryRouter>
    );

    const listView = screen.getByTestId('list-view');
    expect(listView).toHaveAttribute('data-route-config', 'pool');
  });
});
