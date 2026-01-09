// Input: React hooks testing library, Vitest
// Output: usePoolKanban hook tests
// Pos: Unit tests for usePoolKanban hook

import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { usePoolKanban } from '../usePoolKanban';
import { POOL_COLUMN_GROUPS } from '@/config/pool-columns.config';
import type { PoolItem } from '@/api/pool';

// Mock API
const mockGetList = vi.fn();
vi.mock('@/api/pool', () => ({
  poolApi: {
    getList: () => mockGetList(),
  },
}));

describe('usePoolKanban', () => {
  let queryClient: QueryClient;
  let wrapper: React.FC<{ children: React.ReactNode }>;

  beforeEach(() => {
    vi.clearAllMocks();
    queryClient = new QueryClient({
      defaultOptions: {
        queries: {
          retry: false,
        },
      },
    });

    wrapper = ({ children }) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
  });

  it('should initialize with loading state and columns', async () => {
    mockGetList.mockResolvedValue([]);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    expect(result.current.loading).toBe(true);
    expect(result.current.columns).toEqual(POOL_COLUMN_GROUPS);

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });
  });

  it('should group cards by column and sub-state', async () => {
    const mockCards: PoolItem[] = [
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: 'DRAFT' },
      { id: '2', code: 'C002', source: 'test', type: 'invoice', amount: '200', date: '2024-01-02', status: 'PENDING_CHECK' },
      { id: '3', code: 'C003', source: 'test', type: 'invoice', amount: '300', date: '2024-01-03', status: 'CHECK_FAILED' },
    ];
    mockGetList.mockResolvedValue(mockCards);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // Verify pending column has DRAFT cards
    const pendingCards = result.current.getCardsForColumn('pending', 'DRAFT');
    expect(pendingCards).toHaveLength(1);
    expect(pendingCards[0].id).toBe('1');

    // Verify needs-attention column has CHECK_FAILED cards
    const needsCards = result.current.getCardsForColumn('needs-attention', 'CHECK_FAILED');
    expect(needsCards).toHaveLength(1);
    expect(needsCards[0].id).toBe('3');
  });

  it('should return count for each sub-state', async () => {
    const mockCards: PoolItem[] = [
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: 'DRAFT' },
      { id: '2', code: 'C002', source: 'test', type: 'invoice', amount: '200', date: '2024-01-02', status: 'DRAFT' },
      { id: '3', code: 'C003', source: 'test', type: 'invoice', amount: '300', date: '2024-01-03', status: 'PENDING_CHECK' },
    ];
    mockGetList.mockResolvedValue(mockCards);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const draftCount = result.current.getSubStateCount('pending', 'DRAFT');
    expect(draftCount).toBe(2);

    const pendingCheckCount = result.current.getSubStateCount('pending', 'PENDING_CHECK');
    expect(pendingCheckCount).toBe(1);
  });

  it('should return total count for a column', async () => {
    const mockCards: PoolItem[] = [
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: 'DRAFT' },
      { id: '2', code: 'C002', source: 'test', type: 'invoice', amount: '200', date: '2024-01-02', status: 'DRAFT' },
      { id: '3', code: 'C003', source: 'test', type: 'invoice', amount: '300', date: '2024-01-03', status: 'PENDING_CHECK' },
    ];
    mockGetList.mockResolvedValue(mockCards);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const pendingTotal = result.current.getTotalCount('pending');
    expect(pendingTotal).toBe(3); // 2 DRAFT + 1 PENDING_CHECK
  });

  it('should refresh data when refetch is called', async () => {
    const mockCards: PoolItem[] = [
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: 'DRAFT' },
    ];
    mockGetList.mockResolvedValue(mockCards);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(mockGetList).toHaveBeenCalledTimes(1);

    await act(async () => {
      await result.current.refetch();
    });

    expect(mockGetList).toHaveBeenCalledTimes(2);
  });

  it('should handle empty data gracefully', async () => {
    mockGetList.mockResolvedValue([]);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.cards).toEqual([]);
    expect(result.current.getSubStateCount('pending', 'DRAFT')).toBe(0);
    expect(result.current.getTotalCount('pending')).toBe(0);
  });

  it('should handle API error', async () => {
    mockGetList.mockRejectedValue(new Error('API Error'));

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.error).toBeInstanceOf(Error);
    expect(result.current.error?.message).toBe('API Error');
  });

  it('should assign _columnId to each card based on status', async () => {
    const mockCards: PoolItem[] = [
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: 'DRAFT' },
      { id: '2', code: 'C002', source: 'test', type: 'invoice', amount: '200', date: '2024-01-02', status: 'CHECK_FAILED' },
      { id: '3', code: 'C003', source: 'test', type: 'invoice', amount: '300', date: '2024-01-03', status: 'MATCHED' },
    ];
    mockGetList.mockResolvedValue(mockCards);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.cards[0]._columnId).toBe('pending');
    expect(result.current.cards[1]._columnId).toBe('needs-attention');
    expect(result.current.cards[2]._columnId).toBe('ready');
  });
});
