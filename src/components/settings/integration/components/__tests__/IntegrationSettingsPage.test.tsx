import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { IntegrationSettingsPage } from '../../IntegrationSettingsPage';
import { erpApi } from '@/api/erp';

vi.mock('@/api/erp', () => ({
  erpApi: {
    getConfigs: vi.fn(),
    testConnection: vi.fn(),
    diagnoseConfig: vi.fn(),
    triggerReconciliation: vi.fn(),
  },
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
    erpApi.getConfigs.mockResolvedValue({ code: 200, data: [] });
    render(<IntegrationSettingsPage erpApi={erpApi} />);
    expect(screen.getByText('集成设置')).toBeInTheDocument();
    expect(screen.getByText('+ 添加连接器')).toBeInTheDocument();
  });

  it('should render connector grid when configs exist', async () => {
    erpApi.getConfigs.mockResolvedValue({
      code: 200,
      data: [
        { id: 1, name: 'YonSuite', erpType: 'yonsuite', configJson: '{}', createdAt: '2026-01-01T00:00:00Z' }
      ]
    });
    render(<IntegrationSettingsPage erpApi={erpApi} />);
    await waitFor(() => {
      expect(screen.getByText('YonSuite')).toBeInTheDocument();
    });
  });
});
