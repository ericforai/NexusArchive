// Input: React、组织树数据、Tree组件
// Output: OrgSelector 组织选择器组件（可复用）
// Pos: 组织架构组件 - 独立模块，不影响其他功能
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState, useMemo } from 'react';
import { TreeNode } from './Tree';
import { OrgNode } from '../../types';
import { Loader2, ChevronRight, ChevronDown, Folder, File, Check } from 'lucide-react';

/**
 * 组织选择器组件（可复用）
 * 
 * 功能：
 * - 支持单选或多选组织
 * - 显示组织树结构
 * - 可搜索过滤
 * - 独立模块，组织架构变更不影响其他功能
 */
export interface OrgSelectorProps {
    /**
     * 组织树数据
     */
    orgTree: OrgNode[];
    
    /**
     * 是否多选
     */
    multiple?: boolean;
    
    /**
     * 当前选中的组织ID（单选时为string，多选时为string[]）
     */
    value?: string | string[];
    
    /**
     * 选择变化回调
     */
    onChange?: (value: string | string[]) => void;
    
    /**
     * 是否禁用
     */
    disabled?: boolean;
    
    /**
     * 占位符文本
     */
    placeholder?: string;
    
    /**
     * 是否显示搜索框
     */
    showSearch?: boolean;
    
    /**
     * 自定义样式类名
     */
    className?: string;
}

/**
 * 组织选择器组件
 */
export const OrgSelector: React.FC<OrgSelectorProps> = ({
    orgTree,
    multiple = false,
    value,
    onChange,
    disabled = false,
    placeholder = '请选择组织',
    showSearch = true,
    className = '',
}) => {
    const [searchText, setSearchText] = useState('');
    const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
    const [selectedKeys, setSelectedKeys] = useState<string[]>(() => {
        if (!value) return [];
        return Array.isArray(value) ? value : [value];
    });

    // 转换组织树数据为Tree组件格式
    const treeData = useMemo<TreeNode[]>(() => {
        const mapNodes = (nodes: OrgNode[]): TreeNode[] => nodes.map((node) => ({
            id: node.id,
            label: node.name,
            type: node.type,
            parentId: node.parentId,
            children: node.children ? mapNodes(node.children) : [],
        }));
        return mapNodes(orgTree);
    }, [orgTree]);

    // 过滤树数据（根据搜索文本）
    const filteredTreeData = useMemo<TreeNode[]>(() => {
        if (!searchText.trim()) return treeData;
        
        const filterNode = (node: TreeNode): TreeNode | null => {
            const matchesSearch = node.label.toLowerCase().includes(searchText.toLowerCase());
            const filteredChildren = node.children
                ? node.children.map(filterNode).filter((n): n is TreeNode => n !== null)
                : [];
            
            if (matchesSearch || filteredChildren.length > 0) {
                return {
                    ...node,
                    children: filteredChildren,
                };
            }
            return null;
        };
        
        return treeData.map(filterNode).filter((n): n is TreeNode => n !== null);
    }, [treeData, searchText]);

    // 展开所有节点（当搜索时）
    useEffect(() => {
        if (searchText.trim() && filteredTreeData.length > 0) {
            const getAllKeys = (nodes: TreeNode[]): string[] => {
                const keys: string[] = [];
                nodes.forEach(node => {
                    keys.push(node.id);
                    if (node.children && node.children.length > 0) {
                        keys.push(...getAllKeys(node.children));
                    }
                });
                return keys;
            };
            setExpandedKeys(getAllKeys(filteredTreeData));
        }
    }, [searchText, filteredTreeData]);

    // 同步外部value变化
    useEffect(() => {
        if (value !== undefined) {
            const newSelectedKeys = Array.isArray(value) ? value : (value ? [value] : []);
            setSelectedKeys(newSelectedKeys);
        }
    }, [value]);

    // 处理节点选择
    const handleSelect = (nodeId: string) => {
        if (disabled) return;
        
        let newSelectedKeys: string[];
        if (multiple) {
            // 多选模式：切换选中状态
            if (selectedKeys.includes(nodeId)) {
                newSelectedKeys = selectedKeys.filter(id => id !== nodeId);
            } else {
                newSelectedKeys = [...selectedKeys, nodeId];
            }
        } else {
            // 单选模式：直接替换
            newSelectedKeys = [nodeId];
        }
        
        setSelectedKeys(newSelectedKeys);
        
        // 触发onChange回调
        if (onChange) {
            onChange(multiple ? newSelectedKeys : (newSelectedKeys[0] || ''));
        }
    };

    // 清除选择
    const handleClear = () => {
        if (disabled) return;
        setSelectedKeys([]);
        if (onChange) {
            onChange(multiple ? [] : '');
        }
    };

    return (
        <div className={`org-selector ${className}`}>
            {/* 搜索框 */}
            {showSearch && (
                <div className="mb-3">
                    <input
                        type="text"
                        value={searchText}
                        onChange={(e) => setSearchText(e.target.value)}
                        placeholder="搜索组织..."
                        disabled={disabled}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 disabled:bg-slate-100 disabled:cursor-not-allowed"
                    />
                </div>
            )}

            {/* 已选择的组织显示 */}
            {selectedKeys.length > 0 && (
                <div className="mb-3 flex flex-wrap gap-2">
                    {selectedKeys.map(orgId => {
                        // 查找组织名称
                        const findOrgName = (nodes: OrgNode[], id: string): string | null => {
                            for (const node of nodes) {
                                if (node.id === id) return node.name;
                                if (node.children) {
                                    const found = findOrgName(node.children, id);
                                    if (found) return found;
                                }
                            }
                            return null;
                        };
                        const orgName = findOrgName(orgTree, orgId) || orgId;
                        
                        return (
                            <span
                                key={orgId}
                                className="inline-flex items-center gap-1 px-2 py-1 bg-primary-100 text-primary-800 rounded text-sm"
                            >
                                {orgName}
                                {!disabled && (
                                    <button
                                        onClick={() => handleSelect(orgId)}
                                        className="text-primary-600 hover:text-primary-800"
                                    >
                                        ×
                                    </button>
                                )}
                            </span>
                        );
                    })}
                    {!disabled && (
                        <button
                            onClick={handleClear}
                            className="text-sm text-slate-500 hover:text-slate-700"
                        >
                            清除全部
                        </button>
                    )}
                </div>
            )}

            {/* 组织树 */}
            <div className="border border-slate-200 rounded-lg p-3 max-h-96 overflow-auto">
                {filteredTreeData.length === 0 ? (
                    <div className="text-center py-8 text-slate-500 text-sm">
                        {searchText ? '未找到匹配的组织' : '暂无组织数据'}
                    </div>
                ) : (
                    <OrgTreeView
                        data={filteredTreeData}
                        selectedKeys={selectedKeys}
                        expandedKeys={expandedKeys}
                        onSelect={handleSelect}
                        onExpand={(keys) => setExpandedKeys(keys)}
                        multiple={multiple}
                        disabled={disabled}
                    />
                )}
            </div>
        </div>
    );
};

