import React from 'react';
import { Sidebar } from '../Sidebar';
import { TopBar } from '../TopBar';

interface AdminLayoutProps {
    onExit: () => void;
}

export const AdminLayout: React.FC<AdminLayoutProps> = ({ onExit, children }) => {
    return (
        <div className="flex h-screen bg-slate-50">
            <Sidebar activeView={null as any} setActiveView={() => { }} activeSubItem={''} setActiveSubItem={() => { }} />
            <div className="flex-1 flex flex-col">
                <TopBar />
                <main className="flex-1 overflow-y-auto p-4">
                    {children}
                </main>
                <footer className="p-2 text-center text-sm text-gray-500">
                    <button onClick={onExit} className="text-blue-600 hover:underline">退出管理</button>
                </footer>
            </div>
        </div>
    );
};
