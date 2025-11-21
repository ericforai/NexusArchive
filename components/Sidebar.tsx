import React from 'react';
import { NAV_ITEMS } from '../constants';
import { ViewState } from '../types';
import { ChevronRight, Command } from 'lucide-react';

interface SidebarProps {
  activeView: ViewState;
  setActiveView: (view: ViewState) => void;
  activeSubItem: string;
  setActiveSubItem: (item: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ activeView, setActiveView, activeSubItem, setActiveSubItem }) => {
  const handleMainClick = (id: ViewState, hasSubItems: boolean) => {
    setActiveView(id);
    // If switching main views, clear sub-item unless we want to default to first
    if (id !== activeView) {
      setActiveSubItem('');
    }
  };

  const handleSubClick = (e: React.MouseEvent, subItem: string) => {
    e.stopPropagation();
    setActiveSubItem(subItem);
  };

  return (
    <aside className="w-64 h-screen bg-slate-900 text-slate-300 flex flex-col border-r border-slate-800 shadow-2xl z-20 relative overflow-hidden">
      {/* Background Decorative Elements */}
      <div className="absolute top-0 left-0 w-full h-64 bg-gradient-to-b from-blue-900/20 to-transparent pointer-events-none" />
      
      {/* Logo Area */}
      <div className="p-6 flex items-center space-x-3 border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm z-10">
        <div className="w-10 h-10 bg-gradient-to-br from-blue-500 to-cyan-400 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/20 text-white">
          <Command size={20} />
        </div>
        <div>
          <h1 className="font-bold text-white text-lg leading-tight tracking-tight">NexusArchive</h1>
          <p className="text-xs text-slate-500 font-medium">电子会计档案</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-6 px-3 space-y-1 scrollbar-thin scrollbar-thumb-slate-700">
        {NAV_ITEMS.map((item) => {
          const isActive = activeView === item.id;
          return (
            <div key={item.id} className="group">
              <button
                onClick={() => handleMainClick(item.id, !!item.subItems)}
                className={`w-full flex items-center justify-between px-4 py-3 rounded-xl transition-all duration-300 ${
                  isActive
                    ? 'bg-primary-600/10 text-white shadow-md shadow-primary-900/20 border border-primary-500/20'
                    : 'hover:bg-slate-800/50 hover:text-slate-100'
                }`}
              >
                <div className="flex items-center space-x-3">
                  <item.icon
                    size={20}
                    className={`transition-colors ${isActive ? 'text-primary-400' : 'text-slate-400 group-hover:text-slate-300'}`}
                  />
                  <span className="font-medium text-sm tracking-wide">{item.label}</span>
                </div>
                {isActive && <div className="w-1.5 h-1.5 rounded-full bg-primary-400 shadow-[0_0_8px_rgba(56,189,248,0.8)]" />}
              </button>
              
              {/* Subitems (Visible if active and exist) */}
              {isActive && item.subItems && (
                <div className="mt-1 ml-10 space-y-1 border-l border-slate-800 pl-2 animate-in slide-in-from-left-2 duration-300">
                  {item.subItems.map((sub) => {
                    const isSubActive = activeSubItem === sub;
                    return (
                      <button
                        key={sub}
                        onClick={(e) => handleSubClick(e, sub)}
                        className={`w-full text-left px-3 py-2 text-xs rounded-lg transition-colors flex items-center group/sub ${
                          isSubActive 
                            ? 'text-primary-300 bg-slate-800/60 font-medium' 
                            : 'text-slate-400 hover:text-primary-300 hover:bg-slate-800/30'
                        }`}
                      >
                        <ChevronRight size={10} className={`mr-2 transition-opacity ${isSubActive ? 'opacity-100' : 'opacity-0 group-hover/sub:opacity-100'}`} />
                        {sub}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}
      </nav>

      {/* Footer User Info */}
      <div className="p-4 border-t border-slate-800 bg-slate-900/80 z-10">
        <button 
          onClick={() => alert('打开用户个人中心')}
          className="w-full flex items-center space-x-3 px-2 py-2 rounded-lg bg-slate-800/50 border border-slate-700/50 hover:bg-slate-800 transition-colors"
        >
          <img src="https://picsum.photos/32/32" alt="User" className="w-8 h-8 rounded-full ring-2 ring-slate-700" />
          <div className="flex-1 overflow-hidden text-left">
            <p className="text-sm font-medium text-white truncate">管理员</p>
            <p className="text-xs text-slate-400 truncate">系统维护部</p>
          </div>
        </button>
      </div>
    </aside>
  );
};