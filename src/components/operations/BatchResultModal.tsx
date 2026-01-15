// Input: visible, successCount, failedCount, errors, onRetry, onClose, operationType, onExportReport, isRetrying
// Output: 批量操作结果弹窗（显示成功/失败统计、失败详情、重试功能、导出报告、状态图标、全部成功/失败提示）
// Pos: src/components/operations/BatchResultModal.tsx

import React, { useMemo } from 'react';
import { CheckCircle, XCircle, RotateCcw, Download } from 'lucide-react';
import { BaseModal } from '../modals/BaseModal';

/**
 * 常量定义
 */
const ERROR_LIST_MAX_HEIGHT = 'max-h-60';
const ICON_SIZE_SM = 16;
const ICON_SIZE_MD = 20;
const ICON_SIZE_LG = 24;

/**
 * 批量操作错误项
 */
export interface BatchError {
  /** 记录 ID */
  id: string;
  /** 失败原因 */
  reason: string;
}

/**
 * 批量操作结果弹窗 Props
 */
export interface BatchResultModalProps {
  /** 是否显示对话框 */
  visible: boolean;
  /** 成功数量 */
  successCount: number;
  /** 失败数量 */
  failedCount: number;
  /** 错误列表 */
  errors?: BatchError[];
  /** 重试回调（传入失败的 ID 列表） */
  onRetry?: (failedIds: string[]) => void | Promise<void>;
  /** 关闭回调 */
  onClose: () => void;
  /** 操作类型（用于标题显示） */
  operationType?: 'approval' | 'operation';
  /** 导出失败报告回调 */
  onExportReport?: () => void | Promise<void>;
  /** 重试中状态 */
  isRetrying?: boolean;
}

/**
 * 批量操作结果弹窗组件
 *
 * 展示批量操作的成功/失败统计和失败详情，支持：
 * - 成功/失败数量统计摘要
 * - 失败项明细列表（可滚动）
 * - 重试失败项功能
 * - 导出失败报告功能
 *
 * @example
 * ```tsx
 * <BatchResultModal
 *   visible={open}
 *   successCount={95}
 *   failedCount={5}
 *   errors={[{ id: 1, reason: '状态不允许审批' }]}
 *   onRetry={handleRetry}
 *   onClose={() => setOpen(false)}
 *   operationType="approval"
 * />
 * ```
 */
