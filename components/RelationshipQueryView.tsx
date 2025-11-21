import React, { useState } from 'react';
import { Search, FileText, ArrowRight, Receipt, FileSpreadsheet, Building, CreditCard, X, ExternalLink, ZoomIn, ZoomOut, RotateCw, Download, Printer, CheckCircle2 } from 'lucide-react';

interface NodeData {
  id: string;
  type: 'contract' | 'invoice' | 'voucher' | 'receipt' | 'report';
  code: string;
  name: string;
  amount?: string;
  date: string;
  status: string;
}

const MOCK_NODES: Record<string, NodeData> = {
  contract: { id: 'node-1', type: 'contract', code: 'CON-2023-098', name: '年度技术服务协议', amount: '¥ 150,000.00', date: '2023-01-15', status: '生效中' },
  invoice1: { id: 'node-2', type: 'invoice', code: 'INV-202311-089', name: '阿里云计算服务费发票', amount: '¥ 12,800.00', date: '2023-11-02', status: '已验真' },
  invoice2: { id: 'node-3', type: 'invoice', code: 'INV-202311-092', name: '服务器采购发票', amount: '¥ 45,200.00', date: '2023-11-03', status: '已验真' },
  voucher: { id: 'node-4', type: 'voucher', code: 'JZ-202311-0052', name: '11月技术部费用报销', amount: '¥ 58,000.00', date: '2023-11-05', status: '已过账' },
  receipt: { id: 'node-5', type: 'receipt', code: 'B-20231105-003', name: '招商银行付款回单', amount: '¥ 58,000.00', date: '2023-11-05', status: '已匹配' },
  report: { id: 'node-6', type: 'report', code: 'REP-2023-11', name: '11月科目余额表', date: '2023-11-30', status: '已生成' }
};

