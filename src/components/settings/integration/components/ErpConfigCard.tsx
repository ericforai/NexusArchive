// Input: ErpConfig types, State/Actions interfaces
// Output: ErpConfigCard component with accbook-fonds mapping display
// Pos: src/components/settings/integration/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

// src/components/settings/integration/components/ErpConfigCard.tsx
import React, { useState } from 'react';
import { Settings, Zap, Activity, ShieldCheck, ChevronRight, Building2, ArrowRight, Server, ChevronDown, ChevronUp } from 'lucide-react';
import { Tag } from 'antd';
import { ErpConfig } from '@/types';
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

/**
 * 解析账套-全宗映射
 * 优先从 accbookMapping 字段读取，兼容旧数据从 configJson 解析
 */
function parseAccbookMapping(config: ErpConfig): Record<string, string> {
  // 新版本：使用独立的 accbookMapping 字段
  if (config.accbookMapping) {
    try {
      const parsed = JSON.parse(config.accbookMapping);
      if (typeof parsed === 'object' && parsed !== null) {
        return parsed;
      }
    } catch {
      // 继续尝试从 configJson 读取
    }
  }

  // 兼容旧版本：从 configJson 读取 accbookCodes
  if (config.configJson) {
    try {
      const parsed = JSON.parse(config.configJson);
      // 支持两种格式：
      // 1. accbookCodes 数组: ["BR01", "BR02"]
      // 2. accbookMapping 对象: {"BR01": "FONDS_A", "BR02": "FONDS_B"}
      if (parsed.accbookMapping && typeof parsed.accbookMapping === 'object') {
        return parsed.accbookMapping;
      }
      if (Array.isArray(parsed.accbookCodes) && parsed.accbookCodes.length > 0) {
        // 旧数据没有全宗信息，用账套编码作为全宗编码
        const mapping: Record<string, string> = {};
        parsed.accbookCodes.forEach((code: string) => {
          mapping[code] = code;
        });
        return mapping;
      }
      if (parsed.accbookCode) {
        return { [parsed.accbookCode]: parsed.accbookCode };
      }
    } catch {
      // ignore
    }
  }

  return {};
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
  // 展开/折叠状态
  const [mappingExpanded, setMappingExpanded] = useState(false);

  // 解析账套-全宗映射
  const accbookMapping = parseAccbookMapping(config);
  const mappingEntries = Object.entries(accbookMapping);

  // 显示的映射条目数量（折叠时显示前 3 个，展开时显示全部）
  const DISPLAY_LIMIT = 3;
  const displayedEntries = mappingExpanded ? mappingEntries : mappingEntries.slice(0, DISPLAY_LIMIT);
  const showExpandButton = mappingEntries.length > DISPLAY_LIMIT;

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
              <div className="flex items-center gap-2">
                <h3 className="text-base font-semibold text-gray-900 truncate">{config.name}</h3>
                {/* SAP 接口类型标签 */}
                {config.erpType === 'SAP' && config.sapInterfaceType && (
                  <Tag
                    icon={<Server size={12} />}
                    color="blue"
                    className="text-xs"
                  >
                    {config.sapInterfaceType}
                  </Tag>
                )}
              </div>
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

        {/* SAP 接口类型说明（仅 SAP ERP 显示） */}
        {config.erpType === 'SAP' && config.sapInterfaceType && (
          <div className="flex flex-col gap-1.5 text-xs">
            <div className="flex items-center gap-1.5 text-gray-600">
              <Server size={12} className="text-blue-400" />
              <span>集成接口: {config.sapInterfaceType}</span>
            </div>
            <div className="ml-4 text-gray-500">
              {config.sapInterfaceType === 'ODATA' && 'OData V4 REST 风格集成'}
              {config.sapInterfaceType === 'RFC' && 'RFC/BAPI 传统集成方式（预留）'}
              {config.sapInterfaceType === 'IDOC' && 'IDoc 异步批量交换（预留）'}
              {config.sapInterfaceType === 'GATEWAY' && 'SAP Gateway 自定义服务（预留）'}
            </div>
          </div>
        )}

        {/* Accbook-Fonds Mapping Row */}
        {mappingEntries.length > 0 && (
          <div className="flex flex-col gap-1.5 text-xs">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5 text-gray-600">
                <Building2 size={12} className="text-gray-400" />
                <span>账套-全宗映射 ({mappingEntries.length})</span>
              </div>
              {showExpandButton && (
                <button
                  onClick={() => setMappingExpanded(!mappingExpanded)}
                  className="flex items-center gap-1 px-2 py-0.5 text-blue-600 hover:bg-blue-50 rounded transition-colors"
                >
                  {mappingExpanded ? (
                    <>
                      <span>收起</span>
                      <ChevronUp size={12} />
                    </>
                  ) : (
                    <>
                      <span>更多</span>
                      <ChevronDown size={12} />
                    </>
                  )}
                </button>
              )}
            </div>
            <div className="flex flex-wrap gap-1 ml-4">
              {displayedEntries.map(([accbookCode, fondsCode]) => (
                <div
                  key={accbookCode}
                  className="flex items-center gap-1 px-2 py-0.5 bg-blue-50 text-blue-700 rounded border border-blue-100"
                  title={`${accbookCode} → ${fondsCode}`}
                >
                  <code className="text-xs">{accbookCode}</code>
                  <ArrowRight size={10} className="text-blue-400" />
                  <span className="text-xs font-medium">{fondsCode}</span>
                </div>
              ))}
            </div>
          </div>
        )}

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
