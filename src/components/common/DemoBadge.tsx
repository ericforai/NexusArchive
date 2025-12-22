// Input: React、lucide-react 图标
// Output: React 组件 DemoBadge
// Pos: 通用复用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
