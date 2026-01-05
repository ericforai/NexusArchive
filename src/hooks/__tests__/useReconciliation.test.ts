// Input: React hooks testing library
// Output: useReconciliation hook tests
// Pos: Unit tests for useReconciliation hook

import { renderHook, act } from '@testing-library/react';
import { useReconciliation } from '../useReconciliation';

describe('useReconciliation', () => {
  it('should have correct initial state', () => {
    const { result } = renderHook(() => useReconciliation());

    expect(result.current.state).toEqual({
      show: false,
      record: null,
      loading: false,
    });
  });

  it('should show reconciliation dialog with record', () => {
    const { result } = renderHook(() => useReconciliation());

    const mockRecord = {
      fiscalYear: '2024',
      fiscalPeriod: '01',
      subjectName: 'Test Subject',
      erpDebitTotal: 1000,
      erpVoucherCount: 5,
      arcDebitTotal: 1000,
      arcVoucherCount: 5,
      attachmentCount: 5,
      attachmentMissingCount: 0,
      reconStatus: 'SUCCESS' as const,
      reconMessage: 'Reconciliation successful',
    };

    act(() => {
      result.current.actions.showReconciliation(mockRecord);
    });

    expect(result.current.state.show).toBe(true);
    expect(result.current.state.record).toEqual(mockRecord);
    expect(result.current.state.loading).toBe(false);
  });

  it('should close reconciliation dialog', () => {
    const { result } = renderHook(() => useReconciliation());

    const mockRecord = {
      fiscalYear: '2024',
      fiscalPeriod: '01',
      subjectName: 'Test Subject',
      erpDebitTotal: 1000,
      erpVoucherCount: 5,
      arcDebitTotal: 1000,
      arcVoucherCount: 5,
      attachmentCount: 5,
      attachmentMissingCount: 0,
      reconStatus: 'SUCCESS' as const,
      reconMessage: 'Reconciliation successful',
    };

    // First show the dialog
    act(() => {
      result.current.actions.showReconciliation(mockRecord);
    });

    expect(result.current.state.show).toBe(true);

    // Then close it
    act(() => {
      result.current.actions.closeReconciliation();
    });

    expect(result.current.state.show).toBe(false);
    expect(result.current.state.record).toBeNull();
  });

  it('should handle multiple show/close cycles', () => {
    const { result } = renderHook(() => useReconciliation());

    const mockRecord1 = {
      fiscalYear: '2024',
      fiscalPeriod: '01',
      subjectName: 'Subject 1',
      erpDebitTotal: 1000,
      erpVoucherCount: 5,
      arcDebitTotal: 1000,
      arcVoucherCount: 5,
      attachmentCount: 5,
      attachmentMissingCount: 0,
      reconStatus: 'SUCCESS' as const,
      reconMessage: 'Success',
    };

    const mockRecord2 = {
      fiscalYear: '2024',
      fiscalPeriod: '02',
      subjectName: 'Subject 2',
      erpDebitTotal: 2000,
      erpVoucherCount: 10,
      arcDebitTotal: 2000,
      arcVoucherCount: 10,
      attachmentCount: 10,
      attachmentMissingCount: 1,
      reconStatus: 'DISCREPANCY' as const,
      reconMessage: 'Discrepancy found',
    };

    // Show first record
    act(() => {
      result.current.actions.showReconciliation(mockRecord1);
    });

    expect(result.current.state.record?.subjectName).toBe('Subject 1');

    // Close
    act(() => {
      result.current.actions.closeReconciliation();
    });

    expect(result.current.state.record).toBeNull();

    // Show second record
    act(() => {
      result.current.actions.showReconciliation(mockRecord2);
    });

    expect(result.current.state.record?.subjectName).toBe('Subject 2');
    expect(result.current.state.record?.reconStatus).toBe('DISCREPANCY');

    // Close again
    act(() => {
      result.current.actions.closeReconciliation();
    });

    expect(result.current.state.show).toBe(false);
    expect(result.current.state.record).toBeNull();
  });

  it('should maintain state consistency across operations', () => {
    const { result } = renderHook(() => useReconciliation());

    // Initial state
    expect(result.current.state.show).toBe(false);
    expect(result.current.state.record).toBeNull();
    expect(result.current.state.loading).toBe(false);

    const mockRecord = {
      fiscalYear: '2024',
      fiscalPeriod: '01',
      subjectName: 'Test',
      erpDebitTotal: 1000,
      erpVoucherCount: 5,
      arcDebitTotal: 1000,
      arcVoucherCount: 5,
      attachmentCount: 5,
      attachmentMissingCount: 0,
      reconStatus: 'SUCCESS' as const,
      reconMessage: 'Success',
    };

    // After showing
    act(() => {
      result.current.actions.showReconciliation(mockRecord);
    });

    expect(result.current.state.show).toBe(true);
    expect(result.current.state.record).not.toBeNull();
    expect(result.current.state.loading).toBe(false);

    // After closing
    act(() => {
      result.current.actions.closeReconciliation();
    });

    // Should return to initial state
    expect(result.current.state.show).toBe(false);
    expect(result.current.state.record).toBeNull();
    expect(result.current.state.loading).toBe(false);
  });
});
