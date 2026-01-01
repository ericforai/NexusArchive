// Input: React、lucide-react 图标、settings feature、utils/audit、audit pages
// Output: React 组件 AuditLogView（集成日志查看、验真、导出功能）
// Pos: src/pages/settings/AuditLogView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useCallback, useEffect, useState } from 'react';
import { Filter, Loader2, Search, Copy, Check, Shield, FileArchive } from 'lucide-react';
import { subscribeAuditRefresh } from '../../utils/audit';
import { useAuditSettingsApi } from '../../features/settings';
import { AuditLog } from '../../types';
import { AuditVerificationPage } from '../audit/AuditVerificationPage';
import { AuditEvidencePackagePage } from '../audit/AuditEvidencePackagePage';

type AuditTab = 'logs' | 'verification' | 'export';

export const AuditLogView: React.FC = () => {
  const { auditApi } = useAuditSettingsApi();
  const [activeTab, setActiveTab] = useState<AuditTab>('logs');
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [filterUser, setFilterUser] = useState('');
  const [filterAction, setFilterAction] = useState('');
  const [filterResource, setFilterResource] = useState('');
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const res = await auditApi.getLogs({
        page,
        limit: 20,
        userId: filterUser || undefined,
        action: filterAction || undefined,
        resourceType: filterResource || undefined
      });
      if (res.code === 200 && res.data) {
        setLogs(res.data.records || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [auditApi, page, filterUser, filterAction, filterResource]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs, page]);

  useEffect(() => {
    const unsubscribe = subscribeAuditRefresh(() => {
      setPage(1);
      fetchLogs();
    });
    return unsubscribe;
  }, [fetchLogs]);

  const copyLogId = async (logId: string) => {
    try {
      await navigator.clipboard.writeText(logId);
      setCopiedId(logId);
      setTimeout(() => setCopiedId(null), 2000);
    } catch (err) {
      console.error('复制失败:', err);
    }
  };

  const totalPages = Math.max(1, Math.ceil(total / 20));

  const tabs = [
    { key: 'logs' as AuditTab, label: '日志查看', icon: Search },
    { key: 'verification' as AuditTab, label: '证据链验真', icon: Shield },
    { key: 'export' as AuditTab, label: '证据包导出', icon: FileArchive },
  ];

  const renderContent = () => {
    switch (activeTab) {
      case 'verification':
        return (
          <div className="-m-6">
            <AuditVerificationPage />
          </div>
        );
      case 'export':
        return (
          <div className="-m-6">
            <AuditEvidencePackagePage />
          </div>
        );
      case 'logs':
      default:
        return renderLogsView();
    }
  };

  const renderLogsView = () => (
    <>
      <div className="flex items-center justify-between mb-4">
        <div>
          <h3 className="text-lg font-semibold text-slate-800">审计日志列表</h3>
          <p className="text-slate-500 text-sm mt-1">查看关键操作的审计留痕（用户、角色、设置等）。</p>
        </div>
        <button
          onClick={fetchLogs}
          className="inline-flex items-center px-3 py-2 rounded-lg border border-slate-200 text-sm text-slate-700 hover:bg-slate-50"
        >
          <Filter size={16} className="mr-2" /> 刷新
        </button>
      </div>

      <div className="bg-white border border-slate-200 rounded-lg p-4 flex flex-wrap gap-3 items-center">
        <div className="flex items-center space-x-2">
          <Search size={16} className="text-slate-400" />
          <input
            className="border border-slate-200 rounded px-3 py-2 text-sm"
            placeholder="用户ID"
            value={filterUser}
            onChange={(e) => setFilterUser(e.target.value)}
          />
        </div>
        <input
          className="border border-slate-200 rounded px-3 py-2 text-sm"
          placeholder="操作类型 (CREATE/UPDATE/DELETE/RESET_PASSWORD...)"
          value={filterAction}
          onChange={(e) => setFilterAction(e.target.value)}
        />
        <input
          className="border border-slate-200 rounded px-3 py-2 text-sm"
          placeholder="资源类型 (USER/ROLE/SETTING/ARCHIVE)"
          value={filterResource}
          onChange={(e) => setFilterResource(e.target.value)}
        />
        <button
          onClick={() => { setPage(1); fetchLogs(); }}
          className="px-3 py-2 bg-primary-600 text-white text-sm rounded-lg hover:bg-primary-700"
        >
          应用筛选
        </button>
        <button
          onClick={() => { setFilterUser(''); setFilterAction(''); setFilterResource(''); setPage(1); fetchLogs(); }}
          className="px-3 py-2 text-sm text-slate-600 hover:text-primary-600"
        >
          重置
        </button>
      </div>

      <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-slate-500 border-b">
            <tr>
              <th className="px-4 py-2">日志ID</th>
              <th className="px-4 py-2">时间</th>
              <th className="px-4 py-2">用户</th>
              <th className="px-4 py-2">操作</th>
              <th className="px-4 py-2">资源</th>
              <th className="px-4 py-2">结果</th>
              <th className="px-4 py-2">IP</th>
              <th className="px-4 py-2">详情</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? (
              <tr><td colSpan={8} className="py-6 text-center text-slate-400"><Loader2 className="animate-spin inline-block mr-1" size={16} />加载中...</td></tr>
            ) : logs.length === 0 ? (
              <tr><td colSpan={8} className="py-6 text-center text-slate-400">暂无数据</td></tr>
            ) : (
              logs.map(log => (
                <tr key={log.id}>
                  <td className="px-4 py-2">
                    <div className="flex items-center gap-2 group">
                      <span className="font-mono text-xs text-slate-600">{log.id}</span>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          copyLogId(log.id);
                        }}
                        className="opacity-0 group-hover:opacity-100 transition-opacity p-1 hover:bg-slate-100 rounded"
                        title="复制日志ID"
                      >
                        {copiedId === log.id ? (
                          <Check size={14} className="text-green-600" />
                        ) : (
                          <Copy size={14} className="text-slate-400 hover:text-primary-600" />
                        )}
                      </button>
                    </div>
                  </td>
                  <td className="px-4 py-2 text-slate-600">{log.createdTime || ''}</td>
                  <td className="px-4 py-2 text-slate-800">{log.username || log.userId}</td>
                  <td className="px-4 py-2 text-slate-800">{log.action}</td>
                  <td className="px-4 py-2 text-slate-600">{log.resourceType}</td>
                  <td className="px-4 py-2 text-slate-600">{log.operationResult || '-'}</td>
                  <td className="px-4 py-2 text-slate-600">{log.clientIp || '-'}</td>
                  <td className="px-4 py-2 text-slate-600 max-w-xs truncate" title={log.details}>{log.details || '-'}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="flex items-center justify-between text-xs text-slate-600">
        <div>第 {page} / {totalPages} 页（共 {total} 条）</div>
        <div className="space-x-2">
          <button
            className="px-3 py-1 border rounded disabled:opacity-40"
            disabled={page <= 1 || loading}
            onClick={() => setPage((p) => Math.max(1, p - 1))}
          >上一页</button>
          <button
            className="px-3 py-1 border rounded disabled:opacity-40"
            disabled={page >= totalPages || loading}
            onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
          >下一页</button>
        </div>
      </div>
    </>
  );

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">审计日志</h2>
          <p className="text-slate-500 text-sm">查看关键操作的审计留痕（用户、角色、设置等）。</p>
        </div>
      </div>

      {/* Tab 导航 */}
      <div className="bg-white border-b border-slate-200">
        <nav className="flex space-x-1" aria-label="审计功能导航">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.key;
            return (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`
                  flex items-center px-4 py-3 text-sm font-medium border-b-2 whitespace-nowrap
                  transition-colors duration-200
                  ${isActive
                    ? 'border-primary-500 text-primary-600'
                    : 'border-transparent text-slate-500 hover:text-slate-700 hover:border-slate-300'
                  }
                `}
              >
                <Icon size={16} className="mr-2" />
                {tab.label}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Tab 内容 */}
      <div className="animate-in fade-in slide-in-from-bottom-4 duration-300">
        {renderContent()}
      </div>
    </div>
  );
};

export default AuditLogView;
