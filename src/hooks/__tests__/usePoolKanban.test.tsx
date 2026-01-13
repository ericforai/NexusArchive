// Input: React hooks testing library, Vitest
// Output: usePoolKanban hook tests
// Pos: Unit tests for usePoolKanban hook

import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { usePoolKanban } from '../usePoolKanban';
import { POOL_COLUMN_GROUPS, SimplifiedPreArchiveStatus } from '@/config/pool-columns.config';
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
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: SimplifiedPreArchiveStatus.PENDING_CHECK },
      { id: '2', code: 'C002', source: 'test', type: 'invoice', amount: '200', date: '2024-01-02', status: SimplifiedPreArchiveStatus.NEEDS_ACTION },
      { id: '3', code: 'C003', source: 'test', type: 'invoice', amount: '300', date: '2024-01-03', status: SimplifiedPreArchiveStatus.READY_TO_MATCH },
    ];
    mockGetList.mockResolvedValue(mockCards);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    // Verify pending column has PENDING_CHECK cards
    const pendingCards = result.current.getCardsForColumn('pending', SimplifiedPreArchiveStatus.PENDING_CHECK);
    expect(pendingCards).toHaveLength(1);
    expect(pendingCards[0].id).toBe('1');

    // Verify needs-action column has NEEDS_ACTION cards
    const needsCards = result.current.getCardsForColumn('needs-action', SimplifiedPreArchiveStatus.NEEDS_ACTION);
    expect(needsCards).toHaveLength(1);
    expect(needsCards[0].id).toBe('2');

    // Verify ready-to-match column has READY_TO_MATCH cards
    const readyToMatchCards = result.current.getCardsForColumn('ready-to-match', SimplifiedPreArchiveStatus.READY_TO_MATCH);
    expect(readyToMatchCards).toHaveLength(1);
    expect(readyToMatchCards[0].id).toBe('3');
  });

  it('should return count for each sub-state', async () => {
    const mockCards: PoolItem[] = [
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: SimplifiedPreArchiveStatus.PENDING_CHECK },
      { id: '2', code: 'C002', source: 'test', type: 'invoice', amount: '200', date: '2024-01-02', status: SimplifiedPreArchiveStatus.PENDING_CHECK },
      { id: '3', code: 'C003', source: 'test', type: 'invoice', amount: '300', date: '2024-01-03', status: SimplifiedPreArchiveStatus.NEEDS_ACTION },
    ];
    mockGetList.mockResolvedValue(mockCards);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const pendingCheckCount = result.current.getSubStateCount('pending', SimplifiedPreArchiveStatus.PENDING_CHECK);
    expect(pendingCheckCount).toBe(2);

    const needsActionCount = result.current.getSubStateCount('needs-action', SimplifiedPreArchiveStatus.NEEDS_ACTION);
    expect(needsActionCount).toBe(1);
  });

  it('should return total count for a column', async () => {
    const mockCards: PoolItem[] = [
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: SimplifiedPreArchiveStatus.PENDING_CHECK },
      { id: '2', code: 'C002', source: 'test', type: 'invoice', amount: '200', date: '2024-01-02', status: SimplifiedPreArchiveStatus.PENDING_CHECK },
      { id: '3', code: 'C003', source: 'test', type: 'invoice', amount: '300', date: '2024-01-03', status: SimplifiedPreArchiveStatus.NEEDS_ACTION },
    ];
    mockGetList.mockResolvedValue(mockCards);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    const pendingTotal = result.current.getTotalCount('pending');
    expect(pendingTotal).toBe(2); // 2 PENDING_CHECK

    const needsActionTotal = result.current.getTotalCount('needs-action');
    expect(needsActionTotal).toBe(1); // 1 NEEDS_ACTION
  });

  it('should refresh data when refetch is called', async () => {
    const mockCards: PoolItem[] = [
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: SimplifiedPreArchiveStatus.PENDING_CHECK },
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
    expect(result.current.getSubStateCount('pending', SimplifiedPreArchiveStatus.PENDING_CHECK)).toBe(0);
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
      { id: '1', code: 'C001', source: 'test', type: 'invoice', amount: '100', date: '2024-01-01', status: SimplifiedPreArchiveStatus.PENDING_CHECK },
      { id: '2', code: 'C002', source: 'test', type: 'invoice', amount: '200', date: '2024-01-02', status: SimplifiedPreArchiveStatus.NEEDS_ACTION },
      { id: '3', code: 'C003', source: 'test', type: 'invoice', amount: '300', date: '2024-01-03', status: SimplifiedPreArchiveStatus.READY_TO_MATCH },
      { id: '4', code: 'C004', source: 'test', type: 'invoice', amount: '400', date: '2024-01-04', status: SimplifiedPreArchiveStatus.READY_TO_ARCHIVE },
      { id: '5', code: 'C005', source: 'test', type: 'invoice', amount: '500', date: '2024-01-05', status: SimplifiedPreArchiveStatus.COMPLETED },
    ];
    mockGetList.mockResolvedValue(mockCards);

    const { result } = renderHook(() => usePoolKanban(), { wrapper });

    await waitFor(() => {
      expect(result.current.loading).toBe(false);
    });

    expect(result.current.cards[0]._columnId).toBe('pending');
    expect(result.current.cards[1]._columnId).toBe('needs-action');
    expect(result.current.cards[2]._columnId).toBe('ready-to-match');
    expect(result.current.cards[3]._columnId).toBe('ready-to-archive');
    expect(result.current.cards[4]._columnId).toBe('completed');
  });
});
