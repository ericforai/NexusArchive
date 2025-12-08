import React from 'react';
import { AlertTriangle } from 'lucide-react';

interface DemoBadgeProps {
  text?: string;
  className?: string;
}

export const DemoBadge: React.FC<DemoBadgeProps> = ({ text = '演示数据，仅供展示，待接入真实接口', className = '' }) => (
  <div className={`mb-4 flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700 ${className}`}>
    <AlertTriangle size={16} />
    <span>{text}</span>
  </div>
);
