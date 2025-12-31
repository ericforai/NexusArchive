// Input: React
// Output: MetadataFormField 组件
// Pos: 通用复用组件 - 元数据字段输入组件

import React from 'react';

export interface FieldOption {
  code: string;
  label: string;
  desc?: string;
}

export interface MetadataFormFieldProps {
  /** 字段名称 */
  name: string;
  /** 字段标签 */
  label: string;
  /** 是否必填 */
  required?: boolean;
  /** 字段类型 */
  type?: 'text' | 'select' | 'textarea' | 'number';
  /** 字段值 */
  value: string;
  /** 值变化回调 */
  onChange: (value: string) => void;
  /** 占位符 */
  placeholder?: string;
  /** Select 选项 */
  options?: FieldOption[];
  /** 选中选项的描述 */
  selectedOptionDesc?: string;
  /** 输入模式 */
  pattern?: string;
  /** 行数（textarea） */
  rows?: number;
  /** 辅助文本 */
  helperText?: string;
}

export const MetadataFormField: React.FC<MetadataFormFieldProps> = ({
  name,
  label,
  required = false,
  type = 'text',
  value,
  onChange,
  placeholder,
  options,
  selectedOptionDesc,
  pattern,
  rows = 2,
  helperText,
}) => {
  const baseClassName =
    'w-full px-4 py-2.5 border border-slate-200 dark:border-slate-600 rounded-xl bg-white dark:bg-slate-700 text-slate-800 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all';

  const renderInput = () => {
    switch (type) {
      case 'select':
        return (
          <select
            name={name}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            className={baseClassName}
          >
            {options?.map((opt) => (
              <option key={opt.code} value={opt.code}>
                {opt.code} - {opt.label}
              </option>
            ))}
          </select>
        );

      case 'textarea':
        return (
          <textarea
            name={name}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
            rows={rows}
            className={`${baseClassName} resize-none`}
          />
        );

      case 'number':
        return (
          <input
            type="number"
            name={name}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
            className={baseClassName}
          />
        );

      default:
        return (
          <input
            type="text"
            name={name}
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
            pattern={pattern}
            className={baseClassName}
          />
        );
    }
  };

  return (
    <div>
      <label className="block text-sm font-medium text-slate-700 dark:text-slate-300 mb-1">
        {label} {required && <span className="text-rose-500">*</span>}
        {helperText && <span className="ml-2 text-xs text-slate-400">({helperText})</span>}
      </label>
      {renderInput()}
      {selectedOptionDesc && (
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
          {selectedOptionDesc}
        </p>
      )}
    </div>
  );
};

export default MetadataFormField;
