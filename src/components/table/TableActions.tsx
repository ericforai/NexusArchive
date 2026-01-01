// Input: React, lucide-react
// Output: TableActions 组件
// Pos: 通用复用组件 - 表格操作栏

import React from 'react';
import { MoreVertical, Eye, Pencil, Trash2, Download, Copy, CheckCircle, XCircle } from 'lucide-react';

export type ActionType = 'view' | 'edit' | 'delete' | 'download' | 'copy' | 'approve' | 'reject' | 'custom';

export interface ActionItem {
  key: ActionType | string;
  label: string;
  icon?: React.ReactNode;
  onClick: (record: any) => void;
  disabled?: boolean | ((record: any) => boolean);
  danger?: boolean;
  show?: boolean | ((record: any) => boolean);
}

export interface TableActionsProps {
  actions: ActionItem[];
  record: any;
  maxVisible?: number;
  className?: string;
}

/**
 * 统一的表格操作组件
 * <p>
 * 提供下拉菜单形式的操作按钮，支持自定义图标和样式
 * </p>
 */
export function TableActions({ actions, record, maxVisible = 3, className = '' }: TableActionsProps) {
  const [isOpen, setIsOpen] = React.useState(false);
  const dropdownRef = React.useRef<HTMLDivElement>(null);

  // Close dropdown when clicking outside
  React.useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // Filter visible actions
  const visibleActions = actions.filter(action => {
    if (typeof action.show === 'function') {
      return action.show(record);
    }
    return action.show !== false;
  });

  const primaryActions = visibleActions.slice(0, maxVisible);
  const moreActions = visibleActions.slice(maxVisible);

  const isDisabled = (action: ActionItem) => {
    if (typeof action.disabled === 'function') {
      return action.disabled(record);
    }
    return action.disabled || false;
  };

  const getDefaultIcon = (key: ActionType | string) => {
    switch (key) {
      case 'view': return <Eye size={16} />;
      case 'edit': return <Pencil size={16} />;
      case 'delete': return <Trash2 size={16} />;
      case 'download': return <Download size={16} />;
      case 'copy': return <Copy size={16} />;
      case 'approve': return <CheckCircle size={16} />;
      case 'reject': return <XCircle size={16} />;
      default: return null;
    }
  };

  const renderButton = (action: ActionItem) => {
    const disabled = isDisabled(action);

    return (
      <button
        key={action.key}
        onClick={() => !disabled && action.onClick(record)}
        disabled={disabled}
        className={`
          p-1.5 rounded-lg transition-colors
          ${action.danger
            ? 'text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-900/20'
            : 'text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-700'
          }
          ${disabled ? 'opacity-40 cursor-not-allowed' : ''}
        `}
        title={action.label}
      >
        {action.icon || getDefaultIcon(action.key)}
      </button>
    );
  };

  return (
    <div ref={dropdownRef} className={`table-actions flex items-center gap-1 ${className}`}>
      {/* Primary Actions */}
      {primaryActions.map(renderButton)}

      {/* More Actions Dropdown */}
      {moreActions.length > 0 && (
        <div className="relative">
          <button
            onClick={() => setIsOpen(!isOpen)}
            className="p-1.5 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
            title="更多操作"
          >
            <MoreVertical size={16} />
          </button>

          {isOpen && (
            <div className="absolute right-0 z-10 mt-1 w-48 bg-white dark:bg-slate-700 rounded-lg shadow-lg border border-slate-200 dark:border-slate-600 py-1">
              {moreActions.map(action => {
                const disabled = isDisabled(action);
                return (
                  <button
                    key={action.key}
                    onClick={() => {
                      if (!disabled) {
                        setIsOpen(false);
                        action.onClick(record);
                      }
                    }}
                    disabled={disabled}
                    className={`
                      w-full px-4 py-2 text-left text-sm flex items-center gap-2 transition-colors
                      ${action.danger
                        ? 'text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-900/20'
                        : 'text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-600'
                      }
                      ${disabled ? 'opacity-40 cursor-not-allowed' : ''}
                    `}
                  >
                    {action.icon || getDefaultIcon(action.key)}
                    {action.label}
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default TableActions;
