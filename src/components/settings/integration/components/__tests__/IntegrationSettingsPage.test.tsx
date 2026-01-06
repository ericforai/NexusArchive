import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { IntegrationSettingsPage } from '../../IntegrationSettingsPage';
import { erpApi } from '@/api/erp';

const mockErpApi = {
  getConfigs: vi.fn(),
  testConnection: vi.fn(),
  diagnoseConfig: vi.fn(),
  triggerReconciliation: vi.fn(),
};

vi.mock('@/api/erp', () => ({
  erpApi: mockErpApi,
}));

vi.mock('react-hot-toast', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('IntegrationSettingsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render page header and add button', () => {
    mockErpApi.getConfigs.mockResolvedValue({ code: 200, data: [] });
    render(<IntegrationSettingsPage erpApi={mockErpApi as any} />);
    expect(screen.getByText('集成设置')).toBeInTheDocument();
    expect(screen.getByText('+ 添加连接器')).toBeInTheDocument();
  });

  it('should render connector grid when configs exist', async () => {
    mockErpApi.getConfigs.mockResolvedValue({
      code: 200,
      data: [
        { id: 1, name: 'YonSuite', erpType: 'yonsuite', configJson: '{}', isActive: 1 }
      ]
    });
    render(<IntegrationSettingsPage erpApi={mockErpApi as any} />);
    await waitFor(() => {
      expect(screen.getByText('YonSuite')).toBeInTheDocument();
    });
  });

  it('should open ConnectorForm modal when clicking config center button', async () => {
    mockErpApi.getConfigs.mockResolvedValue({
      code: 200,
      data: [
        { id: 1, name: 'YonSuite', erpType: 'yonsuite', configJson: '{}', isActive: 1 }
      ]
    });
    render(<IntegrationSettingsPage erpApi={mockErpApi as any} />);

    // Wait for configs to load
    await waitFor(() => {
      expect(screen.getByText('YonSuite')).toBeInTheDocument();
    });

    // Click the "配置中心" button
    const configButtons = screen.getAllByText('配置中心');
    fireEvent.click(configButtons[0]);

    // Verify modal opens with edit title
    await waitFor(() => {
      expect(screen.getByText('编辑连接器配置')).toBeInTheDocument();
    });
  });
});
