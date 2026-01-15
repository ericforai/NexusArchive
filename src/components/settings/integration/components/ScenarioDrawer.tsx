// Input: React, Ant Design Drawer, scenarios data, sync callback
// Output: Right-side slide-in drawer component for scenario detail view
// Pos: src/components/settings/integration/components/
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect } from 'react';
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

  // Auto-scroll to make drawer visible when it opens
  useEffect(() => {
    if (visible) {
      // Small delay to ensure drawer is rendered
      setTimeout(() => {
        // Scroll to make the drawer visible
        window.scrollTo({
          top: 0,
          behavior: 'smooth'
        });
      }, 100);
    }
  }, [visible]);

  const formatSyncTime = (dateString?: string | number[]) => {
    if (!dateString) return null;
    try {
      // Handle array format [yyyy, MM, dd, HH, mm, ss] (Jackson default for LocalDateTime)
      if (Array.isArray(dateString)) {
        if (dateString.length >= 3) {
          const [year, month, day, hour = 0, minute = 0, second = 0] = dateString;
          // Note: month is 1-indexed in Java/Jackson array, but 0-indexed in JS Date constructor.
          // Using string interpolation to ensure correct ISO parsing or manual formatting is safer.
          // Let's use manual string construction to be safe across browsers.
          return `${year}/${month}/${day} ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}:${second.toString().padStart(2, '0')}`;
        }
        return '无效日期';
      }

      if (typeof dateString !== 'string') return '无效日期';

      // Fix for Safari/Firefox which might not support "YYYY-MM-DD HH:mm:ss" directly
      const normalizedDate = dateString.replace(' ', 'T');
      const date = new Date(normalizedDate);

      // Check for Invalid Date
      if (isNaN(date.getTime())) {
        // Try fallback for simple SQL timestamp without replacing (some browsers might prefer space)
        const fallbackDate = new Date(dateString);
        if (!isNaN(fallbackDate.getTime())) {
          return fallbackDate.toLocaleString('zh-CN');
        }
        return '无效日期';
      }

      return date.toLocaleString('zh-CN');
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
      size="default"
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
