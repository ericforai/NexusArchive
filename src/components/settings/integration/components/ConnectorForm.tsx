// src/components/settings/integration/components/ConnectorForm.tsx

import React, { useEffect } from 'react';
import { X, Plus, Trash2, Loader2, CheckCircle, XCircle } from 'lucide-react';
import { ConnectorModalState, ConnectorModalActions } from '../types';

interface ConnectorFormProps {
  state: ConnectorModalState;
  actions: ConnectorModalActions;
}

export function ConnectorForm({ state, actions }: ConnectorFormProps) {
  const {
    show,
    editingConfig,
    configForm,
    newAccbookCode,
    detectedType,
    testing,
  } = state;

  if (!show) return null;

  const isEdit = !!editingConfig?.id;
  const isTesting = testing;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-xl font-semibold">
            {isEdit ? '编辑连接器配置' : '创建连接器配置'}
          </h2>
          <button
            onClick={actions.closeModal}
            className="p-2 hover:bg-gray-100 rounded"
          >
            <X size={20} />
          </button>
        </div>

        {/* Form */}
        <div className="p-6 space-y-4">
          {/* Config Name */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              配置名称 <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={configForm.name}
              onChange={e => actions.updateForm('name', e.target.value)}
              placeholder="例如: 用友YonSuite生产环境"
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* ERP Type */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              ERP类型 <span className="text-red-500">*</span>
            </label>
            <select
              value={configForm.erpType}
              onChange={e => actions.updateForm('erpType', e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="">请选择ERP类型</option>
              <option value="yonsuite">用友 YonSuite</option>
              <option value="kingdee">金蝶云星空</option>
              <option value="weaver">泛微 OA</option>
              <option value="generic">通用 REST API</option>
            </select>
          </div>

          {/* Base URL */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              服务地址 <span className="text-red-500">*</span>
            </label>
            <div className="flex gap-2">
              <input
                type="url"
                value={configForm.baseUrl}
                onChange={e => actions.updateForm('baseUrl', e.target.value)}
                placeholder="https://api.example.com"
                className="flex-1 px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <button
                onClick={() => actions.detectErpType(configForm.baseUrl)}
                disabled={!configForm.baseUrl}
                className="px-4 py-2 text-sm bg-gray-100 hover:bg-gray-200 rounded disabled:opacity-50"
              >
                自动检测
              </button>
            </div>
            {detectedType && (
              <div className="mt-1 text-sm text-green-600 flex items-center gap-1">
                <CheckCircle size={14} />
                检测到: {detectedType}
              </div>
            )}
          </div>

          {/* App Key */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              应用密钥 (App Key) <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              value={configForm.appKey}
              onChange={e => actions.updateForm('appKey', e.target.value)}
              placeholder="您的应用密钥"
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* App Secret */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              应用密钥 (App Secret) <span className="text-red-500">*</span>
            </label>
            <input
              type="password"
              value={configForm.appSecret}
              onChange={e => actions.updateForm('appSecret', e.target.value)}
              placeholder="您的应用密钥"
              className="w-full px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Accbook Codes */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              账套编码
            </label>
            <div className="space-y-2">
              {configForm.accbookCodes.map(code => (
                <div key={code} className="flex items-center gap-2">
                  <input
                    type="text"
                    value={code}
                    readOnly
                    className="flex-1 px-3 py-2 border border-gray-300 rounded bg-gray-50"
                  />
                  <button
                    onClick={() => actions.removeAccbookCode(code)}
                    className="p-2 text-red-600 hover:bg-red-50 rounded"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              ))}
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newAccbookCode}
                  onChange={e => actions.updateForm('newAccbookCode', e.target.value)}
                  placeholder="输入账套编码"
                  className="flex-1 px-3 py-2 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <button
                  onClick={() => actions.addAccbookCode(newAccbookCode)}
                  disabled={!newAccbookCode}
                  className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 flex items-center gap-1"
                >
                  <Plus size={16} />
                  添加
                </button>
              </div>
            </div>
          </div>

          {/* Connection Test */}
          <div className="pt-4 border-t">
            <button
              onClick={actions.testConnection}
              disabled={!configForm.baseUrl || !configForm.appKey || !configForm.appSecret || isTesting}
              className="w-full px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isTesting ? (
                <>
                  <Loader2 size={16} className="animate-spin" />
                  测试连接中...
                </>
              ) : (
                <>
                  <CheckCircle size={16} />
                  测试连接
                </>
              )}
            </button>
          </div>
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-3 p-6 border-t bg-gray-50">
          <button
            onClick={actions.closeModal}
            className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-100"
          >
            取消
          </button>
          <button
            onClick={actions.saveConfig}
            disabled={!configForm.name || !configForm.erpType || !configForm.baseUrl || !configForm.appKey || !configForm.appSecret}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isEdit ? '保存' : '创建'}
          </button>
        </div>
      </div>
    </div>
  );
}
