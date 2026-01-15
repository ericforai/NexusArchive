// src/pages/archives/__tests__/ArchiveDetailPage.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ArchiveDetailPage } from '../ArchiveDetailPage';

// Mock the hooks
vi.mock('../../../hooks/useFilePreview', () => ({
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
    },
    isLoading: false
  })
}));

// Mock useParams to return an ID
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<any>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ id: 'test-archive-123' }),
    useNavigate: () => vi.fn()
  };
});

function renderWithRouter(component: React.ReactElement) {
  return render(
    <BrowserRouter>
      {component}
    </BrowserRouter>
  );
}

describe('ArchiveDetailPage', () => {
  it('should render page with voucher data', () => {
    renderWithRouter(<ArchiveDetailPage />);
    // "凭证详情" appears in both breadcrumb and h1
    expect(screen.getAllByText('凭证详情')).toHaveLength(2);
  });

  it('should display archive ID from URL params', () => {
    renderWithRouter(<ArchiveDetailPage />);
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

  it('should display breadcrumb with archive info', () => {
    renderWithRouter(<ArchiveDetailPage />);
    expect(screen.getByText('档案管理')).toBeInTheDocument();
    // "凭证详情" appears in both breadcrumb and h1, so use getAllByText
    expect(screen.getAllByText('凭证详情')).toHaveLength(2);
  });
});
