// src/components/settings/integration/components/ErpConfigCard.tsx
import React from 'react';
import { Settings, Zap, Activity, ShieldCheck, ChevronRight } from 'lucide-react';
import { ErpConfig } from '@/types';
// import { ScenarioSummaryCard } from './ScenarioSummaryCard';
import { ConnectionHealthBadge } from './ConnectionHealthBadge';

interface ErpConfigCardProps {
  config: ErpConfig;
  status: 'connected' | 'disconnected' | 'error';
  scenarioCount?: number;
  runningCount?: number;
  errorCount?: number;
  healthStatus?: 'healthy' | 'warning' | 'error';
  lastHealthCheck?: string;
  onTest?: (configId: number) => void;
  onDiagnose?: (configId: number) => void;
  onReconcile?: (configId: number) => void;
  onConfig?: (config: ErpConfig) => void;
  onViewDetails?: (configId: number) => void;
}

export function ErpConfigCard({
  config,
  status,
  scenarioCount = 0,
  runningCount = 0,
  errorCount = 0,
  healthStatus,
  lastHealthCheck,
  onTest,
  onDiagnose,
  onReconcile,
  onConfig,
  onViewDetails
}: ErpConfigCardProps) {
  const statusConfig = {
    connected: { text: '已连接', color: 'text-green-600', bg: 'bg-green-50', dot: '●' },
    disconnected: { text: '未连接', color: 'text-gray-500', bg: 'bg-gray-50', dot: '○' },
    error: { text: '异常', color: 'text-red-600', bg: 'bg-red-50', dot: '●' },
  };

  const { text: statusText, color: statusColor, bg: statusBg, dot: statusDot } = statusConfig[status];

  return (
    <div className="bg-white rounded-xl border border-gray-200 hover:border-blue-200 hover:shadow-md transition-all duration-200 overflow-hidden">
      {/* Header Section */}
      <div className="p-4">
        <div className="flex items-start justify-between mb-3">
          <div className="flex items-center gap-3 flex-1">
            <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center flex-shrink-0">
              <Settings size={18} className="text-blue-600" />
            </div>
            <div className="flex-1 min-w-0">
              <h3 className="text-base font-semibold text-gray-900 truncate">{config.name}</h3>
              <div className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium mt-1 ${statusBg} ${statusColor}`}>
                <span>{statusDot}</span>
                <span>{statusText}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Action Bar - Grid layout */}
        <div className="grid grid-cols-2 gap-1.5 mb-2">
          <button
            onClick={() => onConfig?.(config)}
            className="flex items-center justify-center gap-2 px-2.5 py-1.5 text-xs font-medium text-gray-700 bg-gray-50 hover:bg-gray-100 border border-gray-200 rounded-lg transition-colors"
          >
            <Settings size={13} className="text-gray-500 flex-shrink-0" />
            <span className="whitespace-nowrap">配置中心</span>
          </button>

          <button
            onClick={() => onTest?.(config.id)}
            className="flex items-center justify-center gap-2 px-2.5 py-1.5 text-xs font-medium text-gray-700 bg-white hover:bg-blue-50 border border-gray-200 hover:border-blue-200 hover:text-blue-600 rounded-lg transition-colors"
          >
            <Zap size={13} className="text-blue-500 flex-shrink-0" />
            <span className="whitespace-nowrap">检查连接</span>
          </button>

          <button
            onClick={() => onDiagnose?.(config.id)}
            className="flex items-center justify-center gap-2 px-2.5 py-1.5 text-xs font-medium text-white bg-slate-700 hover:bg-slate-600 rounded-lg transition-colors"
          >
            <Activity size={13} className="text-white flex-shrink-0" />
            <span className="whitespace-nowrap">健康检查</span>
          </button>

          <button
            onClick={() => onReconcile?.(config.id)}
            className="flex items-center justify-center gap-2 px-2.5 py-1.5 text-xs font-medium text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg transition-colors"
          >
            <ShieldCheck size={13} className="text-emerald-600 flex-shrink-0" />
            <span className="whitespace-nowrap">账务核对</span>
          </button>
        </div>
      </div>

      {/* Summary Section - Compact */}
      <div className="border-t border-gray-100 p-3 space-y-2">
        {/* Compact Status Row */}
        <div className="flex items-center justify-between text-xs">
          <div className="flex items-center gap-3">
            {/* Health Status */}
            {healthStatus && (
              <ConnectionHealthBadge status={healthStatus} lastCheckTime={lastHealthCheck} />
            )}
            {/* Scenario Count */}
            {scenarioCount > 0 && (
              <span className="text-gray-600">
                场景: {scenarioCount} / 运行{runningCount} / 错误{errorCount}
              </span>
            )}
          </div>
        </div>
        {/* View Details Button */}
        <button
          onClick={() => onViewDetails?.(config.id)}
          className="w-full flex items-center justify-center gap-1.5 px-3 py-1.5 text-xs font-medium text-blue-600 bg-blue-50 hover:bg-blue-100 rounded-lg transition-colors group"
        >
          <span>查看详情</span>
          <ChevronRight size={14} className="group-hover:translate-x-0.5 transition-transform" />
        </button>
      </div>
    </div>
  );
}
