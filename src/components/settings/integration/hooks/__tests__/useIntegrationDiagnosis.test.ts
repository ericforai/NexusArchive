// src/components/settings/integration/hooks/__tests__/useIntegrationDiagnosis.test.ts

import { renderHook, act, waitFor } from '@testing-library/react';
import { useIntegrationDiagnosis } from '../useIntegrationDiagnosis';
import { IntegrationDiagnosisResult } from '../../../../../types';

// Mock API
const mockErpApi = {
  runDiagnosis: vi.fn(),
};

const mockDiagnosisResult: IntegrationDiagnosisResult = {
  status: 'SUCCESS',
  configName: 'YonSuite Dev',
  erpType: 'yonsuite',
  steps: [
    { name: '配置检查', status: 'SUCCESS', message: '配置有效' },
    { name: '网络连接', status: 'SUCCESS', message: '连接正常' },
    { name: '认证测试', status: 'SUCCESS', message: '认证成功' },
  ],
};

describe('useIntegrationDiagnosis', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('initial state', () => {
    it('should have correct initial state', () => {
      const { result } = renderHook(() => useIntegrationDiagnosis({ erpApi: mockErpApi }));

      expect(result.current.state.show).toBe(false);
      expect(result.current.state.diagnosing).toBe(false);
      expect(result.current.state.result).toBeNull();
    });
  });

  describe('startDiagnosis', () => {
    it('should start diagnosis and show success result', async () => {
      mockErpApi.runDiagnosis.mockResolvedValue({ code: 200, data: mockDiagnosisResult });

      const { result } = renderHook(() => useIntegrationDiagnosis({ erpApi: mockErpApi }));

      await act(async () => {
        await result.current.actions.startDiagnosis();
      });

      await waitFor(() => {
        expect(result.current.state.diagnosing).toBe(false);
      });

      expect(result.current.state.show).toBe(true);
      expect(result.current.state.result).toEqual(mockDiagnosisResult);
      expect(mockErpApi.runDiagnosis).toHaveBeenCalled();
    });

    it('should handle loading state during diagnosis', async () => {
      mockErpApi.runDiagnosis.mockImplementation(() => new Promise(resolve => {
        setTimeout(() => resolve({ code: 200, data: mockDiagnosisResult }), 100);
      }));

      const { result } = renderHook(() => useIntegrationDiagnosis({ erpApi: mockErpApi }));

      act(() => {
        result.current.actions.startDiagnosis();
      });

      await waitFor(() => {
        expect(result.current.state.diagnosing).toBe(true);
      });

      await waitFor(() => {
        expect(result.current.state.diagnosing).toBe(false);
      });

      expect(result.current.state.result).toEqual(mockDiagnosisResult);
    });

    it('should handle API error', async () => {
      mockErpApi.runDiagnosis.mockRejectedValue(new Error('Network error'));

      const { result } = renderHook(() => useIntegrationDiagnosis({ erpApi: mockErpApi }));

      await act(async () => {
        await result.current.actions.startDiagnosis();
      });

      await waitFor(() => {
        expect(result.current.state.diagnosing).toBe(false);
      });

      expect(result.current.state.show).toBe(false);
      expect(result.current.state.result).toBeNull();
    });

    it('should handle non-200 response code', async () => {
      mockErpApi.runDiagnosis.mockResolvedValue({ code: 500, message: 'Server error' });

      const { result } = renderHook(() => useIntegrationDiagnosis({ erpApi: mockErpApi }));

      await act(async () => {
        await result.current.actions.startDiagnosis();
      });

      await waitFor(() => {
        expect(result.current.state.diagnosing).toBe(false);
      });

      expect(result.current.state.show).toBe(false);
      expect(result.current.state.result).toBeNull();
    });
  });

  describe('closeDiagnosis', () => {
    it('should close diagnosis modal and reset result', async () => {
      mockErpApi.runDiagnosis.mockResolvedValue({ code: 200, data: mockDiagnosisResult });

      const { result } = renderHook(() => useIntegrationDiagnosis({ erpApi: mockErpApi }));

      // First run diagnosis
      await act(async () => {
        await result.current.actions.startDiagnosis();
      });

      await waitFor(() => {
        expect(result.current.state.show).toBe(true);
      });

      // Then close it
      act(() => {
        result.current.actions.closeDiagnosis();
      });

      expect(result.current.state.show).toBe(false);
      expect(result.current.state.result).toBeNull();
    });

    it('should be safe to call when no diagnosis is shown', () => {
      const { result } = renderHook(() => useIntegrationDiagnosis({ erpApi: mockErpApi }));

      act(() => {
        result.current.actions.closeDiagnosis();
      });

      expect(result.current.state.show).toBe(false);
      expect(result.current.state.result).toBeNull();
    });
  });
});
