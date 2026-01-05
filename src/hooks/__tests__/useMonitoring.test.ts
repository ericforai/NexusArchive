// Input: React hooks testing library
// Output: useMonitoring hook tests
// Pos: Unit tests for useMonitoring hook

import { renderHook, act, waitFor } from '@testing-library/react';
import { useMonitoring } from '../useMonitoring';
import { erpApi } from '../../api/erp';

// Mock the ERP API
vi.mock('../../api/erp', () => ({
  erpApi: {
    getIntegrationMonitoring: vi.fn(),
  },
}));

describe('useMonitoring', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should have correct initial state', () => {
    const { result } = renderHook(() => useMonitoring());

    expect(result.current.state).toEqual({
      data: null,
      loading: false,
    });
  });

  it('should load monitoring data successfully', async () => {
    const mockMonitoringData = {
      totalSyncCount: 100,
      successRate: 95.5,
      evidenceCoverage: 88.2,
    };

    vi.mocked(erpApi.getIntegrationMonitoring).mockResolvedValue({
      success: true,
      data: mockMonitoringData,
      message: 'Success',
    });

    const { result } = renderHook(() => useMonitoring());

    await act(async () => {
      await result.current.actions.loadMonitoring();
    });

    expect(erpApi.getIntegrationMonitoring).toHaveBeenCalledTimes(1);
    expect(result.current.state.data).toEqual(mockMonitoringData);
    expect(result.current.state.loading).toBe(false);
  });

  it('should set loading to true during load', async () => {
    vi.mocked(erpApi.getIntegrationMonitoring).mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve({
        success: true,
        data: { totalSyncCount: 100, successRate: 95.5, evidenceCoverage: 88.2 },
        message: 'Success',
      }), 100))
    );

    const { result } = renderHook(() => useMonitoring());

    act(() => {
      result.current.actions.loadMonitoring();
    });

    // Loading should be true immediately after calling loadMonitoring
    expect(result.current.state.loading).toBe(true);

    await waitFor(() => {
      expect(result.current.state.loading).toBe(false);
    });
  });

  it('should handle load errors gracefully', async () => {
    const mockError = new Error('Network error');
    vi.mocked(erpApi.getIntegrationMonitoring).mockRejectedValue(mockError);

    const { result } = renderHook(() => useMonitoring());

    await act(async () => {
      await result.current.actions.loadMonitoring();
    });

    expect(result.current.state.data).toBeNull();
    expect(result.current.state.loading).toBe(false);
  });

  it('should handle API failure response', async () => {
    vi.mocked(erpApi.getIntegrationMonitoring).mockResolvedValue({
      success: false,
      message: 'Failed to load monitoring data',
      data: null as any,
    });

    const { result } = renderHook(() => useMonitoring());

    await act(async () => {
      await result.current.actions.loadMonitoring();
    });

    expect(result.current.state.data).toBeNull();
    expect(result.current.state.loading).toBe(false);
  });
});
