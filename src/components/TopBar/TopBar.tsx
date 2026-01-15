// Input: React、lucide-react 图标、本地模块 GlobalSearch、api/search、FondsSwitcher、useFondsStore
// Output: React 组件 TopBar
// Pos: 业务页面组件

import React, { useState } from 'react';
import type { GlobalSearchDTO } from '../../types';
import { useAuthStore } from '../../store';
import { ProfileDrawer } from '../layout/ProfileDrawer';
import { UserProfile } from './UserProfile';
import { FondsSection } from './FondsSection';
import { SearchSection } from './SearchSection';

interface TopBarProps {
    onLogout?: () => void;
    onNavigate?: (item: GlobalSearchDTO) => void;
}

export const TopBar: React.FC<TopBarProps> = ({ onLogout: _onLogout, onNavigate }) => {
    const [profileOpen, setProfileOpen] = useState(false);

    // Get user info from auth store
    const { user } = useAuthStore();
    const mainRole = user?.roleNames?.[0] || user?.roles?.[0] || '-';
    const displayName = user?.fullName || user?.username || '-';

    const handleProfileOpen = () => setProfileOpen(true);
    const handleProfileClose = () => setProfileOpen(false);

    return (
        <>
            <header className="h-16 bg-white/80 backdrop-blur-md border-b border-slate-200 px-8 flex items-center justify-between sticky top-0 z-10">
                {/* Left: Fonds Switcher */}
                <FondsSection />

                {/* Center: Global Search */}
                <SearchSection onNavigate={onNavigate} />

                {/* Right Actions */}
                <div className="flex items-center space-x-4 ml-4">
                    <div className="h-6 w-px bg-slate-200 mx-2" />
                    <UserProfile
                        displayName={displayName}
                        mainRole={mainRole}
                        avatarUrl={user?.avatar}
                        onClick={handleProfileOpen}
                    />
                </div>
            </header>

            <ProfileDrawer open={profileOpen} onClose={handleProfileClose} />
        </>
    );
};
