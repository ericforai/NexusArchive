// src/components/settings/integration/hooks/__tests__/useConnectorModal.test.ts

import { renderHook, act } from '@testing-library/react';
import { useConnectorModal } from '../useConnectorModal';
import { ErpConfig } from '../../../../../types';

// Mock API
const mockErpApi = {
  createConfig: vi.fn(),
  updateConfig: vi.fn(),
  testConnection: vi.fn(),
  detectErpType: vi.fn(),
};

const mockEditingConfig: Partial<ErpConfig> = {
  id: 1,
  name: 'Test',
  erpType: 'yonsuite',
};

describe('useConnectorModal', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should open modal for new config', () => {
    const { result } = renderHook(() => useConnectorModal({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.openModal();
    });

    expect(result.current.state.show).toBe(true);
    expect(result.current.state.editingConfig).toBeNull();
  });

  it('should open modal for editing config', () => {
    const { result } = renderHook(() => useConnectorModal({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.openModal(mockEditingConfig);
    });

    expect(result.current.state.editingConfig).toEqual(mockEditingConfig);
    expect(result.current.state.show).toBe(true);
  });

  it('should close modal', () => {
    const { result } = renderHook(() => useConnectorModal({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.openModal();
      result.current.actions.closeModal();
    });

    expect(result.current.state.show).toBe(false);
  });

  it('should update form field', () => {
    const { result } = renderHook(() => useConnectorModal({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.updateForm('name', 'New Name');
    });

    expect(result.current.state.configForm.name).toBe('New Name');
  });

  it('should add accbook code', () => {
    const { result } = renderHook(() => useConnectorModal({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.addAccbookCode('001');
    });

    expect(result.current.state.configForm.accbookCodes).toContain('001');
    expect(result.current.state.newAccbookCode).toBe('');
  });

  it('should remove accbook code', () => {
    const { result } = renderHook(() => useConnectorModal({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.addAccbookCode('001');
      result.current.actions.addAccbookCode('002');
      result.current.actions.removeAccbookCode('001');
    });

    expect(result.current.state.configForm.accbookCodes).toEqual(['002']);
  });
});
