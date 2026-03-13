// src/pages/archives/__tests__/ArchiveDetailPage.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { ArchiveDetailPage } from '../ArchiveDetailPage';

vi.mock('antd', async () => {
  const React = await import('react');

  const Breadcrumb = ({ items, ...props }: any) => React.createElement(
    'nav',
    { 'data-mock': 'Breadcrumb', ...props },
    ...(items || []).map((item: any) => React.createElement('span', { key: item.title, className: 'breadcrumb-item' }, item.title)),
  );

  const Tabs = ({ items, activeKey, onChange, ...props }: any) => {
    const activeItem = (items || []).find((item: any) => item.key === activeKey) || items?.[0];
    return React.createElement(
      'div',
      { 'data-mock': 'Tabs', ...props },
      React.createElement(
        'div',
        { className: 'tabs-nav' },
        ...(items || []).map((item: any) => React.createElement('button', {
          key: item.key,
          role: 'tab',
          'aria-selected': activeKey === item.key,
          onClick: () => onChange?.(item.key),
        }, item.label)),
      ),
      React.createElement('div', { className: 'tabs-content' }, activeItem?.children),
    );
  };

  return { Breadcrumb, Tabs };
});

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
      attachments: [
        { id: 'att-1', fileId: 'att-1', fileName: 'invoice.ofd', type: 'application/ofd', previewResourceType: 'file' },
      ],
      entries: []
    },
    isLoading: false
  })
}));

vi.mock('../../../components/voucher', async () => {
  const actual = await vi.importActual<typeof import('../../../components/voucher')>('../../../components/voucher');
  return {
    ...actual,
    OriginalDocumentPreview: ({ files }: { files?: Array<{ previewResourceType?: string; fileId?: string }> }) => (
      <div
        data-testid="original-document-preview"
        data-file-id={files?.[0]?.fileId}
        data-resource-type={files?.[0]?.previewResourceType}
      />
    ),
  };
});

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

  it('should pass file preview resource metadata into attachment preview entry', () => {
    renderWithRouter(<ArchiveDetailPage />);
    fireEvent.click(screen.getByRole('tab', { name: /关联附件/ }));
    expect(screen.getByTestId('original-document-preview')).toHaveAttribute('data-file-id', 'att-1');
    expect(screen.getByTestId('original-document-preview')).toHaveAttribute('data-resource-type', 'file');
  });
});
