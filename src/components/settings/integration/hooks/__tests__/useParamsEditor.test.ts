// src/components/settings/integration/hooks/__tests__/useParamsEditor.test.ts

import { renderHook, act } from '@testing-library/react';
import { useParamsEditor } from '../useParamsEditor';

// Mock API
const mockErpApi = {
  syncScenario: vi.fn(),
};

// Mock toast
vi.mock('react-hot-toast', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));

describe('useParamsEditor', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should have correct initial state', () => {
    const { result } = renderHook(() => useParamsEditor({ erpApi: mockErpApi }));

    expect(result.current.state.showFor).toBeNull();
    expect(result.current.state.pendingSyncId).toBeNull();
    expect(result.current.state.form).toEqual({
      startDate: '',
      endDate: '',
      pageSize: 100,
    });
  });

  it('should open editor and set showFor', () => {
    const { result } = renderHook(() => useParamsEditor({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.openEditor(123);
    });

    expect(result.current.state.showFor).toBe(123);
    expect(result.current.state.pendingSyncId).toBe(123);
    expect(result.current.state.form).toEqual({
      startDate: '',
      endDate: '',
      pageSize: 100,
    });
  });

  it('should close editor and clear state', () => {
    const { result } = renderHook(() => useParamsEditor({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.openEditor(123);
      result.current.actions.closeEditor();
    });

    expect(result.current.state.showFor).toBeNull();
    expect(result.current.state.pendingSyncId).toBeNull();
  });

  it('should update form field', () => {
    const { result } = renderHook(() => useParamsEditor({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.updateForm('startDate', '2024-01-01');
    });

    expect(result.current.state.form.startDate).toBe('2024-01-01');
  });

  it('should update multiple form fields', () => {
    const { result } = renderHook(() => useParamsEditor({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.updateForm('startDate', '2024-01-01');
      result.current.actions.updateForm('endDate', '2024-12-31');
      result.current.actions.updateForm('pageSize', 200);
    });

    expect(result.current.state.form).toEqual({
      startDate: '2024-01-01',
      endDate: '2024-12-31',
      pageSize: 200,
    });
  });

  it('should submit sync and call API', async () => {
    const onSyncComplete = vi.fn();
    mockErpApi.syncScenario.mockResolvedValue({ code: 200 });

    const { result } = renderHook(() =>
      useParamsEditor({ erpApi: mockErpApi, onSyncComplete })
    );

    act(() => {
      result.current.actions.openEditor(123);
      result.current.actions.updateForm('startDate', '2024-01-01');
      result.current.actions.updateForm('endDate', '2024-12-31');
      result.current.actions.updateForm('pageSize', 50);
    });

    await act(async () => {
      await result.current.actions.submitSync();
    });

    expect(mockErpApi.syncScenario).toHaveBeenCalledWith(123, {
      startDate: '2024-01-01',
      endDate: '2024-12-31',
      pageSize: 50,
    });
    expect(result.current.state.showFor).toBeNull();
    expect(onSyncComplete).toHaveBeenCalled();
  });

  it('should handle sync API error', async () => {
    const onSyncComplete = vi.fn();
    mockErpApi.syncScenario.mockRejectedValue(new Error('Sync failed'));

    const { result } = renderHook(() =>
      useParamsEditor({ erpApi: mockErpApi, onSyncComplete })
    );

    act(() => {
      result.current.actions.openEditor(123);
    });

    await act(async () => {
      await result.current.actions.submitSync();
    });

    expect(result.current.state.showFor).toBeNull();
    expect(onSyncComplete).not.toHaveBeenCalled();
  });

  it('should reset form when opening editor for different scenario', () => {
    const { result } = renderHook(() => useParamsEditor({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.openEditor(123);
      result.current.actions.updateForm('startDate', '2024-01-01');
      result.current.actions.updateForm('pageSize', 200);
    });

    act(() => {
      result.current.actions.openEditor(456);
    });

    expect(result.current.state.showFor).toBe(456);
    expect(result.current.state.form).toEqual({
      startDate: '',
      endDate: '',
      pageSize: 100,
    });
  });
});
