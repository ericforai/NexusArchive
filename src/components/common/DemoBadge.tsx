import React from 'react';
import { AlertCircle } from 'lucide-react';

interface DemoBadgeProps {
  text: string;
}

export const DemoBadge: React.FC<DemoBadgeProps> = ({ text }) => (
  <div className="flex items-center gap-2 bg-amber-50 border border-amber-200 px-3 py-2 rounded-lg text-sm text-amber-700">
    <AlertCircle size={16} />
    <span>{text}</span>
  </div>
);
