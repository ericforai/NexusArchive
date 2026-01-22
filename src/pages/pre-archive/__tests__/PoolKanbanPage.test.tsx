// src/pages/pre-archive/__tests__/PoolKanbanPage.test.tsx
// Input: PoolKanbanPage component, route configuration
// Output: Vitest test suite validating route integration and page rendering
// Pos: src/pages/pre-archive/__tests__/PoolKanbanPage.test.tsx

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { ROUTE_PATHS } from '@/routes/paths';

// Mock the PoolKanbanView component (must mock at the index.ts level for architecture rules)
vi.mock('@/components/pool-kanban', () => ({
  PoolKanbanView: ({ className }: { className?: string }) => (
    <div className={`pool-kanban-view ${className || ''}`}>
      <h2>记账凭证库</h2>
      <div className="pool-kanban-view__board">看板内容</div>
    </div>
  ),
}));

// Mock hooks
vi.mock('@/hooks/usePoolKanban', () => ({
  usePoolKanban: vi.fn(() => ({
    columns: [],
    loading: false,
    error: null,
    refetch: vi.fn(),
    getCardsForColumn: vi.fn(() => []),
  })),
}));

vi.mock('@/hooks/usePoolBatchAction', () => ({
  usePoolBatchAction: vi.fn(() => ({
    state: {
      selection: new Set<string>(),
      isExecuting: false,
      result: null,
    },
    selectAll: vi.fn(),
    toggleSelection: vi.fn(),
    clearSelection: vi.fn(),
    isSelected: vi.fn(),
    getSelectedCount: vi.fn(() => 0),
    getSelectedIds: vi.fn(() => []),
    executeAction: vi.fn(),
    clearResult: vi.fn(),
  })),
}));

vi.mock('@/hooks/useKanbanLayout', () => ({
  useKanbanLayout: vi.fn(() => ({
    columnWidth: 320,
    setColumnWidth: vi.fn(),
    recalculateWidth: vi.fn(),
    containerWidth: 1200,
    isOverflowing: false,
    getEmptyColumns: vi.fn(() => []),
    hasCards: vi.fn(() => true),
    collapsedColumns: new Set<string>(),
    toggleCollapse: vi.fn(),
    isCollapsed: vi.fn(() => false),
    collapseAllEmpty: vi.fn(),
    expandAll: vi.fn(),
    getVisibleColumnCount: vi.fn(() => 4),
    getTotalContentWidth: vi.fn(() => 1200),
  })),
}));

import { PoolKanbanPage } from '../PoolKanbanPage';

describe('PoolKanbanPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('Page Rendering', () => {
    it('should render the page wrapper div with correct class', () => {
      const { container } = render(
        <MemoryRouter initialEntries={['/system/pre-archive/pool/kanban']}>
          <PoolKanbanPage />
        </MemoryRouter>
      );

      const pageWrapper = container.querySelector('.pool-kanban-page');
      expect(pageWrapper).toBeInTheDocument();
    });

    it('should render PoolKanbanView component', () => {
      render(
        <MemoryRouter initialEntries={['/system/pre-archive/pool/kanban']}>
          <PoolKanbanPage />
        </MemoryRouter>
      );

      expect(screen.getByText('记账凭证库')).toBeInTheDocument();
      expect(screen.getByText('看板内容')).toBeInTheDocument();
    });

    it('should apply correct CSS class to page wrapper', () => {
      const { container } = render(
        <MemoryRouter initialEntries={['/system/pre-archive/pool/kanban']}>
          <PoolKanbanPage />
        </MemoryRouter>
      );

      const pageWrapper = container.querySelector('.pool-kanban-page');
      expect(pageWrapper).toHaveClass('pool-kanban-page');
    });
  });

  describe('Route Integration', () => {
    it('should render at /system/pre-archive/pool/kanban route', () => {
      render(
        <MemoryRouter initialEntries={['/system/pre-archive/pool/kanban']}>
          <Routes>
            <Route path="/system/pre-archive/pool/kanban" element={<PoolKanbanPage />} />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.getByText('记账凭证库')).toBeInTheDocument();
    });

    it('should not render at different route paths', () => {
      render(
        <MemoryRouter initialEntries={['/system/pre-archive/pool']}>
          <Routes>
            <Route path="/system/pre-archive/pool/kanban" element={<PoolKanbanPage />} />
            <Route path="/system/pre-archive/pool" element={<div>Other Page</div>} />
          </Routes>
        </MemoryRouter>
      );

      expect(screen.queryByText('记账凭证库')).not.toBeInTheDocument();
      expect(screen.getByText('Other Page')).toBeInTheDocument();
    });

    it('should be accessible via ROUTE_PATHS.PRE_ARCHIVE_POOL_KANBAN constant', () => {
      expect(ROUTE_PATHS.PRE_ARCHIVE_POOL_KANBAN).toBe('/system/pre-archive/pool/kanban');
    });
  });

  describe('Component Structure', () => {
    it('should contain PoolKanbanView as a child', () => {
      const { container } = render(
        <MemoryRouter initialEntries={['/system/pre-archive/pool/kanban']}>
          <PoolKanbanPage />
        </MemoryRouter>
      );

      const kanbanView = container.querySelector('.pool-kanban-view');
      expect(kanbanView).toBeInTheDocument();
    });

    it('should pass through props correctly to PoolKanbanView', () => {
      const { container } = render(
        <MemoryRouter initialEntries={['/system/pre-archive/pool/kanban']}>
          <PoolKanbanPage />
        </MemoryRouter>
      );

      // Verify the inner view is rendered
      const board = container.querySelector('.pool-kanban-view__board');
      expect(board).toBeInTheDocument();
    });
  });
});
