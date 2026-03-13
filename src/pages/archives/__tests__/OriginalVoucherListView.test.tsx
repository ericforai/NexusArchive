// Input: vitest、@testing-library/react、react-router-dom、React Query、OriginalVoucherListView
// Output: 原始凭证列表视图测试
// Pos: src/pages/archives/__tests__/OriginalVoucherListView.test.tsx

import { describe, it, expect, beforeEach, vi, Mock } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import OriginalVoucherListView from '@/pages/archives/OriginalVoucherListView';
import { getOriginalVouchers, getOriginalVoucherTypes } from '@/api/originalVoucher';

vi.mock('@/components/pages', () => ({
  VoucherPreviewDrawer: () => null,
}));

vi.mock('@/api/originalVoucher', async () => {
  const actual = await vi.importActual<typeof import('@/api/originalVoucher')>('@/api/originalVoucher');
  return {
    ...actual,
    getOriginalVouchers: vi.fn(),
    getOriginalVoucherTypes: vi.fn(),
    deleteOriginalVoucher: vi.fn(),
    submitForArchive: vi.fn(),
  };
});

vi.mock('@/store/useFondsStore', () => ({
  useFondsStore: vi.fn(() => ({
    currentFonds: { fondsCode: '001', fondsName: 'Test Fonds' },
    _hasHydrated: true,
  })),
}));

describe('OriginalVoucherListView', () => {
  let queryClient: QueryClient;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
      },
    });

    (getOriginalVouchers as Mock).mockResolvedValue({
      records: [],
      pages: 1,
    });
    (getOriginalVoucherTypes as Mock).mockResolvedValue([]);
  });

  const renderView = (initialEntry: string) =>
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={[initialEntry]}>
          <OriginalVoucherListView />
        </MemoryRouter>
      </QueryClientProvider>
    );

  it('应根据原始凭证类型显示菜单标题与副标题', () => {
    renderView('/system/archive/original-vouchers?type=SALES_ORDER');

    expect(screen.getByRole('heading', { name: '销售订单' })).toBeInTheDocument();
    expect(screen.getByText('销售订单管理')).toBeInTheDocument();
  });

  it('应按 URL type 过滤原始凭证列表', async () => {
    renderView('/system/archive/original-vouchers?type=SALES_ORDER');

    await waitFor(() => {
      expect(getOriginalVouchers).toHaveBeenCalled();
    });

    expect(getOriginalVouchers).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'SALES_ORDER',
      })
    );
  });
});
