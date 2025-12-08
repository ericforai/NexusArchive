import React, { useState, useRef, useEffect } from 'react';
import { UploadCloud, FileText, CheckCircle2, AlertTriangle, X, ScanLine, Loader2, ArrowRight, Eye, Save, RefreshCw, ChevronDown, Tag, Receipt, Building, CreditCard, FileBadge, Cloud, Info } from 'lucide-react';
import { isDemoMode } from '../utils/env';
import { safeStorage } from '../utils/storage';
import { DemoBadge } from './common/DemoBadge';

interface OCRField {
  name: string;
  value: string;
  confidence: number;
}

interface OCRResult {
  id: string;
  fileName: string;
  fileSize: string;
  uploadTime: string;
  status: 'processing' | 'review' | 'completed';
  type: 'invoice' | 'contract' | 'receipt' | 'id_card' | 'unknown';
  imageUrl: string;
  fields: OCRField[];
  overallScore: number;
}

const DOC_TYPES = [
  { value: 'invoice', label: '增值税发票', icon: Receipt },
  { value: 'contract', label: '合同协议', icon: Building },
  { value: 'receipt', label: '银行回单', icon: CreditCard },
  { value: 'id_card', label: '身份/资质证件', icon: FileBadge },
  { value: 'unknown', label: '其他/未知类型', icon: FileText },
];

// Mock Initial Data
const MOCK_HISTORY: OCRResult[] = [
  {
    id: 'ocr-001',
    fileName: '2023年11月服务器采购发票.jpg',
    fileSize: '2.4 MB',
    uploadTime: '10:23 AM',
    status: 'completed',
    type: 'invoice',
    imageUrl: 'https://placehold.co/600x800/e2e8f0/475569?text=INVOICE+SCAN',
    overallScore: 98,
    fields: [
      { name: '发票代码', value: '031001900111', confidence: 99 },
      { name: '发票号码', value: '88291023', confidence: 99 },
      { name: '开票日期', value: '2023年11月05日', confidence: 96 },
      { name: '购买方', value: 'DigiVoucher 科技有限公司', confidence: 98 },
      { name: '价税合计', value: '¥ 12,800.00', confidence: 99 },
    ]
  },
  {
    id: 'ocr-002',
    fileName: '技术服务合同_签字页.pdf',
    fileSize: '1.8 MB',
    uploadTime: '09:15 AM',
    status: 'review',
    type: 'contract',
    imageUrl: 'https://placehold.co/600x800/f1f5f9/475569?text=CONTRACT+SCAN',
    overallScore: 75,
    fields: [
      { name: '合同编号', value: 'CON-2023-9982', confidence: 92 },
      { name: '甲方', value: 'DigiVoucher 科技', confidence: 88 },
      { name: '乙方', value: '云智算力服务商', confidence: 65 },
      { name: '签署日期', value: '2023/10/-- (模糊)', confidence: 40 },
    ]
  }
];

