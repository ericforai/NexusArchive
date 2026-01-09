// src/components/pool-kanban/BatchActionBar.tsx
// Input: Batch selection state, action label, execution state, and callbacks
// Output: Fixed bottom action bar displaying selection count and action buttons
// Pos: src/components/pool-kanban/BatchActionBar.tsx
import { memo } from 'react';
import { Button, Space, Alert } from 'antd';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import type { BatchActionResult } from '@/hooks/usePoolBatchAction';
import './BatchActionBar.css';

export interface BatchActionBarProps {
  selectedCount: number;                  // 选中的文件数量
  actionLabel?: string;                    // 待执行的操作标签
  isExecuting: boolean;                    // 是否执行中
  onExecute: () => void;                   // 执行操作回调
  onCancel: () => void;                    // 取消选择回调
  result?: BatchActionResult | null;       // 操作结果
}

/**
 * 批量操作栏组件
 *
 * 显示在底部的固定操作栏，用于批量操作文件。
 * 当没有选中文件时自动隐藏。
 */
export const BatchActionBar = memo<BatchActionBarProps>(({
  selectedCount,
  actionLabel = '执行',
  isExecuting,
  onExecute,
  onCancel,
  result,
}) => {
  // 没有选中文件时隐藏整个栏
  if (selectedCount === 0 && !result) {
    return null;
  }

  const hasResult = result !== null && result !== undefined;

  return (
    <div className="batch-action-bar">
      <div className="batch-action-bar__container">
        {/* 左侧：选择计数 */}
        <div className="batch-action-bar__info">
          {hasResult ? (
            <ResultMessage result={result} />
          ) : (
            <span className="batch-action-bar__count">
              已选 <strong>{selectedCount}</strong> 个文件
            </span>
          )}
        </div>

        {/* 右侧：操作按钮 */}
        <Space size="middle">
          {!hasResult && (
            <Button onClick={onCancel} disabled={isExecuting}>
              取消选择
            </Button>
          )}
          {!hasResult && (
            <Button
              type="primary"
              onClick={onExecute}
              loading={isExecuting}
              icon={isExecuting ? <Loader2 size={16} /> : undefined}
            >
              {isExecuting ? '执行中...' : actionLabel}
            </Button>
          )}
          {hasResult && (
            <Button onClick={onCancel}>
              关闭
            </Button>
          )}
        </Space>
      </div>

      {/* 操作结果提示条（仅在有结果且有错误时显示） */}
      {hasResult && result.errors && result.errors.length > 0 && (
        <ErrorAlert errors={result.errors} />
      )}
    </div>
  );
});

BatchActionBar.displayName = 'BatchActionBar';

/**
 * 结果消息组件
 */
const ResultMessage = ({ result }: { result: BatchActionResult }) => {
  if (result.success) {
    return (
      <div className="batch-action-bar__result batch-action-bar__result--success">
        <CheckCircle2 size={18} />
        <span>{result.message}</span>
      </div>
    );
  }

  return (
    <div className="batch-action-bar__result batch-action-bar__result--error">
      <XCircle size={18} />
      <span>{result.message}</span>
    </div>
  );
};

/**
 * 错误提示组件
 */
const ErrorAlert = ({ errors }: { errors: Array<{ id: string; error: string }> }) => (
  <div className="batch-action-bar__errors">
    <Alert
      type="warning"
      title={`部分操作失败: ${errors.length} 个文件`}
      description={
        <ul className="batch-action-bar__error-list">
          {errors.slice(0, 3).map((err, index) => (
            <li key={index}>
              ID: {err.id} - {err.error}
            </li>
          ))}
          {errors.length > 3 && (
            <li>还有 {errors.length - 3} 个错误...</li>
          )}
        </ul>
      }
      showIcon
    />
  </div>
);
