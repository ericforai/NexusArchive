// Input: React、lucide-react、scanApi
// Output: FolderMonitorDialog 组件 - 监控文件夹设置对话框
// Pos: src/components/scan/FolderMonitorDialog.tsx

import React, { useState, useEffect } from 'react';
import { X, FolderOpen, Plus, Trash2, CheckCircle, Loader2, AlertCircle } from 'lucide-react';
import { toast } from '../../utils/notificationService';
import { scanApi } from '@api/scan';

/**
 * 文件夹监控配置接口（组件内部使用）
 */
export interface FolderMonitor {
  id?: number;
  folderPath: string;
  isActive: boolean;
  fileFilter: string;
  autoDelete: boolean;
  moveToPath?: string;
}

interface FolderMonitorDialogProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

/**
 * 监控文件夹设置对话框
 *
 * 允许用户配置服务器端文件夹监控，扫描仪将文件保存到指定文件夹后，
 * 系统会自动检测并导入到工作区。
 */
export const FolderMonitorDialog: React.FC<FolderMonitorDialogProps> = ({ open, onClose, onSuccess }) => {
  const [monitors, setMonitors] = useState<FolderMonitor[]>([]);
  const [newPath, setNewPath] = useState('');
  const [fileFilter, setFileFilter] = useState('*.pdf;*.jpg;*.jpeg;*.png');
  const [autoDelete, setAutoDelete] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      loadMonitors();
    }
  }, [open]);

  const loadMonitors = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await scanApi.getFolderMonitors();
      // Transform API response to component format
      setMonitors(
        response.map(m => ({
          id: m.id,
          folderPath: m.folderPath,
          isActive: m.isActive ?? true,
          fileFilter: m.fileFilter || '*.pdf;*.jpg;*.jpeg;*.png',
          autoDelete: m.autoDelete ?? false,
          moveToPath: m.moveToPath,
        }))
      );
    } catch (err) {
      console.error('Failed to load folder monitors:', err);
      setError('加载监控配置失败');
      toast.error('加载监控配置失败');
    } finally {
      setIsLoading(false);
    }
  };

  const handleAdd = async () => {
    if (!newPath.trim()) {
      toast.error('请输入文件夹路径');
      return;
    }

    setIsSaving(true);
    setError(null);
    try {
      const response = await scanApi.addFolderMonitor({
        folderPath: newPath,
        fileFilter,
        autoDelete,
      });

      setMonitors(prev => [
        ...prev,
        {
          id: response.id,
          folderPath: response.folderPath,
          isActive: response.isActive ?? true,
          fileFilter: response.fileFilter || '*.pdf;*.jpg;*.jpeg;*.png',
          autoDelete: response.autoDelete ?? false,
          moveToPath: response.moveToPath,
        },
      ]);
      setNewPath('');
      setFileFilter('*.pdf;*.jpg;*.jpeg;*.png');
      setAutoDelete(false);
      toast.success('监控文件夹已添加');
    } catch (err) {
      console.error('Failed to add folder monitor:', err);
      setError('添加监控文件夹失败');
      toast.error('添加监控文件夹失败');
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await scanApi.deleteFolderMonitor(id);
      setMonitors(prev => prev.filter(m => m.id !== id));
      toast.success('监控文件夹已删除');
    } catch (err) {
      console.error('Failed to delete folder monitor:', err);
      toast.error('删除失败');
    }
  };

  const handleToggleActive = async (id: number, isActive: boolean) => {
    try {
      const response = await scanApi.toggleFolderMonitor(id);
      setMonitors(prev =>
        prev.map(m =>
          m.id === id
            ? {
                ...m,
                isActive: response.isActive ?? !isActive,
              }
            : m
        )
      );
      toast.success(response.isActive ?? !isActive ? '监控已启用' : '监控已暂停');
    } catch (err) {
      console.error('Failed to toggle folder monitor:', err);
      toast.error('切换状态失败');
    }
  };

  const handleClose = () => {
    setNewPath('');
    setFileFilter('*.pdf;*.jpg;*.jpeg;*.png');
    setAutoDelete(false);
    setError(null);
    onClose();
    onSuccess?.();
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-in fade-in duration-200">
      <div className="bg-white rounded-2xl w-full max-w-2xl mx-4 max-h-[85vh] overflow-hidden flex flex-col animate-in zoom-in-95 duration-200">
        {/* Header */}
        <div className="p-6 border-b border-slate-200 flex justify-between items-center">
          <h2 className="text-xl font-bold flex items-center gap-2 text-slate-800">
            <FolderOpen className="text-primary-600" size={24} />
            监控文件夹设置
          </h2>
          <button
            onClick={handleClose}
            className="p-2 hover:bg-slate-100 rounded-lg transition-colors"
          >
            <X size={20} className="text-slate-400" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 flex-1 overflow-y-auto">
          {error && (
            <div className="mb-4 p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-start gap-2">
              <AlertCircle size={18} className="text-rose-500 shrink-0 mt-0.5" />
              <p className="text-sm text-rose-700">{error}</p>
            </div>
          )}

          {/* Add New Monitor */}
          <div className="mb-6 p-4 bg-slate-50 rounded-xl border border-slate-200">
            <h3 className="text-sm font-bold text-slate-700 mb-4 flex items-center gap-2">
              <Plus size={16} className="text-primary-600" />
              添加新监控
            </h3>

            <div className="space-y-4">
              {/* 文件夹路径 */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  文件夹路径 <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={newPath}
                  onChange={e => setNewPath(e.target.value)}
                  placeholder="例如: /home/user/Documents/Scan 或 C:\Users\user\Documents\Scan"
                  className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
                />
                <p className="text-xs text-slate-500 mt-1">
                  这是服务器上的文件系统路径，请确保后端服务有访问权限
                </p>
              </div>

              {/* 文件类型过滤 */}
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-2">
                  文件类型过滤
                </label>
                <input
                  type="text"
                  value={fileFilter}
                  onChange={e => setFileFilter(e.target.value)}
                  placeholder="*.pdf;*.jpg;*.png"
                  className="w-full px-4 py-2 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
                />
                <p className="text-xs text-slate-500 mt-1">
                  使用分号分隔多个文件类型，支持通配符 *
                </p>
              </div>

              {/* 选项 */}
              <div className="flex items-center gap-6">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={autoDelete}
                    onChange={e => setAutoDelete(e.target.checked)}
                    className="w-4 h-4 text-primary-600 border-slate-300 rounded focus:ring-primary-500"
                  />
                  <span className="text-sm text-slate-700">导入后删除源文件</span>
                </label>
              </div>

              {/* 添加按钮 */}
              <button
                onClick={handleAdd}
                disabled={isSaving || !newPath.trim()}
                className="w-full py-2.5 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 active:scale-[0.98] transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {isSaving ? (
                  <>
                    <Loader2 size={18} className="animate-spin" />
                    添加中...
                  </>
                ) : (
                  <>
                    <Plus size={18} />
                    添加监控
                  </>
                )}
              </button>
            </div>
          </div>

          {/* 监控列表 */}
          <div>
            <h3 className="text-sm font-bold text-slate-700 mb-3">当前监控列表</h3>

            {isLoading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 size={24} className="animate-spin text-slate-300" />
              </div>
            ) : monitors.length === 0 ? (
              <div className="text-center py-8 bg-slate-50 rounded-xl border border-dashed border-slate-300">
                <FolderOpen size={48} className="mx-auto mb-3 text-slate-300" />
                <p className="text-slate-400">暂无监控文件夹</p>
                <p className="text-xs text-slate-400 mt-1">添加监控后，扫描仪保存的文件将自动导入</p>
              </div>
            ) : (
              <div className="space-y-3">
                {monitors.map(monitor => (
                  <div
                    key={monitor.id}
                    className="flex items-center justify-between p-4 bg-white border border-slate-200 rounded-xl hover:border-slate-300 transition-all"
                  >
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <button
                        onClick={() => handleToggleActive(monitor.id!, !monitor.isActive)}
                        className={`shrink-0 p-1 rounded-full transition-colors ${
                          monitor.isActive
                            ? 'text-emerald-500 hover:bg-emerald-50'
                            : 'text-slate-300 hover:bg-slate-100'
                        }`}
                      >
                        {monitor.isActive ? (
                          <CheckCircle size={20} />
                        ) : (
                          <div className="w-5 h-5 rounded-full border-2 border-current" />
                        )}
                      </button>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-slate-800 truncate">{monitor.folderPath}</p>
                        <p className="text-xs text-slate-500">
                          过滤: {monitor.fileFilter}
                          {monitor.autoDelete && ' · 导入后删除'}
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={() => handleDelete(monitor.id!)}
                      className="p-2 text-slate-400 hover:text-rose-500 hover:bg-rose-50 rounded-lg transition-colors"
                      title="删除监控"
                    >
                      <Trash2 size={16} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* 说明信息 */}
          <div className="mt-6 p-3 bg-blue-50 border border-blue-100 rounded-lg">
            <p className="text-xs text-blue-700">
              <span className="font-bold">使用说明：</span>
            </p>
            <ol className="text-xs text-blue-600 mt-2 space-y-1 list-decimal list-inside">
              <li>配置扫描仪软件，将扫描文件保存到指定文件夹</li>
              <li>系统会自动检测新文件并导入到工作区</li>
              <li>支持 Linux/Windows 路径格式</li>
              <li>请确保后端服务对监控路径有读取权限</li>
            </ol>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-end gap-3 rounded-b-2xl">
          <button
            onClick={handleClose}
            className="px-6 py-2 text-slate-600 hover:bg-slate-200 rounded-lg font-medium transition-colors"
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  );
};

export default FolderMonitorDialog;
