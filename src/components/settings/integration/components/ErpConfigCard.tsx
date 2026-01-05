// src/components/settings/integration/components/ErpConfigCard.tsx
import React, { useRef, useEffect, useState, useCallback } from 'react';
import { Settings, Zap, Activity, ShieldCheck, Sliders, MoreHorizontal, ChevronDown, ChevronRight } from 'lucide-react';
import { ErpConfig } from '@/types';

interface ErpConfigCardProps {
  config: ErpConfig;
  status: 'connected' | 'disconnected' | 'error';
  scenarioCount?: number;
  scenarios?: Array<{
    id: number;
    name: string;
    lastSyncTime?: string;
    recordCount?: number;
  }>;
  loadingScenarios?: boolean;
  onTest?: (configId: number) => void;
  onDiagnose?: (configId: number) => void;
  onReconcile?: (configId: number) => void;
  onConfig?: (config: ErpConfig) => void;
  onDelete?: (configId: number) => void;
  onLoadScenarios?: (configId: number) => void;
}

export function ErpConfigCard({
  config,
  status,
  scenarioCount = 0,
  scenarios = [],
  loadingScenarios = false,
  onTest,
  onDiagnose,
  onReconcile,
  onConfig,
  onDelete,
  onLoadScenarios
}: ErpConfigCardProps) {
  const [showMoreMenu, setShowMoreMenu] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({ name: config.name });
  const [scenariosExpanded, setScenariosExpanded] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setShowMoreMenu(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const formatSyncTime = (dateString?: string) => {
    if (!dateString) return null;
    try {
      return new Date(dateString).toLocaleString('zh-CN');
    } catch {
      return '无效日期';
    }
  };

  const statusConfig = {
    connected: { text: '已连接', color: 'text-green-600', bg: 'bg-green-50', dot: '●' },
    disconnected: { text: '未连接', color: 'text-gray-500', bg: 'bg-gray-50', dot: '○' },
    error: { text: '异常', color: 'text-red-600', bg: 'bg-red-50', dot: '●' },
  };

  const { text: statusText, color: statusColor, bg: statusBg, dot: statusDot } = statusConfig[status];

  const handleToggleScenarios = useCallback(() => {
    if (!scenariosExpanded && scenarios.length === 0 && onLoadScenarios) {
      // First time expanding - load scenarios
      onLoadScenarios(config.id);
    }
    setScenariosExpanded(!scenariosExpanded);
  }, [scenariosExpanded, scenarios.length, onLoadScenarios, config.id]);

  return (
    <div className="bg-white rounded-xl border border-gray-200 hover:border-blue-200 hover:shadow-md transition-all duration-200 overflow-hidden">
      {/* Header Section */}
      <div className="p-5 border-b border-gray-100">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3 flex-1">
            <div className="w-11 h-11 rounded-lg bg-blue-50 flex items-center justify-center flex-shrink-0">
              <Settings size={20} className="text-blue-600" />
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
        <div className="grid grid-cols-2 gap-2">
          <button
            onClick={() => setIsEditing(!isEditing)}
            className="flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium text-gray-700 bg-gray-50 hover:bg-gray-100 border border-gray-200 rounded-lg transition-colors"
          >
            <Settings size={14} className="text-gray-500 flex-shrink-0" />
            <span>配置中心</span>
          </button>

          <button
            onClick={() => onTest?.(config.id)}
            className="flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium text-gray-700 bg-white hover:bg-blue-50 border border-gray-200 hover:border-blue-200 hover:text-blue-600 rounded-lg transition-colors"
          >
            <Zap size={14} className="text-blue-500 flex-shrink-0" />
            <span>检查连接</span>
          </button>

          <button
            onClick={() => onDiagnose?.(config.id)}
            className="flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium text-gray-700 bg-gray-900 hover:bg-gray-800 rounded-lg transition-colors"
          >
            <Activity size={14} className="text-gray-300 flex-shrink-0" />
            <span>健康检查</span>
          </button>

          <button
            onClick={() => onReconcile?.(config.id)}
            className="flex items-center justify-center gap-2 px-3 py-2 text-xs font-medium text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg transition-colors"
          >
            <ShieldCheck size={14} className="text-emerald-600 flex-shrink-0" />
            <span>账务核对</span>
          </button>
        </div>

        {/* More Menu */}
        <div className="relative mt-2" ref={menuRef}>
          <button
            onClick={() => setShowMoreMenu(!showMoreMenu)}
            aria-label="更多选项"
            aria-expanded={showMoreMenu}
            aria-haspopup="true"
            className="absolute right-0 top-0 h-8 w-8 flex items-center justify-center text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <MoreHorizontal size={16} />
          </button>

          {showMoreMenu && (
            <div
              className="absolute right-0 top-full z-50 mt-1 bg-white rounded-lg shadow-lg border border-gray-200 py-1 min-w-[140px]"
              role="menu"
            >
              <button
                role="menuitem"
                onClick={() => {
                  setIsEditing(true);
                  setShowMoreMenu(false);
                }}
                className="w-full px-3 py-2 text-left text-sm text-gray-700 hover:bg-gray-50 flex items-center gap-2"
              >
                <Sliders size={14} className="text-gray-500" />
                <span>编辑配置</span>
              </button>
              <button
                role="menuitem"
                onClick={() => {
                  onDelete?.(config.id);
                  setShowMoreMenu(false);
                }}
                className="w-full px-3 py-2 text-left text-sm text-red-600 hover:bg-red-50 flex items-center gap-2"
              >
                <span>移除此连接器</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Inline Edit Form */}
      {isEditing && (
        <div className="p-5 bg-blue-50 border-b border-blue-100" data-testid="inline-config-form">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">连接器名称</label>
              <input
                type="text"
                value={editForm.name}
                onChange={(e) => setEditForm({ name: e.target.value })}
                className="w-full px-3 py-2.5 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="输入连接器名称"
              />
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => {
                  onConfig?.({ ...config, name: editForm.name });
                  setIsEditing(false);
                }}
                className="flex-1 px-4 py-2.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors"
              >
                保存
              </button>
              <button
                onClick={() => setIsEditing(false)}
                className="flex-1 px-4 py-2.5 bg-white border border-gray-300 text-gray-700 text-sm font-medium rounded-lg hover:bg-gray-50 transition-colors"
              >
                取消
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Scenarios Section - Lazy Loading */}
      <div className="border-t border-gray-100">
        {/* Collapsed State - Show Count */}
        {!scenariosExpanded && (
          <button
            onClick={handleToggleScenarios}
            className="w-full px-5 py-4 flex items-center justify-between hover:bg-gray-50 transition-colors group"
          >
            <div className="flex items-center gap-2">
              <span className="text-sm text-gray-700 font-medium">场景</span>
              {scenarioCount > 0 && (
                <span className="inline-flex items-center px-2 py-0.5 bg-blue-50 text-blue-700 text-xs font-medium rounded-full">
                  {scenarioCount} 个
                </span>
              )}
            </div>
            <div className="flex items-center gap-1 text-gray-400 group-hover:text-gray-600">
              <ChevronRight size={16} />
              <span className="text-xs text-gray-500">点击展开</span>
            </div>
          </button>
        )}

        {/* Expanded State - Show List */}
        {scenariosExpanded && (
          <div className="p-5">
            {/* Loading State */}
            {loadingScenarios ? (
              <div className="flex items-center justify-center py-8">
                <div className="animate-spin rounded-full h-6 w-6 border-2 border-blue-600 border-t-transparent"></div>
                <span className="ml-3 text-sm text-gray-500">加载场景中...</span>
              </div>
            ) : scenarios.length === 0 ? (
              <div className="text-center py-8">
                <p className="text-sm text-gray-500">暂无同步场景</p>
              </div>
            ) : (
              <div className="space-y-3">
                <button
                  onClick={() => setScenariosExpanded(false)}
                  className="w-full flex items-center justify-between mb-3 text-sm text-gray-600 hover:text-gray-900 pb-2 border-b border-gray-200"
                >
                  <span>收起场景列表</span>
                  <ChevronDown size={16} />
                </button>
                {scenarios.map((scenario) => (
                  <div
                    key={scenario.id}
                    className="p-4 bg-gray-50 rounded-lg border border-gray-100 hover:border-gray-200 transition-colors"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <h4 className="text-sm font-medium text-gray-900">{scenario.name}</h4>
                      {scenario.recordCount !== undefined && (
                        <span className="text-xs font-medium text-blue-600 bg-blue-50 px-2 py-0.5 rounded-full">
                          已同步 {scenario.recordCount} 条
                        </span>
                      )}
                    </div>
                    {scenario.lastSyncTime && (
                      <p className="text-xs text-gray-500">
                        最后同步: {formatSyncTime(scenario.lastSyncTime)}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
