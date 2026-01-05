import React from 'react';
import { Settings, Zap, Activity, ShieldCheck, Sliders, MoreHorizontal } from 'lucide-react';
import { ErpConfig } from '@/types';

interface ErpConfigCardProps {
  config: ErpConfig;
  status: 'connected' | 'disconnected' | 'error';
  onTest?: (configId: number) => void;
  onDiagnose?: (configId: number) => void;
  onReconcile?: (configId: number) => void;
  onConfig?: (config: ErpConfig) => void;
  onDelete?: (configId: number) => void;
  scenarios?: Array<{
    id: number;
    name: string;
    lastSyncTime?: string;
    recordCount?: number;
  }>;
}

export function ErpConfigCard({
  config,
  status,
  onTest,
  onDiagnose,
  onReconcile,
  onConfig,
  onDelete,
  scenarios = []
}: ErpConfigCardProps) {
  const [showMoreMenu, setShowMoreMenu] = React.useState(false);

  const statusConfig = {
    connected: { text: '已连接', color: 'text-green-600', dot: '●' },
    disconnected: { text: '未连接', color: 'text-gray-400', dot: '○' },
    error: { text: '连接异常', color: 'text-red-600', dot: '●' },
  };

  const { text: statusText, color: statusColor, dot: statusDot } = statusConfig[status];

  return (
    <div className="bg-white rounded-xl border border-slate-200 shadow-sm hover:shadow-md transition-shadow">
      {/* Card Header */}
      <div className="h-[60px] px-4 border-b border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
            <Settings size={20} className="text-blue-600" />
          </div>
          <div>
            <h3 className="text-base font-semibold text-slate-800">{config.name}</h3>
            <p className={`text-xs ${statusColor} flex items-center gap-1`}>
              <span>{statusDot}</span>
              <span>{statusText}</span>
            </p>
          </div>
        </div>
      </div>

      {/* Action Bar - INSIDE the card as requested */}
      <div className="h-[48px] px-4 border-b border-slate-100 flex items-center gap-2">
        <button
          onClick={() => onConfig?.(config)}
          className="h-8 px-3 text-xs font-medium text-slate-600 bg-white border border-slate-200 rounded-lg hover:bg-slate-50 transition-colors flex items-center gap-1.5"
        >
          <Settings size={14} className="text-slate-400" />
          配置中心
        </button>
        <button
          onClick={() => onTest?.(config.id)}
          className="h-8 px-3 text-xs font-medium text-slate-600 bg-white border border-slate-200 rounded-lg hover:bg-blue-50 hover:text-blue-600 hover:border-blue-100 transition-colors flex items-center gap-1.5"
        >
          <Zap size={14} className="text-blue-500" />
          检查连接
        </button>
        <button
          onClick={() => onDiagnose?.(config.id)}
          className="h-8 px-3 text-xs font-medium text-white bg-slate-800 rounded-lg hover:bg-slate-700 transition-colors flex items-center gap-1.5"
        >
          <Activity size={14} />
          健康检查
        </button>
        <button
          onClick={() => onReconcile?.(config.id)}
          className="h-8 px-3 text-xs font-medium text-emerald-700 bg-emerald-50 border border-emerald-100 rounded-lg hover:bg-emerald-100 transition-colors flex items-center gap-1.5"
        >
          <ShieldCheck size={14} />
          账务核对
        </button>

        {/* More Menu with Delete Option */}
        <div className="ml-auto relative">
          <button
            onClick={() => setShowMoreMenu(!showMoreMenu)}
            className="h-8 w-8 flex items-center justify-center text-slate-400 hover:text-slate-600 hover:bg-slate-50 rounded-lg transition-colors"
          >
            <MoreHorizontal size={16} />
          </button>

          {showMoreMenu && (
            <div className="absolute right-0 top-full z-50 mt-1 bg-white rounded-lg shadow-lg border border-slate-100 py-1 min-w-[160px]">
              <button
                onClick={() => {
                  onConfig?.(config);
                  setShowMoreMenu(false);
                }}
                className="w-full px-3 py-2 text-left text-sm text-slate-700 hover:bg-slate-50 flex items-center gap-2"
              >
                <Sliders size={14} />
                编辑配置
              </button>
              <button
                onClick={() => {
                  onDelete?.(config.id);
                  setShowMoreMenu(false);
                }}
                className="w-full px-3 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
              >
                移除此连接器
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Scenario List */}
      <div className="p-4">
        {scenarios.length === 0 ? (
          <div className="py-8 text-center text-slate-400 text-sm">
            暂无同步场景
          </div>
        ) : (
          <div className="space-y-2">
            {scenarios.map((scenario) => (
              <div
                key={scenario.id}
                className="p-3 bg-slate-50 rounded-lg border border-slate-100"
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-slate-700">{scenario.name}</span>
                  {scenario.recordCount !== undefined && (
                    <span className="text-xs text-slate-500">
                      已同步 {scenario.recordCount} 条
                    </span>
                  )}
                </div>
                {scenario.lastSyncTime && (
                  <p className="text-xs text-slate-400 mt-1">
                    最后同步: {new Date(scenario.lastSyncTime).toLocaleString('zh-CN')}
                  </p>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
