// Input: React Flow Edge 类型、RelationEdgeData 类型
// Output: React Flow 自定义连线组件
// Pos: 关系图谱连线组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { memo } from 'react';
import {
  EdgeLabelRenderer,
  type EdgeProps,
  getBezierPath
} from '@xyflow/react';
import type { RelationEdgeData, RelationType } from '@/types/relationGraph';
import { RELATION_TYPE_LABELS } from '@/types/relationGraph';

// 关系类型颜色配置
const RELATION_TYPE_STYLES: Record<string, { color: string; strokeWidth: number; dasharray?: string }> = {
  BASIS: { color: '#818cf8', strokeWidth: 3 }, // 依据 - 靛蓝色
  ORIGINAL_VOUCHER: { color: '#c084fc', strokeWidth: 3 }, // 原始凭证 - 紫色
  CASH_FLOW: { color: '#34d399', strokeWidth: 3 }, // 资金流 - 绿色
  ARCHIVE: { color: '#fbbf24', strokeWidth: 3 }, // 归档 - 黄色
  SYSTEM_AUTO: { color: '#94a3b8', strokeWidth: 2.5, dasharray: '5,5' }, // 系统自动 - 灰色虚线
  default: { color: '#64748b', strokeWidth: 2.5 }
};

/**
 * 关系图谱连线组件
 * 使用双向箭头和标签展示关系类型
 */
export const RelationEdge = memo((props: EdgeProps) => {
  const {
    id,
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    selected
  } = props;

  const data = props.data as RelationEdgeData | undefined;

  // 计算贝塞尔曲线路径
  const [edgePath, labelX, labelY] = getBezierPath({
    sourceX,
    sourceY,
    sourcePosition,
    targetX,
    targetY,
    targetPosition,
  });

  // 关系类型标签
  const relationType = (data?.relationType || 'default') as RelationType;
  const relationLabel = relationType !== 'default'
    ? RELATION_TYPE_LABELS[relationType] || data?.description || relationType
    : (data?.description || '关联');

  // 获取关系类型样式
  const edgeStyle = RELATION_TYPE_STYLES[relationType] || RELATION_TYPE_STYLES.default;
  
  // 选中状态时加深颜色并加粗
  const strokeColor = selected 
    ? edgeStyle.color 
    : edgeStyle.color;
  const strokeWidth = selected 
    ? edgeStyle.strokeWidth + 1 
    : edgeStyle.strokeWidth;

  return (
    <>
      {/* 连线背景（白色描边，增强对比度） */}
      <path
        id={`${id}-bg`}
        d={edgePath}
        stroke="white"
        strokeWidth={strokeWidth + 2}
        fill="none"
        strokeLinecap="round"
        strokeDasharray={edgeStyle.dasharray}
        opacity={0.6}
        className="transition-all duration-200"
      />
      
      {/* 主连线路径 */}
      <path
        id={id}
        d={edgePath}
        stroke={strokeColor}
        strokeWidth={strokeWidth}
        fill="none"
        strokeLinecap="round"
        strokeDasharray={edgeStyle.dasharray}
        className="transition-all duration-200"
        style={{ filter: 'drop-shadow(0 1px 2px rgba(0,0,0,0.1))' }}
      />

      {/* 标签渲染器 */}
      <EdgeLabelRenderer>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
            pointerEvents: 'all',
            backgroundColor: 'white',
            border: `1.5px solid ${strokeColor}`,
            color: strokeColor,
          }}
          className="px-2.5 py-1 rounded-full text-[11px] font-semibold shadow-md whitespace-nowrap hover:shadow-lg transition-all cursor-default"
        >
          {relationLabel}
        </div>
      </EdgeLabelRenderer>
    </>
  );
});

RelationEdge.displayName = 'RelationEdge';

/**
 * 自定义边类型配置（用于 React Flow）
 */
export const edgeTypes = {
  relationEdge: RelationEdge,
};
