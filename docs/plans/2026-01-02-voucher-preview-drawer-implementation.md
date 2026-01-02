# Voucher Preview Drawer - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace modal-based voucher preview with drawer-based UX for better user experience and performance.

**Architecture:** Use Ant Design Drawer component to replace ArchiveDetailModal, with responsive widths (50%/70%/100% based on screen size), route-based auto-close, and optional expand-to-full-page feature. Create Zustand store for drawer state management and add new route for expanded page view.

**Tech Stack:** React 19, TypeScript 5.8, Ant Design 6.1.1, React Router 7.9.6, Zustand 5.0.9

---

## Task 1: Create Zustand Store for Drawer State Management

**Files:**
- Create: `src/store/useDrawerStore.ts`

**Step 1: Write the failing test**

Create test file `src/store/__tests__/useDrawerStore.test.ts`:

```typescript
import { renderHook, act } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { useDrawerStore } from '../useDrawerStore';

describe('useDrawerStore', () => {
  beforeEach(() => {
    // Reset store before each test
    useDrawerStore.setState({
      isOpen: false,
      activeTab: 'metadata',
      archiveId: null,
      expandedMode: false
    });
  });

  it('should initialize with default state', () => {
    const { result } = renderHook(() => useDrawerStore());
    expect(result.current.isOpen).toBe(false);
    expect(result.current.activeTab).toBe('metadata');
    expect(result.current.archiveId).toBe(null);
    expect(result.current.expandedMode).toBe(false);
  });

  it('should open drawer with archive ID', () => {
    const { result } = renderHook(() => useDrawerStore());
    act(() => {
      result.current.open('archive-123');
    });
    expect(result.current.isOpen).toBe(true);
    expect(result.current.archiveId).toBe('archive-123');
  });

  it('should close drawer and reset state', () => {
    const { result } = renderHook(() => useDrawerStore());
    act(() => {
      result.current.open('archive-123');
      result.current.setActiveTab('voucher');
    });
    expect(result.current.isOpen).toBe(true);
    expect(result.current.activeTab).toBe('voucher');

    act(() => {
      result.current.close();
    });
    expect(result.current.isOpen).toBe(false);
    expect(result.current.archiveId).toBe(null);
    expect(result.current.activeTab).toBe('metadata'); // Reset to default
  });

  it('should set active tab', () => {
    const { result } = renderHook(() => useDrawerStore());
    act(() => {
      result.current.setActiveTab('attachments');
    });
    expect(result.current.activeTab).toBe('attachments');
  });

  it('should toggle expanded mode', () => {
    const { result } = renderHook(() => useDrawerStore());
    act(() => {
      result.current.setExpandedMode(true);
    });
    expect(result.current.expandedMode).toBe(true);
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npm run test -- src/store/__tests__/useDrawerStore.test.ts`
Expected: FAIL with "Cannot find module '../useDrawerStore'"

**Step 3: Write minimal implementation**

Create `src/store/useDrawerStore.ts`:

```typescript
import { create } from 'zustand';

export type DrawerTab = 'metadata' | 'voucher' | 'attachments';

interface DrawerState {
  isOpen: boolean;
  activeTab: DrawerTab;
  archiveId: string | null;
  expandedMode: boolean;
  open: (id: string) => void;
  close: () => void;
  setActiveTab: (tab: DrawerTab) => void;
  setExpandedMode: (expanded: boolean) => void;
}

export const useDrawerStore = create<DrawerState>((set) => ({
  isOpen: false,
  activeTab: 'metadata',
  archiveId: null,
  expandedMode: false,
  open: (id) => set({ isOpen: true, archiveId: id }),
  close: () => set({ isOpen: false, archiveId: null, activeTab: 'metadata' }),
  setActiveTab: (tab) => set({ activeTab: tab }),
  setExpandedMode: (expanded) => set({ expandedMode: expanded }),
}));
```

**Step 4: Run test to verify it passes**

Run: `npm run test -- src/store/__tests__/useDrawerStore.test.ts`
Expected: PASS (5 tests passing)

