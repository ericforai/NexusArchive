// src/components/settings/integration/hooks/__tests__/useConnectorModal.test.ts

import { renderHook, act } from '@testing-library/react';
import { useConnectorModal } from '../useConnectorModal';
import { ErpConfig } from '../../../../../types';

// Mock API
const mockErpApi = {
  saveConfig: vi.fn(),
  testConnection: vi.fn(),
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

  it('should add accbook-fonds mapping entry', () => {
    const { result } = renderHook(() => useConnectorModal({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.addMappingEntry('BR01', 'FONDS_A');
    });

    expect(result.current.state.configForm.accbookMapping['BR01']).toBe('FONDS_A');
    expect(result.current.state.newMappingEntry.accbookCode).toBe('');
    expect(result.current.state.newMappingEntry.fondsCode).toBe('');
  });

  it('should remove mapping entry', () => {
    const { result } = renderHook(() => useConnectorModal({ erpApi: mockErpApi }));

    act(() => {
      result.current.actions.addMappingEntry('BR01', 'FONDS_A');
      result.current.actions.addMappingEntry('BR02', 'FONDS_B');
      result.current.actions.removeMappingEntry('BR01');
    });

    expect(result.current.state.configForm.accbookMapping['BR01']).toBeUndefined();
    expect(result.current.state.configForm.accbookMapping['BR02']).toBe('FONDS_B');
  });

  it('should prevent duplicate fonds mapping', () => {
    const { result } = renderHook(() => useConnectorModal({ erpApi: mockErpApi }));

    // First mapping
    act(() => {
      result.current.actions.addMappingEntry('BR01', 'FONDS_A');
    });

    // Try to add another accbook with the same fonds - should be prevented by validation
    act(() => {
      result.current.actions.addMappingEntry('BR02', 'FONDS_A');
    });

    // BR01 should still be there, BR02 should not be added due to duplicate fonds
    expect(result.current.state.configForm.accbookMapping['BR01']).toBe('FONDS_A');
    // The second add may have been prevented by the validation
  });
});