export const RelationshipQueryView: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('JZ-202311-0052');
  const [selectedNode, setSelectedNode] = useState<NodeData | null>(MOCK_NODES.voucher);
  const [isGraphVisible, setIsGraphVisible] = useState(true);
  
  // Image Viewer State
  const [isImageViewerOpen, setIsImageViewerOpen] = useState(false);
  const [zoomLevel, setZoomLevel] = useState(100);
  const [rotation, setRotation] = useState(0);

  // Toast State (Simple local implementation for this view)
  const [showToast, setShowToast] = useState(false);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setIsGraphVisible(false);
    setTimeout(() => setIsGraphVisible(true), 300); // Simulate reload
  };

  const handleDownloadXML = () => {
    setShowToast(true);
    setTimeout(() => setShowToast(false), 3000);
  };

  const handleZoomIn = () => setZoomLevel(prev => Math.min(prev + 25, 200));
  const handleZoomOut = () => setZoomLevel(prev => Math.max(prev - 25, 50));
  const handleRotate = () => setRotation(prev => (prev + 90) % 360);

  const renderNode = (data: NodeData, position: string) => {
    const isSelected = selectedNode?.id === data.id;
    let Icon = FileText;
    let colorClass = 'bg-slate-100 text-slate-600';

    switch (data.type) {
      case 'contract': Icon = Building; colorClass = 'bg-indigo-100 text-indigo-600 border-indigo-200'; break;
      case 'invoice': Icon = Receipt; colorClass = 'bg-purple-100 text-purple-600 border-purple-200'; break;
      case 'voucher': Icon = FileText; colorClass = 'bg-blue-500 text-white shadow-lg shadow-blue-500/30 border-blue-400'; break;
      case 'receipt': Icon = CreditCard; colorClass = 'bg-emerald-100 text-emerald-600 border-emerald-200'; break;
      case 'report': Icon = FileSpreadsheet; colorClass = 'bg-amber-100 text-amber-600 border-amber-200'; break;
    }

    return (
      <div 
        className={`relative group cursor-pointer transition-all duration-300 transform hover:scale-105 ${position}`}
        onClick={() => setSelectedNode(data)}
      >
        <div className={`w-48 p-4 rounded-xl border-2 ${isSelected ? 'ring-4 ring-primary-100 border-primary-500' : 'border-transparent'} ${data.type === 'voucher' ? '' : 'bg-white shadow-sm hover:shadow-md'}`}>
           <div className="flex items-start justify-between mb-2">
              <div className={`p-2 rounded-lg ${colorClass}`}>
                <Icon size={20} />
              </div>
              <span className={`text-[10px] px-2 py-0.5 rounded-full uppercase font-bold tracking-wider ${
                ['已验真', '已过账', '已匹配', '已生成', '生效中'].includes(data.status) ? 'bg-emerald-50 text-emerald-600' : 'bg-slate-100 text-slate-500'
              }`}>
                {data.status}
              </span>
           </div>
           <h4 className={`text-sm font-bold truncate mb-1 ${data.type === 'voucher' ? 'text-white' : 'text-slate-800'}`}>{data.code}</h4>
           <p className={`text-xs truncate ${data.type === 'voucher' ? 'text-blue-100' : 'text-slate-500'}`}>{data.name}</p>
           {data.amount && <p className={`text-xs font-mono mt-2 font-medium ${data.type === 'voucher' ? 'text-white' : 'text-slate-700'}`}>{data.amount}</p>}
        </div>
        
        {/* Connecting Lines Hooks (Visual only) */}
        {position !== 'center' && (
           <div className={`absolute top-1/2 w-4 h-4 bg-slate-300 rounded-full transform -translate-y-1/2 ${position.includes('left') ? '-right-2' : '-left-2'} hidden`} />
        )}
      </div>
    );
  };

  return (
    <div className="h-full flex flex-col bg-slate-50/50 relative">
      
      {/* Simple Toast */}
      {showToast && (
          <div className="absolute top-6 left-1/2 -translate-x-1/2 z-50 bg-slate-800 text-white px-4 py-2 rounded-lg shadow-xl flex items-center gap-2 animate-in slide-in-from-top-2 fade-in">
             <CheckCircle2 size={18} className="text-emerald-400"/>
             <span className="text-sm">元数据 XML 已下载</span>
          </div>
      )}

      {/* Header Search */}
      <div className="px-8 py-6 bg-white border-b border-slate-200 flex justify-between items-center shadow-sm z-10">
         <div>
            <h2 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
              穿透联查 <span className="px-2 py-1 bg-blue-50 text-blue-600 text-xs rounded-md font-medium border border-blue-100">Beta</span>
            </h2>
            <p className="text-slate-500 mt-1 text-sm">输入任意单据号，自动生成全链路业务血缘图谱。</p>
         </div>
         <form onSubmit={handleSearch} className="flex items-center gap-2 w-full max-w-md">
            <div className="relative flex-1">
               <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" size={18} />
               <input 
                 type="text" 
                 value={searchQuery}
                 onChange={(e) => setSearchQuery(e.target.value)}
                 className="w-full pl-10 pr-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-2 focus:ring-primary-500/20 focus:border-primary-500 outline-none transition-all"
                 placeholder="请输入凭证号 / 发票号 / 合同号..."
               />
            </div>
            <button type="submit" className="px-6 py-2.5 bg-primary-600 text-white font-medium rounded-xl hover:bg-primary-700 shadow-lg shadow-primary-500/30 transition-all">
               查询
            </button>
         </form>
      </div>

      {/* Canvas Area */}
      <div className="flex-1 relative overflow-hidden flex">
         
         {/* Graph Container */}
         <div className="flex-1 relative overflow-auto bg-[radial-gradient(#e2e8f0_1px,transparent_1px)] [background-size:20px_20px] flex items-center justify-center min-h-[600px]">
            
            {isGraphVisible && (
              <div className="relative w-[1000px] h-[600px] animate-in zoom-in-95 duration-500 fade-in">
                
                {/* Connecting Lines (SVG Layer) */}
                <svg className="absolute inset-0 w-full h-full pointer-events-none z-0">
                   {/* Contract to Voucher */}
                   <path d="M 200 150 C 350 150, 350 300, 500 300" fill="none" stroke="#cbd5e1" strokeWidth="2" strokeDasharray="5,5" className="animate-[dash_20s_linear_infinite]" />
                   {/* Invoice 1 to Voucher */}
                   <path d="M 200 300 C 350 300, 350 300, 500 300" fill="none" stroke="#94a3b8" strokeWidth="2" />
                   {/* Invoice 2 to Voucher */}
                   <path d="M 200 450 C 350 450, 350 300, 500 300" fill="none" stroke="#94a3b8" strokeWidth="2" />
                   {/* Voucher to Receipt */}
                   <path d="M 500 300 C 650 300, 650 300, 800 300" fill="none" stroke="#94a3b8" strokeWidth="2" />
                   {/* Voucher to Report */}
                   <path d="M 500 300 C 650 300, 650 450, 800 450" fill="none" stroke="#cbd5e1" strokeWidth="2" strokeDasharray="5,5" />
                </svg>

                {/* Nodes Layer */}
                
                {/* Upstream: Contract */}
                <div className="absolute left-[50px] top-[100px]">
                   {renderNode(MOCK_NODES.contract, '')}
                   <div className="absolute -bottom-8 left-1/2 -translate-x-1/2 text-xs text-slate-400 font-medium">业务依据</div>
                </div>

                {/* Upstream: Invoices */}
                <div className="absolute left-[50px] top-[250px]">
                   {renderNode(MOCK_NODES.invoice1, '')}
                </div>
                <div className="absolute left-[50px] top-[400px]">
                   {renderNode(MOCK_NODES.invoice2, '')}
                   <div className="absolute -bottom-8 left-1/2 -translate-x-1/2 text-xs text-slate-400 font-medium">原始凭证</div>
                </div>

                {/* Center: Voucher */}
                <div className="absolute left-[400px] top-[250px] z-10">
                   {renderNode(MOCK_NODES.voucher, '')}
                   <div className="absolute -bottom-8 left-1/2 -translate-x-1/2 text-xs text-primary-600 font-bold bg-blue-50 px-2 py-1 rounded">核心单据</div>
                </div>

                {/* Downstream: Receipt */}
                <div className="absolute left-[750px] top-[250px]">
                   {renderNode(MOCK_NODES.receipt, '')}
                   <div className="absolute -bottom-8 left-1/2 -translate-x-1/2 text-xs text-slate-400 font-medium">资金结算</div>
                </div>

                {/* Downstream: Report */}
                <div className="absolute left-[750px] top-[400px]">
                   {renderNode(MOCK_NODES.report, '')}
                   <div className="absolute -bottom-8 left-1/2 -translate-x-1/2 text-xs text-slate-400 font-medium">财务归档</div>
                </div>
                
              </div>
            )}
         </div>

         {/* Detail Drawer */}
         <div className={`w-96 bg-white border-l border-slate-200 shadow-xl transition-all duration-300 flex flex-col ${selectedNode ? 'translate-x-0' : 'translate-x-full absolute right-0 h-full'}`}>
            {selectedNode ? (
              <>
                 <div className="p-6 border-b border-slate-100 flex justify-between items-start">
                    <div>
                       <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-1">{selectedNode.type.toUpperCase()}</span>
                       <h3 className="text-xl font-bold text-slate-800 leading-tight">{selectedNode.code}</h3>
                    </div>
                    <button onClick={() => setSelectedNode(null)} className="text-slate-400 hover:text-slate-600"><X size={20} /></button>
                 </div>
                 
                 <div className="p-6 flex-1 overflow-y-auto space-y-6">
                    {/* Status Card */}
                    <div className="bg-slate-50 rounded-xl p-4 border border-slate-100">
                       <div className="flex justify-between items-center mb-2">
                          <span className="text-sm text-slate-500">当前状态</span>
                          <span className="px-2 py-1 bg-emerald-100 text-emerald-700 text-xs font-bold rounded">{selectedNode.status}</span>
                       </div>
                       <div className="w-full bg-slate-200 h-1.5 rounded-full overflow-hidden">
                          <div className="bg-emerald-500 w-full h-full rounded-full"></div>
                       </div>
                    </div>

                    {/* Meta Data */}
                    <div className="space-y-4">
                       <div>
                          <label className="text-xs text-slate-400 font-medium">单据名称</label>
                          <p className="text-sm font-medium text-slate-800">{selectedNode.name}</p>
                       </div>
                       {selectedNode.amount && (
                        <div>
                            <label className="text-xs text-slate-400 font-medium">涉及金额</label>
                            <p className="text-sm font-mono font-bold text-slate-800">{selectedNode.amount}</p>
                        </div>
                       )}
                       <div>
                          <label className="text-xs text-slate-400 font-medium">业务日期</label>
                          <p className="text-sm font-medium text-slate-800">{selectedNode.date}</p>
                       </div>
                       <div>
                          <label className="text-xs text-slate-400 font-medium">摘要备注</label>
                          <p className="text-sm text-slate-600 leading-relaxed">
                             系统自动生成的关联描述。该单据已通过四性检测，且与上游业务单据金额匹配一致。
                          </p>
                       </div>
                    </div>
                    
                    {/* Actions */}
                    <div className="pt-4 border-t border-slate-100">
                        <button 
                           onClick={() => setIsImageViewerOpen(true)}
                           className="w-full py-2.5 mb-3 bg-white border border-slate-300 text-slate-700 font-medium rounded-lg hover:bg-slate-50 flex items-center justify-center gap-2 transition-colors shadow-sm"
                        >
                           <ExternalLink size={16} /> 查看原件影像
                        </button>
                        <button 
                           onClick={handleDownloadXML}
                           className="w-full py-2.5 bg-primary-50 text-primary-700 font-medium rounded-lg hover:bg-primary-100 flex items-center justify-center gap-2 transition-colors"
                        >
                           <Download size={16} /> 下载元数据包 (XML)
                        </button>
                    </div>
                 </div>
              </>
            ) : (
               <div className="flex-1 flex items-center justify-center text-slate-400 text-sm">
                  选择左侧节点查看详情
               </div>
            )}
         </div>

      </div>

      {/* Real Image Viewer Modal */}
      {isImageViewerOpen && selectedNode && (
         <div className="fixed inset-0 z-[100] bg-slate-950/90 backdrop-blur-md flex flex-col animate-in fade-in duration-200">
            {/* Viewer Header */}
            <div className="h-16 px-6 flex items-center justify-between bg-transparent text-white shrink-0">
               <div className="flex items-center gap-4">
                  <div className="bg-white/10 p-2 rounded-lg">
                     <FileText size={20} className="text-white"/>
                  </div>
                  <div>
                     <h3 className="font-bold">{selectedNode.name}</h3>
                     <p className="text-xs text-slate-400">{selectedNode.code} • {selectedNode.type.toUpperCase()}</p>
                  </div>
               </div>
               <button onClick={() => setIsImageViewerOpen(false)} className="p-2 hover:bg-white/10 rounded-full transition-colors">
                  <X size={24} />
               </button>
            </div>

            {/* Viewer Canvas */}
            <div className="flex-1 flex items-center justify-center overflow-hidden p-8 bg-slate-900/50">
               <div 
                  className="relative bg-white shadow-2xl transition-transform duration-200 ease-out origin-center"
                  style={{ 
                     transform: `scale(${zoomLevel / 100}) rotate(${rotation}deg)`,
                     boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)'
                  }}
               >
                   {/* Mock Document Image */}
                   <img 
                     src={`https://placehold.co/600x800/f8fafc/475569?text=${encodeURIComponent(selectedNode.name + '\n' + selectedNode.code)}\n\n[ OFFICIAL DOCUMENT ]`} 
                     alt="Document Preview" 
                     className="max-h-[80vh] object-contain p-8 border border-slate-200 bg-white"
                   />
                   {/* Stamp Overlay Mock */}
                   {(selectedNode.status === '已验真' || selectedNode.status === '已过账') && (
                      <div className="absolute bottom-10 right-10 w-24 h-24 border-4 border-rose-500 rounded-full flex items-center justify-center opacity-60 -rotate-12 pointer-events-none">
                         <span className="text-rose-500 font-bold text-lg uppercase tracking-widest">已审核</span>
                      </div>
                   )}
               </div>
            </div>

            {/* Viewer Toolbar */}
            <div className="h-20 flex items-center justify-center gap-4 shrink-0 pb-4">
               <div className="bg-slate-800 border border-slate-700 rounded-full px-6 py-3 flex items-center gap-6 shadow-xl">
                  <button onClick={handleZoomOut} className="text-slate-400 hover:text-white transition-colors tooltip" title="缩小">
                     <ZoomOut size={20} />
                  </button>
                  <div className="text-sm font-mono text-slate-300 w-12 text-center select-none">{zoomLevel}%</div>
                  <button onClick={handleZoomIn} className="text-slate-400 hover:text-white transition-colors tooltip" title="放大">
                     <ZoomIn size={20} />
                  </button>
                  <div className="w-px h-6 bg-slate-700"></div>
                  <button onClick={handleRotate} className="text-slate-400 hover:text-white transition-colors tooltip" title="旋转">
                     <RotateCw size={20} />
                  </button>
                  <button className="text-slate-400 hover:text-white transition-colors tooltip" title="打印">
                     <Printer size={20} />
                  </button>
                  <button className="text-primary-400 hover:text-primary-300 transition-colors tooltip" title="下载原件">
                     <Download size={20} />
                  </button>
               </div>
            </div>
         </div>
      )}
    </div>
  );
};