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
import type { RelationEdgeData } from '@/types/relationGraph';
import { RELATION_TYPE_LABELS } from '@/types/relationGraph';

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
  const relationLabel = data?.relationType
    ? RELATION_TYPE_LABELS[data.relationType] || data.relationType
    : (data?.description || '关联');

  // 选中状态样式
  const strokeColor = selected ? '#3b82f6' : '#94a3b8';
  const strokeWidth = selected ? 2.5 : 2;

  return (
    <>
      {/* 连线路径 */}
      <path
        id={id}
        d={edgePath}
        stroke={strokeColor}
        strokeWidth={strokeWidth}
        fill="none"
        className="transition-all duration-200"
      />

      {/* 标签渲染器 */}
      <EdgeLabelRenderer>
        <div
          style={{
            position: 'absolute',
            transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
            pointerEvents: 'all',
          }}
          className="px-2 py-0.5 bg-white border border-slate-200 rounded-full text-[10px] font-medium text-slate-600 shadow-sm whitespace-nowrap hover:border-primary-300 hover:text-primary-600 transition-colors cursor-default"
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
