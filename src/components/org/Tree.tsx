import React, { useState } from 'react';
import { ChevronRight, ChevronDown, Folder, File, GripVertical } from 'lucide-react';

export interface TreeNode {
  id: string;
  label: string;
  type?: string;
  parentId?: string;
  children?: TreeNode[];
}

interface TreeProps {
  data: TreeNode[];
}

export const Tree: React.FC<TreeProps> = ({ data }) => {
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());

  const toggleExpand = (id: string) => {
    const newExpanded = new Set(expandedIds);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedIds(newExpanded);
  };

  const renderNode = (node: TreeNode, depth: number) => {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expandedIds.has(node.id);

    return (
      <div key={node.id} style={{ marginLeft: depth * 12 }} className="py-1">
        <div className="flex items-center group hover:bg-slate-50 rounded px-1 py-1 transition-colors">
          {/* Drag Handle (Visual Only) */}
          <span className="text-slate-300 cursor-grab opacity-0 group-hover:opacity-100 mr-1" title="拖拽排序 (即将上线)">
            <GripVertical size={14} />
          </span>

          {/* Expand/Collapse Icon */}
          <span
            className={`cursor-pointer text-slate-400 hover:text-slate-600 mr-1 ${!hasChildren ? 'invisible' : ''}`}
            onClick={() => toggleExpand(node.id)}
          >
            {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
          </span>

          {/* Type Icon */}
          <span className="text-slate-500 mr-2">
            {node.type === 'COMPANY' ? <Folder size={16} className="fill-blue-50 text-blue-500" /> : <File size={14} />}
          </span>

          {/* Label */}
          <div className="text-sm text-slate-800 flex-1">
            {node.label}
            {node.type && <span className="ml-2 text-[10px] text-slate-400 uppercase border border-slate-100 rounded px-1">{node.type}</span>}
          </div>
        </div>

        {/* Children */}
        {hasChildren && isExpanded && (
          <div className="mt-1 border-l border-slate-100 ml-4 pl-1">
            {node.children!.map((child) => renderNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return <div>{data.map((n) => renderNode(n, 0))}</div>;
};
