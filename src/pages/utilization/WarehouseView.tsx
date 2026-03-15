// Input: React、lucide-react 图标、本地模块 api/warehouse
// Output: React 组件 WarehouseView
// Pos: src/pages/utilization/WarehouseView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Warehouse, Thermometer, Droplets, Lock, Unlock, Wind, AlertCircle, Battery, Signal, ChevronRight, Loader2, Home } from 'lucide-react';
import { warehouseApi, Shelf, WarehouseEnvironment } from '../../api/warehouse';

type ViewMode = 'racks' | 'environment' | null;

interface Rack {
  id: string;
  label: string;
  status: 'closed' | 'open-left' | 'open-right' | 'ventilating';
  locked: boolean;
  usage: number;
  temp: number;
  humidity: number;
}

/**
 * 密集架控制视图
 */
const RacksView: React.FC<{
  racks: Rack[];
  selectedRack: Rack | null;
  onSelectRack: (rack: Rack) => void;
  onToggleLock: (id: string) => void;
  onOperateRack: (id: string, action: 'open' | 'close' | 'vent') => void;
}> = ({ racks, selectedRack, onSelectRack, onToggleLock, onOperateRack }) => {
  if (racks.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center">
        <div className="text-slate-400 flex items-center gap-2">
          <AlertCircle size={18} />
          <span>暂无架位数据</span>
        </div>
      </div>
    );
  }

  return (
    <>
      {/* Racks Visualization */}
      <div className="flex-1 bg-slate-200/50 rounded-2xl border-2 border-dashed border-slate-300 p-8 relative overflow-auto flex items-center justify-center perspective-[1000px]">
        <div className="flex gap-4 items-end transform rotate-x-12">
          {racks.map((rack) => (
            <div
              key={rack.id}
              onClick={() => onSelectRack(rack)}
              className={`relative w-24 transition-all duration-700 ease-in-out cursor-pointer group ${selectedRack?.id === rack.id ? 'translate-y-[-20px]' : ''}`}
              style={{
                height: '400px',
                transformStyle: 'preserve-3d',
                marginRight: rack.status.includes('open') ? '80px' : '0',
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
                  <div className="text-xl font-bold text-slate-700 font-mono mb-1">{rack.label}</div>
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
      {selectedRack && (
        <div className="w-80 bg-white rounded-2xl shadow-lg border border-slate-100 flex flex-col">
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
                <div className="text-xs text-slate-400 mb-1 flex items-center gap-1"><Thermometer size={12} /> 温度</div>
                <div className="text-lg font-bold text-slate-700">{selectedRack.temp.toFixed(1)}°C</div>
              </div>
              <div className="bg-slate-50 p-3 rounded-xl border border-slate-100">
                <div className="text-xs text-slate-400 mb-1 flex items-center gap-1"><Droplets size={12} /> 湿度</div>
                <div className="text-lg font-bold text-slate-700">{selectedRack.humidity.toFixed(1)}%</div>
              </div>
              <div className="col-span-2 bg-slate-50 p-3 rounded-xl border border-slate-100">
                <div className="flex justify-between mb-1">
                  <span className="text-xs text-slate-400">存储空间使用率</span>
                  <span className="text-xs font-bold text-slate-700">{selectedRack.usage}%</span>
                </div>
                <div className="w-full h-2 bg-slate-200 rounded-full overflow-hidden">
                  <div className="h-full bg-primary-500 rounded-full" style={{ width: `${selectedRack.usage}%` }}></div>
                </div>
              </div>
            </div>

            {/* Controls */}
            <div className="space-y-3">
              <p className="text-xs font-bold text-slate-400 uppercase tracking-wider">操作指令（前端模拟）</p>
              <button
                onClick={() => onOperateRack(selectedRack.id, 'open')}
                disabled={selectedRack.locked}
                className="w-full py-3 bg-primary-600 hover:bg-primary-700 text-white rounded-xl font-medium shadow-lg shadow-primary-500/30 transition-all active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                <ChevronRight size={18} /> {selectedRack.status === 'closed' ? '打开架体' : '闭合架体'}
              </button>
              <div className="grid grid-cols-2 gap-3">
                <button
                  onClick={() => onOperateRack(selectedRack.id, 'vent')}
                  disabled={selectedRack.locked}
                  className="py-3 bg-white border border-slate-200 text-slate-600 hover:bg-slate-50 rounded-xl font-medium transition-all flex items-center justify-center gap-2 disabled:opacity-50"
                >
                  <Wind size={18} /> 通风
                </button>
                <button
                  onClick={() => onToggleLock(selectedRack.id)}
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
                <span className="text-slate-500 flex items-center gap-1"><Signal size={12} /> 信号强度</span>
                <span className="text-emerald-600 font-medium">强 (-45dBm)</span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-500 flex items-center gap-1"><Battery size={12} /> 备用电源</span>
                <span className="text-slate-700 font-medium">100%</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

/**
 * 环境监控视图
 */
const EnvironmentView: React.FC<{
  environment: WarehouseEnvironment | null;
  onRefresh: () => void;
}> = ({ environment, onRefresh }) => {
  return (
    <div className="flex-1 flex flex-col">
      {/* Environment Dashboard Header */}
      <div className="bg-white border-b border-slate-200 p-6 grid grid-cols-1 md:grid-cols-4 gap-6 shadow-sm z-10">
        <div className="flex items-center gap-4">
          <div className="p-3 bg-blue-50 rounded-xl text-blue-600">
            <Warehouse size={24} />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-800">库房环境监控</h2>
            <p className="text-xs text-slate-500">智能传感器实时监控{environment ? '在线' : '加载中...'}</p>
          </div>
        </div>

        <div className="flex items-center gap-4 border-l border-slate-100 pl-6">
          <div className="p-2 bg-emerald-50 rounded-lg text-emerald-600">
            <Thermometer size={20} />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-medium uppercase">平均温度</p>
            <p className="text-xl font-bold text-slate-800">
              {environment?.temperature !== undefined ? `${environment.temperature.toFixed(1)}°C` : '--'}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-4 border-l border-slate-100 pl-6">
          <div className="p-2 bg-cyan-50 rounded-lg text-cyan-600">
            <Droplets size={20} />
          </div>
          <div>
            <p className="text-xs text-slate-400 font-medium uppercase">平均湿度</p>
            <p className="text-xl font-bold text-slate-800">
              {environment?.humidity !== undefined ? `${environment.humidity.toFixed(1)}%` : '--'}
            </p>
          </div>
        </div>

        <div className="flex items-center justify-end gap-3 border-l border-slate-100 pl-6">
          <div className="text-xs text-slate-400 text-right">
            {environment?.lastUpdated ? `最近上报 ${environment.lastUpdated}` : '等待传感器上报'}
          </div>
          <button
            onClick={onRefresh}
            className="px-3 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 shadow-sm active:scale-95 transition-all"
          >
            刷新数据
          </button>
        </div>
      </div>

      {/* Device Status Cards */}
      <div className="flex-1 p-8 bg-slate-50">
        {environment ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 max-w-6xl mx-auto">
            {/* Temperature Card */}
            <div className="bg-white rounded-xl shadow-md border border-slate-200 p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2 bg-red-50 rounded-lg text-red-600">
                  <Thermometer size={24} />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-800">温度</h3>
                  <p className="text-xs text-slate-500">实时监测</p>
                </div>
              </div>
              <div className="text-3xl font-bold text-slate-900">
                {environment.temperature?.toFixed(1)}°C
              </div>
              <div className={`mt-2 px-3 py-1 rounded text-center text-sm font-medium ${
                parseFloat(environment.temperature?.toFixed(1) || '0') > 30 ? 'bg-red-100 text-red-600' :
                parseFloat(environment.temperature?.toFixed(1) || '0') < 18 ? 'bg-blue-100 text-blue-600' :
                'bg-emerald-100 text-emerald-600'
              }`}>
                {parseFloat(environment.temperature?.toFixed(1) || '0') > 30 ? '温度过高' :
                 parseFloat(environment.temperature?.toFixed(1) || '0') < 18 ? '温度偏低' : '正常'}
              </div>
            </div>

            {/* Humidity Card */}
            <div className="bg-white rounded-xl shadow-md border border-slate-200 p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2 bg-blue-50 rounded-lg text-blue-600">
                  <Droplets size={24} />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-800">湿度</h3>
                  <p className="text-xs text-slate-500">实时监测</p>
                </div>
              </div>
              <div className="text-3xl font-bold text-slate-900">
                {environment.humidity?.toFixed(1)}%
              </div>
              <div className={`mt-2 px-3 py-1 rounded text-center text-sm font-medium ${
                parseFloat(environment.humidity?.toFixed(1) || '0') > 70 ? 'bg-red-100 text-red-600' :
                parseFloat(environment.humidity?.toFixed(1) || '0') < 40 ? 'bg-amber-100 text-amber-600' :
                'bg-emerald-100 text-emerald-600'
              }`}>
                {parseFloat(environment.humidity?.toFixed(1) || '0') > 70 ? '湿度过高' :
                 parseFloat(environment.humidity?.toFixed(1) || '0') < 40 ? '湿度过低' : '正常'}
              </div>
            </div>

            {/* Status Card */}
            <div className="bg-white rounded-xl shadow-md border border-slate-200 p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2 bg-emerald-50 rounded-lg text-emerald-600">
                  <Signal size={24} />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-800">设备状态</h3>
                  <p className="text-xs text-slate-500">传感器网络</p>
                </div>
              </div>
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-slate-600">信号强度</span>
                  <span className="text-lg font-bold text-emerald-600">强 (-45dBm)</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-slate-600">备用电源</span>
                  <span className="text-lg font-bold text-emerald-600">{environment.status || '正常'}</span>
                </div>
              </div>
            </div>

            {/* Last Update Card */}
            <div className="bg-white rounded-xl shadow-md border border-slate-200 p-6">
              <div className="flex items-center gap-3 mb-4">
                <div className="p-2 bg-slate-100 rounded-lg text-slate-600">
                  <Battery size={24} />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-slate-800">数据更新</h3>
                  <p className="text-xs text-slate-500">传感器上报时间</p>
                </div>
              </div>
              <div className="text-center">
                <p className="text-sm text-slate-600">{environment.lastUpdated || '等待上报'}</p>
              </div>
            </div>
          </div>
        ) : (
          <div className="flex-1 flex items-center justify-center text-slate-300">
            <Loader2 className="animate-spin" size={48} />
            <p className="ml-4 text-slate-500">正在加载环境数据...</p>
          </div>
        )}
      </div>
    </div>
  );
};

/**
 * 空状态/兜底视图
 */
const EmptyView: React.FC<{ onNavigateBack: () => void }> = ({ onNavigateBack }) => {
  return (
    <div className="flex-1 flex items-center justify-center text-slate-300">
      <div className="text-center">
        <Warehouse size={64} className="mx-auto mb-4 opacity-30" />
        <h2 className="text-xl font-bold text-slate-600 mb-2">库房管理系统</h2>
        <p className="text-slate-500 mb-6">请从上方菜单选择要查看的功能</p>
        <div className="flex gap-3 justify-center">
          <button
            onClick={() => onNavigateBack()}
            className="px-4 py-2 bg-white border border-slate-300 text-slate-600 hover:bg-slate-50 rounded-lg text-sm font-medium transition-all"
          >
            <Home size={16} className="mr-2" />
            返回首页
          </button>
        </div>
      </div>
    </div>
  );
};

export const WarehouseView: React.FC = () => {
  const [searchParams] = useSearchParams();
  const viewParam = (searchParams.get('view') || null) as ViewMode;

  const [racks, setRacks] = useState<Rack[]>([]);
  const [selectedRack, setSelectedRack] = useState<Rack | null>(null);
  const [environment, setEnvironment] = useState<WarehouseEnvironment | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const mapShelvesToRacks = useCallback((shelves: Shelf[], env?: WarehouseEnvironment): Rack[] => {
    return shelves.map((shelf, i) => {
      const usage = shelf.capacity && shelf.capacity > 0
        ? Math.min(100, Math.round(((shelf.usedCount || 0) / shelf.capacity) * 100))
        : shelf.usedCount || 0;
      let status: Rack['status'] = 'closed';
      switch ((shelf.status || '').toUpperCase()) {
        case 'OPEN':
          status = 'open-left';
          break;
        case 'VENTILATING':
        case 'MAINTENANCE':
          status = 'ventilating';
          break;
        case 'FULL':
        case 'CLOSED':
        case 'NORMAL':
        default:
          status = 'closed';
      }

      return {
        id: shelf.id,
        label: shelf.name || shelf.code || `架位 ${i + 1}`,
        status: status as Rack['status'],
        locked: false,
        usage,
        temp: env?.temperature || 22,
        humidity: env?.humidity || 45
      };
    });
  }, []);

  const loadEnvironment = useCallback(async () => {
    try {
      const res = await warehouseApi.getEnvironment();
      if (res.code === 200 && res.data) {
        setEnvironment(res.data);
        return res.data;
      }
    } catch (e) {
      console.warn('Failed to load environment', e);
    }
    setEnvironment(null);
    return null;
  }, []);

  const _loadShelves = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const env = await loadEnvironment();
      const res = await warehouseApi.getShelves();
      if (res.code === 200 && res.data && res.data.length > 0) {
        const mapped = mapShelvesToRacks(res.data, env || undefined);
        setRacks(mapped);
        setSelectedRack(mapped[0] || null);
      } else {
        setRacks([]);
        setSelectedRack(null);
      }
    } catch (e) {
      console.warn('Failed to load shelves', e);
      setRacks([]);
      setSelectedRack(null);
    } finally {
      setLoading(false);
    }
  }, [loadEnvironment, mapShelvesToRacks]);

  const toggleLock = async (id: string) => {
    setError(null);
    let nextLocked = false;
    setRacks(prev => prev.map(r => {
      if (r.id === id) {
        nextLocked = !r.locked;
        return { ...r, locked: nextLocked };
      }
      return r;
    }));
    try {
      await warehouseApi.sendCommand(id, nextLocked ? 'lock' : 'unlock');
    } catch (e) {
      console.warn('Lock operation failed', e);
    }
  };

  const operateRack = async (id: string, action: 'open' | 'close' | 'vent') => {
    setError(null);
    let blocked = false;
    setRacks(prev => prev.map(r => {
      if (r.id !== id) return r;
      if (r.locked) {
        blocked = true;
        return r;
      }
      let newStatus = r.status;
      if (action === 'open') newStatus = r.status === 'closed' ? 'open-left' : 'closed';
      if (action === 'close') newStatus = 'closed';
      if (action === 'vent') newStatus = 'ventilating';
      return { ...r, status: newStatus as any };
    }));
    if (blocked) {
      setError('密集架已锁定，请先解锁');
      return;
    }
    try {
      await warehouseApi.sendCommand(id, action === 'vent' ? 'vent' : action === 'open' ? 'open' : 'close');
    } catch (e) {
      console.warn('Rack operation failed', e);
    }
  };

  // 根据视图模式渲染不同内容
  if (viewParam === 'environment') {
    return (
      <div className="h-full flex flex-col bg-slate-50">
        {error && (
          <div className="bg-rose-50 border border-rose-200 text-rose-700 px-4 py-2 text-sm m-4 rounded-lg shadow-sm">
            {error}
          </div>
        )}
        {loading && (
          <div className="absolute inset-0 bg-white/70 backdrop-blur-sm flex items-center justify-center z-20">
            <div className="flex items-center gap-2 text-slate-600 text-sm">
              <Loader2 className="animate-spin" size={18} /> 正在刷新环境数据...
            </div>
          </div>
        )}
        <EnvironmentView environment={environment} onRefresh={loadEnvironment} />
      </div>
    );
  }

  if (viewParam === 'racks' || viewParam === null) {
    return (
      <div className="h-full flex flex-col bg-slate-50">
        {error && (
          <div className="bg-rose-50 border border-rose-200 text-rose-700 px-4 py-2 text-sm m-4 rounded-lg shadow-sm">
            {error}
          </div>
        )}
        {loading && (
          <div className="absolute inset-0 bg-white/70 backdrop-blur-sm flex items-center justify-center z-20">
            <div className="flex items-center gap-2 text-slate-600 text-sm">
              <Loader2 className="animate-spin" size={18} /> 正在刷新架位数据...
            </div>
          </div>
        )}
        <RacksView
          racks={racks}
          selectedRack={selectedRack}
          onSelectRack={setSelectedRack}
          onToggleLock={toggleLock}
          onOperateRack={operateRack}
        />
      </div>
    );
  }

  // 无效视图参数时显示空状态
  return <EmptyView onNavigateBack={() => window.location.href = '/system'} />;
};

export default WarehouseView;
