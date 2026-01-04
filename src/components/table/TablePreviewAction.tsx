// Input: React, lucide-react icons
// Output: TablePreviewAction 组件
// Pos: src/components/table/TablePreviewAction.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * TablePreviewAction - 表格行预览操作组件
 *
 * 通用的表格预览操作按钮组件，支持：
 * - 悬停高亮效果
 * - 整行点击预览
 * - 可选的额外操作按钮
 * - 可配置的提示文字和图标
 */
import React from 'react';
import { Eye, Trash2 } from 'lucide-react';

export interface TablePreviewActionProps {
  /** 是否悬停 */
  hovered?: boolean;
  /** 预览提示文字 */
  previewLabel?: string;
  /** 是否显示删除按钮 */
  showDelete?: boolean;
  /** 删除按钮的点击处理 */
  onDelete?: () => void;
  /** 预览按钮的点击处理 */
  onPreview: () => void;
}

/**
 * 表格行预览操作组件
 *
 * @example
 * ```tsx
 * <TablePreviewAction
 *   hovered={isHovered}
 *   onPreview={() => openPreview(row)}
 *   showDelete={canDelete}
 *   onDelete={() => deleteRow(row.id)}
 * />
 * ```
 */
export const TablePreviewAction: React.FC<TablePreviewActionProps> = ({
  hovered = false,
  previewLabel = '查看',
  showDelete = false,
  onDelete,
  onPreview
}) => {
  return (
    <div className="flex items-center justify-end gap-2">
      {/* 查看按钮 - 始终显示，悬停时更明显 */}
      <button
        onClick={(e) => {
          e.stopPropagation();
          onPreview();
        }}
        className={`
          flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium
          transition-all duration-200
          ${hovered
            ? 'bg-blue-600 text-white shadow-md shadow-blue-500/30'
            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}
        `}
        title="点击预览详情"
      >
        <Eye size={14} />
        <span>{previewLabel}</span>
      </button>

      {/* 删除按钮 - 可选 */}
      {showDelete && onDelete && (
        <button
          onClick={(e) => {
            e.stopPropagation();
            onDelete();
          }}
          className="p-1.5 text-rose-600 hover:bg-rose-50 rounded-lg transition-colors"
          title="删除"
        >
          <Trash2 size={16} />
        </button>
      )}
    </div>
  );
};

export default TablePreviewAction;
