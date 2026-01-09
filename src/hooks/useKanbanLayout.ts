// Input: React hooks, ResizeObserver API, POOL_COLUMN_GROUPS config
// Output: useKanbanLayout hook for responsive kanban layout
// Pos: 看板响应式布局 Hook

import { useState, useEffect, useCallback, useMemo } from 'react';
import { POOL_COLUMN_GROUPS } from '@/config/pool-columns.config';

// 列宽配置常量
export const MIN_COLUMN_WIDTH = 280;
export const MAX_COLUMN_WIDTH = 400;
export const COLLAPSED_COLUMN_WIDTH = 48;

// 容器内边距（两侧总和）
export const CONTAINER_PADDING = 32;

// 列间距
const COLUMN_GAP = 16;

interface KanbanLayoutOptions {
  containerRef?: React.RefObject<HTMLElement>;
  enabled?: boolean;
}

interface UseKanbanLayoutResult {
  // 列宽计算
  columnWidth: number;
  setColumnWidth: (width: number) => void;
  recalculateWidth: () => void;

  // 容器尺寸
  containerWidth: number;
  isOverflowing: boolean;

  // 空列检测
  getEmptyColumns: (cardsByColumn: Record<string, unknown[]>) => string[];
  hasCards: (columnId: string, cardsByColumn: Record<string, unknown[]>) => boolean;

  // 折叠状态
  collapsedColumns: Set<string>;
  toggleCollapse: (columnId: string) => void;
  isCollapsed: (columnId: string) => boolean;
  collapseAllEmpty: (cardsByColumn: Record<string, unknown[]>) => void;
  expandAll: () => void;

  // 计算辅助
  getVisibleColumnCount: () => number;
  getTotalContentWidth: () => number;
}

interface ColumnCardCounts {
  [columnId: string]: number;
}

/**
 * 计算列宽
 * 基于容器宽度、列数量和折叠状态动态计算
 */
function calculateColumnWidth(width: number, collapsedCount: number): number {
  if (width <= 0) return MIN_COLUMN_WIDTH;

  const totalColumns = POOL_COLUMN_GROUPS.length;
  const visibleColumns = totalColumns - collapsedCount;

  if (visibleColumns <= 0) return MIN_COLUMN_WIDTH;

  const totalGapWidth = (visibleColumns - 1) * COLUMN_GAP;
  const collapsedWidth = collapsedCount * COLLAPSED_COLUMN_WIDTH;
  const availableWidth = width - CONTAINER_PADDING - totalGapWidth - collapsedWidth;
  const calculatedWidth = Math.floor(availableWidth / visibleColumns);

  return Math.max(MIN_COLUMN_WIDTH, Math.min(MAX_COLUMN_WIDTH, calculatedWidth));
}

/**
 * 计算内容总宽度
 */
function calculateTotalContentWidth(
  columnWidth: number,
  collapsedCount: number
): number {
  const totalColumns = POOL_COLUMN_GROUPS.length;
  const visibleColumns = totalColumns - collapsedCount;
  const totalGapWidth = (visibleColumns - 1) * COLUMN_GAP;
  const collapsedWidth = collapsedCount * COLLAPSED_COLUMN_WIDTH;

  return visibleColumns * columnWidth + totalGapWidth + collapsedWidth + CONTAINER_PADDING;
}

/**
 * 列宽状态管理 Hook
 */
function useColumnWidth(
  containerRef: React.RefObject<HTMLElement> | undefined,
  enabled: boolean,
  collapsedCount: number
): [number, number, () => void, (width: number) => void] {
  const [columnWidth, setColumnWidth] = useState<number>(MIN_COLUMN_WIDTH);
  const [containerWidth, setContainerWidth] = useState<number>(0);

  const recalculateWidth = useCallback(() => {
    if (!enabled || !containerRef?.current) {
      setColumnWidth(MIN_COLUMN_WIDTH);
      return;
    }
    const width = containerRef.current.clientWidth;
    const newWidth = calculateColumnWidth(width, collapsedCount);
    setColumnWidth(newWidth);
  }, [enabled, containerRef, collapsedCount]);

  const setColumnWidthHandler = useCallback((width: number) => {
    const clampedWidth = Math.max(MIN_COLUMN_WIDTH, Math.min(MAX_COLUMN_WIDTH, width));
    setColumnWidth(clampedWidth);
  }, []);

  // 监听容器尺寸变化
  useEffect(() => {
    if (!enabled) return;

    const element = containerRef?.current;
    if (!element) return;

    setContainerWidth(element.clientWidth);

    const observer = new ResizeObserver(entries => {
      for (const entry of entries) {
        const newWidth = entry.contentRect.width;
        setContainerWidth(newWidth);
        const newColumnWidth = calculateColumnWidth(newWidth, collapsedCount);
        setColumnWidth(newColumnWidth);
      }
    });

    observer.observe(element);

    return () => {
      observer.disconnect();
    };
  }, [enabled, containerRef, collapsedCount]);

  return [columnWidth, containerWidth, recalculateWidth, setColumnWidthHandler];
}

/**
 * 溢出检测 Hook
 */
