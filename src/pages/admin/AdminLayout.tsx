// Input: React、本地模块 Sidebar、TopBar、FondsManagement 等
// Output: React 组件 AdminLayout
// Pos: src/pages/admin/AdminLayout.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { Sidebar } from '../../components/Sidebar';
import { TopBar } from '../../components/TopBar';
import { FondsManagement } from './FondsManagement';
import { PositionManagement } from './PositionManagement';
import { EntityManagementPage } from './EntityManagementPage';
import { EntityConfigPage } from './EntityConfigPage';
import { EnterpriseArchitecturePage } from './EnterpriseArchitecturePage';

interface AdminLayoutProps {
    onExit: () => void;
    children?: React.ReactNode;
}

export const AdminLayout: React.FC<AdminLayoutProps> = ({ onExit, children }) => {
    const [activeTab, setActiveTab] = useState<'entity' | 'entity-config' | 'architecture' | 'fonds' | 'positions' | 'custom'>('custom');
    const [collapsed, setCollapsed] = useState(false);

    const renderContent = () => {
        if (children) return children;
        if (activeTab === 'entity') return <EntityManagementPage />;
        if (activeTab === 'entity-config') return <EntityConfigPage />;
        if (activeTab === 'architecture') return <EnterpriseArchitecturePage />;
        if (activeTab === 'fonds') return <FondsManagement />;
        if (activeTab === 'positions') return <PositionManagement />;
        return null;
    };

    return (
        <div className="flex h-screen bg-slate-50">
            <Sidebar
                collapsed={collapsed}
                onToggle={() => setCollapsed(!collapsed)}
                onVisitLanding={onExit}
            />
            <div className="flex-1 flex flex-col">
                <TopBar />
                <div className="px-4 pt-4 flex gap-2 flex-wrap">
                    <button
                        onClick={() => setActiveTab('entity')}
                        className={`px-3 py-2 text-sm rounded-lg border ${activeTab === 'entity' ? 'bg-primary-600 text-white border-primary-600' : 'border-slate-200 text-slate-600'}`}
                    >
                        法人管理
                    </button>
                    <button
                        onClick={() => setActiveTab('entity-config')}
                        className={`px-3 py-2 text-sm rounded-lg border ${activeTab === 'entity-config' ? 'bg-primary-600 text-white border-primary-600' : 'border-slate-200 text-slate-600'}`}
                    >
                        法人配置
                    </button>
                    <button
                        onClick={() => setActiveTab('architecture')}
                        className={`px-3 py-2 text-sm rounded-lg border ${activeTab === 'architecture' ? 'bg-primary-600 text-white border-primary-600' : 'border-slate-200 text-slate-600'}`}
                    >
                        集团架构
                    </button>
                    <button
                        onClick={() => setActiveTab('fonds')}
                        className={`px-3 py-2 text-sm rounded-lg border ${activeTab === 'fonds' ? 'bg-primary-600 text-white border-primary-600' : 'border-slate-200 text-slate-600'}`}
                    >
                        全宗管理
                    </button>
                    <button
                        onClick={() => setActiveTab('positions')}
                        className={`px-3 py-2 text-sm rounded-lg border ${activeTab === 'positions' ? 'bg-primary-600 text-white border-primary-600' : 'border-slate-200 text-slate-600'}`}
                    >
                        岗位管理
                    </button>
                    <button
                        onClick={() => setActiveTab('custom')}
                        className={`px-3 py-2 text-sm rounded-lg border ${activeTab === 'custom' ? 'bg-primary-600 text-white border-primary-600' : 'border-slate-200 text-slate-600'}`}
                    >
                        自定义内容
                    </button>
                </div>
                <main className="flex-1 overflow-y-auto p-4">
                    {renderContent()}
                </main>
                <footer className="p-2 text-center text-sm text-gray-500">
                    <button onClick={onExit} className="text-blue-600 hover:underline">退出管理</button>
                </footer>
            </div>
        </div>
    );
};

export default AdminLayout;
