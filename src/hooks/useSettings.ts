// Input: React
// Output: useSettings Hook
// Pos: 通用复用组件 - 设置管理 Hook
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback, useMemo } from 'react';

export interface SettingsValue<T = any> {
  value: T;
  defaultValue: T;
  category: string;
  key: string;
  label: string;
  description?: string;
  type: 'text' | 'number' | 'boolean' | 'select' | 'multiselect';
  options?: Array<{ label: string; value: any }>;
  required?: boolean;
  validator?: (value: T) => string | null;
}

export interface UseSettingsOptions<T = any> {
  initialValues?: Record<string, T>;
  onSave?: (values: Record<string, T>) => Promise<void>;
  autoSave?: boolean;
}

export interface UseSettingsReturn<T = any> {
  values: Record<string, T>;
  errors: Record<string, string>;
  isDirty: boolean;
  isSaving: boolean;
  setValue: (key: string, value: T) => void;
  setValues: (values: Record<string, T>) => void;
  resetValue: (key: string) => void;
  resetAll: () => void;
  save: () => Promise<void>;
  validate: () => boolean;
}

/**
 * 设置管理 Hook
 * <p>
 * 封装设置页面的状态管理、验证和保存逻辑
 * 修复：使用 useMemo 稳定返回值引用
 * </p>
 */
export function useSettings<T = any>({
  initialValues = {} as Record<string, T>,
  onSave,
  autoSave: _autoSave = false,
}: UseSettingsOptions<T> = {}): UseSettingsReturn<T> {
  const [values, setValues] = useState<Record<string, T>>(initialValues);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isDirty, setIsDirty] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  const setValue = useCallback((key: string, value: T) => {
    setValues(prev => ({ ...prev, [key]: value }));
    setErrors(prev => ({ ...prev, [key]: '' }));
    setIsDirty(true);
  }, []);

  const setValuesMultiple = useCallback((newValues: Record<string, T>) => {
    setValues(prev => ({ ...prev, ...newValues }));
    setIsDirty(true);
  }, []);

  const resetValue = useCallback((key: string) => {
    const defaultValue = initialValues[key];
    setValues(prev => ({ ...prev, [key]: defaultValue }));
    setErrors(prev => ({ ...prev, [key]: '' }));
  }, [initialValues]);

  const resetAll = useCallback(() => {
    setValues(initialValues);
    setErrors({});
    setIsDirty(false);
  }, [initialValues]);

  const validate = useCallback((): boolean => {
    const newErrors: Record<string, string> = {};
    let isValid = true;

    // TODO: Add validation logic based on settings config
    for (const [key, value] of Object.entries(values)) {
      if (value === null || value === undefined || value === '') {
        newErrors[key] = '此字段不能为空';
        isValid = false;
      }
    }

    setErrors(newErrors);
    return isValid;
  }, [values]);

  const save = useCallback(async () => {
    if (!validate()) {
      return;
    }

    setIsSaving(true);
    try {
      await onSave?.(values);
      setIsDirty(false);
    } catch (error) {
      console.error('Failed to save settings:', error);
      throw error;
    } finally {
      setIsSaving(false);
    }
  }, [values, validate, onSave]);

  // 使用 useMemo 稳定返回值引用
  return useMemo(() => ({
    values,
    errors,
    isDirty,
    isSaving,
    setValue,
    setValues: setValuesMultiple,
    resetValue,
    resetAll,
    save,
    validate,
  }), [values, errors, isDirty, isSaving, setValue, setValuesMultiple, resetValue, resetAll, save, validate]);
}

export default useSettings;
