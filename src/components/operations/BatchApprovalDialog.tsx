// Input: React, lucide-react 图标, 批量审批逻辑
// Output: BatchApprovalDialog 组件 - 批量审批确认弹窗
// Pos: src/components/operations/BatchApprovalDialog.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useMemo } from 'react';
import { CheckCircle, XCircle, AlertTriangle, Loader2, ChevronDown, ChevronUp } from 'lucide-react';
import { BaseModal } from '../modals/BaseModal';

/**
 * 待审批记录项
 */
export interface ApprovalRecord {
  id: number;
  title?: string;
  code?: string;
}

/**
 * 批量审批弹窗 Props
 */
export interface BatchApprovalDialogProps {
  /** 是否显示对话框 */
  visible: boolean;
  /** 已选中记录数量 */
  selectedCount: number;
  /** 操作类型：批准或拒绝 */
  action: 'approve' | 'reject';
  /** 确认回调（审批意见，跳过的记录 ID） */
  onConfirm: (comment: string, skipIds: number[]) => void | Promise<void>;
  /** 取消回调 */
  onCancel: () => void;
  /** 已选中记录列表（用于跳过部分记录） */
  selectedRecords?: ApprovalRecord[];
  /** 加载状态 */
  loading?: boolean;
}

/**
 * 批量审批弹窗组件
 *
 * 提供批量审批/拒绝的确认界面，支持：
 * - 统一审批意见输入
 * - 跳过部分记录单独处理
 * - 超过阈值时的确认提示
 *
 * @example
 * ```tsx
 * <BatchApprovalDialog
 *   visible={open}
 *   selectedCount={selectedIds.size}
 *   action="approve"
 *   onConfirm={handleConfirm}
 *   onCancel={() => setOpen(false)}
 *   selectedRecords={selectedRecords}
 *   loading={isProcessing}
 * />
 * ```
 */
