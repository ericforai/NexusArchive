// Input: React、lucide-react 图标、本地模块 GlobalSearch、api/search、FondsSwitcher
// Output: React 组件 TopBar
// Pos: 业务页面组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { GlobalSearch } from './GlobalSearch';
import { GlobalSearchDTO } from '../types';
import { FondsSwitcher } from './common/FondsSwitcher';

interface TopBarProps {
  onLogout?: () => void;
  onNavigate?: (item: GlobalSearchDTO) => void;
}

export const TopBar: React.FC<TopBarProps> = ({ onLogout, onNavigate }) => {
  const handleClick = (item: string) => {
    if (item === '退出登录' && onLogout) {
      onLogout();
      return;
    }
    alert(`打开: ${item}`);
  }

  return (
    <header className="h-16 bg-white/80 backdrop-blur-md border-b border-slate-200 px-8 flex items-center justify-between sticky top-0 z-10">

      {/* Left: Fonds Switcher */}
      <div className="flex items-center gap-4">
        <FondsSwitcher />
      </div>

      {/* Center: Global Search */}
      <div className="flex-1 max-w-2xl mx-4">
        <GlobalSearch onNavigate={onNavigate} />
      </div>

      {/* Right Actions */}
      <div className="flex items-center space-x-4 ml-4">



        <div className="h-6 w-px bg-slate-200 mx-2"></div>

        <div className="flex items-center space-x-2 cursor-pointer hover:opacity-80 transition-opacity group relative">
          <div className="text-right hidden md:block" onClick={() => handleClick('用户个人资料')}>
            <p className="text-sm font-bold text-slate-800">企业管理员</p>
            <p className="text-xs text-slate-500">集团财务部</p>
          </div>
          <img src="https://picsum.photos/40/40" alt="Profile" className="h-9 w-9 rounded-full ring-2 ring-white shadow-sm" onClick={() => handleClick('用户个人资料')} />

          {/* Dropdown for Logout */}
          {onLogout && (
            <div className="absolute top-full right-0 mt-2 w-48 bg-white rounded-lg shadow-xl border border-slate-100 py-1 hidden group-hover:block animate-in fade-in slide-in-from-top-2 before:absolute before:-top-2 before:left-0 before:w-full before:h-2 before:content-['']">
              <button
                onClick={() => handleClick('退出登录')}
                className="w-full text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 font-medium"
              >
                退出登录
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};

