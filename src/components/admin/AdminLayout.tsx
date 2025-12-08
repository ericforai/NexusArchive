import React, { useState } from 'react';
import { Sidebar } from '../Sidebar';
import { TopBar } from '../TopBar';
import { FondsManagement } from './FondsManagement';
import { PositionManagement } from './PositionManagement';

interface AdminLayoutProps {
    onExit: () => void;
}

export const AdminLayout: React.FC<AdminLayoutProps> = ({ onExit, children }) => {
    const [activeTab, setActiveTab] = useState<'fonds' | 'positions' | 'custom'>('custom');

    const renderContent = () => {
        if (children) return children;
        if (activeTab === 'fonds') return <FondsManagement />;
        if (activeTab === 'positions') return <PositionManagement />;
        return null;
    };

    return (
        <div className="flex h-screen bg-slate-50">
            <Sidebar activeView={null as any} setActiveView={() => { }} activeSubItem={''} setActiveSubItem={() => { }} />
            <div className="flex-1 flex flex-col">
                <TopBar />
                <div className="px-4 pt-4 flex gap-2">
                    <button
                        onClick={() => setActiveTab('custom')}
                        className={`px-3 py-2 text-sm rounded-lg border ${activeTab === 'custom' ? 'bg-primary-600 text-white border-primary-600' : 'border-slate-200 text-slate-600'}`}
                    >
                        自定义内容
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
