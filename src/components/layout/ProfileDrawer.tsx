// Input: React、Ant Design、useAuthStore、authApi
// Output: ProfileDrawer 组件
// Pos: 个人资料抽屉组件

import React from 'react';
import { Drawer, Avatar, Button } from 'antd';
import { useAuthStore } from '../../store';
import { authApi } from '../../api/auth';
import { useNavigate } from 'react-router-dom';

interface ProfileDrawerProps {
    open: boolean;
    onClose: () => void;
}

const InfoRow: React.FC<{ label: string; value?: string }> = ({ label, value }) => (
    <div className="flex">
        <span className="w-16 text-slate-500">{label}</span>
        <span className="flex-1 text-slate-800">{value || '-'}</span>
    </div>
);

export const ProfileDrawer: React.FC<ProfileDrawerProps> = ({ open, onClose }) => {
    const { user } = useAuthStore();
    const navigate = useNavigate();
    const mainRole = user?.roleNames?.[0] || '-';

    const handleLogout = async () => {
        try {
            await authApi.logout();
        } catch (error) {
            console.error('登出失败:', error);
        } finally {
            useAuthStore.getState().logout();
            navigate('/login');
        }
    };

    return (
        <Drawer open={open} onClose={onClose} placement="right" width={400}>
            <div className="flex flex-col h-full">
                {/* 标题栏 */}
                <div className="flex items-center justify-between px-6 py-4 border-b">
                    <h2 className="text-lg font-semibold">个人资料</h2>
                    <Button type="text" onClick={onClose}>✕</Button>
                </div>

                {/* 头像区域 */}
                <div className="flex flex-col items-center py-8 border-b">
                    <Avatar size={80} src={user?.avatar}>{user?.fullName?.[0]}</Avatar>
                    <div className="mt-3 text-lg font-medium">{user?.fullName}</div>
                    <div className="text-sm text-slate-500">
                        {mainRole} · {user?.employeeId || '-'}
                    </div>
                </div>

                {/* 信息列表 */}
                <div className="flex-1 px-6 py-4 space-y-4">
                    <InfoRow label="姓名" value={user?.fullName} />
                    <InfoRow label="工号" value={user?.employeeId} />
                    <InfoRow label="邮箱" value={user?.email} />
                    <InfoRow label="手机" value={user?.phone} />
                </div>

                {/* 底部按钮 */}
                <div className="p-6 border-t">
                    <Button danger block onClick={handleLogout}>
                        退出登录
                    </Button>
                </div>
            </div>
        </Drawer>
    );
};
