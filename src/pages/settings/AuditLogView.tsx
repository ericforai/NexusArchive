// Input: React、lucide-react 图标、settings feature、utils/audit
// Output: React 组件 AuditLogView
// Pos: src/pages/settings/AuditLogView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useCallback, useEffect, useState } from 'react';
import { Filter, Loader2, Search } from 'lucide-react';
import { subscribeAuditRefresh } from '../../utils/audit';
import { useAuditSettingsApi } from '../../features/settings';
import { AuditLog } from '../../types';

export const AuditLogView: React.FC = () => {
  const { auditApi } = useAuditSettingsApi();
  const [logs, setLogs] = useState<AuditLog[]>([]);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [filterUser, setFilterUser] = useState('');
  const [filterAction, setFilterAction] = useState('');
  const [filterResource, setFilterResource] = useState('');

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

  const totalPages = Math.max(1, Math.ceil(total / 20));

  return (
    <div className="p-6 space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-slate-800">审计日志</h2>
          <p className="text-slate-500 text-sm">查看关键操作的审计留痕（用户、角色、设置等）。</p>
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
              <tr><td colSpan={7} className="py-6 text-center text-slate-400"><Loader2 className="animate-spin inline-block mr-1" size={16} />加载中...</td></tr>
            ) : logs.length === 0 ? (
              <tr><td colSpan={7} className="py-6 text-center text-slate-400">暂无数据</td></tr>
            ) : (
              logs.map(log => (
                <tr key={log.id}>
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
    </div>
  );
};

export default AuditLogView;
