// Input: React、lucide-react 图标、本地模块 api/destruction、api/stats
// Output: React 组件 DestructionView
// Pos: src/pages/operations/DestructionView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { destructionApi, ExpiredArchive, Destruction } from '../../api/destruction';
import { FileWarning, ShieldAlert, FileSignature, Flame, CheckCircle2, History, Trash2, RefreshCw, BrainCircuit, BookOpen, Loader2 } from 'lucide-react';
import { toast } from '../../utils/notificationService';

import { statsApi, DestructionStats } from '../../api/stats';

export const DestructionView: React.FC = () => {
   const [activeTab, setActiveTab] = useState<'appraisal' | 'workflow'>('appraisal');
   const [candidates, setCandidates] = useState<ExpiredArchive[]>([]);
   const [batches, setBatches] = useState<Destruction[]>([]);
   const [selectedRows, setSelectedRows] = useState<string[]>([]);
   const [stats, setStats] = useState<DestructionStats>({
      pendingAppraisal: 0,
      aiSuggested: 0,
      activeBatches: 0,
      safeDestructionCount: 0
   });
   const [loading, setLoading] = useState(true);

   // 加载统计数据
   useEffect(() => {
      const fetchStats = async () => {
         try {
            const res = await statsApi.getDestructionStats();
            if (res.code === 200) {
               setStats(res.data);
            }
         } catch (e) {
            console.error(e);
         }
      };
      fetchStats();
   }, []);

   // 加载待鉴定档案（到期档案）
   useEffect(() => {
      const fetchCandidates = async () => {
         try {
            const res = await destructionApi.getExpiredArchives({ page: 1, size: 50 });
            if (res.code === 200) {
               setCandidates(res.data?.records || []);
            }
         } catch (e) {
            console.error(e);
         } finally {
            setLoading(false);
         }
      };
      fetchCandidates();
   }, []);

   // 加载销毁批次
   useEffect(() => {
      const fetchBatches = async () => {
         try {
            const res = await destructionApi.getDestructions({ page: 1, limit: 20 });
            if (res.code === 200) {
               setBatches(res.data?.records || []);
            }
         } catch (e) {
            console.error(e);
         }
      };
      fetchBatches();
   }, []);

   const toggleRow = (id: string) => {
      setSelectedRows(prev => prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id]);
   };

   const handleAction = (action: string) => {
      toast.info(`执行操作: ${action} - 选中 ${selectedRows.length} 项`);
      setSelectedRows([]);
   };

   const handleExecuteDestruction = async (id: string) => {
      try {
         await destructionApi.executeDestruction(id);
         toast.success('销毁执行成功');
         // 刷新批次列表
         const res = await destructionApi.getDestructions({ page: 1, limit: 20 });
         if (res.code === 200) {
            setBatches(res.data?.records || []);
         }
      } catch (e) {
         toast.error('销毁执行失败');
      }
   };

   return (
      <div className="h-full flex flex-col bg-slate-50 animate-in fade-in duration-500">

         {/* Header Dashboard */}
         <div className="bg-slate-900 text-white p-8 pb-24 relative overflow-hidden shrink-0">
            <div className="absolute top-0 right-0 p-8 opacity-10">
               <ShieldAlert size={200} />
            </div>
            <div className="relative z-10 max-w-[1600px] mx-auto">
               <h2 className="text-2xl font-bold flex items-center gap-3">
                  <Flame className="text-rose-500" /> 档案鉴定与销毁控制台
               </h2>
               <p className="text-slate-400 mt-2 max-w-2xl">
                  严格遵循《电子会计档案管理规范》(DA/T 94-2022) 第13章要求。所有销毁操作均需经过智能鉴定、三员监销与不可逆粉碎处理。
               </p>

               <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mt-8">
                  <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/10">
                     <div className="flex justify-between items-start mb-2">
                        <span className="text-slate-300 text-xs font-bold uppercase">待鉴定档案</span>
                        <FileWarning size={16} className="text-amber-400" />
                     </div>
                     <div className="text-3xl font-bold">{stats.pendingAppraisal} <span className="text-xs font-normal text-slate-400">件</span></div>
                  </div>
                  <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/10">
                     <div className="flex justify-between items-start mb-2">
                        <span className="text-slate-300 text-xs font-bold uppercase">AI 建议销毁</span>
                        <BrainCircuit size={16} className="text-rose-400" />
                     </div>
                     <div className="text-3xl font-bold text-rose-400">{stats.aiSuggested} <span className="text-xs font-normal text-white">件</span></div>
                  </div>
                  <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/10">
                     <div className="flex justify-between items-start mb-2">
                        <span className="text-slate-300 text-xs font-bold uppercase">进行中批次</span>
                        <FileSignature size={16} className="text-blue-400" />
                     </div>
                     <div className="text-3xl font-bold">{stats.activeBatches} <span className="text-xs font-normal text-slate-400">批</span></div>
                  </div>
                  <div className="bg-white/10 backdrop-blur-sm rounded-xl p-4 border border-white/10">
                     <div className="flex justify-between items-start mb-2">
                        <span className="text-slate-300 text-xs font-bold uppercase">已安全销毁</span>
                        <Trash2 size={16} className="text-emerald-400" />
                     </div>
                     <div className="text-3xl font-bold">{stats.safeDestructionCount} <span className="text-xs font-normal text-slate-400">件</span></div>
                  </div>
               </div>
            </div>
         </div>

         {/* Main Content Area (Overlapping Header) */}
         <div className="flex-1 overflow-hidden max-w-[1600px] mx-auto w-full px-8 pb-8 -mt-16 relative z-20 flex flex-col">

            {/* Tabs */}
            <div className="flex gap-1 mb-4">
               <button
                  onClick={() => setActiveTab('appraisal')}
                  className={`px-6 py-3 rounded-t-xl font-bold text-sm transition-all ${activeTab === 'appraisal' ? 'bg-white text-slate-800 shadow-sm' : 'bg-slate-800/50 text-slate-300 hover:bg-slate-800'}`}
               >
                  智能鉴定工作台
               </button>
               <button
                  onClick={() => setActiveTab('workflow')}
                  className={`px-6 py-3 rounded-t-xl font-bold text-sm transition-all ${activeTab === 'workflow' ? 'bg-white text-slate-800 shadow-sm' : 'bg-slate-800/50 text-slate-300 hover:bg-slate-800'}`}
               >
                  销毁流程监控
               </button>
            </div>

            <div className="flex-1 bg-white rounded-b-xl rounded-tr-xl shadow-lg border border-slate-200 overflow-hidden flex flex-col">

               {activeTab === 'appraisal' && (
                  <>
                     <div className="p-4 border-b border-slate-100 flex justify-between items-center">
                        <div className="flex items-center gap-4">
                           <span className="text-sm text-slate-500">已选择 <strong className="text-slate-800">{selectedRows.length}</strong> 项</span>
                           <div className="h-4 w-px bg-slate-300"></div>
                           <button onClick={() => handleAction('加入销毁清单')} disabled={selectedRows.length === 0} className="text-sm font-medium text-rose-600 hover:text-rose-700 disabled:opacity-50 flex items-center gap-1">
                              <Trash2 size={14} /> 纳入销毁清单
                           </button>
                           <button onClick={() => handleAction('延长保管期限')} disabled={selectedRows.length === 0} className="text-sm font-medium text-blue-600 hover:text-blue-700 disabled:opacity-50 flex items-center gap-1">
                              <RefreshCw size={14} /> 延长保管期限
                           </button>
                        </div>
                        <div className="flex gap-2">
                           <span className="text-xs bg-slate-100 text-slate-500 px-2 py-1 rounded border border-slate-200 flex items-center gap-1">
                              <BookOpen size={12} /> 依据：会计档案保管期限表 (附录B)
                           </span>
                        </div>
                     </div>
                     <div className="flex-1 overflow-auto">
                        {loading ? (
                           <div className="flex items-center justify-center p-8">
                              <Loader2 className="animate-spin mr-2" /> 加载中...
                           </div>
                        ) : candidates.length === 0 ? (
                           <div className="flex flex-col items-center justify-center p-8 text-slate-400">
                              <FileWarning size={48} className="mb-4 opacity-50" />
                              <p>暂无到期档案</p>
                           </div>
                        ) : (
                           <table className="w-full text-left text-sm">
                              <thead className="bg-slate-50 text-slate-500 font-medium border-b border-slate-200 sticky top-0">
                                 <tr>
                                    <th className="p-4 w-10"><input type="checkbox" className="rounded border-slate-300" /></th>
                                    <th className="p-4">档案编号</th>
                                    <th className="p-4">题名</th>
                                    <th className="p-4">保管期限</th>
                                    <th className="p-4">到期日期</th>
                                    <th className="p-4">状态</th>
                                 </tr>
                              </thead>
                              <tbody className="divide-y divide-slate-100">
                                 {candidates.map(row => (
                                    <tr key={row.id} className={`hover:bg-slate-50 ${selectedRows.includes(row.id) ? 'bg-blue-50/30' : ''}`}>
                                       <td className="p-4"><input type="checkbox" checked={selectedRows.includes(row.id)} onChange={() => toggleRow(row.id)} className="rounded border-slate-300 cursor-pointer" /></td>
                                       <td className="p-4 font-mono text-slate-600">{row.archiveCode}</td>
                                       <td className="p-4 font-medium text-slate-800">{row.title}</td>
                                       <td className="p-4 text-slate-500">{row.retentionPeriod}</td>
                                       <td className="p-4 font-mono text-rose-600">{row.expiredDate}</td>
                                       <td className="p-4">
                                          <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${row.status === 'EXPIRED' ? 'bg-rose-100 text-rose-600' : 'bg-slate-100 text-slate-600'}`}>
                                             {row.status === 'EXPIRED' ? '已到期' : row.status}
                                          </span>
                                       </td>
                                    </tr>
                                 ))}
                              </tbody>
                           </table>
                        )}
                     </div>
                  </>
               )}

               {activeTab === 'workflow' && (
                  <div className="p-8 flex-1 overflow-auto">
                     <h3 className="font-bold text-lg text-slate-800 mb-6">进行中的销毁批次</h3>
                     {batches.length === 0 ? (
                        <div className="flex flex-col items-center justify-center p-8 text-slate-400">
                           <FileSignature size={48} className="mb-4 opacity-50" />
                           <p>暂无销毁批次</p>
                        </div>
                     ) : (
                        <div className="space-y-4">
                           {batches.map(batch => {
                              const progress = batch.status === 'COMPLETED' ? 100 : batch.status === 'EXECUTING' ? 66 : batch.status === 'APPROVED' ? 33 : 0;
                              return (
                                 <div key={batch.id} className="border border-slate-200 rounded-xl p-6 flex items-center justify-between hover:shadow-md transition-shadow">
                                    <div className="flex items-center gap-6">
                                       <div className={`p-4 rounded-full ${batch.status === 'COMPLETED' ? 'bg-slate-100 text-slate-400' : 'bg-rose-50 text-rose-600'}`}>
                                          {batch.status === 'COMPLETED' ? <CheckCircle2 size={24} /> : <Flame size={24} className="animate-pulse" />}
                                       </div>
                                       <div>
                                          <h4 className="font-bold text-slate-800 text-lg">{batch.id}</h4>
                                          <p className="text-sm text-slate-500 mt-1">包含档案 {batch.archiveCount} 件 • 申请于 {new Date(batch.createdAt).toLocaleDateString()}</p>

                                          {/* Workflow Steps */}
                                          <div className="flex items-center gap-2 mt-4">
                                             <div className="flex items-center gap-2 text-xs font-medium text-emerald-600">
                                                <CheckCircle2 size={12} /> 生成清册 ({batch.applicantName})
                                             </div>
                                             <div className="w-8 h-px bg-slate-300"></div>
                                             <div className={`flex items-center gap-2 text-xs font-medium ${progress >= 33 ? 'text-emerald-600' : 'text-slate-400'}`}>
                                                <CheckCircle2 size={12} /> 审批 ({batch.firstApproverName || '待审批'})
                                             </div>
                                             <div className="w-8 h-px bg-slate-300"></div>
                                             <div className={`flex items-center gap-2 text-xs font-medium ${progress >= 66 ? 'text-emerald-600' : 'text-slate-400'}`}>
                                                {batch.status === 'EXECUTING' || batch.status === 'COMPLETED' ? <CheckCircle2 size={12} /> : '三员监销'}
                                             </div>
                                             <div className="w-8 h-px bg-slate-300"></div>
                                             <div className={`flex items-center gap-2 text-xs font-medium ${progress === 100 ? 'text-emerald-600' : 'text-slate-400'}`}>
                                                {batch.status === 'COMPLETED' ? <CheckCircle2 size={12} /> : '物理粉碎'}
                                             </div>
                                          </div>
                                       </div>
                                    </div>

                                    <div className="flex flex-col items-end gap-2">
                                       {batch.status === 'APPROVED' ? (
                                          <button
                                             onClick={() => handleExecuteDestruction(batch.id)}
                                             className="px-6 py-2 bg-rose-600 text-white font-bold rounded-lg shadow-lg shadow-rose-500/30 hover:bg-rose-700 active:scale-95 transition-all"
                                          >
                                             执行销毁
                                          </button>
                                       ) : (
                                          <button className="px-6 py-2 border border-slate-200 text-slate-600 font-medium rounded-lg hover:bg-slate-50">
                                             查看详情
                                          </button>
                                       )}
                                       <span className="text-xs text-slate-400">
                                          {batch.status === 'PENDING' ? '等待审批' : batch.status === 'APPROVED' ? '已批准，等待执行' : batch.status === 'EXECUTING' ? '执行中' : '已完成'}
                                       </span>
                                    </div>
                                 </div>
                              );
                           })}
                        </div>
                     )}
                  </div>
               )}

            </div>
         </div>
      </div>
   );
};
export default DestructionView;
