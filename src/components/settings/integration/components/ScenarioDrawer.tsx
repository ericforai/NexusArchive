import React from 'react';
import { Drawer, Button } from 'antd';
import { X } from 'lucide-react';
import { Scenario } from '@/types';

interface ScenarioDrawerProps {
  visible: boolean;
  configName: string;
  scenarios: Scenario[];
  onClose: () => void;
  onSync?: (scenarioId: number) => void;
}

export function ScenarioDrawer({
  visible,
  configName,
  scenarios,
  onClose,
  onSync
}: ScenarioDrawerProps) {
  const formatSyncTime = (dateString?: string) => {
    if (!dateString) return null;
    try {
      return new Date(dateString).toLocaleString('zh-CN');
    } catch {
      return '无效日期';
    }
  };

  const getStatusConfig = (status: Scenario['status']) => {
    switch (status) {
      case 'running':
        return { text: '运行中', color: 'text-blue-600', bg: 'bg-blue-50', dot: '●' };
      case 'success':
        return { text: '成功', color: 'text-green-600', bg: 'bg-green-50', dot: '●' };
      case 'error':
        return { text: '异常', color: 'text-red-600', bg: 'bg-red-50', dot: '●' };
      default:
        return { text: '空闲', color: 'text-gray-500', bg: 'bg-gray-50', dot: '○' };
    }
  };

  return (
    <Drawer
      title={
        <div className="flex items-center justify-between">
          <span className="text-lg font-semibold">{configName} 场景列表</span>
          <Button
            type="text"
            icon={<X size={18} />}
            onClick={onClose}
            className="hover:bg-gray-100"
          />
        </div>
      }
      placement="right"
      width={480}
      open={visible}
      onClose={onClose}
      styles={{
        body: { padding: '16px' },
      }}
    >
      <div className="space-y-3">
        {scenarios.map((scenario) => {
          const statusConfig = getStatusConfig(scenario.status);
          return (
            <div
              key={scenario.id}
              className="p-4 bg-white rounded-lg border border-gray-200 hover:border-blue-300 transition-colors"
            >
              <div className="flex items-center justify-between mb-2">
                <h4 className="text-sm font-medium text-gray-900">{scenario.name}</h4>
                <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium ${statusConfig.bg} ${statusConfig.color}`}>
                  <span>{statusConfig.dot}</span>
                  <span>{statusConfig.text}</span>
                </span>
              </div>
              {scenario.lastSyncTime && (
                <p className="text-xs text-gray-500 mb-3">
                  最后同步: {formatSyncTime(scenario.lastSyncTime)}
                </p>
              )}
              {onSync && (
                <Button
                  size="small"
                  type="primary"
                  onClick={() => onSync(scenario.id)}
                  disabled={scenario.status === 'running'}
                  className="w-full"
                >
                  {scenario.status === 'running' ? '同步中...' : '立即同步'}
                </Button>
              )}
            </div>
          );
        })}
      </div>
    </Drawer>
  );
}
