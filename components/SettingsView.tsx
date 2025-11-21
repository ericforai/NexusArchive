import React from 'react';
import { Save } from 'lucide-react';

export const SettingsView: React.FC = () => {
  return (
    <div className="p-8 space-y-6 max-w-4xl mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500">
       <div className="flex justify-between items-center">
         <div>
            <h2 className="text-2xl font-bold text-slate-800">系统设置</h2>
            <p className="text-slate-500">配置全局参数、用户权限及安全策略。</p>
         </div>
         <button className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 flex items-center shadow-lg shadow-primary-500/30 transition-all">
            <Save size={16} className="mr-2" /> 保存更改
         </button>
       </div>

       <div className="space-y-6">
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
             <h3 className="text-lg font-bold text-slate-800 mb-4 border-b border-slate-100 pb-2">基础设置</h3>
             <div className="grid grid-cols-1 gap-4">
                <div className="flex flex-col">
                   <label className="text-sm font-medium text-slate-700 mb-1">系统名称</label>
                   <input type="text" defaultValue="NexusArchive 电子会计档案" className="border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none" />
                </div>
                <div className="flex flex-col">
                   <label className="text-sm font-medium text-slate-700 mb-1">归档全宗号前缀</label>
                   <input type="text" defaultValue="QZ-2024-" className="border border-slate-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none" />
                </div>
             </div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
             <h3 className="text-lg font-bold text-slate-800 mb-4 border-b border-slate-100 pb-2">安全与合规</h3>
             <div className="space-y-4">
                <div className="flex items-center justify-between">
                   <div>
                      <p className="text-sm font-medium text-slate-800">开启四性检测强控</p>
                      <p className="text-xs text-slate-500">归档前必须通过四性检测，否则无法入库</p>
                   </div>
                   <div className="relative inline-block w-10 mr-2 align-middle select-none transition duration-200 ease-in">
                        <input type="checkbox" name="toggle" id="toggle" className="toggle-checkbox absolute block w-5 h-5 rounded-full bg-white border-4 appearance-none cursor-pointer checked:right-0 checked:border-primary-500"/>
                        <label htmlFor="toggle" className="toggle-label block overflow-hidden h-5 rounded-full bg-gray-300 cursor-pointer checked:bg-primary-500"></label>
                    </div>
                </div>
                <div className="flex items-center justify-between">
                   <div>
                      <p className="text-sm font-medium text-slate-800">水印强制开启</p>
                      <p className="text-xs text-slate-500">所有借阅预览必须强制添加动态水印</p>
                   </div>
                   <div className="relative inline-block w-10 mr-2 align-middle select-none transition duration-200 ease-in">
                        <input type="checkbox" checked readOnly className="toggle-checkbox absolute block w-5 h-5 rounded-full bg-white border-4 appearance-none cursor-pointer right-0 border-primary-500"/>
                        <label className="toggle-label block overflow-hidden h-5 rounded-full bg-primary-500 cursor-pointer"></label>
                    </div>
                </div>
             </div>
          </div>
       </div>
    </div>
  );
};
