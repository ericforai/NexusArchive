// Input: React、lucide-react 图标、scan API、QRCode
// Output: React 组件 OCRProcessingView（对接真实 API + 移动端扫码）
// Pos: src/pages/pre-archive/OCRProcessingView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useRef, useEffect } from 'react';
import { UploadCloud, FileText, CheckCircle2, AlertTriangle, ScanLine, Loader2, Eye, Save, RefreshCw, ChevronDown, Tag, Receipt, Building, CreditCard, FileBadge, Cloud, Info, Smartphone, X } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import { scanApi, type ScanWorkspaceItem, type OcrField } from '../../api/scan';
import { toast } from '../../utils/notificationService';

const DOC_TYPES = [
  { value: 'invoice', label: '增值税发票', icon: Receipt },
  { value: 'contract', label: '合同协议', icon: Building },
  { value: 'receipt', label: '银行回单', icon: CreditCard },
  { value: 'id_card', label: '身份/资质证件', icon: FileBadge },
  { value: 'unknown', label: '其他/未知类型', icon: FileText },
];

export const OCRProcessingView: React.FC = () => {
  const [taskList, setTaskList] = useState<ScanWorkspaceItem[]>([]);
  const [activeTask, setActiveTask] = useState<ScanWorkspaceItem | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const pollingIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // Auto-save State
  const [lastSavedTime, setLastSavedTime] = useState<string | null>(null);
  const [isAutoSaving, setIsAutoSaving] = useState(false);

  // Mobile Scan QR Code State
  const [isQrModalOpen, setIsQrModalOpen] = useState(false);
  const [qrSessionId, setQrSessionId] = useState<string | null>(null);
  const [isCreatingSession, setIsCreatingSession] = useState(false);

  // Ref to track activeTask for interval closure
  const activeTaskRef = useRef(activeTask);

  useEffect(() => {
    activeTaskRef.current = activeTask;
  }, [activeTask]);

  // Load workspace from backend
  const loadWorkspace = async () => {
    try {
      const items = await scanApi.getWorkspace();
      setTaskList(items.data);
      // Auto-select first item if none selected
      if (items.data.length > 0 && !activeTask) {
        setActiveTask(items.data[0]);
      }
    } catch (error) {
      console.error('Failed to load workspace:', error);
      toast.error('加载工作区失败');
    }
  };

  // Initial load and polling
  useEffect(() => {
    loadWorkspace();

    // Poll for updates every 5 seconds
    pollingIntervalRef.current = setInterval(() => {
      loadWorkspace();
    }, 5000);

    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
    };
  }, []);

  // Auto-save Interval
  useEffect(() => {
    const saveInterval = setInterval(() => {
      const currentTask = activeTaskRef.current;
      if (currentTask && currentTask.ocrStatus === 'review' && currentTask.id) {
        // Auto-save to backend
        handleAutoSave(currentTask.id);
      }
    }, 30000); // 30 Seconds

    return () => clearInterval(saveInterval);
  }, []);

  // Auto-save handler
  const handleAutoSave = async (id: number) => {
    if (!activeTaskRef.current) return;

    setIsAutoSaving(true);
    try {
      await scanApi.update(id, {
        ocrResult: activeTaskRef.current.ocrResult,
        docType: activeTaskRef.current.docType,
      });
      const now = new Date();
      setLastSavedTime(now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }));
    } catch (error) {
      console.error('Auto-save failed:', error);
      toast.error('自动保存失败');
    } finally {
      setIsAutoSaving(false);
    }
  };

  // Mobile Scan QR Code handler
  const handleOpenMobileScan = async () => {
    setIsCreatingSession(true);
    try {
      const response = await scanApi.createSession();
      setQrSessionId(response.data.sessionId);
      setIsQrModalOpen(true);
      toast.success('会话已创建，请扫码上传');
    } catch (error) {
      console.error('Failed to create session:', error);
      toast.error('创建会话失败，请重试');
    } finally {
      setIsCreatingSession(false);
    }
  };

  const handleCloseQrModal = () => {
    setIsQrModalOpen(false);
    setQrSessionId(null);
  };

  // Build QR Code URL
  const qrCodeUrl = qrSessionId
    ? `${window.location.origin}/mobile/scan?session=${qrSessionId}`
    : '';

  // Parse OCR result to fields
  const parseOcrFields = (ocrResult?: string): OcrField[] => {
    if (!ocrResult) return [];

    try {
      const parsed = JSON.parse(ocrResult) as Record<string, any>;
      const fields: OcrField[] = [];

      // Map common OCR fields
      const fieldMapping: Record<string, string> = {
        invoiceNumber: '发票号码',
        invoiceDate: '开票日期',
        amount: '金额',
        taxAmount: '税额',
        totalAmount: '价税合计',
        sellerName: '销售方名称',
        sellerTaxId: '销售方税号',
        buyerName: '购买方名称',
        buyerTaxId: '购买方税号',
      };

      Object.entries(parsed).forEach(([key, value]) => {
        if (value && typeof value === 'string') {
          fields.push({
            label: fieldMapping[key] || key,
            value,
            confidence: 90, // Default confidence for parsed fields
            editable: true,
          });
        }
      });

      return fields;
    } catch (error) {
      console.error('Failed to parse OCR result:', error);
      return [];
    }
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      handleFileUpload(e.dataTransfer.files[0]);
    }
  };

  const handleFileUpload = async (file: File) => {
    try {
      toast.loading('正在上传文件...', { id: 'upload', duration: 0 });

      // Upload file to backend
      const uploadedItem = await scanApi.upload(file);

      toast.dismiss('upload');
      toast.success('文件上传成功，开始 OCR 识别...');

      // Add to local state
      setTaskList(prev => [uploadedItem.data, ...prev]);
      setActiveTask(uploadedItem.data);

      // Auto-trigger OCR
      if (uploadedItem.data.id) {
        toast.loading('正在进行 OCR 识别...', { id: 'ocr', duration: 0 });
        await scanApi.processOcr(uploadedItem.data.id);

        // Poll for OCR completion
        const checkOcrStatusRef = useRef<NodeJS.Timeout | null>(null);
        checkOcrStatusRef.current = setInterval(async () => {
          try {
            const items = await scanApi.getWorkspace();
            const updatedItem = items.data.find(i => i.id === uploadedItem.data.id);

            if (updatedItem && updatedItem.ocrStatus === 'review') {
              if (checkOcrStatusRef.current) clearInterval(checkOcrStatusRef.current);
              toast.dismiss('ocr');
              toast.success('OCR 识别完成');

              // Update local state
              setTaskList(items.data);
              setActiveTask(updatedItem);
            } else if (updatedItem && updatedItem.ocrStatus === 'failed') {
              if (checkOcrStatusRef.current) clearInterval(checkOcrStatusRef.current);
              toast.dismiss('ocr');
              toast.error('OCR 识别失败，请重试');
            }
          } catch (error) {
            console.error('Failed to check OCR status:', error);
          }
        }, 2000); // Check every 2 seconds

        // Timeout after 60 seconds
        setTimeout(() => {
          if (checkOcrStatusRef.current) clearInterval(checkOcrStatusRef.current);
          toast.dismiss('ocr');
        }, 60000);
      }
    } catch (error) {
      console.error('Upload failed:', error);
      toast.dismiss('upload');
      toast.error('文件上传失败');
    }
  };

  const handleTypeChange = async (newType: string) => {
    if (!activeTask || !activeTask.id) return;

    try {
      const updatedTask = { ...activeTask, docType: newType };
      setActiveTask(updatedTask);
      setTaskList(prev => prev.map(t => t.id === activeTask.id ? updatedTask : t));

      // Auto-save to backend
      await scanApi.update(activeTask.id, { docType: newType });
    } catch (error) {
      console.error('Failed to update document type:', error);
      toast.error('更新文档类型失败');
    }
  };

  const handleFieldChange = async (index: number, value: string) => {
    if (!activeTask || !activeTask.id) return;

    try {
      // Parse current OCR result
      const fields = parseOcrFields(activeTask.ocrResult);
      if (!fields[index]) return;

      // Update field value
      fields[index].value = value;

      // Convert back to JSON string
      const updatedOcrResult = JSON.stringify(
        JSON.parse(activeTask.ocrResult || '{}'),
        (key, val) => {
          // Find matching field and update value
          const field = fields.find(f => f.label === key);
          if (field) return field.value;
          return val;
        }
      );

      const updatedTask = { ...activeTask, ocrResult: updatedOcrResult };
      setActiveTask(updatedTask);
      setTaskList(prev => prev.map(t => t.id === activeTask.id ? updatedTask : t));

      // Auto-save to backend
      await scanApi.update(activeTask.id, { ocrResult: updatedOcrResult });
    } catch (error) {
      console.error('Failed to update field:', error);
      toast.error('更新字段失败');
    }
  };

  const handleSubmitToArchive = async () => {
    if (!activeTask || !activeTask.id) return;

    try {
      toast.loading('正在提交到预归档池...', { id: 'submit', duration: 0 });

      const result = await scanApi.submit(activeTask.id);

      toast.dismiss('submit');
      toast.success('提交成功！档案 ID: ' + result.data.archiveId);

      // Remove from local list
      setTaskList(prev => prev.filter(t => t.id !== activeTask.id));
      setActiveTask(null);

      // Reload workspace
      await loadWorkspace();
    } catch (error) {
      console.error('Submit failed:', error);
      toast.dismiss('submit');
      toast.error('提交失败，请重试');
    }
  };

  const getScoreColor = (score: number) => {
    if (score >= 90) return 'bg-emerald-500 text-emerald-600';
    if (score >= 70) return 'bg-amber-500 text-amber-600';
    return 'bg-rose-500 text-rose-600';
  };

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

          {/* Mobile Scan Button */}
          <div className="px-4 py-3 border-b border-slate-100">
            <button
              onClick={handleOpenMobileScan}
              disabled={isCreatingSession}
              className="w-full flex items-center justify-center gap-2 py-3 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-xl font-medium hover:from-blue-600 hover:to-purple-700 transition-all shadow-lg disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isCreatingSession ? (
                <>
                  <Loader2 size={18} className="animate-spin" />
                  <span>创建会话中...</span>
                </>
              ) : (
                <>
                  <Smartphone size={18} />
                  <span>手机扫码上传</span>
                </>
              )}
            </button>
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
                  {task.ocrStatus === 'processing' && (
                    <div className="absolute bottom-0 left-0 h-1 bg-primary-500 animate-[progress_2s_ease-in-out_infinite] w-full"></div>
                  )}
                  <div className="flex justify-between items-start mb-1">
                    <div className="flex items-center gap-2 overflow-hidden">
                      <FileText size={16} className={activeTask?.id === task.id ? 'text-primary-600' : 'text-slate-400'} />
                      <span className="text-sm font-medium text-slate-800 truncate">{task.fileName}</span>
                    </div>
                    {task.ocrStatus === 'completed' && <CheckCircle2 size={14} className="text-emerald-500 shrink-0" />}
                    {task.ocrStatus === 'review' && <AlertTriangle size={14} className="text-amber-500 shrink-0" />}
                    {task.ocrStatus === 'processing' && <Loader2 size={14} className="text-primary-500 animate-spin shrink-0" />}
                  </div>
                  <div className="flex justify-between items-center text-xs text-slate-400 pl-6">
                    <span>{task.createdAt ? new Date(task.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}</span>
                    <span>{task.fileSize ? `${(task.fileSize / 1024 / 1024).toFixed(2)} MB` : '--'}</span>
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
                  src={activeTask.filePath}
                  alt="Preview"
                  className="max-w-full max-h-full shadow-2xl object-contain transition-transform duration-300"
                />

                {/* Scanning Overlay Effect */}
                {activeTask.ocrStatus === 'processing' && (
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
                      <span className={`w-2 h-2 rounded-full ${activeTask.ocrStatus === 'processing' ? 'bg-slate-300' : getScoreColor(activeTask.overallScore || 0).split(' ')[0]}`}></span>
                      <span className="text-xs text-slate-500">整体置信度: {activeTask.ocrStatus === 'processing' ? '--' : `${activeTask.overallScore || 0}%`}</span>
                    </div>
                  </div>
                  <div className="flex gap-1">
                    <button className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 transition-colors" title="重新识别"><RefreshCw size={16} /></button>
                    <button className="p-2 hover:bg-slate-100 rounded-lg text-slate-400 transition-colors" title="查看原件"><Eye size={16} /></button>
                  </div>
                </div>

                <div className="flex-1 overflow-y-auto p-4 space-y-6">
                  {activeTask.ocrStatus === 'processing' ? (
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
                            value={activeTask.docType || 'unknown'}
                            onChange={(e) => handleTypeChange(e.target.value)}
                            className="w-full appearance-none bg-white border border-slate-200 text-slate-700 text-sm font-bold rounded-lg py-2 pl-9 pr-8 focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none cursor-pointer shadow-sm"
                          >
                            {DOC_TYPES.map(type => (
                              <option key={type.value} value={type.value}>{type.label}</option>
                            ))}
                          </select>
                          {(() => {
                            const TypeIcon = DOC_TYPES.find(d => d.value === (activeTask.docType || 'unknown'))?.icon || FileText;
                            return <TypeIcon size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />;
                          })()}
                          <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none" />
                        </div>
                      </div>

                      {/* Fields List */}
                      <div className="space-y-4">
                        {parseOcrFields(activeTask.ocrResult).map((field, idx) => (
                          <div key={idx} className="group">
                            <div className="flex justify-between mb-1">
                              <label className="text-xs font-bold text-slate-500 uppercase">{field.label}</label>
                              <span className={`text-xs font-medium ${(field.confidence || 0) < 70 ? 'text-rose-500' : 'text-slate-400'}`}>
                                {field.confidence || 0}%
                              </span>
                            </div>
                            <div className="relative">
                              <input
                                type="text"
                                value={field.value}
                                onChange={(e) => handleFieldChange(idx, e.target.value)}
                                className={`w-full p-2 text-sm border rounded-lg outline-none transition-all ${(field.confidence || 0) < 70 ? 'border-rose-300 bg-rose-50 text-rose-700 focus:ring-rose-200 pr-10' : 'border-slate-200 bg-slate-50 text-slate-800 focus:border-primary-500 focus:ring-2 focus:ring-primary-100'}`}
                              />
                              {(field.confidence || 0) < 70 && (
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
                                className={`h-full rounded-full ${getScoreColor(field.confidence || 0).split(' ')[0]}`}
                                style={{ width: `${field.confidence || 0}%` }}
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
                    disabled={activeTask.ocrStatus === 'processing'}
                    onClick={handleSubmitToArchive}
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

      {/* QR Code Modal for Mobile Scan */}
      {isQrModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 animate-in fade-in duration-200" onClick={handleCloseQrModal}>
          <div className="bg-white rounded-2xl p-8 max-w-sm mx-4 animate-in zoom-in-95 duration-200" onClick={e => e.stopPropagation()}>
            {/* Header */}
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-xl font-bold text-slate-800 flex items-center gap-2">
                <Smartphone className="text-primary-600" />
                手机扫码上传
              </h3>
              <button
                onClick={handleCloseQrModal}
                className="p-1 hover:bg-slate-100 rounded-lg transition-colors"
              >
                <X size={20} className="text-slate-400" />
              </button>
            </div>

            {/* QR Code */}
            <div className="bg-slate-50 p-6 rounded-xl flex items-center justify-center mb-4">
              {qrCodeUrl ? (
                <QRCodeSVG
                  value={qrCodeUrl}
                  size={200}
                  level="M"
                  className="rounded-lg"
                />
              ) : (
                <div className="w-[200px] h-[200px] flex items-center justify-center">
                  <Loader2 size={32} className="animate-spin text-slate-300" />
                </div>
              )}
            </div>

            {/* Instructions */}
            <div className="space-y-3 mb-6">
              <p className="text-sm text-slate-600 text-center">
                使用手机相机扫描二维码，即可直接拍照上传文件
              </p>
              <div className="bg-blue-50 border border-blue-100 rounded-lg p-3">
                <p className="text-xs text-blue-700">
                  <span className="font-bold">操作步骤：</span>
                </p>
                <ol className="text-xs text-blue-600 mt-1 space-y-1 list-decimal list-inside">
                  <li>打开手机相机或微信扫一扫</li>
                  <li>对准二维码扫描</li>
                  <li>在打开的页面中拍照或选择相册</li>
                  <li>文件将自动上传到工作区</li>
                </ol>
              </div>
            </div>

            {/* Session ID Display */}
            {qrSessionId && (
              <div className="bg-slate-100 rounded-lg p-2 mb-4">
                <p className="text-xs text-slate-500 mb-1">会话 ID</p>
                <p className="text-sm font-mono text-slate-700 break-all">{qrSessionId}</p>
              </div>
            )}

            {/* Close Button */}
            <button
              onClick={handleCloseQrModal}
              className="w-full py-3 bg-slate-200 text-slate-700 rounded-lg font-medium hover:bg-slate-300 transition-colors"
            >
              关闭
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default OCRProcessingView;