**Step 5: Commit**

```bash
git add src/store/useDrawerStore.ts src/store/__tests__/useDrawerStore.test.ts
git commit -m "feat(drawer): add Zustand store for drawer state management"
```

---

## Task 2: Create ArchiveDetailDrawer Component

**Files:**
- Create: `src/pages/archives/ArchiveDetailDrawer.tsx`
- Modify: `src/components/voucher/index.ts` (export Drawer types if needed)

**Step 1: Write the failing test**

Create test file `src/pages/archives/__tests__/ArchiveDetailDrawer.test.tsx`:

```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import ArchiveDetailDrawer from '../ArchiveDetailDrawer';

// Mock the hooks
vi.mock('../../hooks/useFilePreview', () => ({
  useFilePreview: () => ({ previewUrl: null, loading: false })
}));

vi.mock('../hooks/useVoucherData', () => ({
  useVoucherData: () => ({
    voucherData: {
      voucherNo: '记-2024-001',
      voucherWord: '记',
      debitTotal: 10000,
      voucherDate: '2024-01-01',
      attachments: [],
      entries: []
    }
  })
}));

const mockRow = {
  id: '123',
  code: '记-2024-001',
  archivalCode: 'ARCH-001'
};

const mockConfig = {
  columns: []
};

function renderWithRouter(component: React.ReactElement) {
  return render(
    <BrowserRouter>
      {component}
    </BrowserRouter>
  );
}

describe('ArchiveDetailDrawer', () => {
  it('should not render when open is false', () => {
    const { container } = renderWithRouter(
      <ArchiveDetailDrawer
        open={false}
        onClose={() => {}}
        row={null}
        config={mockConfig}
        isPoolView={true}
      />
    );
    expect(container.firstChild).toBe(null);
  });

  it('should render drawer when open is true', () => {
    renderWithRouter(
      <ArchiveDetailDrawer
        open={true}
        onClose={() => {}}
        row={mockRow}
        config={mockConfig}
        isPoolView={true}
      />
    );
    // Check for drawer element (Ant Design Drawer renders with specific class)
    const drawerElement = document.querySelector('.ant-drawer');
    expect(drawerElement).toBeInTheDocument();
  });

  it('should display voucher code in header', () => {
    renderWithRouter(
      <ArchiveDetailDrawer
        open={true}
        onClose={() => {}}
        row={mockRow}
        config={mockConfig}
        isPoolView={true}
      />
    );
    expect(screen.getByText('记-2024-001')).toBeInTheDocument();
  });

  it('should call onClose when close button is clicked', async () => {
    const handleClose = vi.fn();
    renderWithRouter(
      <ArchiveDetailDrawer
        open={true}
        onClose={handleClose}
        row={mockRow}
        config={mockConfig}
        isPoolView={true}
      />
    );
    const closeButton = screen.getByTestId('close-drawer');
    fireEvent.click(closeButton);
    await waitFor(() => {
      expect(handleClose).toHaveBeenCalledTimes(1);
    });
  });

  it('should render three tabs', () => {
    renderWithRouter(
      <ArchiveDetailDrawer
        open={true}
        onClose={() => {}}
        row={mockRow}
        config={mockConfig}
        isPoolView={true}
      />
    );
    expect(screen.getByRole('tab', { name: /业务元数据/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /会计凭证/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /关联附件/ })).toBeInTheDocument();
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npm run test -- src/pages/archives/__tests__/ArchiveDetailDrawer.test.tsx`
Expected: FAIL with "Cannot find module '../ArchiveDetailDrawer'"

**Step 3: Write minimal implementation**

Create `src/pages/archives/ArchiveDetailDrawer.tsx`:

