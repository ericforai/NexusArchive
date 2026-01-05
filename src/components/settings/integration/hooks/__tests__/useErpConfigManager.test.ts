// src/components/settings/integration/hooks/__tests__/useErpConfigManager.test.ts

import { renderHook, act, waitFor } from '@testing-library/react';
import { useErpConfigManager } from '../useErpConfigManager';
import { ErpConfig } from '../../../../../types';

// Mock API
const mockErpApi = {
  getConfigs: vi.fn(),
  createConfig: vi.fn(),
  updateConfig: vi.fn(),
  deleteConfig: vi.fn(),
  testConnection: vi.fn(),
};

const mockConfigs: ErpConfig[] = [
  { id: 1, name: 'YonSuite Dev', erpType: 'yonsuite', configJson: '{"baseUrl":"https://api.test.com"}', isActive: 1 },
  { id: 2, name: 'Kingdee Prod', erpType: 'kingdee', configJson: '{"baseUrl":"https://api.prod.com"}', isActive: 1 },
];

describe('useErpConfigManager', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('initial state', () => {
    it('should have empty initial state', () => {
      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      expect(result.current.state.configs).toEqual([]);
      expect(result.current.state.adapterTypes).toEqual([]);
      expect(result.current.state.activeConfigId).toBeNull();
      expect(result.current.state.loading).toBe(false);
    });
  });

  describe('loadConfigs', () => {
    it('should load configs successfully', async () => {
      mockErpApi.getConfigs.mockResolvedValue({ code: 200, data: mockConfigs });

      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      await act(async () => {
        await result.current.actions.loadConfigs();
      });

      await waitFor(() => {
        expect(result.current.state.configs).toEqual(mockConfigs);
      });

      expect(result.current.state.adapterTypes).toEqual(['yonsuite', 'kingdee']);
      expect(result.current.state.activeConfigId).toBe(1);
      expect(result.current.state.loading).toBe(false);
    });

    it('should handle loading state', async () => {
      mockErpApi.getConfigs.mockImplementation(() => new Promise(resolve => {
        setTimeout(() => resolve({ code: 200, data: mockConfigs }), 100);
      }));

      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      act(() => {
        result.current.actions.loadConfigs();
      });

      await waitFor(() => {
        expect(result.current.state.loading).toBe(true);
      });

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });
    });

    it('should handle API error', async () => {
      mockErpApi.getConfigs.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      await act(async () => {
        await result.current.actions.loadConfigs();
      });

      await waitFor(() => {
        expect(result.current.state.loading).toBe(false);
      });

      expect(result.current.state.configs).toEqual([]);
    });
  });

  describe('setActiveConfig', () => {
    it('should set active config', () => {
      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      act(() => {
        result.current.actions.setActiveConfig(5);
      });

      expect(result.current.state.activeConfigId).toBe(5);
    });

    it('should clear active config', () => {
      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      act(() => {
        result.current.actions.setActiveConfig(5);
        result.current.actions.setActiveConfig(null);
      });

      expect(result.current.state.activeConfigId).toBeNull();
    });
  });

  describe('toggleTypeExpansion', () => {
    it('should expand collapsed type', () => {
      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      act(() => {
        result.current.actions.toggleTypeExpansion('yonsuite');
      });

      expect(result.current.state.expandedTypes.has('yonsuite')).toBe(true);
    });

    it('should collapse expanded type', () => {
      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      act(() => {
        result.current.actions.toggleTypeExpansion('yonsuite');
        result.current.actions.toggleTypeExpansion('yonsuite');
      });

      expect(result.current.state.expandedTypes.has('yonsuite')).toBe(false);
    });
  });

  describe('createConfig', () => {
    it('should create new config', async () => {
      const newConfig = { name: 'New Config', erpType: 'yonsuite' as const, baseUrl: 'https://test.com' };
      mockErpApi.createConfig.mockResolvedValue({ code: 200, data: { id: 3, ...newConfig } });
      mockErpApi.getConfigs.mockResolvedValue({ code: 200, data: [...mockConfigs, { id: 3, ...newConfig }] });

      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      await act(async () => {
        await result.current.actions.createConfig(newConfig);
      });

      expect(mockErpApi.createConfig).toHaveBeenCalledWith(newConfig);
      expect(mockErpApi.getConfigs).toHaveBeenCalled();
    });
  });

  describe('deleteConfig', () => {
    it('should delete config and refresh list', async () => {
      mockErpApi.deleteConfig.mockResolvedValue({ code: 200 });
      mockErpApi.getConfigs.mockResolvedValue({
        code: 200,
        data: [{ id: 1, name: 'YonSuite', erpType: 'yonsuite', configJson: '{}', createdAt: '2026-01-01T00:00:00Z' }]
      });

      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      // Load configs first
      await act(async () => {
        await result.current.actions.loadConfigs();
      });

      expect(result.current.state.configs).toHaveLength(1);

      // Delete config
      await act(async () => {
        await result.current.actions.deleteConfig(1);
      });

      expect(mockErpApi.deleteConfig).toHaveBeenCalledWith(1);
      expect(mockErpApi.getConfigs).toHaveBeenCalled(); // Should refresh
    });

    it('should handle delete error', async () => {
      mockErpApi.getConfigs.mockResolvedValue({
        code: 200,
        data: [{ id: 1, name: 'YonSuite', erpType: 'yonsuite', configJson: '{}', createdAt: '2026-01-01T00:00:00Z' }]
      });
      mockErpApi.deleteConfig.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      await act(async () => {
        await result.current.actions.loadConfigs();
      });

      // Should not throw, just show error toast
      await act(async () => {
        await result.current.actions.deleteConfig(1);
      });

      expect(mockErpApi.deleteConfig).toHaveBeenCalledWith(1);
    });

    it('should handle delete API error response', async () => {
      mockErpApi.getConfigs.mockResolvedValue({
        code: 200,
        data: [{ id: 1, name: 'YonSuite', erpType: 'yonsuite', configJson: '{}', createdAt: '2026-01-01T00:00:00Z' }]
      });
      mockErpApi.deleteConfig.mockResolvedValue({ code: 400, message: 'Config not found' });

      const { result } = renderHook(() => useErpConfigManager({ erpApi: mockErpApi }));

      await act(async () => {
        await result.current.actions.loadConfigs();
      });

      await act(async () => {
        await result.current.actions.deleteConfig(1);
      });

      expect(mockErpApi.deleteConfig).toHaveBeenCalledWith(1);
    });
  });
});
