// Input: React Flow、lucide-react 图标、RelationNodeData 类型
// Output: React Flow 自定义节点组件
// Pos: 关系图谱节点组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { memo } from 'react';
import { Handle, Position, NodeProps } from '@xyflow/react';
import {
  FileText,
  Receipt,
  FileSpreadsheet,
  Building,
  CreditCard,
  Loader2,
  AlertCircle,
  ChevronDown,
  ChevronRight
} from 'lucide-react';
import type { RelationNodeData } from '@/types/relationGraph';
import { ARCHIVE_TYPE_STYLES } from '@/types/relationGraph';

// 图标映射
const iconMap = {
  Building,
  Receipt,
  FileText,
  CreditCard,
  FileSpreadsheet
};

/**
 * 关系图谱节点组件
 */
export const RelationNode = memo((props: NodeProps) => {
  const data = (props.data as unknown) as RelationNodeData;
  const { selected = false } = props;

  const meta = ARCHIVE_TYPE_STYLES[data.type] || ARCHIVE_TYPE_STYLES.other;
  const IconComponent = iconMap[meta.icon as keyof typeof iconMap] || FileText;

  const isCenter = data.isCenter;
  const isExpanded = data.isExpanded;
  const isLoading = data.isLoading;
  const hasError = !!data.error;

  // 根据类型获取边框颜色（用于小地图）
  const borderColorMap: Record<string, string> = {
    contract: '#818cf8',
    invoice: '#c084fc',
    voucher: '#60a5fa',
    receipt: '#34d399',
    report: '#fbbf24',
    ledger: '#94a3b8',
    other: '#94a3b8'
  };

  return (
    <div
      style={{
        width: 200,
        height: isCenter ? 140 : 110,
        background: '#ffffff',
        border: `2px solid ${isCenter ? '#f59e0b' : (borderColorMap[data.type] || '#cbd5e1')}`,
        borderRadius: 12,
        boxShadow: selected ? '0 10px 15px -3px rgba(0, 0, 0, 0.1)' : '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
        transition: 'all 0.2s'
      }}
      className="relative"
    >
      {/* 中心节点脉冲动画 */}
      {isCenter && (
        <div
          style={{
            position: 'absolute',
            inset: -4,
            borderRadius: 14,
            background: 'rgba(245, 158, 11, 0.1)',
            animation: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
            zIndex: -1
          }}
        />
      )}

      {/* 连接点（隐藏但保留功能） */}
      <Handle type="target" position={Position.Top} className="!hidden" />
      <Handle type="source" position={Position.Bottom} className="!hidden" />
      <Handle type="target" position={Position.Left} className="!hidden" />
      <Handle type="source" position={Position.Right} className="!hidden" />

      {/* 类型标签 */}
      <div
        style={{
          position: 'absolute',
          top: 8,
          left: 8,
          padding: '2px 8px',
          borderRadius: 9999,
          fontSize: 10,
          fontWeight: 'bold',
          textTransform: 'uppercase',
          letterSpacing: '0.05em',
          background: isCenter ? '#fef3c7' : (meta.bg?.replace('bg-', '#')?.replace('-50', '-50') || '#f1f5f9'),
          color: isCenter ? '#b45309' : (meta.text?.replace('text-', '#')?.replace('-700', '') || '#64748b')
        }}
        className="tw-font-sans"
      >
        {meta.label}
      </div>

      {/* 中心节点标记 */}
      {isCenter && (
        <div
          style={{
            position: 'absolute',
            top: -12,
            left: '50%',
            transform: 'translateX(-50%)',
            background: '#f59e0b',
            color: 'white',
            fontSize: 10,
            fontWeight: 'bold',
            padding: '2px 8px',
            borderRadius: 9999,
            boxShadow: '0 1px 2px rgba(0, 0, 0, 0.1)'
          }}
        >
          核心单据
        </div>
      )}

      {/* 加载状态 */}
      {isLoading && (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            background: 'rgba(255, 255, 255, 0.8)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderRadius: 12,
            zIndex: 10
          }}
        >
          <Loader2 size={24} style={{ color: '#3b82f6' }} className="animate-spin" />
        </div>
      )}

      {/* 错误状态 */}
      {hasError && !isLoading && (
        <div
          style={{ position: 'absolute', top: 8, right: 8, color: '#ef4444' }}
          title={data.error}
        >
          <AlertCircle size={16} />
        </div>
      )}

      {/* 内容区域 */}
      <div
        style={{
          padding: 12,
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          paddingTop: isCenter ? 16 : 12
        }}
      >
        {/* 图标 + 档号 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <div
            style={{
              padding: 6,
              borderRadius: 8,
              background: isCenter ? '#fef3c7' : (meta.bg?.replace('bg-', '#')?.replace('-50', '-50') || '#f1f5f9'),
              color: isCenter ? '#b45309' : (meta.text?.replace('text-', '#')?.replace('-700', '') || '#64748b')
            }}
          >
            <IconComponent size={16} />
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div
              style={{
                fontSize: 14,
                fontWeight: 'bold',
                color: '#1e293b',
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap'
              }}
              title={data.code}
            >
              {data.code || data.id}
            </div>
          </div>
        </div>

        {/* 名称 */}
        {data.name && (
          <div
            style={{
              fontSize: 12,
              color: '#64748b',
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              marginBottom: 8
            }}
            title={data.name}
          >
            {data.name}
          </div>
        )}

        {/* 底部信息 */}
        <div style={{
          marginTop: 'auto',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          fontSize: 12
        }}>
          {/* 金额 */}
          {data.amount && (
            <span style={{ fontFamily: 'monospace', fontWeight: 500, color: '#334155' }}>
              {data.amount}
            </span>
          )}

          {/* 状态 */}
          {data.status && !data.amount && (
            <span
              style={{
                padding: '2px 8px',
                borderRadius: 9999,
                background: (data.status === '已归档' || data.status === 'ARCHIVED') ? '#d1fae5' : '#f1f5f9',
                color: (data.status === '已归档' || data.status === 'ARCHIVED') ? '#059669' : '#64748b'
              }}
            >
              {data.status}
            </span>
          )}

          {/* 展开/折叠指示器 */}
          {!isCenter && (
            <div
              style={{
                color: '#94a3b8',
                transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)',
                transition: 'transform 0.2s'
              }}
            >
              {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
            </div>
          )}
        </div>

        {/* 中心节点额外信息 */}
        {isCenter && data.status && (
          <div
            style={{
              marginTop: 8,
              textAlign: 'center',
              padding: '4px 8px',
              borderRadius: 9999,
              fontSize: 12,
              fontWeight: 500,
              background: (data.status === '已归档' || data.status === 'ARCHIVED') ? '#d1fae5' : '#f1f5f9',
              color: (data.status === '已归档' || data.status === 'ARCHIVED') ? '#059669' : '#64748b'
            }}
          >
            {data.status}
          </div>
        )}
      </div>

      {/* 选中边框 */}
      {selected && (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            borderRadius: 12,
            border: '2px solid #3b82f6',
            pointerEvents: 'none'
          }}
        />
      )}
    </div>
  );
});

RelationNode.displayName = 'RelationNode';
