import React from 'react';

type HealthStatus = 'healthy' | 'warning' | 'error';

interface ConnectionHealthBadgeProps {
  status: HealthStatus;
  lastCheckTime?: string;
}

export function ConnectionHealthBadge({ status, lastCheckTime }: ConnectionHealthBadgeProps) {
  const statusConfig = {
    healthy: { icon: '✅', text: '正常', color: 'text-green-600', bg: 'bg-green-50' },
    warning: { icon: '⚠️', text: '警告', color: 'text-yellow-600', bg: 'bg-yellow-50' },
    error: { icon: '❌', text: '异常', color: 'text-red-600', bg: 'bg-red-50' },
  };

  const config = statusConfig[status];

  const formatCheckTime = (dateString?: string) => {
    if (!dateString) return null;
    try {
      const date = new Date(dateString);
      const now = new Date();
      const diffMs = now.getTime() - date.getTime();
      const diffMins = Math.floor(diffMs / 60000);

      if (diffMins < 1) return '刚刚';
      if (diffMins < 60) return `${diffMins}分钟前`;
      if (diffMins < 1440) return `${Math.floor(diffMins / 60)}小时前`;
      return date.toLocaleDateString('zh-CN');
    } catch {
      return null;
    }
  };

  return (
    <div className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium ${config.bg} ${config.color}`}>
      <span>{config.icon} {config.text}</span>
      {lastCheckTime && (
        <span className="opacity-75">
          · 检查于 {formatCheckTime(lastCheckTime)}
        </span>
      )}
    </div>
  );
}
