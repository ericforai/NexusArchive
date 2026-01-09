// Input: React、antd Avatar
// Output: 用户头像和信息展示组件
// Pos: TopBar 子组件

import React from 'react';
import { Avatar } from 'antd';

interface UserProfileProps {
    displayName: string;
    mainRole: string;
    avatarUrl?: string;
    onClick: () => void;
}

export const UserProfile: React.FC<UserProfileProps> = React.memo(({
    displayName,
    mainRole,
    avatarUrl,
    onClick,
}) => {
    return (
        <div
            className="flex items-center space-x-2 cursor-pointer hover:opacity-80 transition-opacity"
            onClick={onClick}
        >
            <div className="text-right hidden md:block">
                <p className="text-sm font-bold text-slate-800">{displayName}</p>
                <p className="text-xs text-slate-500">{mainRole}</p>
            </div>
            <Avatar size={36} src={avatarUrl} className="ring-2 ring-white shadow-sm">
                {displayName?.[0]?.toUpperCase()}
            </Avatar>
        </div>
    );
});
