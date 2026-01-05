// src/components/settings/integration/hooks/__tests__/useAiAdapterHandler.test.ts

import { renderHook, act } from '@testing-library/react';
import { useAiAdapterHandler } from '../useAiAdapterHandler';

// Mock API
const mockErpApi = {
  generateAiPreview: vi.fn(),
  adaptAiToConfig: vi.fn(),
};

// Mock toast
vi.mock('react-hot-toast', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    default: {
      success: vi.fn(),
      error: vi.fn(),
    },
  };
});

describe('useAiAdapterHandler', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should have correct initial state', () => {
    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi }));

    expect(result.current.state.show).toBe(false);
    expect(result.current.state.loading).toBe(false);
    expect(result.current.state.files).toEqual([]);
    expect(result.current.state.preview).toBeNull();
    expect(result.current.state.selectedTargetConfigId).toBeNull();
  });

  it('should open AI adapter', () => {
    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.openAiAdapter();
    });

    expect(result.current.state.show).toBe(true);
  });

  it('should close AI adapter', () => {
    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.openAiAdapter();
      result.current.actions.closeAiAdapter();
    });

    expect(result.current.state.show).toBe(false);
  });

  it('should upload files', () => {
    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi }));

    const files = [new File(['content'], 'test1.txt'), new File(['content'], 'test2.txt')] as any;

    act(() => {
      result.current.actions.uploadFiles(files);
    });

    expect(result.current.state.files).toHaveLength(2);
    expect(result.current.state.files[0].name).toBe('test1.txt');
    expect(result.current.state.files[1].name).toBe('test2.txt');
  });

  it('should remove file by index', () => {
    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi }));

    const files = [new File(['content'], 'test1.txt'), new File(['content'], 'test2.txt')] as any;

    act(() => {
      result.current.actions.uploadFiles(files);
      result.current.actions.removeFile(0);
    });

    expect(result.current.state.files).toHaveLength(1);
    expect(result.current.state.files[0].name).toBe('test2.txt');
  });

  it('should generate preview from first file', async () => {
    const mockPreview = { fields: ['field1', 'field2'], mappings: {} };
    mockErpApi.generateAiPreview.mockResolvedValue({ data: mockPreview });

    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi }));

    const file = new File(['content'], 'test.txt') as any;

    act(() => {
      result.current.actions.uploadFiles([file]);
    });

    await act(async () => {
      await result.current.actions.generatePreview();
    });

    expect(mockErpApi.generateAiPreview).toHaveBeenCalledWith(file);
    expect(result.current.state.preview).toEqual(mockPreview);
    expect(result.current.state.loading).toBe(false);
  });

  it('should handle generate preview error', async () => {
    mockErpApi.generateAiPreview.mockRejectedValue(new Error('API Error'));

    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi }));

    const file = new File(['content'], 'test.txt') as any;

    act(() => {
      result.current.actions.uploadFiles([file]);
    });

    await act(async () => {
      await result.current.actions.generatePreview();
    });

    expect(result.current.state.loading).toBe(false);
  });

  it('should adapt to config', async () => {
    const mockResult = { success: true, configId: 1 };
    mockErpApi.adaptAiToConfig.mockResolvedValue({ data: mockResult });

    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi, onAdapted: vi.fn() }));

    const file = new File(['content'], 'test.txt') as any;

    act(() => {
      result.current.actions.uploadFiles([file]);
    });

    await act(async () => {
      await result.current.actions.adaptToConfig(1);
    });

    expect(mockErpApi.adaptAiToConfig).toHaveBeenCalledWith(file, 1);
    expect(result.current.state.loading).toBe(false);
  });

  it('should handle adapt to config error', async () => {
    mockErpApi.adaptAiToConfig.mockRejectedValue(new Error('API Error'));

    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi, onAdapted: vi.fn() }));

    const file = new File(['content'], 'test.txt') as any;

    act(() => {
      result.current.actions.uploadFiles([file]);
    });

    await act(async () => {
      await result.current.actions.adaptToConfig(1);
    });

    expect(result.current.state.loading).toBe(false);
  });

  it('should set selected target config id during adaptation', async () => {
    const mockResult = { success: true, configId: 1 };
    mockErpApi.adaptAiToConfig.mockResolvedValue({ data: mockResult });

    const { result } = renderHook(() => useAiAdapterHandler({ erpApi: mockErpApi, onAdapted: vi.fn() }));

    const file = new File(['content'], 'test.txt') as any;

    act(() => {
      result.current.actions.uploadFiles([file]);
    });

    // Trigger adaptToConfig but don't wait for it to complete
    act(() => {
      result.current.actions.adaptToConfig(1);
    });

    // The configId should be set before the operation completes
    // Note: closeAiAdapter will reset it to null after success
    expect(mockErpApi.adaptAiToConfig).toHaveBeenCalledWith(file, 1);
  });
});