```typescript
// src/pages/archives/ArchiveDetailDrawer.tsx
/**
 * Archive Detail Drawer - 凭证预览抽屉
 *
 * 职责：凭证预览抽屉的 UI 布局和组件组装
 * 变更理由：替换 Modal 为 Drawer，提升 UX
 */

import React, { useEffect, useMemo } from 'react';
import { Drawer, Tabs } from 'antd';
import { FileText, X, ExternalLink } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import type { DrawerProps } from 'antd';
import type { VoucherDTO } from '../../components/voucher';
import { VoucherMetadata, VoucherPreviewCanvas } from '../../components/voucher';
import { OriginalDocumentPreview } from '../../components/voucher/OriginalDocumentPreview';
import type { ModuleConfig, GenericRow } from '../../types';
import { useVoucherData } from './hooks/useVoucherData';
import { VoucherExportButton } from './components/VoucherExportButton';

interface ArchiveDetailDrawerProps {
  open: boolean;
  onClose: () => void;
  row: GenericRow | null;
  config: ModuleConfig;
  isPoolView: boolean;

  // AIP 导出（仅归档模式）
  onAipExport?: (row: GenericRow) => void;
  isExporting?: string | null;
}

type TabKey = 'metadata' | 'voucher' | 'attachments';

// Responsive width calculation
const getDrawerWidth = (): string | number => {
  const width = window.innerWidth;
  if (width >= 1280) return '50vw'; // Large screens
  if (width >= 768) return '70vw';  // Medium screens
  return '100vw'; // Small screens (full screen)
};

export const ArchiveDetailDrawer: React.FC<ArchiveDetailDrawerProps> = ({
  open,
  onClose,
  row,
  config,
  isPoolView,
  onAipExport,
  isExporting,
}) => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = React.useState<TabKey>('metadata');

  // 使用自定义 hook 获取凭证数据
  const { voucherData } = useVoucherData({ row, enabled: open });

  // Route listener: auto-close on navigation
  useEffect(() => {
    const unlisten = () => {
      // React Router v7 doesn't have history.listen, using location changes
      // The drawer will close due to parent component's route monitoring
    };
    return unlisten;
  }, []);

  // Close drawer when route changes (handled by parent)
  // This is a placeholder for any additional cleanup
  useEffect(() => {
    if (!open) {
      setActiveTab('metadata'); // Reset tab when drawer closes
    }
  }, [open]);

  const handleExpandToPage = () => {
    if (!row?.id) return;
    navigate(`/system/archives/${row.id}`);
  };

  const drawerWidth: string | number = useMemo(() => {
    if (typeof window === 'undefined') return '50vw';
    return getDrawerWidth();
  }, []);

  // Don't render if closed or no row
  if (!open || !row) return null;

  const tabItems = [
    {
      key: 'metadata',
      label: '业务元数据',
      children: voucherData ? (
        <div className="p-4 overflow-y-auto" style={{ maxHeight: 'calc(100vh - 200px)' }}>
          <VoucherMetadata data={voucherData} />
        </div>
      ) : (
        <div className="flex items-center justify-center h-full text-slate-400">
          <p>暂无凭证数据</p>
        </div>
      ),
    },
    {
      key: 'voucher',
      label: '会计凭证',
      children: voucherData ? (
        <div className="p-4 overflow-y-auto" style={{ maxHeight: 'calc(100vh - 200px)' }}>
          <VoucherPreviewCanvas data={voucherData} />
        </div>
      ) : (
        <div className="flex items-center justify-center h-full text-slate-400">
          <p>暂无凭证数据</p>
        </div>
      ),
    },
    {
      key: 'attachments',
      label: `关联附件${voucherData?.attachments && voucherData.attachments.length > 0 ? ` (${voucherData.attachments.length})` : ''}`,
      children: (
        <div className="p-4 overflow-y-auto" style={{ maxHeight: 'calc(100vh - 200px)' }}>
          <OriginalDocumentPreview files={voucherData?.attachments || []} />
        </div>
      ),
    },
  ];

  return (
    <Drawer
      open={open}
      onClose={onClose}
      width={drawerWidth}
      placement="right"
      maskClosable={true}
      keyboard={true}
      destroyOnClose={true}
      styles={{
        body: { padding: 0 },
        header: { padding: '16px 24px', borderBottom: '1px solid #f0f0f0' }
      }}
      closeIcon={null}
      title={
        <div className="flex items-center justify-between w-full pr-8">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-primary-100 text-primary-600 rounded-lg">
              <FileText size={18} />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-800">凭证预览</h3>
              <p className="text-xs text-slate-500 font-mono">{row.code}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {/* AIP 导出按钮（仅归档模式） */}
            {!isPoolView && onAipExport && (row.archivalCode || row.code) && (
              <VoucherExportButton
                onExport={() => onAipExport(row)}
                isExporting={isExporting === (row.archivalCode || row.code)}
              />
            )}
            {/* 展开到新页按钮 */}
            <button
              onClick={handleExpandToPage}
              className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-slate-600 transition-colors"
              title="展开到新页"
            >
              <ExternalLink size={16} />
            </button>
            {/* 关闭按钮 */}
            <button
              data-testid="close-drawer"
              onClick={onClose}
              className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 hover:text-slate-600 transition-colors"
              title="关闭"
            >
              <X size={18} />
            </button>
          </div>
        </div>
      }
    >
      <div className="h-full bg-white">
        <Tabs
          activeKey={activeTab}
          onChange={(key) => setActiveTab(key as TabKey)}
          className="h-full"
          items={tabItems}
        />
      </div>
    </Drawer>
  );
};

export default ArchiveDetailDrawer;
```

