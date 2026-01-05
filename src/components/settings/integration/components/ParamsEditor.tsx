// src/components/settings/integration/components/ParamsEditor.tsx

import React from 'react';
import { X, Calendar, Loader2 } from 'lucide-react';
import { ParamsEditorState, ParamsEditorActions } from '../types';

interface ParamsEditorProps {
  state: ParamsEditorState;
  actions: ParamsEditorActions;
}

export function ParamsEditor({ state, actions }: ParamsEditorProps) {
  const { showFor, pendingSyncId, form } = state;

  if (showFor === null) return null;

  const isSubmitting = pendingSyncId !== null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-md">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-xl font-semibold">同步参数配置</h2>
          <button
            onClick={actions.closeEditor}
            className="p-2 hover:bg-gray-100 rounded"
          >
            <X size={20} />
          </button>
        </div>

        {/* Form */}
        <div className="p-6 space-y-4">
          {/* Start Date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              开始日期 <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <input
                type="date"
                value={form.startDate}
                onChange={e => actions.updateForm('startDate', e.target.value)}
                className="w-full px-3 py-2 pr-10 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <Calendar size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
            </div>
          </div>

          {/* End Date */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              结束日期 <span className="text-red-500">*</span>
            </label>
            <div className="relative">
              <input
                type="date"
                value={form.endDate}
                onChange={e => actions.updateForm('endDate', e.target.value)}
                className="w-full px-3 py-2 pr-10 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <Calendar size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none" />
            </div>
          </div>

          {/* Page Size */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              分页大小 <span className="text-red-500">*</span>
            </label>
            <input
              type="number"
              min={1}
              max={1000}
              value={form.pageSize}
              onChange={e => actions.updateForm('pageSize', parseInt(e.target.value))}
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
            <div className="mt-1 text-xs text-gray-500">
              建议值: 100-500,过大可能导致内存溢出
            </div>
          </div>

          {/* Date Range Info */}
          {form.startDate && form.endDate && (
            <div className="p-3 bg-blue-50 rounded text-sm">
              <div className="font-medium text-blue-700 mb-1">日期范围</div>
              <div className="text-blue-600">
                {new Date(form.startDate).toLocaleDateString('zh-CN')} 至 {new Date(form.endDate).toLocaleDateString('zh-CN')}
              </div>
              <div className="text-blue-600 mt-1">
                共 {Math.ceil((new Date(form.endDate).getTime() - new Date(form.startDate).getTime()) / (1000 * 60 * 60 * 24))} 天
              </div>
            </div>
          )}

          {/* Validation Warning */}
          {form.startDate && form.endDate && new Date(form.startDate) > new Date(form.endDate) && (
            <div className="p-3 bg-red-50 rounded text-sm text-red-600">
              开始日期不能晚于结束日期
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 p-6 border-t bg-gray-50">
          <button
            onClick={actions.closeEditor}
            disabled={isSubmitting}
            className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-100 disabled:opacity-50"
          >
            取消
          </button>
          <button
            onClick={actions.submitSync}
            disabled={
              !form.startDate ||
              !form.endDate ||
              !form.pageSize ||
              new Date(form.startDate) > new Date(form.endDate) ||
              isSubmitting
            }
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
          >
            {isSubmitting ? (
              <>
                <Loader2 size={16} className="animate-spin" />
                同步中...
              </>
            ) : (
              '开始同步'
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
