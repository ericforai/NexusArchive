// Input: React, lucide-react
// Output: OrgTreePicker 组件
// Pos: 通用复用组件 - 组织树选择器弹窗

import React, { useState, useMemo } from 'react';
import { X, Search, Folder, File, ChevronRight, ChevronDown, Check } from 'lucide-react';
import { BaseModal } from '../modals/BaseModal';
import { OrgNode } from '../../types';

export interface OrgTreePickerProps {
  isOpen: boolean;
  onClose: () => void;
  orgTree: OrgNode[];
  title?: string;
  multiple?: boolean;
  value?: string | string[];
  onChange: (value: string | string[]) => void;
}

/**
 * 组织树选择弹窗
 * <p>
 * 用于在弹窗中选择组织，支持搜索和多选
 * </p>
 */
export function OrgTreePicker({
  isOpen,
  onClose,
  orgTree,
  title = '选择组织',
  multiple = false,
  value,
  onChange,
}: OrgTreePickerProps) {
  const [searchText, setSearchText] = useState('');
  const [expandedKeys, setExpandedKeys] = useState<string[]>(() => {
    // 默认展开所有节点
    const getAllKeys = (nodes: OrgNode[]): string[] => {
      const keys: string[] = [];
      nodes.forEach(node => {
        keys.push(node.id);
        if (node.children) {
          keys.push(...getAllKeys(node.children));
        }
      });
      return keys;
    };
    return getAllKeys(orgTree);
  });

  const selectedKeys = useMemo(() => {
    if (!value) return [];
    return Array.isArray(value) ? value : [value];
  }, [value]);

  // 过滤树数据
  const filteredTreeData = useMemo(() => {
    if (!searchText.trim()) return orgTree;

    const filterNode = (node: OrgNode): OrgNode | null => {
      const matchesSearch = node.name.toLowerCase().includes(searchText.toLowerCase());
      const filteredChildren = node.children
        ? node.children.map(filterNode).filter((n): n is OrgNode => n !== null)
        : [];

      if (matchesSearch || filteredChildren.length > 0) {
        return {
          ...node,
          children: filteredChildren,
        };
      }
      return null;
    };

    return orgTree.map(filterNode).filter((n): n is OrgNode => n !== null);
  }, [orgTree, searchText]);

  const handleSelect = (nodeId: string) => {
    let newSelectedKeys: string[];
    if (multiple) {
      if (selectedKeys.includes(nodeId)) {
        newSelectedKeys = selectedKeys.filter(id => id !== nodeId);
      } else {
        newSelectedKeys = [...selectedKeys, nodeId];
      }
    } else {
      newSelectedKeys = [nodeId];
      onChange(multiple ? newSelectedKeys : newSelectedKeys[0]);
      onClose();
      return;
    }

    onChange(multiple ? newSelectedKeys : newSelectedKeys[0] || '');
  };

  const handleConfirm = () => {
    onChange(multiple ? selectedKeys : (selectedKeys[0] || ''));
    onClose();
  };

  const TreeNode: React.FC<{
    node: OrgNode;
    depth: number;
  }> = ({ node, depth }) => {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expandedKeys.includes(node.id);
    const isSelected = selectedKeys.includes(node.id);

    return (
      <div>
        <div
          className={`flex items-center py-2 px-3 rounded-lg cursor-pointer transition-colors ${
            isSelected
              ? 'bg-blue-50 text-blue-700'
              : 'hover:bg-slate-50 text-slate-700'
          }`}
          style={{ paddingLeft: `${depth * 16 + 12}px` }}
          onClick={() => handleSelect(node.id)}
        >
          {/* Expand/Collapse */}
          <span
            className="text-slate-400 mr-2"
            onClick={(e) => {
              e.stopPropagation();
              setExpandedKeys(prev =>
                prev.includes(node.id)
                  ? prev.filter(id => id !== node.id)
                  : [...prev, node.id]
              );
            }}
          >
            {hasChildren ? (
              isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />
            ) : (
              <span className="w-4" />
            )}
          </span>

          {/* Type Icon */}
          <span className="mr-2 text-slate-500">
            {node.type === 'COMPANY' ? (
              <Folder size={16} className="fill-blue-50 text-blue-500" />
            ) : (
              <File size={14} />
            )}
          </span>

          {/* Checkbox (multi-select) */}
          {multiple && (
            <span className="mr-2 flex items-center justify-center w-4 h-4 border rounded">
              {isSelected && <Check size={12} className="text-blue-600" />}
            </span>
          )}

          {/* Name */}
          <span className="text-sm flex-1">{node.name}</span>

          {/* Type Badge */}
          {node.type && (
            <span className="ml-2 text-[10px] text-slate-400 uppercase border border-slate-100 rounded px-1.5 py-0.5">
              {node.type}
            </span>
          )}
        </div>

        {/* Children */}
        {hasChildren && isExpanded && (
          <div>
            {node.children!.map(child => (
              <TreeNode key={child.id} node={child} depth={depth + 1} />
            ))}
          </div>
        )}
      </div>
    );
  };

  const selectedOrgNames = useMemo(() => {
    const names: string[] = [];
    selectedKeys.forEach(key => {
      const findName = (nodes: OrgNode[], id: string): string | null => {
        for (const node of nodes) {
          if (node.id === id) return node.name;
          if (node.children) {
            const found = findName(node.children, id);
            if (found) return found;
          }
        }
        return null;
      };
      const name = findName(orgTree, key);
      if (name) names.push(name);
    });
    return names;
  }, [selectedKeys, orgTree]);

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={onClose}
      title={title}
      maxWidth="2xl"
    >
      {/* Search */}
      <div className="mb-4">
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
          <input
            type="text"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            placeholder="搜索组织..."
            className="w-full pl-10 pr-4 py-2 border border-slate-200 dark:border-slate-600 rounded-lg bg-white dark:bg-slate-700 text-slate-800 dark:text-white focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>
      </div>

      {/* Selected */}
      {selectedKeys.length > 0 && (
        <div className="mb-4 p-3 bg-slate-50 dark:bg-slate-700 rounded-lg">
          <p className="text-xs text-slate-500 dark:text-slate-400 mb-2">已选择:</p>
          <div className="flex flex-wrap gap-2">
            {selectedOrgNames.map((name, i) => (
              <span
                key={i}
                className="inline-flex items-center gap-1 px-2 py-1 bg-blue-100 text-blue-700 rounded text-sm"
              >
                {name}
                <button
                  onClick={() => handleSelect(selectedKeys[i])}
                  className="text-blue-600 hover:text-blue-800"
                >
                  ×
                </button>
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Tree */}
      <div className="border border-slate-200 dark:border-slate-600 rounded-lg p-2 max-h-80 overflow-auto">
        {filteredTreeData.length === 0 ? (
          <div className="text-center py-8 text-slate-500 text-sm">
            未找到匹配的组织
          </div>
        ) : (
          filteredTreeData.map(node => <TreeNode key={node.id} node={node} depth={0} />)
        )}
      </div>

      {/* Footer Actions */}
      <div className="flex items-center justify-end gap-3 mt-4 pt-4 border-t border-slate-200 dark:border-slate-600">
        <button
          onClick={onClose}
          className="px-4 py-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition-colors"
        >
          取消
        </button>
        <button
          onClick={handleConfirm}
          disabled={selectedKeys.length === 0}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          确定
        </button>
      </div>
    </BaseModal>
  );
}

export default OrgTreePicker;
