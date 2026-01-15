// Input: React, lucide-react
// Output: TableFilters 组件
// Pos: 通用复用组件 - 表格筛选器

import React, { useState } from 'react';
import { Search, X, SlidersHorizontal } from 'lucide-react';

export interface FilterField {
  key: string;
  label: string;
  type: 'text' | 'select' | 'date' | 'dateRange';
  placeholder?: string;
  options?: Array<{ label: string; value: string | number }>;
}

export interface TableFiltersProps {
  fields: FilterField[];
  values: Record<string, any>;
  onChange: (values: Record<string, any>) => void;
  onSearch?: () => void;
  onReset?: () => void;
  className?: string;
  collapsible?: boolean;
}

/**
 * 统一的表格筛选组件
 * <p>
 * 支持多种筛选字段类型，可折叠
 * </p>
 */
export function TableFilters({
  fields,
  values,
  onChange,
  onSearch,
  onReset,
  className = '',
  collapsible = true,
}: TableFiltersProps) {
  const [isExpanded, setIsExpanded] = useState(!collapsible);
  const hasActiveFilters = Object.values(values).some(v => v !== '' && v !== undefined && v !== null);

  const handleChange = (key: string, value: any) => {
    onChange({ ...values, [key]: value });
  };

  const handleReset = () => {
    const resetValues: Record<string, any> = {};
    fields.forEach(field => {
      resetValues[field.key] = undefined;
    });
    onChange(resetValues);
    onReset?.();
  };

  const renderField = (field: FilterField) => {
    const value = values[field.key];

    switch (field.type) {
      case 'text':
        return (
          <input
            type="text"
            value={value || ''}
            onChange={(e) => handleChange(field.key, e.target.value)}
            placeholder={field.placeholder || `搜索${field.label}`}
            className="px-3 py-2 border border-slate-200 dark:border-slate-600 rounded-lg bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        );

      case 'select':
        return (
          <select
            value={value || ''}
            onChange={(e) => handleChange(field.key, e.target.value)}
            className="px-3 py-2 border border-slate-200 dark:border-slate-600 rounded-lg bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="">全部</option>
            {field.options?.map(opt => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        );

      case 'date':
        return (
          <input
            type="date"
            value={value || ''}
            onChange={(e) => handleChange(field.key, e.target.value)}
            className="px-3 py-2 border border-slate-200 dark:border-slate-600 rounded-lg bg-white dark:bg-slate-700 text-slate-800 dark:text-white text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        );

      default:
        return null;
    }
  };

  return (
    <div className={`table-filters ${className}`}>
      {/* Filter Toggle */}
      {collapsible && (
        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className="flex items-center gap-2 px-3 py-2 text-sm text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors mb-2"
        >
          <SlidersHorizontal size={16} />
          <span>筛选条件</span>
          {hasActiveFilters && (
            <span className="px-2 py-0.5 bg-blue-100 text-blue-600 rounded-full text-xs">
              {Object.values(values).filter(v => v !== '' && v !== undefined && v !== null).length}
            </span>
          )}
        </button>
      )}

      {/* Filter Fields */}
      {isExpanded && (
        <div className="p-4 bg-slate-50 dark:bg-slate-800 rounded-xl space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {fields.map(field => (
              <div key={field.key} className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-slate-700 dark:text-slate-300">
                  {field.label}
                </label>
                {renderField(field)}
              </div>
            ))}
          </div>

          {/* Actions */}
          <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-200 dark:border-slate-600">
            {hasActiveFilters && (
              <button
                onClick={handleReset}
                className="flex items-center gap-1.5 px-4 py-2 text-sm text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
              >
                <X size={16} />
                重置
              </button>
            )}
            <button
              onClick={onSearch}
              className="flex items-center gap-1.5 px-4 py-2 text-sm text-white bg-blue-600 hover:bg-blue-700 rounded-lg transition-colors"
            >
              <Search size={16} />
              搜索
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default TableFilters;
