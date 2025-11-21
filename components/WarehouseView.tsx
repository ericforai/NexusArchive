import React, { useState } from 'react';
import { Warehouse, Thermometer, Droplets, Lock, Unlock, Wind, AlertCircle, Battery, Signal, ChevronRight } from 'lucide-react';

interface Rack {
  id: string;
  label: string;
  status: 'closed' | 'open-left' | 'open-right' | 'ventilating';
  locked: boolean;
  usage: number;
  temp: number;
  humidity: number;
}

const MOCK_RACKS: Rack[] = Array.from({ length: 8 }).map((_, i) => ({
  id: `rack-${i + 1}`,
  label: `第 ${i + 1} 列`,
  status: i === 2 ? 'open-left' : 'closed',
  locked: i === 0,
  usage: Math.floor(Math.random() * 40) + 50,
  temp: 22 + Math.random(),
  humidity: 45 + Math.random() * 5,
}));

export const WarehouseView: React.FC = () => {
  const [racks, setRacks] = useState<Rack[]>(MOCK_RACKS);
  const [selectedRack, setSelectedRack] = useState<Rack | null>(null);

  const toggleLock = (id: string) => {
    setRacks(prev => prev.map(r => r.id === id ? { ...r, locked: !r.locked } : r));
  };

  const operateRack = (id: string, action: 'open' | 'close' | 'vent') => {
    setRacks(prev => prev.map(r => {
      if (r.id !== id) return r;
      if (r.locked) {
        alert('密集架已锁定，请先解锁');
        return r;
      }
      let newStatus = r.status;
      if (action === 'open') newStatus = r.status === 'closed' ? 'open-left' : 'closed'; // Simplified toggle
      if (action === 'close') newStatus = 'closed';
      if (action === 'vent') newStatus = 'ventilating';
      return { ...r, status: newStatus as any };
    }));
  };

  return (
    <div className="h-full flex flex-col bg-slate-50 animate-in fade-in duration-500">
      
      {/* Environment Dashboard Header */}
      <div className="bg-white border-b border-slate-200 p-6 grid grid-cols-1 md:grid-cols-4 gap-6 shadow-sm z-10">
        <div className="flex items-center gap-4">
          <div className="p-3 bg-blue-50 rounded-xl text-blue-600">
            <Warehouse size={24} />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-800">一号库房 (A区)</h2>
            <p className="text-xs text-slate-500">智能密集架控制系统在线</p>
          </div>
        </div>
        
        <div className="flex items-center gap-4 border-l border-slate-100 pl-6">
          <div className="p-2 bg-emerald-50 rounded-lg text-emerald-600">
            <Thermometer size={20} />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-medium uppercase">平均温度</p>
            <p className="text-xl font-bold text-slate-800">22.4°C <span className="text-xs font-normal text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded ml-1">正常</span></p>
          </div>
        </div>

        <div className="flex items-center gap-4 border-l border-slate-100 pl-6">
          <div className="p-2 bg-cyan-50 rounded-lg text-cyan-600">
            <Droplets size={20} />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-medium uppercase">平均湿度</p>
            <p className="text-xl font-bold text-slate-800">45.8% <span className="text-xs font-normal text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded ml-1">正常</span></p>
          </div>
        </div>

        <div className="flex items-center justify-end gap-2 border-l border-slate-100 pl-6">
            <button className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 shadow-sm active:scale-95 transition-all">
                全库通风
            </button>
            <button className="px-4 py-2 bg-rose-50 border border-rose-100 text-rose-600 rounded-lg text-sm font-medium hover:bg-rose-100 shadow-sm active:scale-95 transition-all">
                紧急停止
            </button>
        </div>
      </div>

      {/* Visual Layout */}
      <div className="flex-1 p-8 overflow-hidden flex gap-8">
        
        {/* Racks Visualization (Top Down / Side View Hybrid) */}
        <div className="flex-1 bg-slate-200/50 rounded-2xl border-2 border-dashed border-slate-300 p-8 relative overflow-auto flex items-center justify-center perspective-[1000px]">
           <div className="flex gap-4 items-end transform rotate-x-12">
              {racks.map((rack) => (
                <div 
                  key={rack.id}
                  onClick={() => setSelectedRack(rack)}
                  className={`relative w-24 transition-all duration-700 ease-in-out cursor-pointer group ${selectedRack?.id === rack.id ? 'translate-y-[-20px]' : ''}`}
                  style={{
                    height: '400px',
                    transformStyle: 'preserve-3d',
                    marginRight: rack.status.includes('open') ? '80px' : '0' // Simulate opening gap
                  }}
                >
                  {/* Rack Face */}
                  <div className={`absolute inset-0 bg-white border border-slate-300 rounded-lg shadow-xl flex flex-col items-center justify-between py-4 transition-colors ${rack.status === 'ventilating' ? 'animate-pulse ring-2 ring-blue-400' : ''} ${selectedRack?.id === rack.id ? 'ring-2 ring-primary-500' : 'hover:ring-2 hover:ring-primary-200'}`}>
                    
                    {/* Top Status Light */}
                    <div className={`w-16 h-2 rounded-full ${rack.locked ? 'bg-rose-500' : rack.status !== 'closed' ? 'bg-emerald-500' : 'bg-slate-300'}`}></div>

                    {/* Shelves Lines */}
                    <div className="flex-1 w-full px-2 py-4 space-y-4 flex flex-col justify-center opacity-50">
                       <div className="w-full h-px bg-slate-300"></div>
                       <div className="w-full h-px bg-slate-300"></div>
                       <div className="w-full h-px bg-slate-300"></div>
                       <div className="w-full h-px bg-slate-300"></div>
                       <div className="w-full h-px bg-slate-300"></div>
                    </div>

                    {/* Label */}
                    <div className="text-center">
                       <div className="text-xl font-bold text-slate-700 font-mono mb-1">{rack.id.split('-')[1]}</div>
                       {rack.locked && <Lock size={14} className="mx-auto text-rose-500" />}
                    </div>
                  </div>

                  {/* Side Face (Simulating 3D depth) */}
                  <div className="absolute top-2 -right-4 w-4 h-[390px] bg-slate-100 border-y border-r border-slate-300 rounded-r-lg transform skew-y-[30deg] origin-top-left"></div>
                </div>
              ))}
           </div>
           
           <div className="absolute bottom-4 left-8 text-slate-400 text-sm flex items-center gap-2">
              <AlertCircle size={16} /> 点击密集架查看详情与控制
           </div>
        </div>

        {/* Control Panel */}
        <div className="w-80 bg-white rounded-2xl shadow-lg border border-slate-100 flex flex-col">
          {selectedRack ? (
            <>
              <div className="p-6 border-b border-slate-100 bg-slate-50/50 rounded-t-2xl">
                <h3 className="text-lg font-bold text-slate-800 flex items-center gap-2">
                  {selectedRack.label} 控制台
                </h3>
                <div className="flex items-center gap-2 mt-2 text-xs">
                   <span className={`px-2 py-0.5 rounded border ${selectedRack.locked ? 'bg-rose-50 text-rose-600 border-rose-100' : 'bg-emerald-50 text-emerald-600 border-emerald-100'}`}>
                      {selectedRack.locked ? '已锁定' : '正常'}
                   </span>
                   <span className="px-2 py-0.5 rounded border bg-blue-50 text-blue-600 border-blue-100 capitalize">
                      {selectedRack.status === 'closed' ? '闭合' : selectedRack.status === 'ventilating' ? '通风中' : '打开'}
                   </span>
                </div>
              </div>
              
              <div className="p-6 space-y-6 flex-1 overflow-y-auto">
                 {/* Stats */}
                 <div className="grid grid-cols-2 gap-4">
                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                       <div className="text-xs text-slate-400 mb-1 flex items-center gap-1"><Thermometer size={12}/> 温度</div>
                       <div className="text-lg font-bold text-slate-700">{selectedRack.temp.toFixed(1)}°C</div>
                    </div>
                    <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                       <div className="text-xs text-slate-400 mb-1 flex items-center gap-1"><Droplets size={12}/> 湿度</div>
                       <div className="text-lg font-bold text-slate-700">{selectedRack.humidity.toFixed(1)}%</div>
                    </div>
                    <div className="col-span-2 bg-slate-50 p-3 rounded-xl border border-slate-100">
                       <div className="flex justify-between mb-1">
                          <span className="text-xs text-slate-400">存储空间使用率</span>
                          <span className="text-xs font-bold text-slate-700">{selectedRack.usage}%</span>
                       </div>
                       <div className="w-full h-2 bg-slate-200 rounded-full overflow-hidden">
                          <div className="h-full bg-primary-500 rounded-full" style={{width: `${selectedRack.usage}%`}}></div>
                       </div>
                    </div>
                 </div>

                 {/* Controls */}
                 <div className="space-y-3">
                    <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">操作指令</p>
                    <button 
                      onClick={() => operateRack(selectedRack.id, 'open')}
                      disabled={selectedRack.locked}
                      className="w-full py-3 bg-primary-600 hover:bg-primary-700 text-white rounded-xl font-medium shadow-lg shadow-primary-500/30 transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                       <ChevronRight size={18} /> {selectedRack.status === 'closed' ? '打开架体' : '闭合架体'}
                    </button>
                    <div className="grid grid-cols-2 gap-3">
                       <button 
                         onClick={() => operateRack(selectedRack.id, 'vent')}
                         disabled={selectedRack.locked}
                         className="py-3 bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-xl font-medium transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                       >
                         <Wind size={18} /> 通风
                       </button>
                       <button 
                         onClick={() => toggleLock(selectedRack.id)}
                         className={`py-3 border rounded-xl font-medium transition-all flex items-center justify-center gap-2 ${selectedRack.locked ? 'bg-amber-50 border-amber-200 text-amber-600 hover:bg-amber-100' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50'}`}
                       >
                         {selectedRack.locked ? <Unlock size={18} /> : <Lock size={18} />}
                         {selectedRack.locked ? '解锁' : '锁定'}
                       </button>
                    </div>
                 </div>

                 {/* Device Status */}
                 <div className="pt-4 border-t border-slate-100 space-y-2">
                    <div className="flex items-center justify-between text-xs">
                       <span className="text-slate-500 flex items-center gap-1"><Signal size={12}/> 信号强度</span>
                       <span className="text-emerald-600 font-medium">强 (-45dBm)</span>
                    </div>
                    <div className="flex items-center justify-between text-xs">
                       <span className="text-slate-500 flex items-center gap-1"><Battery size={12}/> 备用电源</span>
                       <span className="text-slate-700 font-medium">100%</span>
                    </div>
                 </div>
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center flex-col text-slate-300 p-8 text-center">
              <Warehouse size={48} className="mb-4 opacity-50" />
              <p className="font-medium">点击左侧密集架<br/>进行查看与控制</p>
            </div>
          )}
        </div>

      </div>
    </div>
  );
};