import React from 'react';

interface ScenarioSummaryCardProps {
  totalScenarios: number;
  runningCount: number;
  errorCount: number;
}

export function ScenarioSummaryCard({
  totalScenarios,
  runningCount,
  errorCount
}: ScenarioSummaryCardProps) {
  return (
    <div className="flex items-center justify-between py-3 px-4 bg-gray-50 rounded-lg">
      <div className="flex items-center gap-2">
        <span className="text-sm font-medium text-gray-700">场景</span>
        <span className="inline-flex items-center px-2 py-0.5 bg-blue-100 text-blue-700 text-xs font-medium rounded-full">
          {totalScenarios} 个
        </span>
      </div>
      <div className="flex items-center gap-3 text-xs">
        {runningCount > 0 && (
          <span className="inline-flex items-center gap-1 text-blue-600">
            <span className="animate-pulse">●</span>
            <span>{runningCount} 运行中</span>
          </span>
        )}
        {errorCount > 0 && (
          <span className="inline-flex items-center gap-1 text-red-600">
            <span>●</span>
            <span>{errorCount} 失败</span>
          </span>
        )}
        {runningCount === 0 && errorCount === 0 && (
          <span className="text-gray-500">全部空闲</span>
        )}
      </div>
    </div>
  );
}
