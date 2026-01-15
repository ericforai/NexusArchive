// Input: React、RelationNodeData、lucide-react 图标
// Output: ArchiveCard 组件（档案卡片）
// Pos: 关系图谱卡片组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { FileText, Receipt, FileSpreadsheet, Building, CreditCard, Wallet, FileCheck, ClipboardList } from 'lucide-react';
import type { RelationNodeData, RelationType } from '@/types/relationGraph';
import { ARCHIVE_TYPE_STYLES, RELATION_TYPE_LABELS } from '@/types/relationGraph';

// 图标映射
const iconMap = {
  Building,
  Receipt,
  FileText,
  CreditCard,
  FileSpreadsheet,
  Wallet,      // 付款单
  FileCheck,   // 报销单
  ClipboardList // 申请单
};

interface ArchiveCardProps {
  /** 节点数据 */
  node: RelationNodeData;
  /** 关系类型（用于显示关系标签） */
  relationType?: RelationType;
  /** 点击回调 */
  onClick?: () => void;
  /** 是否为中心节点 */
  isCenter?: boolean;
  /** 是否高亮 */
  highlighted?: boolean;
}

/**
 * 档案卡片组件
 * 支持普通卡片和中心卡片两种样式
 */
export const ArchiveCard: React.FC<ArchiveCardProps> = ({
  node,
  relationType,
  onClick,
  isCenter = false,
  highlighted = false
}) => {
  const meta = ARCHIVE_TYPE_STYLES[node.type] || ARCHIVE_TYPE_STYLES.other;
  const IconComponent = iconMap[meta.icon as keyof typeof iconMap] || FileText;
  
  const relationLabel = relationType ? RELATION_TYPE_LABELS[relationType] : null;

  // 卡片样式类
  const cardClasses = isCenter
    ? 'bg-amber-50 border-amber-400 shadow-xl w-[360px] h-[180px]'
    : 'bg-white border-slate-200 shadow-md w-[280px] h-[140px]';
  
  const highlightClasses = highlighted ? 'ring-2 ring-primary-500 ring-offset-2' : '';

  return (
    <div
      className={`
        relative rounded-xl border-2 cursor-pointer transition-all
        hover:shadow-lg hover:scale-105
        ${cardClasses}
        ${highlightClasses}
      `}
      onClick={onClick}
    >
      {/* 类型标签 */}
      <div
        className={`
          absolute top-2 left-2 px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider
          ${isCenter ? 'bg-amber-200 text-amber-800' : `${meta.bg} ${meta.text}`}
        `}
      >
        {meta.label}
      </div>

      {/* 中心节点标记 */}
      {isCenter && (
        <div className="absolute -top-3 left-1/2 -translate-x-1/2 bg-amber-500 text-white text-[10px] font-bold px-3 py-0.5 rounded-full shadow-md">
          核心单据
        </div>
      )}

      {/* 关系标签（非中心卡片） */}
      {!isCenter && relationLabel && (
        <div className="absolute top-2 right-2 px-2 py-0.5 rounded-full text-[10px] font-medium bg-slate-100 text-slate-600">
          {relationLabel}
        </div>
      )}

      {/* 卡片内容 */}
      <div className={`p-3 h-full flex flex-col ${isCenter ? 'pt-5' : ''}`}>
        {/* 图标 + 档号 */}
        <div className="flex items-center gap-2 mb-2">
          <div
            className={`
              p-1.5 rounded-lg
              ${isCenter ? 'bg-amber-200 text-amber-800' : `${meta.bg} ${meta.text}`}
            `}
          >
            <IconComponent size={16} />
          </div>
          <div className="flex-1 min-w-0">
            <div
              className="text-sm font-bold text-slate-800 truncate"
              title={node.code || node.id}
            >
              {node.code || node.id}
            </div>
          </div>
        </div>

        {/* 名称 */}
        {node.name && (
          <div
            className="text-xs text-slate-600 truncate mb-2"
            title={node.name}
          >
            {node.name}
          </div>
        )}

        {/* 金额 */}
        {node.amount && (
          <div className="text-sm font-mono font-semibold text-slate-700 mb-1">
            {node.amount}
          </div>
        )}

        {/* 日期 */}
        {node.date && (
          <div className="text-xs text-slate-500 mb-2">
            {node.date}
          </div>
        )}

        {/* 底部信息 */}
        <div className="mt-auto flex items-center justify-between">
          {/* 状态 */}
          {node.status && (
            <span
              className={`
                px-2 py-0.5 rounded-full text-xs font-medium
                ${(node.status === '已归档' || node.status === 'ARCHIVED')
                  ? 'bg-emerald-100 text-emerald-700'
                  : node.status === '生效中'
                  ? 'bg-blue-100 text-blue-700'
                  : 'bg-slate-100 text-slate-600'
                }
              `}
            >
              {node.status}
            </span>
          )}

          {/* 中心节点显示"关系: 核心单据" */}
          {isCenter && (
            <span className="text-xs text-slate-500">
              关系: 核心单据
            </span>
          )}
        </div>
      </div>
    </div>
  );
};