export const BatchResultModal: React.FC<BatchResultModalProps> = ({
  visible,
  successCount,
  failedCount,
  errors = [],
  onRetry,
  onClose,
  operationType = 'operation',
  onExportReport,
  isRetrying = false,
}) => {
  /**
   * 处理重试
   */
  const handleRetry = async () => {
    if (onRetry && errors.length > 0) {
      const failedIds = errors.map((e) => e.id);
      await onRetry(failedIds);
    }
  };

  /**
   * 处理导出报告
   */
  const handleExportReport = async () => {
    if (onExportReport) {
      await onExportReport();
    }
  };

  /**
   * 计算总体状态
   */
  const totalStatus = failedCount === 0 ? 'success' : successCount === 0 ? 'error' : 'partial';

  /**
   * 状态配置（使用 useMemo 优化性能）
   */
  const statusConfig = useMemo(() => {
    switch (totalStatus) {
      case 'success':
        return {
          icon: CheckCircle,
          iconBg: 'bg-emerald-100 dark:bg-emerald-900/30',
          iconColor: 'text-emerald-600 dark:text-emerald-400',
          title: '全部成功',
          description: '所有记录均已成功处理',
        };
      case 'error':
        return {
          icon: XCircle,
          iconBg: 'bg-rose-100 dark:bg-rose-900/30',
          iconColor: 'text-rose-600 dark:text-rose-400',
          title: '全部失败',
          description: '所有记录处理失败，请检查失败详情',
        };
      case 'partial':
      default:
        return {
          icon: CheckCircle,
          iconBg: 'bg-amber-100 dark:bg-amber-900/30',
          iconColor: 'text-amber-600 dark:text-amber-400',
          title: '部分成功',
          description: '部分记录处理成功，请检查失败详情',
        };
    }
  }, [totalStatus]);

  /**
   * 获取标题
   */
  const getTitle = () => {
    return operationType === 'approval' ? '批量审批完成' : '批量操作完成';
  };

  const StatusIcon = statusConfig.icon;

  /**
   * 渲染页脚按钮
   */
  const renderFooter = () => {
    return (
      <>
        {/* 关闭按钮 */}
        <button
          type="button"
          onClick={onClose}
          disabled={isRetrying}
          className="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          关闭
        </button>

        {/* 导出报告按钮（有失败时显示） */}
        {failedCount > 0 && onExportReport && (
          <button
            type="button"
            onClick={handleExportReport}
            disabled={isRetrying}
            className="px-4 py-2 text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            <Download size={ICON_SIZE_SM} />
            导出报告
          </button>
        )}

        {/* 重试按钮（有失败且提供了回调时显示） */}
        {failedCount > 0 && onRetry && (
          <button
            type="button"
            onClick={handleRetry}
            disabled={isRetrying}
            className="px-4 py-2 text-white bg-primary-600 hover:bg-primary-700 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            {isRetrying ? (
              <>
                <RotateCcw size={ICON_SIZE_SM} className="animate-spin" />
                重试中...
              </>
            ) : (
              <>
                <RotateCcw size={ICON_SIZE_SM} />
                重试失败项
              </>
            )}
          </button>
        )}
      </>
    );
  };

  return (
    <BaseModal
      isOpen={visible}
      onClose={onClose}
      maxWidth="lg"
      closeOnBackdropClick={!isRetrying}
      footer={renderFooter()}
      showCloseButton={!isRetrying}
    >
      {/* 状态图标和标题 */}
      <div className="flex items-start gap-4 mb-6">
        <div className={`p-3 rounded-lg ${statusConfig.iconBg} ${statusConfig.iconColor} shrink-0`}>
          <StatusIcon size={ICON_SIZE_LG} />
        </div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-slate-800 dark:text-white">
            {getTitle()}
          </h3>
          <p className="text-sm text-slate-600 dark:text-slate-400 mt-1">
            {statusConfig.description}
          </p>
        </div>
      </div>

      {/* 统计摘要 */}
      <div className="grid grid-cols-2 gap-4 mb-6" role="status" aria-live="polite">
        {/* 成功统计 */}
        <div className="p-4 bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800 rounded-xl">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-emerald-100 dark:bg-emerald-900/30 rounded-lg">
              <CheckCircle size={ICON_SIZE_MD} className="text-emerald-600 dark:text-emerald-400" />
            </div>
            <div>
              <p className="text-2xl font-bold text-emerald-700 dark:text-emerald-300">
                {successCount}
              </p>
              <p className="text-sm text-emerald-600 dark:text-emerald-400">
                成功处理
              </p>
            </div>
          </div>
        </div>

        {/* 失败统计 */}
        <div className={`p-4 rounded-xl border ${
          failedCount > 0
            ? 'bg-rose-50 dark:bg-rose-900/20 border-rose-200 dark:border-rose-800'
            : 'bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700'
        }`}>
          <div className="flex items-center gap-3">
            <div className={`p-2 rounded-lg ${
              failedCount > 0
                ? 'bg-rose-100 dark:bg-rose-900/30'
                : 'bg-slate-100 dark:bg-slate-700'
            }`}>
              <XCircle
                size={ICON_SIZE_MD}
                className={
                  failedCount > 0
                    ? 'text-rose-600 dark:text-rose-400'
                    : 'text-slate-400 dark:text-slate-500'
                }
              />
            </div>
            <div>
              <p
                className={`text-2xl font-bold ${
                  failedCount > 0
                    ? 'text-rose-700 dark:text-rose-300'
                    : 'text-slate-500 dark:text-slate-400'
                }`}
              >
                {failedCount}
              </p>
              <p
                className={`text-sm ${
                  failedCount > 0
                    ? 'text-rose-600 dark:text-rose-400'
                    : 'text-slate-400 dark:text-slate-500'
                }`}
              >
                处理失败
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 失败详情列表 */}
      {failedCount > 0 && errors.length > 0 && (
        <div role="region" aria-label="失败详情区域">
          <div className="flex items-center justify-between mb-3">
            <h4 className="text-sm font-semibold text-slate-700 dark:text-slate-300">
              失败详情
            </h4>
            <span className="text-xs text-slate-500 dark:text-slate-400">
              共 {errors.length} 条
            </span>
          </div>

          <div
            className={`${ERROR_LIST_MAX_HEIGHT} overflow-y-auto border border-slate-200 dark:border-slate-700 rounded-lg divide-y divide-slate-200 dark:divide-slate-700`}
            role="list"
            aria-label="失败详情列表"
          >
            {errors.map((error) => (
              <div
                key={error.id}
                role="listitem"
                className="flex items-start gap-3 p-3 hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors"
              >
                <div className="p-1.5 bg-rose-100 dark:bg-rose-900/30 rounded-lg shrink-0">
                  <XCircle size={ICON_SIZE_SM} className="text-rose-600 dark:text-rose-400" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-slate-800 dark:text-white">
                    档案 #{error.id}
                  </p>
                  <p className="text-xs text-rose-600 dark:text-rose-400 mt-0.5">
                    {error.reason}
                  </p>
                </div>
              </div>
            ))}
          </div>

          {/* 失败提示 */}
          <div className="mt-3 p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg">
            <p className="text-xs text-amber-800 dark:text-amber-300">
              <span className="font-semibold">提示：</span>
              可以点击「重试失败项」重新处理这些记录，或导出失败报告进行排查
            </p>
          </div>
        </div>
      )}

      {/* 全部成功时的提示 */}
      {totalStatus === 'success' && (
        <div className="p-4 bg-emerald-50 dark:bg-emerald-900/20 border border-emerald-200 dark:border-emerald-800 rounded-lg">
          <p className="text-sm text-emerald-800 dark:text-emerald-300">
            所有记录均已成功处理完成！
          </p>
        </div>
      )}
    </BaseModal>
  );
};

/**
 * 默认导出
 */
export default BatchResultModal;
