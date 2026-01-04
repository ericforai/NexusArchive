// Input: React、UserSettings、UserLifecyclePage、settings feature
// Output: UserSettingsPage 组件（集成用户管理和用户生命周期）
// Pos: src/pages/settings/UserSettingsPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { Users, UserCog } from 'lucide-react';
import { UserSettings } from '../../components/settings/UserSettings';
import { UserLifecyclePage } from '../../components/pages';
import { useAdminSettingsApi } from '../../features/settings';

type UserTab = 'list' | 'lifecycle';

const UserSettingsPage: React.FC = () => {
    const { adminApi } = useAdminSettingsApi();
    const [activeTab, setActiveTab] = useState<UserTab>('list');

    const tabs = [
        { key: 'list' as UserTab, label: '用户列表', icon: Users },
        { key: 'lifecycle' as UserTab, label: '用户生命周期', icon: UserCog },
    ];

    return (
        <div className="space-y-4">
            {/* 子 Tab 导航 */}
            <div className="bg-white border-b border-slate-200">
                <nav className="flex space-x-1" aria-label="用户管理导航">
                    {tabs.map((tab) => {
                        const Icon = tab.icon;
                        const isActive = activeTab === tab.key;
                        return (
                            <button
                                key={tab.key}
                                onClick={() => setActiveTab(tab.key)}
                                className={`
                                    flex items-center px-4 py-3 text-sm font-medium border-b-2 whitespace-nowrap
                                    transition-colors duration-200
                                    ${isActive
                                        ? 'border-primary-500 text-primary-600'
                                        : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                                    }
                                `}
                            >
                                <Icon size={16} className="mr-2" />
                                {tab.label}
                            </button>
                        );
                    })}
                </nav>
            </div>

            {/* Tab 内容 */}
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-300">
                {activeTab === 'list' ? (
                    <UserSettings adminApi={adminApi} />
                ) : (
                    <div className="-m-6">
                        <UserLifecyclePage />
                    </div>
                )}
            </div>
        </div>
    );
};

export default UserSettingsPage;
