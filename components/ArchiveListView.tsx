import React, { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { Filter, Download, Plus, CheckCircle2, AlertTriangle, Clock, XCircle, Eye, Link as LinkIcon, FileText, Receipt, Trash2, X, Search, Settings2, Play, Zap, Loader2, ShieldCheck, BarChart2, Lock } from 'lucide-react';
import { ModuleConfig, TableColumn, GenericRow } from '../types';

interface ArchiveListViewProps {
  title: string;
  subTitle?: string;
  config: ModuleConfig;
}

// Mock candidates for linking with dynamic scores
const MOCK_CANDIDATES = [
    { id: 'inv1', type: 'invoice', code: 'INV-202311-089', name: '阿里云计算服务费发票', amount: '¥ 12,800.00', date: '2023-11-02', score: 98 },
    { id: 'con1', type: 'contract', code: 'CON-2023-098', name: '年度技术服务协议', amount: '¥ 150,000.00', date: '2023-01-01', score: 92 },
    { id: 'inv2', type: 'invoice', code: 'INV-202311-092', name: '服务器采购报销', amount: '¥ 45,200.00', date: '2023-11-03', score: 85 },
    { id: 'inv3', type: 'invoice', code: 'INV-202310-011', name: '办公用品采购', amount: '¥ 2,300.00', date: '2023-10-15', score: 45 },
];

interface MatchRule {
    id: string;
    label: string;
    description: string;
    enabled: boolean;
    weight: number;
}

const DEFAULT_RULES: MatchRule[] = [
    { id: 'amount', label: '金额精确匹配', description: '凭证金额与单据金额必须完全一致', enabled: true, weight: 40 },
    { id: 'date', label: '日期邻近匹配', description: '业务日期差异在 ±3 天以内', enabled: true, weight: 20 },
    { id: 'vendor', label: '客商名称匹配', description: '支持名称模糊匹配与缩写匹配', enabled: true, weight: 20 },
    { id: 'code', label: '票据号关联', description: '摘要中包含发票号或合同号', enabled: true, weight: 20 },
];

export const ArchiveListView: React.FC<ArchiveListViewProps> = ({ title, subTitle, config }) => {
  // State for Real CRUD
  const [localData, setLocalData] = useState<GenericRow[]>(config.data);
  
  // Update local data when config changes (view switch)
  useEffect(() => {
    setLocalData(config.data);
  }, [config.data]);

  const [isFilterOpen, setIsFilterOpen] = useState(false);
  const [filterQuery, setFilterQuery] = useState('');
  
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [newRowData, setNewRowData] = useState<Partial<GenericRow>>({});

  const [selectedRows, setSelectedRows] = useState<string[]>([]);

  // Linking Feature State
  const [isLinkModalOpen, setIsLinkModalOpen] = useState(false);
  const [linkingRow, setLinkingRow] = useState<GenericRow | null>(null);
  const [selectedCandidates, setSelectedCandidates] = useState<string[]>([]);

  // Auto Match Feature State
  const [isRuleModalOpen, setIsRuleModalOpen] = useState(false);
  const [matchRules, setMatchRules] = useState<MatchRule[]>(DEFAULT_RULES);
  const [isMatching, setIsMatching] = useState(false);
  const [matchThreshold, setMatchThreshold] = useState(80);

  // View Feature State
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [viewRow, setViewRow] = useState<GenericRow | null>(null);

  // 4-Nature Compliance Feature State
  const [isComplianceModalOpen, setIsComplianceModalOpen] = useState(false);

  // Toast Notification State
  const [toast, setToast] = useState<{ visible: boolean; message: string; type: 'success' | 'error' }>({ visible: false, message: '', type: 'success' });

  // --- Actions ---

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ visible: true, message, type });
    setTimeout(() => {
      setToast(prev => ({ ...prev, visible: false }));
    }, 3000);
  };

  const handleExport = () => {
     if (localData.length === 0) {
        showToast('没有数据可导出', 'error');
        return;
     }
     const headers = config.columns.map(c => c.header).join(',');
     const rows = localData.map(row => config.columns.map(c => row[c.key]).join(',')).join('\n');
     const csvContent = `data:text/csv;charset=utf-8,\uFEFF${headers}\n${rows}`;
     const encodedUri = encodeURI(csvContent);
     const link = document.createElement("a");
     link.setAttribute("href", encodedUri);
     link.setAttribute("download", `${title}_${subTitle || 'data'}_export.csv`);
     document.body.appendChild(link);
     link.click();
     document.body.removeChild(link);
     showToast('导出成功，正在下载文件');
  };

  const handleDelete = (id: string) => {
    if (confirm('确定要删除这条记录吗？')) {
      setLocalData(prev => prev.filter(row => row.id !== id));
      showToast('记录已删除');
    }
  };

  const handleBatchDelete = () => {
    if (selectedRows.length === 0) {
      alert('请先选择要删除的记录');
      return;
    }
    if (confirm(`确定要删除选中的 ${selectedRows.length} 条记录吗？`)) {
      const count = selectedRows.length;
      setLocalData(prev => prev.filter(row => !selectedRows.includes(row.id)));
      setSelectedRows([]);
      showToast(`已成功删除 ${count} 条记录`);
    }
  };

  const handleAddSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const newId = Math.random().toString(36).substr(2, 9);
    const timestamp = new Date().toISOString().split('T')[0];
    const rowToAdd: GenericRow = { ...newRowData, id: newId };
    config.columns.forEach(col => {
        if (!rowToAdd[col.key]) {
            if (col.type === 'date') rowToAdd[col.key] = timestamp;
            if (col.type === 'status') rowToAdd[col.key] = '待处理';
            if (col.type === 'progress') rowToAdd[col.key] = 0;
        }
    });
    setLocalData([rowToAdd, ...localData]);
    setIsAddModalOpen(false);
    setNewRowData({});
    showToast('新增记录成功');
  };

  const toggleRowSelection = (id: string) => {
    if (selectedRows.includes(id)) {
      setSelectedRows(selectedRows.filter(rowId => rowId !== id));
    } else {
      setSelectedRows([...selectedRows, id]);
    }
  };

  const toggleAllSelection = () => {
    if (selectedRows.length === localData.length) {
      setSelectedRows([]);
    } else {
      setSelectedRows(localData.map(r => r.id));
    }
  };

  // --- Linking Logic ---
  const openLinkModal = (row: GenericRow) => {
      setLinkingRow(row);
      setSelectedCandidates([]); 
      setIsLinkModalOpen(true);
  };

  const toggleCandidateSelection = (id: string) => {
      if (selectedCandidates.includes(id)) {
          setSelectedCandidates(selectedCandidates.filter(c => c !== id));
      } else {
          setSelectedCandidates([...selectedCandidates, id]);
      }
  };

  const handleLinkConfirm = () => {
     if (!linkingRow) return;
     const selectedDocs = MOCK_CANDIDATES.filter(c => selectedCandidates.includes(c.id));
     const invoiceCount = selectedDocs.filter(d => d.type === 'invoice').length;
     const contractDoc = selectedDocs.find(d => d.type === 'contract');
     const updatedData = localData.map(item => {
        if (item.id === linkingRow.id) {
            return {
                ...item,
                invoiceCount: invoiceCount > 0 ? `${invoiceCount} 张` : item.invoiceCount,
                contractNo: contractDoc ? contractDoc.code : item.contractNo,
                matchScore: 100,
                autoLink: '手工关联',
                status: '已关联'
            };
        }
        return item;
     });
     setLocalData(updatedData);
     setIsLinkModalOpen(false);
     setLinkingRow(null);
     showToast(`成功关联 ${selectedCandidates.length} 个单据`);
  };

  // --- Auto Match Logic ---
  const toggleRule = (id: string) => {
      setMatchRules(prev => prev.map(r => r.id === id ? { ...r, enabled: !r.enabled } : r));
  };

  const runAutoMatch = () => {
      setIsMatching(true);
      setIsRuleModalOpen(false);
      setTimeout(() => {
          const updatedData = localData.map(row => {
              if (row.autoLink === '手工关联' || row.status === '已关联') return row;
              let newScore = Math.floor(Math.random() * 30) + 60; 
              if (Math.random() > 0.3) newScore += 10; 
              newScore = Math.min(newScore, 100);
              const isMatched = newScore >= matchThreshold;
              return {
                  ...row,
                  matchScore: newScore,
                  status: isMatched ? '已关联' : '未关联',
                  autoLink: isMatched ? '规则引擎' : '-',
                  invoiceCount: isMatched ? '1 张' : row.invoiceCount
              };
          });
          setLocalData(updatedData);
          setIsMatching(false);
          const matchCount = updatedData.filter(r => r.status === '已关联').length;
          showToast(`自动匹配完成，共关联 ${matchCount} 条凭证`);
      }, 2000);
  };

  // --- Render Helpers ---

  const filteredData = localData.filter(row => {
    if (!filterQuery) return true;
    return Object.values(row).some(val => 
      String(val).toLowerCase().includes(filterQuery.toLowerCase())
    );
  });

  const renderCell = (row: GenericRow, column: TableColumn) => {
    const value = row[column.key];

    if (column.type === 'status') {
      let colorClass = 'bg-slate-100 text-slate-600 border-slate-200';
      let Icon = null;
      const statusLower = String(value).toLowerCase();
      
      // Chinese Status Mappings
      if (['已完成', '已归档', '已通过', '已归还', '正常', '机密', '已关联', '已审核', '已记账', '匹配成功', '打开', '通风中'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-emerald-50 text-emerald-700 border-emerald-100';
        Icon = CheckCircle2;
      } else if (['处理中', '待处理', '待审批', '查看', '激活', '闭合'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-blue-50 text-blue-700 border-blue-100';
        Icon = Clock;
      } else if (['错误', '审计失败', '已拒绝', '警告', '异常', '离线', '锁定'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-rose-50 text-rose-700 border-rose-100';
        Icon = XCircle;
      } else if (['未关联', '空闲'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-slate-100 text-slate-500 border-slate-200';
        Icon = AlertTriangle;
      } else if (['内部'].some(s => statusLower.includes(s))) {
        colorClass = 'bg-amber-50 text-amber-700 border-amber-100';
      }

      return <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${colorClass}`}>{Icon && <Icon size={12} />}{value}</span>;
    }

    if (column.type === 'progress') {
      const numValue = Number(value);
      let color = 'bg-blue-500';
      if (numValue === 100) color = 'bg-emerald-500';
      else if (numValue > 90) color = 'bg-blue-500';
      else if (numValue > 60) color = 'bg-amber-500';
      else color = 'bg-rose-500';
      return (
        <div className="w-full max-w-[120px] flex items-center gap-2">
           <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden">
             <div className={`h-full rounded-full ${color} transition-all duration-1000`} style={{ width: `${numValue}%` }}></div>
           </div>
           <span className="text-xs font-medium text-slate-600">{value}%</span>
        </div>
      );
    }

    if (column.type === 'action') {
        return (
            <div className="flex items-center justify-end gap-2">
                 {subTitle === '凭证关联' && (
                    <button onClick={(e) => { e.stopPropagation(); openLinkModal(row); }} className="text-primary-600 hover:text-primary-700 font-medium text-xs flex items-center gap-1 px-2 py-1 bg-primary-50 hover:bg-primary-100 rounded border border-primary-200 transition-colors shadow-sm">
                        <LinkIcon size={14} /> 手动关联
                    </button>
                )}
                 <button onClick={(e) => { e.stopPropagation(); setViewRow(row); setIsViewModalOpen(true); }} className="text-slate-400 hover:text-primary-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100" title="查看"><Eye size={16} /></button>
                 <button onClick={(e) => { e.stopPropagation(); handleDelete(row.id) }} className="text-slate-400 hover:text-rose-600 font-medium text-xs flex items-center gap-1 p-1 rounded hover:bg-slate-100" title="删除"><Trash2 size={16} /></button>
            </div>
        )
    }
    return <span className="text-slate-700 font-medium">{value}</span>;
  };

  return (
    <div className="p-8 space-y-6 max-w-[1600px] mx-auto animate-in fade-in slide-in-from-bottom-4 duration-500 relative">
      
      {/* Toast (Portal) */}
      {toast.visible && createPortal(
        <div className="fixed top-6 left-1/2 transform -translate-x-1/2 z-[100] animate-in slide-in-from-top-2 fade-in duration-300">
           <div className={`flex items-center gap-3 px-5 py-3 rounded-xl shadow-2xl border ${toast.type === 'success' ? 'bg-slate-800 text-white border-slate-700' : 'bg-rose-600 text-white border-rose-500'}`}>
              {toast.type === 'success' ? <CheckCircle2 size={20} className="text-emerald-400" /> : <AlertTriangle size={20} className="text-white" />}
              <span className="font-medium text-sm">{toast.message}</span>
              <button onClick={() => setToast(prev => ({...prev, visible: false}))} className="ml-2 text-slate-400 hover:text-white"><XCircle size={16} /></button>
           </div>
        </div>,
        document.body
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-baseline gap-3">
            <h2 className="text-2xl font-bold text-slate-800">{title}</h2>
            {subTitle && <><span className="text-slate-300 text-2xl">/</span><span className="text-xl font-medium text-primary-600">{subTitle}</span></>}
          </div>
          <p className="text-slate-500 mt-1">管理和查看 {subTitle || title} 的相关记录。</p>
        </div>
        <div className="flex items-center gap-3">
          {/* 4-Nature Compliance Button */}
          <button 
            onClick={() => setIsComplianceModalOpen(true)}
            className="px-4 py-2 bg-indigo-50 border border-indigo-200 text-indigo-700 rounded-lg text-sm font-medium hover:bg-indigo-100 flex items-center shadow-sm transition-all active:scale-95"
          >
            <ShieldCheck size={16} className="mr-2" /> 四性检测
          </button>
          <div className="h-8 w-px bg-slate-200 mx-1"></div>

          {/* Special Toolbar for Linking */}
          {subTitle === '凭证关联' && (
             <>
                <button onClick={() => setIsRuleModalOpen(true)} className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95">
                    <Settings2 size={16} className="mr-2" /> 匹配规则
                </button>
                <button onClick={runAutoMatch} disabled={isMatching} className="px-4 py-2 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-lg text-sm font-medium hover:bg-emerald-100 flex items-center shadow-sm transition-all active:scale-95 disabled:opacity-70 disabled:cursor-not-allowed">
                    {isMatching ? <Loader2 size={16} className="mr-2 animate-spin" /> : <Zap size={16} className="mr-2" />} {isMatching ? '匹配中...' : '自动匹配'}
                </button>
                <div className="h-8 w-px bg-slate-200 mx-1"></div>
             </>
          )}

          <div className={`flex items-center bg-white border border-slate-200 rounded-lg overflow-hidden transition-all duration-300 ${isFilterOpen ? 'w-64 opacity-100 shadow-sm' : 'w-0 opacity-0 border-none'}`}>
             <div className="pl-3 text-slate-400"><Search size={14}/></div>
             <input type="text" placeholder="搜索..." className="w-full px-3 py-2 text-sm outline-none bg-transparent" value={filterQuery} onChange={(e) => setFilterQuery(e.target.value)}/>
          </div>
          <button onClick={() => setIsFilterOpen(!isFilterOpen)} className={`px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95 ${isFilterOpen ? 'bg-slate-100 text-primary-600 border-primary-200' : ''}`}>
            <Filter size={16} className="mr-2" /> 筛选
          </button>
          <button onClick={handleExport} className="px-4 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-medium hover:bg-slate-50 flex items-center shadow-sm transition-all active:scale-95">
            <Download size={16} className="mr-2" /> 导出
          </button>
          <button onClick={() => setIsAddModalOpen(true)} className="px-4 py-2 bg-primary-600 text-white rounded-lg text-sm font-medium hover:bg-primary-700 flex items-center shadow-lg shadow-primary-500/30 transition-all active:scale-95">
            <Plus size={16} className="mr-2" /> 新增
          </button>
        </div>
      </div>

      {/* Content Card */}
      <div className="bg-white rounded-2xl shadow-sm border border-slate-200 overflow-hidden flex flex-col h-[calc(100vh-240px)]">
        {/* Table Toolbar */}
        <div className="p-4 border-b border-slate-100 bg-slate-50/30 flex items-center gap-4 shrink-0">
           <span className="text-sm text-slate-500">已选择 <strong className="text-slate-800">{selectedRows.length}</strong> 项</span>
           <div className="h-4 w-px bg-slate-300"></div>
           <button onClick={handleBatchDelete} className="text-sm text-slate-600 hover:text-rose-600 font-medium disabled:opacity-50 disabled:cursor-not-allowed" disabled={selectedRows.length === 0}>批量删除</button>
           <button onClick={() => {setFilterQuery(''); setSelectedRows([]); setLocalData(config.data)}} className="text-sm text-slate-600 hover:text-primary-600 font-medium">重置视图</button>
        </div>

        {/* Dynamic Table */}
        <div className="overflow-auto flex-1 relative">
          {isMatching && (
              <div className="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-20 flex items-center justify-center flex-col">
                  <Loader2 size={48} className="text-primary-500 animate-spin mb-4" />
                  <p className="text-slate-600 font-medium animate-pulse">正在执行智能匹配算法...</p>
              </div>
          )}
          <table className="w-full text-left border-collapse relative">
            <thead className="sticky top-0 z-10 shadow-sm">
              <tr className="bg-slate-50 text-slate-500 text-xs uppercase tracking-wider border-b border-slate-200">
                <th className="p-4 w-4 bg-slate-50"><input type="checkbox" checked={selectedRows.length === localData.length && localData.length > 0} onChange={toggleAllSelection} className="rounded border-slate-300 text-primary-600 focus:ring-primary-500 cursor-pointer" /></th>
                {config.columns.map((col) => (
                   <th key={col.key} className="p-4 font-medium bg-slate-50 whitespace-nowrap">{col.header}</th>
                ))}
                <th className="p-4 font-medium text-right bg-slate-50 sticky right-0 shadow-[-12px_0_12px_-12px_rgba(0,0,0,0.1)]">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-sm">
              {filteredData.map((row, idx) => (
                <tr key={`${row.id}-${idx}`} className={`hover:bg-slate-50 transition-colors group ${selectedRows.includes(row.id) ? 'bg-primary-50/30' : ''}`}>
                  <td className="p-4"><input type="checkbox" checked={selectedRows.includes(row.id)} onChange={() => toggleRowSelection(row.id)} className="rounded border-slate-300 text-primary-600 focus:ring-primary-500 cursor-pointer" /></td>
                  {config.columns.map((col) => (
                    <td key={`${row.id}-${col.key}`} className="p-4 whitespace-nowrap">{renderCell(row, col)}</td>
                  ))}
                  <td className="p-4 text-right sticky right-0 bg-white group-hover:bg-slate-50 shadow-[-12px_0_12px_-12px_rgba(0,0,0,0.1)]">
                     {renderCell(row, { key: 'action', header: '操作', type: 'action' })}
                  </td>
                </tr>
              ))}
              {filteredData.length === 0 && (
                  <tr><td colSpan={config.columns.length + 2} className="p-12 text-center text-slate-400">暂无数据</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="p-4 border-t border-slate-200 bg-slate-50 flex justify-between items-center shrink-0">
           <span className="text-xs text-slate-500">显示 {filteredData.length > 0 ? 1 : 0} 到 {filteredData.length} 条，共 {filteredData.length} 条</span>
           <div className="flex gap-1">
              <button className="px-3 py-1 border border-slate-200 rounded bg-white text-xs text-slate-600 disabled:opacity-50" disabled>上一页</button>
              <button className="px-3 py-1 border border-primary-500 rounded bg-primary-50 text-primary-600 text-xs font-medium">1</button>
              <button className="px-3 py-1 border border-slate-200 rounded bg-white text-xs text-slate-600">2</button>
              <button className="px-3 py-1 border border-slate-200 rounded bg-white text-xs text-slate-600">下一页</button>
           </div>
        </div>
      </div>

      {/* 4-Nature Compliance Modal */}
      {isComplianceModalOpen && (
         <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
             <div className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl border border-slate-100">
                 <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                    <div className="flex items-center gap-3">
                       <div className="p-2 bg-indigo-100 rounded-lg text-indigo-600"><ShieldCheck size={24}/></div>
                       <div>
                          <h3 className="text-lg font-bold text-slate-800">四性检测智能诊断报告</h3>
                          <p className="text-xs text-slate-500">依据标准 DA/T 70-2018 执行检测</p>
                       </div>
                    </div>
                    <button onClick={() => setIsComplianceModalOpen(false)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
                 </div>
                 
                 <div className="p-8 grid grid-cols-1 md:grid-cols-3 gap-8">
                    {/* Overall Score */}
                    <div className="md:col-span-1 flex flex-col items-center justify-center bg-slate-50 rounded-2xl p-6 border border-slate-100">
                       <div className="relative w-32 h-32 flex items-center justify-center">
                           <svg className="w-full h-full transform -rotate-90">
                              <circle cx="64" cy="64" r="60" stroke="#e2e8f0" strokeWidth="8" fill="transparent" />
                              <circle cx="64" cy="64" r="60" stroke="#4f46e5" strokeWidth="8" fill="transparent" strokeDasharray="377" strokeDashoffset="37.7" strokeLinecap="round" />
                           </svg>
                           <div className="absolute flex flex-col items-center">
                              <span className="text-4xl font-bold text-indigo-600">90</span>
                              <span className="text-xs font-bold text-slate-400 uppercase">综合得分</span>
                           </div>
                       </div>
                       <p className="text-sm text-center mt-4 text-slate-600">检测对象: <span className="font-bold">{localData.length}</span> 份档案<br/>状态: <span className="text-emerald-600 font-bold">通过</span></p>
                    </div>

                    {/* Details */}
                    <div className="md:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-4">
                       {[
                         { label: '真实性', score: 98, color: 'bg-emerald-500', items: ['电子签名有效', '时间戳完整'] },
                         { label: '完整性', score: 100, color: 'bg-blue-500', items: ['元数据齐全', '附件无缺失'] },
                         { label: '可用性', score: 95, color: 'bg-cyan-500', items: ['格式标准兼容', '索引库正常'] },
                         { label: '安全性', score: 85, color: 'bg-amber-500', items: ['病毒扫描通过', '权限配置待优化'] },
                       ].map((item, idx) => (
                          <div key={idx} className="border border-slate-100 rounded-xl p-4 hover:shadow-sm transition-shadow">
                             <div className="flex justify-between items-center mb-2">
                                <span className="font-bold text-slate-700">{item.label}</span>
                                <span className={`text-xs font-bold text-white px-2 py-0.5 rounded-full ${item.color.replace('bg-', 'bg-')}`}>{item.score}</span>
                             </div>
                             <div className="w-full bg-slate-100 h-1.5 rounded-full mb-3">
                                <div className={`h-full rounded-full ${item.color}`} style={{ width: `${item.score}%` }}></div>
                             </div>
                             <ul className="space-y-1">
                                {item.items.map((check, i) => (
                                   <li key={i} className="text-xs text-slate-500 flex items-center gap-1.5">
                                      <CheckCircle2 size={10} className="text-emerald-500" /> {check}
                                   </li>
                                ))}
                             </ul>
                          </div>
                       ))}
                    </div>
                 </div>

                 <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end">
                    <button onClick={() => setIsComplianceModalOpen(false)} className="px-6 py-2 bg-indigo-600 text-white font-medium rounded-lg hover:bg-indigo-700 shadow-lg shadow-indigo-500/20 transition-all active:scale-95">生成详细报告</button>
                 </div>
             </div>
         </div>
      )}

      {/* Rule, View, Add, Link Modals are implicitly here (reusing previous logic) */}
      {isRuleModalOpen && (
          <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
              <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-100">
                  <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                      <h3 className="text-lg font-bold text-slate-800">关联规则配置</h3>
                      <button onClick={() => setIsRuleModalOpen(false)}><X size={20} className="text-slate-400" /></button>
                  </div>
                  <div className="p-6 space-y-4">
                      <div className="mb-4"><label className="text-sm font-bold">自动关联阈值: {matchThreshold}%</label><input type="range" min="60" max="100" value={matchThreshold} onChange={(e) => setMatchThreshold(Number(e.target.value))} className="w-full h-2 bg-slate-200 rounded-lg accent-primary-500"/></div>
                      {matchRules.map(rule => (
                          <div key={rule.id} className="flex justify-between items-center"><span className="text-sm">{rule.label}</span><input type="checkbox" checked={rule.enabled} onChange={() => toggleRule(rule.id)} className="toggle-checkbox h-5 w-5 text-primary-600 rounded border-slate-300"/></div>
                      ))}
                  </div>
                  <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
                      <button onClick={() => setIsRuleModalOpen(false)} className="px-4 py-2 text-slate-600">取消</button>
                      <button onClick={runAutoMatch} className="px-4 py-2 bg-primary-600 text-white rounded-lg">保存并运行</button>
                  </div>
              </div>
          </div>
      )}

      {isViewModalOpen && viewRow && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
           <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg border border-slate-100 flex flex-col max-h-[90vh]">
               <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl shrink-0">
                 <h3 className="text-lg font-bold text-slate-800">档案详情</h3>
                 <button onClick={() => setIsViewModalOpen(false)}><X size={20} className="text-slate-400" /></button>
               </div>
               <div className="p-6 overflow-y-auto flex-1 grid grid-cols-2 gap-4">
                  {config.columns.filter(c => c.type !== 'action').map((col) => (
                      <div key={col.key} className="space-y-1"><label className="text-xs font-bold text-slate-400">{col.header}</label><div>{renderCell(viewRow, col)}</div></div>
                  ))}
               </div>
           </div>
        </div>
      )}
      
      {/* Re-including Add Modal and Linking Modal logic to ensure file integrity */}
      {isAddModalOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
           <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md border border-slate-100">
              <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                 <h3 className="text-lg font-bold text-slate-800">新增记录</h3>
                 <button onClick={() => setIsAddModalOpen(false)}><X size={20} className="text-slate-400" /></button>
              </div>
              <form onSubmit={handleAddSubmit} className="p-6 space-y-4">
                 {config.columns.filter(c => c.type !== 'status' && c.type !== 'progress' && c.type !== 'action').map(col => (
                    <div key={col.key}><label className="text-sm font-medium">{col.header}</label><input className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm" value={newRowData[col.key] || ''} onChange={(e) => setNewRowData({...newRowData, [col.key]: e.target.value})} /></div>
                 ))}
                 <div className="pt-4 flex justify-end gap-2"><button type="button" onClick={() => setIsAddModalOpen(false)} className="px-4 py-2 text-slate-600">取消</button><button type="submit" className="px-4 py-2 bg-primary-600 text-white rounded-lg">确认</button></div>
              </form>
           </div>
        </div>
      )}
      
      {isLinkModalOpen && (
        <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl border border-slate-100 flex flex-col max-h-[90vh]">
                <div className="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
                    <h3 className="text-lg font-bold text-slate-800">手动凭证关联</h3>
                    <button onClick={() => setIsLinkModalOpen(false)}><X size={20} className="text-slate-400" /></button>
                </div>
                <div className="p-6 overflow-y-auto flex-1 space-y-2">
                    <div className="bg-amber-50 border border-amber-100 rounded-lg p-3 mb-4 text-xs text-amber-800">注意：手动关联将覆盖自动结果。</div>
                    {MOCK_CANDIDATES.map(c => (
                        <div key={c.id} onClick={() => toggleCandidateSelection(c.id)} className={`flex justify-between p-3 rounded-xl border cursor-pointer ${selectedCandidates.includes(c.id) ? 'border-primary-500 bg-primary-50' : 'border-slate-200'}`}>
                           <div><p className="font-bold">{c.name}</p><p className="text-xs text-slate-500">{c.code}</p></div>
                           <div className="text-right"><span className={`font-bold ${c.score > 90 ? 'text-emerald-600' : 'text-amber-600'}`}>{c.score}%</span></div>
                        </div>
                    ))}
                </div>
                <div className="p-6 border-t border-slate-100 bg-slate-50 rounded-b-2xl flex justify-end gap-2">
                    <button onClick={() => setIsLinkModalOpen(false)} className="px-4 py-2 text-slate-600">取消</button>
                    <button onClick={handleLinkConfirm} disabled={selectedCandidates.length===0} className="px-6 py-2 bg-primary-600 text-white rounded-lg disabled:opacity-50">确认关联</button>
                </div>
            </div>
        </div>
      )}
    </div>
  );
};