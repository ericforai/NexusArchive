// src/components/settings/integration/components/ErpConfigList.tsx

import React from 'react';
import { ChevronDown, ChevronRight, Database, Zap, Settings } from 'lucide-react';
import { ErpConfig } from '../../../../types';
import { ErpConfigManagerState, ErpConfigManagerActions } from '../types';

const ADAPTER_CONFIG: Record<string, { icon: React.ReactNode; color: string; label: string }> = {
  yonsuite: { icon: <Database size={16} />, color: 'text-blue-600 bg-blue-50', label: '用友 YonSuite' },
  kingdee: { icon: <Zap size={16} />, color: 'text-blue-600 bg-blue-50', label: '金蝶云星空' },
  weaver: { icon: <Settings size={16} />, color: 'text-slate-600 bg-slate-50', label: '泛微 OA' },
  generic: { icon: <Settings size={16} />, color: 'text-slate-600 bg-slate-50', label: '通用 REST' },
};

interface ErpConfigListProps {
  state: ErpConfigManagerState;
  actions: ErpConfigManagerActions;
  onSelectConfig: (config: ErpConfig) => void;
}

export function ErpConfigList({ state, actions, onSelectConfig }: ErpConfigListProps) {
  const { configs, adapterTypes, expandedTypes, activeConfigId, loading } = state;

  if (loading) {
    return <div className="flex justify-center p-8"><div className="animate-spin" /></div>;
  }

  return (
    <div className="space-y-4">
      {adapterTypes.map(type => {
        const typeConfigs = configs.filter(c => (c.erpType?.toLowerCase() || 'generic') === type);
        const isExpanded = expandedTypes.has(type);
        const config = ADAPTER_CONFIG[type] || ADAPTER_CONFIG.generic;

        return (
          <div key={type} className="border rounded-lg">
            <button
              onClick={() => actions.toggleTypeExpansion(type)}
              className="w-full flex items-center justify-between p-4 hover:bg-gray-50"
            >
              <div className="flex items-center gap-2">
                {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                <span className={`p-2 rounded ${config.color}`}>{config.icon}</span>
                <span className="font-medium">{config.label}</span>
                <span className="text-sm text-gray-500">({typeConfigs.length})</span>
              </div>
            </button>

            {isExpanded && (
              <div className="border-t p-2">
                {typeConfigs.map(config => {
                  // Parse baseUrl from configJson
                  const configData = JSON.parse(config.configJson || '{}');
                  const baseUrl = configData.baseUrl || configData.endpoint || '未配置';

                  return (
                    <div
                      key={config.id}
                      onClick={() => {
                        actions.setActiveConfig(config.id);
                        onSelectConfig(config);
                      }}
                      className={`p-3 rounded cursor-pointer hover:bg-gray-50 ${
                        activeConfigId === config.id ? 'bg-blue-50' : ''
                      }`}
                    >
                      <div className="font-medium">{config.name}</div>
                      <div className="text-sm text-gray-500">{baseUrl}</div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
