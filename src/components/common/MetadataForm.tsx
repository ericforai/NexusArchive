// Input: React, lucide-react 图标
// Output: MetadataForm 组件
// Pos: 通用复用组件 - 元数据表单组件

import React, { ReactNode } from 'react';
import { RefreshCw, AlertTriangle } from 'lucide-react';
import { MetadataFormField, FieldOption } from './MetadataFormField';

export interface MetadataFormData {
  fiscalYear: string;
  voucherType: string;
  creator: string;
  fondsCode?: string;
  modifyReason: string;
}

export interface MetadataFormConfig {
  name: string;
  label: string;
  required?: boolean;
  type?: 'text' | 'select' | 'textarea' | 'number';
  placeholder?: string;
  options?: FieldOption[];
  pattern?: string;
  rows?: number;
  helperText?: string;
}

export interface MetadataFormProps {
  /** 表单数据 */
  data: MetadataFormData;
  /** 数据变化回调 */
  onChange: (data: MetadataFormData) => void;
  /** 字段配置 */
  fields: MetadataFormConfig[];
  /** 加载状态 */
  loading?: boolean;
  /** 错误信息 */
  error?: string | null;
  /** 提交处理 */
  onSubmit?: (e: React.FormEvent) => void;
  /** 额外操作按钮 */
  actions?: ReactNode;
}

export const MetadataForm: React.FC<MetadataFormProps> = ({
  data,
  onChange,
  fields,
  loading = false,
  error,
  onSubmit,
  actions,
}) => {
  const handleChange = (name: keyof MetadataFormData, value: string) => {
    onChange({ ...data, [name]: value });
  };

  const getSelectedOptionDesc = (fieldName: string): string | undefined => {
    const field = fields.find((f) => f.name === fieldName);
    if (!field?.options) return undefined;
    const option = field.options.find((o) => o.code === data[fieldName as keyof MetadataFormData]);
    return option?.desc;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <RefreshCw className="w-6 h-6 text-blue-500 animate-spin" />
      </div>
    );
  }

  return (
    <form onSubmit={onSubmit} className="space-y-4">
      {fields.map((field) => (
        <MetadataFormField
          key={field.name}
          name={field.name}
          label={field.label}
          required={field.required}
          type={field.type}
          value={data[field.name as keyof MetadataFormData] || ''}
          onChange={(value) => handleChange(field.name as keyof MetadataFormData, value)}
          placeholder={field.placeholder}
          options={field.options}
          selectedOptionDesc={getSelectedOptionDesc(field.name)}
          pattern={field.pattern}
          rows={field.rows}
          helperText={field.helperText}
        />
      ))}

      {error && (
        <div className="flex items-center gap-2 p-3 bg-rose-50 dark:bg-rose-900/20 text-rose-600 dark:text-rose-400 rounded-xl text-sm">
          <AlertTriangle size={16} />
          {error}
        </div>
      )}

      {actions}
    </form>
  );
};

export default MetadataForm;
