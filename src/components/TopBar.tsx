// Input: React、lucide-react 图标、本地模块 GlobalSearch、api/search、FondsSwitcher、useFondsStore
// Output: React 组件 TopBar
// Pos: 业务页面组件

import React, { useState } from 'react';
import { Avatar } from 'antd';
import { GlobalSearch } from './GlobalSearch';
import { GlobalSearchDTO } from '../types';
import { FondsSwitcher } from './common/FondsSwitcher';
import { toast } from '../utils/notificationService';
import { useFondsStore, useAuthStore } from '../store';
import { ProfileDrawer } from './layout/ProfileDrawer';

interface TopBarProps {
  onLogout?: () => void;
  onNavigate?: (item: GlobalSearchDTO) => void;
}

export const TopBar: React.FC<TopBarProps> = ({ onLogout, onNavigate }) => {
  const [profileOpen, setProfileOpen] = useState(false);

  // Get fonds state from store
  const {
    currentFonds,
    fondsList,
    isLoading,
    _hasHydrated,
    loadFondsList,
    setCurrentFonds,
  } = useFondsStore();

  // Get user info from auth store
  const { user } = useAuthStore();
  const mainRole = user?.roleNames?.[0] || user?.roles?.[0] || '-';
  const displayName = user?.fullName || user?.username || '-';

  const handleClick = (item: string) => {
    if (item === '退出登录' && onLogout) {
      onLogout();
      return;
    }
    if (item === '用户个人资料') {
      setProfileOpen(true);
      return;
    }
    toast.info(`${item} 功能开发中`);
  }

  return (
    <>
      <header className="h-16 bg-white/80 backdrop-blur-md border-b border-slate-200 px-8 flex items-center justify-between sticky top-0 z-10">

        {/* Left: Fonds Switcher */}
        <div className="flex items-center gap-4">
          <FondsSwitcher
            currentFonds={currentFonds}
            fondsList={fondsList}
            isLoading={isLoading}
            hasHydrated={_hasHydrated}
            onLoadFondsList={loadFondsList}
            onSetCurrentFonds={setCurrentFonds}
          />
        </div>

        {/* Center: Global Search */}
        <div className="flex-1 max-w-2xl mx-4">
          <GlobalSearch onNavigate={onNavigate} />
        </div>

        {/* Right Actions */}
        <div className="flex items-center space-x-4 ml-4">
          <div className="h-6 w-px bg-slate-200 mx-2"></div>

          <div
            className="flex items-center space-x-2 cursor-pointer hover:opacity-80 transition-opacity"
            onClick={() => setProfileOpen(true)}
          >
            <div className="text-right hidden md:block">
              <p className="text-sm font-bold text-slate-800">{displayName}</p>
              <p className="text-xs text-slate-500">{mainRole}</p>
            </div>
            <Avatar
              size={36}
              src={user?.avatar}
              className="ring-2 ring-white shadow-sm"
            >
              {displayName?.[0]?.toUpperCase()}
            </Avatar>
          </div>
        </div>
      </header>

      <ProfileDrawer open={profileOpen} onClose={() => setProfileOpen(false)} />
    </>
  );
};