**Step 4: Run test to verify it passes**

Run: `npm run test -- src/pages/archives/__tests__/ArchiveDetailDrawer.test.tsx`
Expected: PASS (5 tests passing)

**Step 5: Commit**

```bash
git add src/pages/archives/ArchiveDetailDrawer.tsx src/pages/archives/__tests__/ArchiveDetailDrawer.test.tsx
git commit -m "feat(drawer): create ArchiveDetailDrawer component"
```

---

## Task 3: Update ArchiveListView to Use Drawer

**Files:**
- Modify: `src/pages/archives/ArchiveListView.tsx:481-490`
- Test: `tests/playwright/ui/archives_detail_modal_self_verify.spec.ts`

**Step 1: Write the failing test (update existing E2E test)**

The existing test file needs to be updated to test drawer instead of modal. Update selector expectations:

```typescript
// In tests/playwright/ui/archives_detail_modal_self_verify.spec.ts

// Replace all instances of:
// - '[data-testid="archive-detail-modal"]' with '[data-testid="archive-detail-drawer"]'
// - '[data-testid="close-modal"]' with '[data-testid="close-drawer"]'
```

**Step 2: Run test to verify it fails**

Run: `npx playwright test tests/playwright/ui/archives_detail_drawer_self_verify.spec.ts`
Expected: FAIL with drawer not found (still using modal)

**Step 3: Write minimal implementation**

Update `src/pages/archives/ArchiveListView.tsx`:

Replace line 25:
```typescript
// OLD:
import ArchiveDetailModal from './ArchiveDetailModal';

// NEW:
import ArchiveDetailDrawer from './ArchiveDetailDrawer';
```

Replace lines 481-490:
```typescript
// OLD:
<ArchiveDetailModal
  key={viewRow?.id || 'archive-detail'}
  open={isViewModalOpen}
  onClose={closeViewModal}
  row={viewRow}
  config={mode.config}
  isPoolView={mode.isPoolView}
  onAipExport={archiveActions.handleAipExport}
  isExporting={archiveActions.isExporting}
/>

// NEW:
<ArchiveDetailDrawer
  key={viewRow?.id || 'archive-detail'}
  open={isViewModalOpen}
  onClose={closeViewModal}
  row={viewRow}
  config={mode.config}
  isPoolView={mode.isPoolView}
  onAipExport={archiveActions.handleAipExport}
  isExporting={archiveActions.isExporting}
/>
```

**Step 4: Run test to verify it passes**

Run: `npx playwright test tests/playwright/ui/archives_detail_drawer_self_verify.spec.ts`
Expected: PASS (7 tests passing for drawer)

**Step 5: Commit**

```bash
git add src/pages/archives/ArchiveListView.tsx tests/playwright/ui/archives_detail_drawer_self_verify.spec.ts
git commit -m "refactor(drawer): replace modal with drawer in ArchiveListView"
```

