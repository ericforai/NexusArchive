// Input: React、lucide-react 图标
// Output: React 组件 RuleConfigModal
// Pos: 归档流程组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { X } from 'lucide-react';

interface RuleModalProps {
    isOpen: boolean;
    onClose: () => void;
    matchThreshold: number;
    onThresholdChange: (value: number) => void;
    rules: Array<{ id: string; label: string; enabled: boolean }>;
    onToggleRule: (id: string) => void;
    onSaveAndRun: () => void;
}

/**
 * 关联规则配置模态框组件
 * 
 * 从 ArchiveListView 拆分出来
 */
export const RuleConfigModal: React.FC<RuleModalProps> = ({
    isOpen,
    onClose,
    matchThreshold,
    onThresholdChange,
    rules,
    onToggleRule,
    onSaveAndRun
}) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-100">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                    <h3 className="text-lg font-bold text-slate-800">关联规则配置</h3>
                    <button onClick={onClose}><X size={20} className="text-slate-400" /></button>
                </div>
                <div className="p-6 space-y-4">
                    <div className="mb-4">
                        <label className="text-sm font-bold">自动关联阈值: {matchThreshold}%</label>
                        <input
                            type="range"
                            min="60"
                            max="100"
                            value={matchThreshold}
                            onChange={(e) => onThresholdChange(Number(e.target.value))}
                            className="w-full h-2 bg-slate-200 rounded-lg accent-primary-500"
                        />
                    </div>
                    {rules.map(rule => (
                        <div key={rule.id} className="flex justify-between items-center">
                            <span className="text-sm">{rule.label}</span>
                            <input
                                type="checkbox"
                                checked={rule.enabled}
                                onChange={() => onToggleRule(rule.id)}
                                className="toggle-checkbox h-5 w-5 text-primary-600 rounded border-slate-300"
                            />
                        </div>
                    ))}
                </div>
                <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
                    <button onClick={onClose} className="px-4 py-2 text-slate-600">取消</button>
                    <button onClick={onSaveAndRun} className="px-4 py-2 bg-primary-600 text-white rounded-lg">保存并运行</button>
                </div>
            </div>
        </div>
    );
};

export default RuleConfigModal;
