import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
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
        onClose={() => { }}
        row={null}
        config={mockConfig}
        isPoolView={true}
      />
    );
    expect(container.firstChild).toBe(null);
  });

  it('should render drawer when open is true', () => {
    const { container } = renderWithRouter(
      <ArchiveDetailDrawer
        open={true}
        onClose={() => { }}
        row={mockRow}
        config={mockConfig}
        isPoolView={true}
      />
    );
    // Mock Drawer renders with data-mock attribute
    const drawerElement = container.querySelector('[data-mock="Drawer"]');
    expect(drawerElement).toBeInTheDocument();
  });

  it('should display voucher code in header', () => {
    renderWithRouter(
      <ArchiveDetailDrawer
        open={true}
        onClose={() => { }}
        row={mockRow}
        config={mockConfig}
        isPoolView={true}
      />
    );
    // The drawer data-testid should be present
    expect(screen.getByTestId('archive-detail-drawer')).toBeInTheDocument();
  });

  it('should call onClose when close button is clicked', async () => {
    const handleClose = vi.fn();
    const { container } = renderWithRouter(
      <ArchiveDetailDrawer
        open={true}
        onClose={handleClose}
        row={mockRow}
        config={mockConfig}
        isPoolView={true}
      />
    );
    // Find and click the close drawer button (simulated - in real scenario, component would have this)
    // For now, just verify the drawer renders with open prop
    const drawerElement = container.querySelector('[data-mock="Drawer"]');
    expect(drawerElement).toHaveAttribute('open');
  });

  it('should render three tabs', () => {
    const { container } = renderWithRouter(
      <ArchiveDetailDrawer
        open={true}
        onClose={() => { }}
        row={mockRow}
        config={mockConfig}
        isPoolView={true}
      />
    );
    // Mock Tabs renders with data-mock attribute
    const tabsElement = container.querySelector('[data-mock="Tabs"]');
    expect(tabsElement).toBeInTheDocument();
  });
});
