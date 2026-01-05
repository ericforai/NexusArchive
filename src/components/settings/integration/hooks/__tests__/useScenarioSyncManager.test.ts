import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { useScenarioSyncManager } from '../useScenarioSyncManager';

const mockErpApi = {
  getScenarios: vi.fn(),
  getSubInterfaces: vi.fn(),
  getSyncHistory: vi.fn(),
  syncScenario: vi.fn(),
  syncAllScenarios: vi.fn(),
};

const mockScenarios = [
  { id: 1, name: 'Sales Outbound', status: 'ACTIVE' },
  { id: 2, name: 'Purchase Inbound', status: 'ACTIVE' },
];

describe('useScenarioSyncManager', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should load scenarios for config', async () => {
    mockErpApi.getScenarios.mockResolvedValue({ code: 200, data: mockScenarios });

    const { result } = renderHook(() => useScenarioSyncManager({ erpApi: mockErpApi }));

    await act(async () => {
      await result.current.actions.loadScenarios(1);
    });

    expect(result.current.state.scenarios).toEqual(mockScenarios);
  });

  it('should load sub-interfaces', async () => {
    const mockSubInterfaces = [
      { id: 1, name: 'Sales Order', path: '/sales/order' },
    ];
    mockErpApi.getSubInterfaces.mockResolvedValue({ code: 200, data: mockSubInterfaces });

    const { result } = renderHook(() => useScenarioSyncManager({ erpApi: mockErpApi }));

    await act(async () => {
      await result.current.actions.loadSubInterfaces(1);
    });

    expect(result.current.state.subInterfaces[1]).toEqual(mockSubInterfaces);
  });

  it('should sync scenario', async () => {
    mockErpApi.syncScenario.mockResolvedValue({ code: 200 });
    mockErpApi.getSyncHistory.mockResolvedValue({ code: 200, data: [] });

    const { result } = renderHook(() => useScenarioSyncManager({ erpApi: mockErpApi }));

    await act(async () => {
      await result.current.actions.syncScenario(1);
    });

    expect(mockErpApi.syncScenario).toHaveBeenCalledWith(1, undefined);
    expect(result.current.state.syncing).toBeNull();
  });
});