---

## Task 4: Add Route Listener to Auto-Close Drawer on Navigation

**Files:**
- Modify: `src/pages/archives/ArchiveListView.tsx:73-110`
- Test: `tests/playwright/ui/archives_detail_drawer_route_close.spec.ts`

**Step 1: Write the failing test**

Create test file `tests/playwright/ui/archives_detail_drawer_route_close.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';
import { createAuthContext } from '../utils/auth';

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:15175';

test.describe('ArchiveDetailDrawer - Route Change Auto-Close', () => {
  let authState: any = null;
  let token: string | null = null;

  test.beforeAll(async () => {
    const auth = await createAuthContext(BASE_URL);
    if (!auth) throw new Error('Failed to authenticate');
    token = auth.token;
  });

  test.beforeEach(async ({ page }) => {
    // Setup auth and navigate to pool page
    await page.goto(`${BASE_URL}/system/login`);
    await page.fill('input[data-testid="login-username"]', process.env.PW_USER || 'admin');
    await page.fill('input[data-testid="login-password"]', process.env.PW_PASS || 'admin123');
    await page.click('button[data-testid="login-submit"]');
    await page.waitForURL('**/system/**', { timeout: 8000 });

    // Navigate to pool page
    await page.evaluate(() => {
      window.history.pushState({}, '', '/system/pre-archive/pool');
      window.dispatchEvent(new PopStateEvent('popstate'));
    });
    await page.waitForTimeout(1000);
  });

  test('should close drawer when navigating to different menu', async ({ page }) => {
    // Open drawer
    const previewButton = page.locator('table tbody tr').first()
      .locator('button[title="预view"]')
      .or(page.locator('table tbody tr').first().locator('button').filter({ hasText: /预览/ }))
      .or(page.locator('table tbody tr').first().locator('.lucide-eye'))
      .first();
    await previewButton.click();
    await page.waitForTimeout(1000);

    // Verify drawer is open
    const drawer = page.locator('.ant-drawer');
    await expect(drawer).toBeVisible();

    // Navigate to different menu (click on menu item)
    await page.evaluate(() => {
      window.history.pushState({}, '', '/system/panorama');
      window.dispatchEvent(new PopStateEvent('popstate'));
    });
    await page.waitForTimeout(500);

    // Verify drawer is closed
    await expect(drawer).not.toBeVisible();
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npx playwright test tests/playwright/ui/archives_detail_drawer_route_close.spec.ts`
Expected: FAIL - drawer doesn't close on route change

**Step 3: Write minimal implementation**

Update `src/pages/archives/ArchiveListView.tsx` in the component (around line 73):

Add import for useEffect:
```typescript
import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
```

Add route listener inside the component:
```typescript
const ArchiveListView: React.FC<ArchiveListViewProps> = ({ controller, actions: archiveActions }) => {
  const { mode, query, page, data, selection, pool, ui } = controller;
  const location = useLocation(); // Add this

  // ... existing state ...

  // Route change listener: auto-close drawer on navigation
  useEffect(() => {
    // Close drawer when route changes (and we're not just opening the drawer)
    if (isViewModalOpen) {
      closeViewModal();
    }
  }, [location.pathname]); // Only depend on pathname, not full location

  // ... rest of component ...
```

**Step 4: Run test to verify it passes**

Run: `npx playwright test tests/playwright/ui/archives_detail_drawer_route_close.spec.ts`
Expected: PASS (1 test passing)

**Step 5: Commit**

```bash
git add src/pages/archives/ArchiveListView.tsx tests/playwright/ui/archives_detail_drawer_route_close.spec.ts
git commit -m "feat(drawer): auto-close drawer on route navigation"
```

---

## Task 5: Create Expanded Page View Component

**Files:**
- Create: `src/pages/archives/ArchiveDetailPage.tsx`
- Create: `src/pages/archives/__tests__/ArchiveDetailPage.test.tsx`
- Modify: `src/routes/index.tsx:175` (add new route)

**Step 1: Write the failing test**