export const BatchApprovalDialog: React.FC<BatchApprovalDialogProps> = ({
  visible,
  selectedCount,
  action,
  onConfirm,
  onCancel,
  selectedRecords = [],
  loading = false,
}) => {
  const [comment, setComment] = useState('');
  const [skipIds, setSkipIds] = useState<Set<number>>(new Set());
  const [showRecordList, setShowRecordList] = useState(false);

  // 确认阈值（超过此数量时显示确认提示）
  const CONFIRM_THRESHOLD = 10;

  // 重置状态
  const resetState = () => {
    setComment('');
    setSkipIds(new Set());
    setShowRecordList(false);
  };

  // 处理取消
  const handleCancel = () => {
    resetState();
    onCancel();
  };

  // 处理确认
  const handleConfirm = async () => {
    // 拒绝时必须填写审批意见
    if (action === 'reject' && !comment.trim()) {
      return;
    }

    await onConfirm(comment, Array.from(skipIds));
    resetState();
  };

  // 切换跳过状态
  const toggleSkip = (id: number) => {
    setSkipIds((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  // 计算实际处理的记录数
  const actualCount = useMemo(() => {
    return selectedCount - skipIds.size;
  }, [selectedCount, skipIds]);

  // 配置：根据操作类型
  const config = useMemo(() => {
    if (action === 'approve') {
      return {
        title: `批量审批 (已选 ${selectedCount} 条)`,
        icon: CheckCircle,
        iconBg: 'bg-emerald-100 dark:bg-emerald-900/30',
        iconColor: 'text-emerald-600 dark:text-emerald-400',
        buttonText: '确认审批',
        buttonClass: 'bg-emerald-600 hover:bg-emerald-700',
        commentRequired: false,
        commentPlaceholder: '审批意见（可选）',
      };
    } else {
      return {
        title: `批量驳回 (已选 ${selectedCount} 条)`,
        icon: XCircle,
        iconBg: 'bg-rose-100 dark:bg-rose-900/30',
        iconColor: 'text-rose-600 dark:text-rose-400',
        buttonText: '确认驳回',
        buttonClass: 'bg-rose-600 hover:bg-rose-700',
        commentRequired: true,
        commentPlaceholder: '驳回原因（必填）',
      };
    }
  }, [action, selectedCount]);

  const Icon = config.icon;

  // 是否显示确认提示
  const showConfirmNotice = selectedCount > CONFIRM_THRESHOLD;

  // 是否有效（拒绝时需要填写意见）
  const isValid = action === 'approve' || comment.trim().length > 0;

  return (
    <BaseModal
      isOpen={visible}
      onClose={handleCancel}
      maxWidth="lg"
      closeOnBackdropClick={!loading}
      footer={
        <>
          <button
            type="button"
            onClick={handleCancel}
            disabled={loading}
            className="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
          >
            取消
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={loading || !isValid}
            className={`px-4 py-2 text-white rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed ${config.buttonClass}`}
          >
            {loading ? (
              <>
                <Loader2 size={16} className="inline mr-2 animate-spin" />
                处理中...
              </>
            ) : (
              config.buttonText
            )}
          </button>
        </>
      }
    >
      {/* 标题和图标 */}
      <div className="flex items-start gap-4 mb-6">
        <div className={`p-3 rounded-lg ${config.iconBg} ${config.iconColor} shrink-0`}>
          <Icon size={24} />
        </div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-slate-800 dark:text-white">
            {config.title}
          </h3>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            {action === 'approve'
              ? '将对以下记录进行批量审批操作'
              : '将对以下记录进行批量驳回操作'}
          </p>
        </div>
      </div>

      {/* 确认提示（超过阈值） */}
      {showConfirmNotice && (
        <div className="mb-4 p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg flex items-start gap-2">
          <AlertTriangle size={18} className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
          <p className="text-sm text-amber-800 dark:text-amber-300">
            即将处理 <span className="font-bold">{selectedCount}</span> 条记录，请确认操作无误
          </p>
        </div>
      )}

      {/* 审批意见输入 */}
      <div className="mb-4">
        <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-2">
          审批意见
          {config.commentRequired && <span className="text-rose-500 ml-1">*</span>}
        </label>
        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder={config.commentPlaceholder}
          rows={3}
          className="w-full px-3 py-2 border border-slate-300 dark:border-slate-600 rounded-lg focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all bg-white dark:bg-slate-800 text-slate-800 dark:text-white resize-none"
          disabled={loading}
        />
        <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
          {comment.length}/500 字符
        </p>
      </div>

      {/* 跳过部分记录选项 */}
      <div className="border-t border-slate-200 dark:border-slate-700 pt-4">
        <button
          type="button"
          onClick={() => setShowRecordList(!showRecordList)}
          className="flex items-center justify-between w-full px-4 py-3 bg-slate-50 dark:bg-slate-800/50 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors"
        >
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={showRecordList}
              onChange={() => setShowRecordList(!showRecordList)}
              className="w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
            />
            <span className="text-sm font-medium text-slate-700 dark:text-slate-300">
              跳过部分记录单独处理
            </span>
            {skipIds.size > 0 && (
              <span className="px-2 py-0.5 text-xs font-medium bg-primary-100 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300 rounded-full">
                已跳过 {skipIds.size} 条
              </span>
            )}
          </div>
          {showRecordList ? <ChevronUp size={18} className="text-slate-400" /> : <ChevronDown size={18} className="text-slate-400" />}
        </button>

        {/* 记录列表 */}
        {showRecordList && selectedRecords.length > 0 && (
          <div className="mt-3 max-h-60 overflow-y-auto border border-slate-200 dark:border-slate-700 rounded-lg divide-y divide-slate-200 dark:divide-slate-700">
            {selectedRecords.map((record) => {
              const isSkipped = skipIds.has(record.id);
              return (
                <label
                  key={record.id}
                  className={`flex items-start gap-3 p-3 hover:bg-slate-50 dark:hover:bg-slate-800/50 cursor-pointer transition-colors ${
                    isSkipped ? 'opacity-50' : ''
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={!isSkipped}
                    onChange={() => toggleSkip(record.id)}
                    className="mt-0.5 w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
                  />
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-slate-800 dark:text-white truncate">
                      {record.title || `记录 #${record.id}`}
                    </p>
                    {record.code && (
                      <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                        编号: {record.code}
                      </p>
                    )}
                  </div>
                  {isSkipped && (
                    <span className="text-xs text-slate-500 dark:text-slate-400">将跳过</span>
                  )}
                </label>
              );
            })}
          </div>
        )}

        {/* 无记录提示 */}
        {showRecordList && selectedRecords.length === 0 && (
          <div className="mt-3 p-4 text-center text-sm text-slate-500 dark:text-slate-400 bg-slate-50 dark:bg-slate-800/50 rounded-lg">
            无可用记录
          </div>
        )}
      </div>

      {/* 实际处理数量提示 */}
      {skipIds.size > 0 && (
        <div className="mt-4 p-3 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
          <p className="text-sm text-blue-800 dark:text-blue-300">
            实际将处理 <span className="font-bold">{actualCount}</span> 条记录，
            跳过 <span className="font-bold">{skipIds.size}</span> 条记录
          </p>
        </div>
      )}
    </BaseModal>
  );
};

/**
 * 默认导出
 */
export default BatchApprovalDialog;