/**
 * 组织树视图组件（内部使用）
 */
interface OrgTreeViewProps {
    data: TreeNode[];
    selectedKeys: string[];
    expandedKeys: string[];
    onSelect: (nodeId: string) => void;
    onExpand: (keys: string[]) => void;
    multiple: boolean;
    disabled: boolean;
}

const OrgTreeView: React.FC<OrgTreeViewProps> = ({
    data,
    selectedKeys,
    expandedKeys,
    onSelect,
    onExpand,
    multiple,
    disabled,
}) => {
    const toggleExpand = (nodeId: string, e: React.MouseEvent) => {
        e.stopPropagation();
        if (disabled) return;
        const newExpanded = expandedKeys.includes(nodeId)
            ? expandedKeys.filter(id => id !== nodeId)
            : [...expandedKeys, nodeId];
        onExpand(newExpanded);
    };

    const renderNode = (node: TreeNode, depth: number) => {
        const hasChildren = node.children && node.children.length > 0;
        const isExpanded = expandedKeys.includes(node.id);
        const isSelected = selectedKeys.includes(node.id);

        return (
            <div key={node.id} style={{ marginLeft: depth * 16 }} className="py-1">
                <div
                    className={`flex items-center group rounded px-2 py-1.5 transition-colors ${
                        isSelected
                            ? 'bg-primary-50 text-primary-700'
                            : 'hover:bg-slate-50 text-slate-800'
                    } ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
                    onClick={() => !disabled && onSelect(node.id)}
                >
                    {/* Expand/Collapse Icon */}
                    <span
                        className={`text-slate-400 hover:text-slate-600 mr-1 ${
                            !hasChildren ? 'invisible' : ''
                        }`}
                        onClick={(e) => toggleExpand(node.id, e)}
                    >
                        {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    </span>

                    {/* Type Icon */}
                    <span className="text-slate-500 mr-2">
                        {node.type === 'COMPANY' ? (
                            <Folder size={16} className="fill-blue-50 text-blue-500" />
                        ) : (
                            <File size={14} />
                        )}
                    </span>

                    {/* Checkbox (多选模式) */}
                    {multiple && (
                        <span className="mr-2">
                            {isSelected ? (
                                <Check size={16} className="text-primary-600" />
                            ) : (
                                <div className="w-4 h-4 border border-slate-300 rounded" />
                            )}
                        </span>
                    )}

                    {/* Label */}
                    <div className="text-sm flex-1">
                        {node.label}
                        {node.type && (
                            <span className="ml-2 text-[10px] text-slate-400 uppercase border border-slate-100 rounded px-1">
                                {node.type}
                            </span>
                        )}
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