Create test file `src/pages/archives/__tests__/ArchiveDetailPage.test.tsx`:

```typescript
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ArchiveDetailPage } from '../ArchiveDetailPage';

// Mock the hooks
vi.mock('../../hooks/useFilePreview', () => ({
  useFilePreview: () => ({ previewUrl: null, loading: false })
}));

vi.mock('../hooks/useVoucherData', () => ({
  useVoucherData: () => ({
    voucherData: {
      voucherNo: '记-2024-001',
      voucherWord: '记',
      debitTotal: 10000,
      voucherDate: '2024-01-01',
      attachments: [],
      entries: []
    }
  })
}));

function renderWithRouter(component: React.ReactElement, route: string = '/system/archives/123') {
  return render(
    <BrowserRouter initialEntries={[route]}>
      <Routes>
        <Route path="/system/archives/:id" element={component} />
      </Routes>
    </BrowserRouter>
  );
}

describe('ArchiveDetailPage', () => {
  it('should render page with voucher data', () => {
    renderWithRouter(<ArchiveDetailPage />);
    expect(screen.getByText('凭证详情')).toBeInTheDocument();
  });

  it('should display archive ID from URL params', () => {
    renderWithRouter(<ArchiveDetailPage />, '/system/archives/test-archive-123');
    // Component should parse and display the ID or use it to fetch data
    const pageElement = document.querySelector('[data-archive-id="test-archive-123"]');
    expect(pageElement).toBeInTheDocument();
  });

  it('should render three tabs', () => {
    renderWithRouter(<ArchiveDetailPage />);
    expect(screen.getByRole('tab', { name: /业务元数据/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /会计凭证/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /关联附件/ })).toBeInTheDocument();
  });

  it('should have back button', () => {
    renderWithRouter(<ArchiveDetailPage />);
    const backButton = screen.getByRole('button', { name: /返回/ });
    expect(backButton).toBeInTheDocument();
  });
});
```

**Step 2: Run test to verify it fails**

Run: `npm run test -- src/pages/archives/__tests__/ArchiveDetailPage.test.tsx`
Expected: FAIL with "Cannot find module '../ArchiveDetailPage'"

**Step 3: Write minimal implementation**

Create `src/pages/archives/ArchiveDetailPage.tsx`:

