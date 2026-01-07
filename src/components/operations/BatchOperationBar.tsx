// Input: React, lucide-react 图标
// Output: BatchOperationBar 组件 - 批量操作工具栏
// Pos: src/components/operations/BatchOperationBar.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { Check, X, Ban, Loader2, CheckSquare } from 'lucide-react';

/**
 * 批量操作工具栏 Props
 */
export interface BatchOperationBarProps {
  /** 当前选中数量 */
  selectedCount: number;
  /** 筛选结果总数（用于全选） */
  totalCount?: number;
  /** 批量批准回调 */
  onBatchApprove: () => void;
  /** 批量拒绝回调 */
  onBatchReject: () => void;
  /** 全选所有回调 */
  onSelectAll: () => void;
  /** 清空选择回调 */
  onClear: () => void;
  /** 加载状态 */
  loading?: boolean;
}

/**
 * 批量操作工具栏组件
 *
 * 固定在表格上方，显示选中数量和批量操作按钮
 *
 * @example
 * ```tsx
 * <BatchOperationBar
 *   selectedCount={selectedIds.size}
 *   totalCount={filteredData.length}
 *   onBatchApprove={handleBatchApprove}
 *   onBatchReject={handleBatchReject}
 *   onSelectAll={handleSelectAll}
 *   onClear={clearSelection}
 *   loading={isProcessing}
 * />
 * ```
 */
export const BatchOperationBar: React.FC<BatchOperationBarProps> = ({
  selectedCount,
  totalCount,
  onBatchApprove,
  onBatchReject,
  onSelectAll,
  onClear,
  loading = false
}) => {
  // 当没有选中项时不显示
  if (selectedCount === 0) {
    return null;
  }

  // 检查是否超过限制
  const isOverLimit = selectedCount > 100;
  const canSelectAll = totalCount !== undefined && totalCount > selectedCount;

  return (
    <div className="mb-4 p-4 bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800 rounded-xl flex flex-wrap items-center justify-between gap-4 animate-in slide-in-from-top-2 duration-200">
      {/* 左侧：选中信息 */}
      <div className="flex items-center gap-3">
        <div className="p-2 bg-primary-100 dark:bg-primary-900/40 rounded-lg">
          <CheckSquare size={20} className="text-primary-600 dark:text-primary-400" />
        </div>
        <div>
          <p className="text-sm font-medium text-primary-900 dark:text-primary-100">
            已选择 <span className="text-lg font-bold">{selectedCount}</span> 条
          </p>
          {totalCount !== undefined && (
            <p className="text-xs text-primary-600 dark:text-primary-400">
              共 {totalCount} 条筛选结果
            </p>
          )}
        </div>
      </div>

      {/* 右侧：操作按钮组 */}
      <div className="flex flex-wrap items-center gap-2">
        {/* 全选所有按钮 */}
        {canSelectAll && !isOverLimit && (
          <button
            onClick={onSelectAll}
            disabled={loading}
            className="px-3 py-2 text-sm font-medium text-primary-700 dark:text-primary-300 bg-white dark:bg-slate-800 border border-primary-300 dark:border-primary-700 rounded-lg hover:bg-primary-50 dark:hover:bg-primary-900/30 disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
            title={`全选所有 ${totalCount} 条记录`}
          >
            <CheckSquare size={16} />
            全选所有
          </button>
        )}

        {/* 批量批准按钮 */}
        <button
          onClick={onBatchApprove}
          disabled={loading || isOverLimit}
          className="px-4 py-2 text-sm font-medium text-white bg-emerald-600 hover:bg-emerald-700 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2 shadow-sm hover:shadow"
          title={isOverLimit ? '超过 100 条限制' : '批量批准'}
        >
          {loading ? (
            <>
              <Loader2 size={16} className="animate-spin" />
              处理中...
            </>
          ) : (
            <>
              <Check size={16} />
              批量批准
            </>
          )}
        </button>

        {/* 批量拒绝按钮 */}
        <button
          onClick={onBatchReject}
          disabled={loading || isOverLimit}
          className="px-4 py-2 text-sm font-medium text-white bg-rose-600 hover:bg-rose-700 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2 shadow-sm hover:shadow"
          title={isOverLimit ? '超过 100 条限制' : '批量拒绝'}
        >
          {loading ? (
            <>
              <Loader2 size={16} className="animate-spin" />
              处理中...
            </>
          ) : (
            <>
              <X size={16} />
              批量拒绝
            </>
          )}
        </button>

        {/* 分隔线 */}
        <div className="w-px h-6 bg-slate-300 dark:bg-slate-600 mx-1" />

        {/* 清空按钮 */}
        <button
          onClick={onClear}
          disabled={loading}
          className="px-3 py-2 text-sm font-medium text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center gap-2"
          title="清空选择"
        >
          <Ban size={16} />
          清空
        </button>
      </div>

      {/* 超过限制提示 */}
      {isOverLimit && (
        <div className="w-full p-2 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg flex items-start gap-2 animate-in fade-in duration-200">
          <Ban size={16} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
          <p className="text-xs text-amber-800 dark:text-amber-300">
            批量操作最多支持 100 条记录，请减少选择数量
          </p>
        </div>
      )}
    </div>
  );
};

/**
 * 默认导出
 */
export default BatchOperationBar;
