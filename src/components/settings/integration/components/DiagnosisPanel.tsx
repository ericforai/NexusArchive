// Input: Diagnosis State/Actions interfaces
// Output: DiagnosisPanel modal component
// Pos: src/components/settings/integration/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

// src/components/settings/integration/components/DiagnosisPanel.tsx

import React from 'react';
import { X, CheckCircle, XCircle, AlertCircle, Loader2 } from 'lucide-react';
import { IntegrationDiagnosisResult } from '../../../../types';
import { DiagnosisState, DiagnosisActions } from '../types';

interface DiagnosisPanelProps {
  state: DiagnosisState;
  actions: DiagnosisActions;
}

export function DiagnosisPanel({ state, actions }: DiagnosisPanelProps) {
  const { show, diagnosing, result } = state;

  if (!show) return null;

  const getStepIcon = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <CheckCircle size={20} className="text-green-600" />;
      case 'FAIL':
        return <XCircle size={20} className="text-red-600" />;
      case 'WARNING':
        return <AlertCircle size={20} className="text-yellow-600" />;
      default:
        return <Loader2 size={20} className="text-gray-400 animate-spin" />;
    }
  };

  const getStepBadge = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <span className="px-2 py-1 text-xs bg-green-50 text-green-700 rounded">通过</span>;
      case 'FAIL':
        return <span className="px-2 py-1 text-xs bg-red-50 text-red-700 rounded">失败</span>;
      case 'WARNING':
        return <span className="px-2 py-1 text-xs bg-yellow-50 text-yellow-700 rounded">警告</span>;
      default:
        return <span className="px-2 py-1 text-xs bg-gray-50 text-gray-700 rounded">待检查</span>;
    }
  };

  const getOverallStatus = () => {
    if (!result) return null;
    const hasFailures = result.steps.some(s => s.status === 'FAIL');
    const hasWarnings = result.steps.some(s => s.status === 'WARNING');

    if (hasFailures) {
      return {
        icon: <XCircle size={24} className="text-red-600" />,
        text: '诊断失败',
        badge: 'bg-red-50 text-red-700',
        message: '发现严重问题,请查看详细信息'
      };
    } else if (hasWarnings) {
      return {
        icon: <AlertCircle size={24} className="text-yellow-600" />,
        text: '发现警告',
        badge: 'bg-yellow-50 text-yellow-700',
        message: '存在潜在问题,建议查看详情'
      };
    } else {
      return {
        icon: <CheckCircle size={24} className="text-green-600" />,
        text: '诊断通过',
        badge: 'bg-green-50 text-green-700',
        message: '所有检查项正常'
      };
    }
  };

  const overallStatus = getOverallStatus();

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b">
          <h2 className="text-xl font-semibold">集成诊断</h2>
          <button
            onClick={actions.closeDiagnosis}
            className="p-2 hover:bg-gray-100 rounded"
          >
            <X size={20} />
          </button>
        </div>

        {/* Content */}
        <div className="p-6">
          {diagnosing && (
            <div className="flex flex-col items-center justify-center py-12 space-y-4">
              <Loader2 size={48} className="text-blue-600 animate-spin" />
              <div className="text-lg font-medium">正在执行诊断...</div>
              <div className="text-sm text-gray-500">这可能需要几秒钟</div>
            </div>
          )}

          {!diagnosing && !result && (
            <div className="flex flex-col items-center justify-center py-12 space-y-4">
              <AlertCircle size={48} className="text-gray-400" />
              <div className="text-lg font-medium">未执行诊断</div>
              <button
                onClick={actions.startDiagnosis}
                className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
              >
                开始诊断
              </button>
            </div>
          )}

          {!diagnosing && result && (
            <div className="space-y-6">
              {/* Overall Status */}
              <div className="p-4 bg-gray-50 rounded-lg">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    {overallStatus?.icon}
                    <div>
                      <div className="font-medium">{overallStatus?.text}</div>
                      <div className="text-sm text-gray-500">{overallStatus?.message}</div>
                    </div>
                  </div>
                  <span className={`px-3 py-1 text-sm rounded ${overallStatus?.badge}`}>
                    {result.status}
                  </span>
                </div>
                <div className="mt-3 grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-gray-500">配置名称: </span>
                    <span className="font-medium">{result.configName}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">ERP类型: </span>
                    <span className="font-medium">{result.erpType}</span>
                  </div>
                </div>
              </div>

              {/* Diagnosis Steps */}
              <div className="space-y-3">
                <h3 className="font-medium">诊断步骤</h3>
                {result.steps.map((step, index) => (
                  <div key={index} className="border rounded-lg p-4">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        {getStepIcon(step.status)}
                        <span className="font-medium">{step.name}</span>
                      </div>
                      {getStepBadge(step.status)}
                    </div>
                    <div className="text-sm text-gray-600">{step.message}</div>
                    {step.detail && (
                      <div className="mt-2 p-3 bg-gray-50 rounded text-sm">
                        <div className="font-medium text-gray-700 mb-1">详细信息</div>
                        <div className="text-gray-600 whitespace-pre-wrap">{step.detail}</div>
                      </div>
                    )}
                  </div>
                ))}
              </div>

              {/* Actions */}
              <div className="flex justify-end gap-3 pt-4 border-t">
                <button
                  onClick={actions.closeDiagnosis}
                  className="px-4 py-2 border border-gray-300 rounded hover:bg-gray-100"
                >
                  关闭
                </button>
                <button
                  onClick={actions.startDiagnosis}
                  className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
                >
                  重新诊断
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