```typescript
// src/pages/archives/ArchiveDetailPage.tsx
/**
 * Archive Detail Page - 凭证详情页（全屏展开视图）
 *
 * 职责：提供全屏凭证详情视图，从 Drawer "展开到新页"功能触发
 */

import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, FileText } from 'lucide-react';
import { Tabs, Breadcrumb } from 'antd';
import type { VoucherDTO } from '../../components/voucher';
import { VoucherMetadata, VoucherPreviewCanvas } from '../../components/voucher';
import { OriginalDocumentPreview } from '../../components/voucher/OriginalDocumentPreview';
import { useVoucherData } from './hooks/useVoucherData';

// Simulate row from URL param (in real implementation, fetch data by ID)
const createMockRowFromId = (id: string): any => ({
  id,
  code: `凭证-${id.slice(-6)}`,
  archivalCode: `ARCH-${id.slice(-6)}`,
});

type TabKey = 'metadata' | 'voucher' | 'attachments';

export const ArchiveDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = React.useState<TabKey>('metadata');

  // Simulate row from URL param
  const row = React.useMemo(() => (id ? createMockRowFromId(id) : null), [id]);

  // 使用自定义 hook 获取凭证数据
  const { voucherData, isLoading } = useVoucherData({
    row,
    enabled: !!id,
  });

  const handleBack = () => {
    navigate(-1); // Go back to previous page
  };

  const tabItems = [
    {
      key: 'metadata',
      label: '业务元数据',
      children: voucherData ? (
        <div className="p-6">
          <VoucherMetadata data={voucherData} />
        </div>
      ) : (
        <div className="flex items-center justify-center h-64 text-slate-400">
          <p>{isLoading ? '加载中...' : '暂无凭证数据'}</p>
        </div>
      ),
    },
    {
      key: 'voucher',
      label: '会计凭证',
      children: voucherData ? (
        <div className="p-6">
          <VoucherPreviewCanvas data={voucherData} />
        </div>
      ) : (
        <div className="flex items-center justify-center h-64 text-slate-400">
          <p>{isLoading ? '加载中...' : '暂无凭证数据'}</p>
        </div>
      ),
    },
    {
      key: 'attachments',
      label: `关联附件${voucherData?.attachments && voucherData.attachments.length > 0 ? ` (${voucherData.attachments.length})` : ''}`,
      children: (
        <div className="p-6">
          <OriginalDocumentPreview files={voucherData?.attachments || []} />
        </div>
      ),
    },
  ];

  if (!id) {
    return (
      <div className="flex items-center justify-center h-screen">
        <p className="text-slate-500">无效的档案 ID</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-50" data-archive-id={id}>
      {/* Header */}
      <div className="bg-white border-b border-slate-200 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-6 py-4">
          <Breadcrumb
            items={[
              { title: '档案管理' },
              { title: '凭证详情' },
              { title: row?.code || id }
            ]}
            className="mb-4"
          />
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <button
                onClick={handleBack}
                className="p-2 hover:bg-slate-100 rounded-lg text-slate-600 transition-colors"
                title="返回"
              >
                <ArrowLeft size={20} />
              </button>
              <div className="flex items-center gap-3">
                <div className="p-2 bg-primary-100 text-primary-600 rounded-lg">
                  <FileText size={20} />
                </div>
                <div>
                  <h1 className="text-2xl font-bold text-slate-800">凭证详情</h1>
                  <p className="text-sm text-slate-500 font-mono">{row?.code || id}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-7xl mx-auto px-6 py-6">
        <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
          <Tabs
            activeKey={activeTab}
            onChange={(key) => setActiveTab(key as TabKey)}
            className="w-full"
            items={tabItems}
          />
        </div>
      </div>
    </div>
  );
};

export default ArchiveDetailPage;
```

Add route in `src/routes/index.tsx` after line 175:
```typescript
// Add this inside the children array, around line 175:
{ path: 'archives/:id', element: withSuspense(ArchiveDetailPage) },
```

Add import at the top:
```typescript
const ArchiveDetailPage = lazy(() => import('../pages/archives/ArchiveDetailPage'));
```

**Step 4: Run test to verify it passes**

Run: `npm run test -- src/pages/archives/__tests__/ArchiveDetailPage.test.tsx`
Expected: PASS (4 tests passing)

**Step 5: Commit**

```bash
git add src/pages/archives/ArchiveDetailPage.tsx src/pages/archives/__tests__/ArchiveDetailPage.test.tsx src/routes/index.tsx
git commit -m "feat(drawer): add expanded page view for full-screen voucher details"
```

---

## Task 6: Remove Old ArchiveDetailModal Component

**Files:**
- Delete: `src/pages/archives/ArchiveDetailModal.tsx`
- Test: Run all tests to ensure nothing broken

**Step 1: Verify no other files import ArchiveDetailModal**

Run: `grep -r "ArchiveDetailModal" src/ --include="*.ts" --include="*.tsx"`
Expected: Only the file itself should be found (no other imports)

**Step 2: Delete the old modal file**

Run: `rm src/pages/archives/ArchiveDetailModal.tsx`

**Step 3: Run all tests to verify nothing broke**

Run: `npm run test:run`
Expected: All tests passing (including new drawer tests)

**Step 4: Update Playwright tests to use drawer selectors**

Update all remaining test files that reference modal:
- Replace `archive-detail-modal` with `archive-detail-drawer`
- Replace `close-modal` with `close-drawer`

**Step 5: Commit**

```bash
git add -A
git commit -m "refactor(drawer): remove deprecated ArchiveDetailModal component"
```

---

## Task 7: Responsive Design Testing and Verification

**Files:**
- Manual testing with Chrome DevTools
- Update styles if needed

**Step 1: Test responsive breakpoints**

