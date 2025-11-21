import React from 'react';
import { Search, Bell, HelpCircle, Grid } from 'lucide-react';

export const TopBar: React.FC = () => {
  const handleClick = (item: string) => {
    alert(`打开: ${item}`);
  }

  return (
    <header className="h-16 bg-white/80 backdrop-blur-md border-b border-slate-200 px-8 flex items-center justify-between sticky top-0 z-10">
      
      {/* Global Search */}
      <div className="flex-1 max-w-2xl">
        <div className="relative group">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <Search size={18} className="text-slate-400 group-focus-within:text-primary-500 transition-colors" />
          </div>
          <input
            type="text"
            className="block w-full pl-10 pr-3 py-2 border border-slate-200 rounded-xl leading-5 bg-slate-50 text-slate-900 placeholder-slate-400 focus:outline-none focus:bg-white focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 transition-all sm:text-sm"
            placeholder="全库检索: 凭证号、摘要、金额、关联单据..."
            onKeyDown={(e) => e.key === 'Enter' && handleClick(`搜索: ${e.currentTarget.value}`)}
          />
          <div className="absolute inset-y-0 right-0 pr-2 flex items-center">
            <kbd className="inline-flex items-center border border-slate-200 rounded px-2 text-xs font-sans font-medium text-slate-400 cursor-pointer hover:bg-slate-100" onClick={() => handleClick('快捷键帮助')}>
              ⌘K
            </kbd>
          </div>
        </div>
      </div>

      {/* Right Actions */}
      <div className="flex items-center space-x-4 ml-4">
        
        <button onClick={() => handleClick('应用网格')} className="p-2 text-slate-500 hover:bg-slate-100 rounded-full transition-colors relative active:scale-95">
          <Grid size={20} />
        </button>
        
        <button onClick={() => handleClick('消息通知中心')} className="p-2 text-slate-500 hover:bg-slate-100 rounded-full transition-colors relative active:scale-95">
          <Bell size={20} />
          <span className="absolute top-1.5 right-1.5 w-2 h-2 bg-rose-500 rounded-full border-2 border-white"></span>
        </button>

        <button onClick={() => handleClick('帮助中心')} className="p-2 text-slate-500 hover:bg-slate-100 rounded-full transition-colors active:scale-95">
          <HelpCircle size={20} />
        </button>
        
        <div className="h-6 w-px bg-slate-200 mx-2"></div>
        
        <div onClick={() => handleClick('用户个人资料')} className="flex items-center space-x-2 cursor-pointer hover:opacity-80 transition-opacity">
           <div className="text-right hidden md:block">
             <p className="text-sm font-bold text-slate-800">企业管理员</p>
             <p className="text-xs text-slate-500">集团财务部</p>
           </div>
           <img src="https://picsum.photos/40/40" alt="Profile" className="h-9 w-9 rounded-full ring-2 ring-white shadow-sm" />
        </div>
      </div>
    </header>
  );
};