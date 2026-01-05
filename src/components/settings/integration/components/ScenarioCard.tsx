// Input: ErpScenario types, State/Actions interfaces
// Output: ScenarioCard UI component
// Pos: src/components/settings/integration/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

// src/components/settings/integration/components/ScenarioCard.tsx

import React from 'react';
import { ChevronDown, ChevronRight, RefreshCw, Clock, CheckCircle, XCircle, AlertCircle } from 'lucide-react';
import { ErpScenario, ErpSubInterface, SyncHistory } from '../../../../types';
import { ScenarioSyncManagerState, ScenarioSyncManagerActions } from '../types';

interface ScenarioCardProps {
  scenario: ErpScenario;
  state: ScenarioSyncManagerState;
  actions: ScenarioSyncManagerActions;
  onOpenParams: (scenarioId: number) => void;
  onViewHistory: (scenarioId: number) => void;
}

export function ScenarioCard({ scenario, state, actions, onOpenParams, onViewHistory }: ScenarioCardProps) {
  const { expandedScenarios, subInterfaces, syncHistory, showHistoryFor, syncing } = state;

  // DEBUG: Log syncing state
  console.log('[ScenarioCard DEBUG]', {
    scenarioId: scenario.id,
    scenarioName: scenario.name,
    syncingValue: syncing,
    isSyncing: syncing === scenario.id
  });

  const isExpanded = expandedScenarios.has(scenario.id);
  const hasHistory = showHistoryFor === scenario.id;
  const scenarioSubInterfaces = subInterfaces[scenario.id] || [];
  const scenarioHistory = syncHistory[scenario.id] || [];
  const isSyncing = syncing === scenario.id;

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <CheckCircle size={14} className="text-green-600" />;
      case 'FAIL':
        return <XCircle size={14} className="text-red-600" />;
      case 'RUNNING':
        return <AlertCircle size={14} className="text-blue-600 animate-pulse" />;
      default:
        return <Clock size={14} className="text-gray-400" />;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'SUCCESS':
        return <span className="px-2 py-1 text-xs bg-green-50 text-green-700 rounded">成功</span>;
      case 'FAIL':
        return <span className="px-2 py-1 text-xs bg-red-50 text-red-700 rounded">失败</span>;
      case 'RUNNING':
        return <span className="px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded">运行中</span>;
      default:
        return <span className="px-2 py-1 text-xs bg-gray-50 text-gray-700 rounded">未同步</span>;
    }
  };

  return (
    <div className="border rounded-lg p-4 space-y-3">
      {/* Scenario Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button
            onClick={() => actions.toggleScenarioExpansion(scenario.id)}
            className="p-1 hover:bg-gray-100 rounded"
          >
            {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
          </button>
          <div>
            <div className="font-medium">{scenario.name}</div>
            <div className="text-sm text-gray-500">{scenario.description}</div>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {scenario.lastSyncTime && (
            <div className="flex items-center gap-2 text-sm text-gray-500">
              {getStatusIcon(scenario.lastSyncStatus)}
              <span>{new Date(scenario.lastSyncTime).toLocaleString('zh-CN')}</span>
            </div>
          )}
          <button
            onClick={() => onOpenParams(scenario.id)}
            disabled={isSyncing}
            className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-1"
          >
            <RefreshCw size={14} className={isSyncing ? 'animate-spin' : ''} />
            同步
          </button>
          <button
            onClick={() => onViewHistory(scenario.id)}
            className="px-3 py-1.5 text-sm border border-gray-300 rounded hover:bg-gray-50 flex items-center gap-1"
          >
            <Clock size={14} />
            历史
          </button>
        </div>
      </div>

      {/* Sync Strategy Badge */}
      <div className="flex items-center gap-2">
        <span className="text-sm text-gray-500">同步策略:</span>
        {scenario.syncStrategy === 'REALTIME' && (
          <span className="px-2 py-1 text-xs bg-blue-50 text-blue-700 rounded">实时</span>
        )}
        {scenario.syncStrategy === 'CRON' && (
          <span className="px-2 py-1 text-xs bg-purple-50 text-purple-700 rounded">
            定时: {scenario.cronExpression}
          </span>
        )}
        {scenario.syncStrategy === 'MANUAL' && (
          <span className="px-2 py-1 text-xs bg-gray-50 text-gray-700 rounded">手动</span>
        )}
      </div>

      {/* Expanded Content: Sub-interfaces */}
      {isExpanded && scenarioSubInterfaces.length > 0 && (
        <div className="border-t pt-3 space-y-2">
          <div className="text-sm font-medium text-gray-700">子接口列表</div>
          {scenarioSubInterfaces.map(sub => (
            <div key={sub.id} className="flex items-center justify-between p-2 bg-gray-50 rounded">
              <div>
                <div className="text-sm font-medium">{sub.interfaceName}</div>
                {sub.description && (
                  <div className="text-xs text-gray-500">{sub.description}</div>
                )}
              </div>
              <div className="text-xs text-gray-500">{sub.interfaceKey}</div>
            </div>
          ))}
        </div>
      )}

      {/* Sync History */}
      {hasHistory && scenarioHistory.length > 0 && (
        <div className="border-t pt-3 space-y-2">
          <div className="text-sm font-medium text-gray-700">同步历史</div>
          <div className="space-y-2">
            {scenarioHistory.slice(0, 5).map(history => (
              <div key={history.id} className="p-3 bg-gray-50 rounded space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    {getStatusIcon(history.status)}
                    <span className="text-sm">
                      {new Date(history.syncStartTime).toLocaleString('zh-CN')}
                    </span>
                  </div>
                  {getStatusBadge(history.status)}
                </div>
                <div className="grid grid-cols-3 gap-4 text-sm">
                  <div>
                    <span className="text-gray-500">总数: </span>
                    <span className="font-medium">{history.totalCount}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">成功: </span>
                    <span className="font-medium text-green-600">{history.successCount}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">失败: </span>
                    <span className="font-medium text-red-600">{history.failCount}</span>
                  </div>
                </div>
                {history.errorMessage && (
                  <div className="text-sm text-red-600">{history.errorMessage}</div>
                )}
                {history.syncEndTime && (
                  <div className="text-xs text-gray-500">
                    耗时: {new Date(history.syncEndTime).getTime() - new Date(history.syncStartTime).getTime()}ms
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {hasHistory && scenarioHistory.length === 0 && (
        <div className="border-t pt-3 text-sm text-gray-500 text-center">
          暂无同步历史
        </div>
      )}
    </div>
  );
}