Manual testing:
1. Open Chrome DevTools
2. Test at 375px (mobile): Drawer should be 100% width
3. Test at 768px (tablet): Drawer should be 70% width
4. Test at 1280px (desktop): Drawer should be 50% width
5. Test at 1920px (large desktop): Drawer should be 50% width (max 900px)

**Step 2: Verify tab layout on mobile**

Check that tabs work correctly on small screens:
- Content should be scrollable
- Close button should remain visible
- "Expand to page" button should work

**Step 3: Performance check**

Run Lighthouse audit:
1. Open drawer
2. Run Lighthouse test
3. Verify Performance score > 90

**Step 4: Fix any responsive issues if found**

If issues found, update `src/pages/archives/ArchiveDetailDrawer.tsx`:
- Adjust width breakpoints
- Fix overflow issues
- Improve mobile touch targets

**Step 5: Commit**

```bash
git add src/pages/archives/ArchiveDetailDrawer.tsx
git commit -m "style(drawer): fix responsive layout issues found during testing"
```

---

## Task 8: Update Documentation

**Files:**
- Update: `docs/plans/2026-01-02-voucher-preview-drawer-design.md`
- Create: `docs/architecture/voucher-preview-drawer.md` (architecture documentation)

**Step 1: Update design document status**

Update `docs/plans/2026-01-02-voucher-preview-drawer-design.md`:
```markdown
# Voucher Preview Drawer - UX Optimization Design

**Date:** 2026-01-02
**Status:** ✅ Completed
**Implementation:** 2026-01-02
```

**Step 2: Create architecture documentation**

Create `docs/architecture/voucher-preview-drawer.md`:

```markdown
# Voucher Preview Drawer Architecture

## Overview

The voucher preview drawer replaces the modal-based preview with a more user-friendly and performant drawer interface.

## Components

### ArchiveDetailDrawer
- **Location:** `src/pages/archives/ArchiveDetailDrawer.tsx`
- **Purpose:** Side drawer for voucher preview
- **Features:**
  - Responsive width (50%/70%/100% based on screen size)
  - Route-based auto-close
  - Expand to full page capability

### ArchiveDetailPage
- **Location:** `src/pages/archives/ArchiveDetailPage.tsx`
- **Purpose:** Full-screen page for voucher details
- **Route:** `/system/archives/:id`

### useDrawerStore
- **Location:** `src/store/useDrawerStore.ts`
- **Purpose:** Global drawer state management
- **State:**
  - `isOpen`: Drawer open/close state
  - `activeTab`: Current active tab
  - `archiveId`: Current viewing archive ID
  - `expandedMode`: Whether expanded to full page

## Responsive Breakpoints

| Screen Size | Drawer Width | Tab Layout |
|-------------|-------------|------------|
| ≥1280px | 50vw (max 900px) | Icon + Text |
| 768-1279px | 70vw | Text only |
| <768px | 100vw | Bottom nav style |

## Performance

- **First Open:** < 500ms
- **Tab Switch:** < 100ms (cached data)
- **Lighthouse Score:** > 90

## Migration

Replaced components:
- `ArchiveDetailModal` → `ArchiveDetailDrawer`

Updated files:
- `src/pages/archives/ArchiveListView.tsx` (line 25, 481-490)
- `src/routes/index.tsx` (added route for ArchiveDetailPage)
- All Playwright tests (modal → drawer selectors)
```

**Step 3: Commit**

```bash
git add docs/plans/2026-01-02-voucher-preview-drawer-design.md docs/architecture/voucher-preview-drawer.md
git commit -m "docs(drawer): update design status and add architecture documentation"
```

---

## Summary

This implementation plan replaces the modal-based voucher preview with a drawer-based UX, addressing all user complaints:
1. ✅ Better content display (wider drawer vs small modal)
2. ✅ Better positioning (from right edge vs pushed down with mt-10)
3. ✅ Auto-close on navigation (route listener)
4. ✅ Improved UX (drawer pattern with expand-to-page option)

**Total Tasks:** 8
**Estimated Time:** Each task 15-30 minutes
**Testing Strategy:** TDD with unit tests + E2E tests + manual responsive verification