export const OCRProcessingView: React.FC = () => {
  const [demoMode, setDemoMode] = useState<boolean>(isDemoMode());
  const [taskList, setTaskList] = useState<OCRResult[]>(MOCK_HISTORY);
  const [activeTask, setActiveTask] = useState<OCRResult | null>(MOCK_HISTORY[0]);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Auto-save State
  const [lastSavedTime, setLastSavedTime] = useState<string | null>(null);
  const [isAutoSaving, setIsAutoSaving] = useState(false);

  // Ref to track activeTask for interval closure
  const activeTaskRef = useRef(activeTask);

  useEffect(() => {
    activeTaskRef.current = activeTask;
  }, [activeTask]);

  // Auto-save Interval
  useEffect(() => {
    const saveInterval = setInterval(() => {
      const currentTask = activeTaskRef.current;
      if (currentTask && currentTask.status === 'review') {
        setIsAutoSaving(true);
        // Simulate API Save Delay
        setTimeout(() => {
          const now = new Date();
          setLastSavedTime(now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
          setIsAutoSaving(false);
        }, 800);
      }
    }, 30000); // 30 Seconds

    return () => clearInterval(saveInterval);
  }, []);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    if (!demoMode) {
      e.preventDefault();
      setIsDragging(false);
      alert('当前为生产模式，请接入真实 OCR 服务后再上传。可切换为演示模式体验。');
      return;
    }
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  };

  const handleFileUpload = (file: File) => {
    if (!demoMode) {
      alert('当前为生产模式，请接入真实 OCR 服务后再上传。可切换为演示模式体验。');
      return;
    }
    // Create a temporary mock task
    const newTask: OCRResult = {
      id: `ocr-${Date.now()}`,
      fileName: file.name,
      fileSize: `${(file.size / 1024 / 1024).toFixed(2)} MB`,
      uploadTime: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      status: 'processing',
      type: 'unknown',
      imageUrl: URL.createObjectURL(file), // In real app, this would be uploaded URL
      overallScore: 0,
      fields: []
    };

    setTaskList([newTask, ...taskList]);
    setActiveTask(newTask);

    // Simulate Processing Delay
    setTimeout(() => {
      setTaskList(prev => prev.map(t => {
        if (t.id === newTask.id) {
          return {
            ...t,
            status: 'review',
            type: 'invoice',
            overallScore: 88,
            fields: [
              { name: '识别结果-字段A', value: '模拟数据 123', confidence: 95 },
              { name: '识别结果-字段B', value: '模拟金额 ¥500.00', confidence: 85 },
              { name: '识别结果-字段C', value: '模糊不清', confidence: 45 },
            ]
          };
        }
        return t;
      }));
      // Also update active task to reflect changes
      setActiveTask(prev => prev && prev.id === newTask.id ? {
        ...prev,
        status: 'review',
        type: 'invoice',
        overallScore: 88,
        fields: [
          { name: '识别结果-字段A', value: '模拟数据 123', confidence: 95 },
          { name: '识别结果-字段B', value: '模拟金额 ¥500.00', confidence: 85 },
          { name: '识别结果-字段C', value: '模糊不清', confidence: 45 },
        ]
      } : prev);
    }, 3000);
  };

  const handleTypeChange = (newType: string) => {
    if (!activeTask) return;

    const updatedTask = { ...activeTask, type: newType as any };

    setActiveTask(updatedTask);
    setTaskList(prev => prev.map(t => t.id === activeTask.id ? updatedTask : t));
  };

  const handleFieldChange = (index: number, value: string) => {
    if (!activeTask) return;

    const newFields = [...activeTask.fields];
    newFields[index] = { ...newFields[index], value };

    const updatedTask = { ...activeTask, fields: newFields };

    setActiveTask(updatedTask);
    setTaskList(prev => prev.map(t => t.id === activeTask.id ? updatedTask : t));
  };

  const getScoreColor = (score: number) => {
    if (score >= 90) return 'bg-emerald-500 text-emerald-600';
    if (score >= 70) return 'bg-amber-500 text-amber-600';
    return 'bg-rose-500 text-rose-600';
  };

  const toggleDemoMode = (flag: boolean) => {
    safeStorage.setItem('demoMode', flag ? 'true' : 'false');
    setDemoMode(flag);
    // 简单处理：刷新以让其他视图同步状态
    window.location.reload();
  };

  if (!demoMode) {
    return (
      <div className="p-8 bg-slate-50 h-full flex flex-col items-center justify-center text-center gap-4">
        <DemoBadge text="当前为生产模式，OCR 模块未接入真实引擎，已关闭演示数据。" />
        <p className="text-slate-600">接入真实 OCR 服务后启用此模块，或切换到演示模式体验界面。</p>
        <button
          onClick={() => toggleDemoMode(true)}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
        >
          切换到演示模式
        </button>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-slate-50/50 animate-in fade-in slide-in-from-bottom-4 duration-500">

      {/* Header */}
      <div className="px-8 py-6 bg-white border-b border-slate-200 flex justify-between items-center shrink-0">
        <div>
          <h2 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
            <ScanLine className="text-primary-600" /> OCR 智能识别工坊
          </h2>
          <p className="text-slate-500 mt-1 text-sm">上传图像或 PDF，AI 引擎将自动提取关键结构化数据。</p>
        </div>
        <div className="flex gap-4 items-center">
          <DemoBadge text="演示模式：使用模拟识别结果，不调用真实 OCR 引擎。" className="mb-0" />
          <button
            onClick={() => toggleDemoMode(false)}
            className="px-3 py-2 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100"
          >
            关闭演示模式
          </button>
          <div className="text-right">
            <p className="text-xs text-slate-400">今日识别</p>
            <p className="text-xl font-bold text-slate-800">1,204 <span className="text-xs font-normal text-slate-400">页</span></p>
          </div>
          <div className="w-px h-10 bg-slate-200"></div>
          <div className="text-right">
            <p className="text-xs text-slate-400">平均准确率</p>
            <p className="text-xl font-bold text-emerald-600">98.5%</p>
          </div>
        </div>
      </div>

      {/* Main Workspace */}
      <div className="flex-1 flex overflow-hidden">

        {/* Left Sidebar: Upload & List */}
        <div className="w-80 bg-white border-r border-slate-200 flex flex-col shrink-0">
          {/* Upload Area */}
          <div className="p-4 border-b border-slate-100">
            <div
              className={`border-2 border-dashed rounded-xl p-6 flex flex-col items-center justify-center text-center transition-all cursor-pointer ${isDragging ? 'border-primary-500 bg-primary-50' : 'border-slate-300 hover:border-primary-400 hover:bg-slate-50'}`}
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
            >
              <input type="file" className="hidden" ref={fileInputRef} onChange={(e) => e.target.files && handleFileUpload(e.target.files[0])} accept="image/*,application/pdf" />
              <div className="bg-primary-100 p-3 rounded-full mb-3 text-primary-600">
                <UploadCloud size={24} />
              </div>
              <p className="text-sm font-bold text-slate-700">点击或拖拽上传</p>
              <p className="text-xs text-slate-400 mt-1">支持 JPG, PNG, PDF (Max 10MB)</p>
            </div>
          </div>

          {/* Task List */}
          <div className="flex-1 overflow-y-auto">
            <div className="p-3 space-y-2">
              {taskList.map(task => (
                <div
                  key={task.id}
                  onClick={() => setActiveTask(task)}
                  className={`p-3 rounded-lg border cursor-pointer transition-all relative overflow-hidden group ${activeTask?.id === task.id ? 'bg-primary-50 border-primary-200 ring-1 ring-primary-200' : 'bg-white border-slate-100 hover:border-slate-300'}`}
                >
                  {task.status === 'processing' && (
                    <div className="absolute bottom-0 left-0 h-1 bg-primary-500 animate-[progress_2s_ease-in-out_infinite] w-full"></div>
                  )}
                  <div className="flex justify-between items-start mb-1">
                    <div className="flex items-center gap-2 overflow-hidden">
                      <FileText size={16} className={activeTask?.id === task.id ? 'text-primary-600' : 'text-slate-400'} />
                      <span className="text-sm font-medium text-slate-800 truncate">{task.fileName}</span>
                    </div>
                    {task.status === 'completed' && <CheckCircle2 size={14} className="text-emerald-500 shrink-0" />}
                    {task.status === 'review' && <AlertTriangle size={14} className="text-amber-500 shrink-0" />}
                    {task.status === 'processing' && <Loader2 size={14} className="text-primary-500 animate-spin shrink-0" />}
                  </div>
                  <div className="flex justify-between items-center text-xs text-slate-400 pl-6">
                    <span>{task.uploadTime}</span>
                    <span>{task.fileSize}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Right Content: Review Area */}
        <div className="flex-1 bg-slate-50 p-6 overflow-hidden flex flex-col">
          {activeTask ? (
            <div className="flex-1 flex gap-6 h-full">

              {/* Image Preview */}
              <div className="flex-1 bg-slate-200 rounded-xl overflow-hidden shadow-inner border border-slate-300 relative flex items-center justify-center group">
                {/* Background Grid */}
                <div className="absolute inset-0 opacity-20 pointer-events-none bg-[linear-gradient(45deg,#ccc_25%,transparent_25%,transparent_75%,#ccc_75%,#ccc),linear-gradient(45deg,#ccc_25%,transparent_25%,transparent_75%,#ccc_75%,#ccc)] [background-size:20px_20px] [background-position:0_0,10px_10px]"></div>

                <img
                  src={activeTask.imageUrl}
                  alt="Preview"
                  className="max-w-full max-h-full shadow-2xl object-contain transition-transform duration-300"
                />

                {/* Scanning Overlay Effect */}
                {activeTask.status === 'processing' && (
                  <div className="absolute inset-0 bg-primary-900/10 z-10">
                    <div className="w-full h-1 bg-primary-400 shadow-[0_0_15px_rgba(56,189,248,0.8)] absolute top-0 animate-[scan_3s_linear_infinite]"></div>
                  </div>
                )}
              </div>

              {/* Extraction Results */}
              <div className="w-96 bg-white rounded-xl shadow-sm border border-slate-200 flex flex-col">
                <div className="p-4 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-xl">
                  <div>
                    <h3 className="font-bold text-slate-800">识别结果</h3>
                    <div className="flex items-center gap-2 mt-1">
                      <span className={`w-2 h-2 rounded-full ${activeTask.status === 'processing' ? 'bg-slate-300' : getScoreColor(activeTask.overallScore).split(' ')[0]}`}></span>
                      <span className="text-xs text-slate-500">整体置信度: {activeTask.status === 'processing' ? '--' : `${activeTask.overallScore}%`}</span>
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <button className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 transition-colors" title="重新识别"><RefreshCw size={16} /></button>
                    <button className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 transition-colors" title="查看原件"><Eye size={16} /></button>
                  </div>
                </div>

                <div className="flex-1 overflow-y-auto p-4 space-y-6">
                  {activeTask.status === 'processing' ? (
                    <div className="h-full flex flex-col items-center justify-center text-slate-400 space-y-4">
                      <Loader2 size={32} className="animate-spin text-primary-500" />
                      <p className="text-sm">AI 引擎正在解析文档结构...</p>
                    </div>
                  ) : (
                    <>
                      {/* Document Type Classifier */}
                      <div className="space-y-2 bg-slate-50 p-3 rounded-lg border border-slate-100">
                        <div className="flex items-center justify-between">
                          <label className="text-xs font-bold text-slate-500 uppercase flex items-center gap-1">
                            <Tag size={12} /> 文档分类
                          </label>
                          <span className="text-xs text-primary-600 bg-primary-50 px-2 py-0.5 rounded-full font-medium border border-primary-100">AI 自动推荐</span>
                        </div>
                        <div className="relative">
                          <select
                            value={activeTask.type}
                            onChange={(e) => handleTypeChange(e.target.value)}
                            className="w-full appearance-none bg-white border border-slate-200 text-slate-700 text-sm font-bold rounded-lg py-2 pl-9 pr-8 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none cursor-pointer shadow-sm"
                          >
                            {DOC_TYPES.map(type => (
                              <option key={type.value} value={type.value}>{type.label}</option>
                            ))}
                          </select>
                          {(() => {
                            const TypeIcon = DOC_TYPES.find(d => d.value === activeTask.type)?.icon || FileText;
                            return <TypeIcon size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />;
                          })()}
                          <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
                        </div>
                      </div>

                      {/* Fields List */}
                      <div className="space-y-4">
                        {activeTask.fields.map((field, idx) => (
                          <div key={idx} className="group">
                            <div className="flex justify-between mb-1">
                              <label className="text-xs font-bold text-slate-500 uppercase">{field.name}</label>
                              <span className={`text-xs font-medium ${field.confidence < 70 ? 'text-rose-500' : 'text-slate-400'}`}>
                                {field.confidence}%
                              </span>
                            </div>
                            <div className="relative">
                              <input
                                type="text"
                                value={field.value}
                                onChange={(e) => handleFieldChange(idx, e.target.value)}
                                className={`w-full p-2 text-sm border rounded-lg outline-none transition-all ${field.confidence < 70 ? 'border-rose-300 bg-rose-50 text-rose-700 focus:ring-rose-200 pr-10' : 'border-slate-200 bg-slate-50 text-slate-800 focus:border-primary-500 focus:ring-2 focus:ring-primary-100'}`}
                              />
                              {field.confidence < 70 && (
                                <div className="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-1">
                                  <div className="group/info relative">
                                    <Info size={16} className="text-rose-500 cursor-help" />
                                    {/* Tooltip */}
                                    <div className="absolute bottom-full right-0 mb-2 w-48 p-3 bg-slate-800 text-white text-xs rounded-lg shadow-xl opacity-0 group-hover/info:opacity-100 transition-opacity pointer-events-none z-20 animate-in fade-in zoom-in-95 duration-200">
                                      <div className="font-bold mb-1 border-b border-slate-600 pb-1">AI 识别分析</div>
                                      <p className="text-slate-300 mb-1">预测内容: <span className="text-white font-mono break-all">{field.value}</span></p>
                                      <p className="text-slate-300">置信度: <span className="text-rose-400 font-bold">{field.confidence}%</span> (偏低)</p>
                                      <div className="mt-2 text-[10px] text-slate-400 italic flex items-center gap-1">
                                        <AlertTriangle size={10} /> 建议人工复核
                                      </div>
                                      {/* Arrow */}
                                      <div className="absolute -bottom-1 right-1.5 w-2 h-2 bg-slate-800 rotate-45"></div>
                                    </div>
                                  </div>
                                </div>
                              )}
                            </div>
                            <div className="w-full bg-slate-100 h-1 mt-1.5 rounded-full overflow-hidden">
                              <div
                                className={`h-full rounded-full ${getScoreColor(field.confidence).split(' ')[0]}`}
                                style={{ width: `${field.confidence}%` }}
                              ></div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </>
                  )}
                </div>

                <div className="p-4 border-t border-slate-100 bg-slate-50 rounded-b-xl space-y-2">
                  {/* Auto-save Indicator */}
                  <div className="flex items-center justify-between text-xs px-1 h-5">
                    <span className="flex items-center gap-1.5 text-slate-400">
                      {isAutoSaving ? (
                        <>
                          <Loader2 size={10} className="animate-spin" />
                          正在保存草稿...
                        </>
                      ) : lastSavedTime ? (
                        <>
                          <Cloud size={12} className="text-emerald-500" />
                          已自动保存于 {lastSavedTime}
                        </>
                      ) : (
                        <span className="opacity-0">Placeholder</span>
                      )}
                    </span>
                  </div>

                  <button
                    disabled={activeTask.status === 'processing'}
                    className="w-full py-2 bg-primary-600 text-white font-bold rounded-lg shadow-lg shadow-primary-500/20 hover:bg-primary-700 active:scale-95 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                  >
                    <Save size={16} /> 确认并归档
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="flex-1 flex items-center justify-center flex-col text-slate-300">
              <ScanLine size={64} className="mb-4 opacity-50" />
              <p className="text-lg font-medium">选择或上传任务开始识别</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default OCRProcessingView;