function useOverflowDetection(
  containerWidth: number,
  columnWidth: number,
  collapsedCount: number
): boolean {
  return useMemo(() => {
    if (containerWidth <= 0) return false;
    const totalContentWidth = calculateTotalContentWidth(columnWidth, collapsedCount);
    return totalContentWidth > containerWidth;
  }, [containerWidth, columnWidth, collapsedCount]);
}

/**
 * 空列检测 Hook
 */
function useEmptyColumnDetection() {
  const getEmptyColumns = useCallback((
    cardsByColumn: Record<string, unknown[]>
  ): string[] => {
    return POOL_COLUMN_GROUPS
      .filter(column => {
        const cards = cardsByColumn[column.id] || [];
        return cards.length === 0;
      })
      .map(column => column.id);
  }, []);

  const hasCards = useCallback((
    columnId: string,
    cardsByColumn: Record<string, unknown[]>
  ): boolean => {
    const cards = cardsByColumn[columnId] || [];
    return cards.length > 0;
  }, []);

  return { getEmptyColumns, hasCards };
}

/**
 * 折叠状态管理 Hook
 */
function useCollapseState(getEmptyColumns: (cardsByColumn: Record<string, unknown[]>) => string[]) {
  const [collapsedColumns, setCollapsedColumns] = useState<Set<string>>(new Set());

  const toggleCollapse = useCallback((columnId: string) => {
    setCollapsedColumns(prev => {
      const newSet = new Set(prev);
      if (newSet.has(columnId)) {
        newSet.delete(columnId);
      } else {
        newSet.add(columnId);
      }
      return newSet;
    });
  }, []);

  const isCollapsed = useCallback((columnId: string): boolean => {
    return collapsedColumns.has(columnId);
  }, [collapsedColumns]);

  const collapseAllEmpty = useCallback((
    cardsByColumn: Record<string, unknown[]>
  ) => {
    const emptyColumnIds = getEmptyColumns(cardsByColumn);
    setCollapsedColumns(new Set(emptyColumnIds));
  }, [getEmptyColumns]);

  const expandAll = useCallback(() => {
    setCollapsedColumns(new Set());
  }, []);

  return {
    collapsedColumns,
    toggleCollapse,
    isCollapsed,
    collapseAllEmpty,
    expandAll,
  };
}

/**
 * 计算辅助函数 Hook
 */
function useLayoutCalculations(
  columnWidth: number,
  collapsedColumns: Set<string>
) {
  const getVisibleColumnCount = useCallback((): number => {
    return POOL_COLUMN_GROUPS.length - collapsedColumns.size;
  }, [collapsedColumns.size]);

  const getTotalContentWidth = useCallback((): number => {
    return calculateTotalContentWidth(columnWidth, collapsedColumns.size);
  }, [columnWidth, collapsedColumns.size]);

  return { getVisibleColumnCount, getTotalContentWidth };
}

/**
 * 看板响应式布局 Hook
 *
 * 提供以下功能：
 * 1. 动态列宽计算（基于容器宽度和列数量）
 * 2. 空列检测和自动折叠
 * 3. 折叠状态管理
 * 4. 容器溢出检测
 */
export function useKanbanLayout(options: KanbanLayoutOptions = {}): UseKanbanLayoutResult {
  const {
    containerRef,
    enabled = true,
  } = options;

  // 空列检测
  const { getEmptyColumns, hasCards } = useEmptyColumnDetection();

  // 折叠状态管理
  const collapseState = useCollapseState(getEmptyColumns);

  // 列宽状态管理
  const [columnWidth, containerWidth, recalculateWidth, setColumnWidthHandler] =
    useColumnWidth(containerRef, enabled, collapseState.collapsedColumns.size);

  // 溢出检测
  const isOverflowing = useOverflowDetection(
    containerWidth,
    columnWidth,
    collapseState.collapsedColumns.size
  );

  // 计算辅助
  const calculations = useLayoutCalculations(columnWidth, collapseState.collapsedColumns);

  return {
    // 列宽计算
    columnWidth,
    setColumnWidth: setColumnWidthHandler,
    recalculateWidth,

    // 容器尺寸
    containerWidth,
    isOverflowing,

    // 空列检测
    getEmptyColumns,
    hasCards,

    // 折叠状态
    ...collapseState,

    // 计算辅助
    ...calculations,
  };
}

/**
 * 计算给定卡片分布中每列的数量
 */
export function calculateColumnCardCounts(
  cardsByColumn: Record<string, unknown[]>
): ColumnCardCounts {
  const counts: ColumnCardCounts = {};

  for (const column of POOL_COLUMN_GROUPS) {
    counts[column.id] = (cardsByColumn[column.id] || []).length;
  }

  return counts;
}

/**
 * 检查是否所有列都为空
 */
export function areAllColumnsEmpty(cardsByColumn: Record<string, unknown[]>): boolean {
  return POOL_COLUMN_GROUPS.every(column => {
    const cards = cardsByColumn[column.id] || [];
    return cards.length === 0;
  });
}

/**
 * 获取第一个有卡片的列 ID
 */
export function getFirstNonEmptyColumnId(cardsByColumn: Record<string, unknown[]>): string | null {
  for (const column of POOL_COLUMN_GROUPS) {
    const cards = cardsByColumn[column.id] || [];
    if (cards.length > 0) {
      return column.id;
    }
  }
  return null;
}
