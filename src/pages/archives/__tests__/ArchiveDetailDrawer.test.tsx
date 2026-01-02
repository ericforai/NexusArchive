import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import ArchiveDetailDrawer from '../ArchiveDetailDrawer';

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
    }
  })
}));

const mockRow = {
  id: '123',
  code: '记-2024-001',
  archivalCode: 'ARCH-001'
};

const mockConfig = {
  columns: [],
  data: []
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
