// src/components/settings/integration/components/ErpConfigList.tsx
// Displays ERP connector configurations in a responsive grid layout

import React from 'react';
import { ErpConfigCard } from './ErpConfigCard';
import { ErpConfig } from '@/types';

interface ErpConfigListProps {
  configs: ErpConfig[];
  scenarioCounts?: Record<number, number>;
  runningCounts?: Record<number, { running: number; error: number }>;
  onTest?: (configId: number) => void;
  onDiagnose?: (configId: number) => void;
  onReconcile?: (configId: number) => void;
  onConfig?: (config: ErpConfig) => void;
  onDelete?: (configId: number) => void;
  onViewDetails?: (configId: number) => void;
}

export function ErpConfigList({
  configs,
  scenarioCounts = {},
  runningCounts = {},
  onTest,
  onDiagnose,
  onReconcile,
  onConfig,
  onDelete,
  onViewDetails
}: ErpConfigListProps) {
  if (configs.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-16 text-center">
        <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center mb-4">
          <span className="text-3xl text-slate-300">🔌</span>
        </div>
        <h3 className="text-lg font-semibold text-slate-700 mb-2">还没有配置任何连接器</h3>
        <p className="text-sm text-slate-500 mb-4">点击下方按钮添加您的第一个 ERP 连接器</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      {configs.map((config) => {
        const stats = runningCounts[config.id] || { running: 0, error: 0 };
        return (
          <ErpConfigCard
            key={config.id}
            config={config}
            status="connected"
            scenarioCount={scenarioCounts[config.id] || 0}
            runningCount={stats.running}
            errorCount={stats.error}
            onTest={onTest}
            onDiagnose={onDiagnose}
            onReconcile={onReconcile}
            onConfig={onConfig}
            onDelete={onDelete}
            onViewDetails={onViewDetails}
          />
        );
      })}
    </div>
  );
}
