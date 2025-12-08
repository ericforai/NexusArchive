import React, { useEffect, useState, useRef } from 'react';
import { FileText, Image as ImageIcon, Paperclip, ExternalLink, Download, Eye, ZoomIn, ZoomOut, RotateCcw } from 'lucide-react';
import { autoAssociationApi, LinkedFile } from '../../api/autoAssociation';

interface EvidencePreviewProps {
    voucherId: string;
    highlightField?: string | null;
}

export const EvidencePreview: React.FC<EvidencePreviewProps> = ({ voucherId, highlightField }) => {
    const [files, setFiles] = useState<LinkedFile[]>([]);
    const [activeTab, setActiveTab] = useState<'invoice' | 'contract' | 'bank_slip' | 'other'>('invoice');
    const [selectedFile, setSelectedFile] = useState<LinkedFile | null>(null);
    const [loading, setLoading] = useState(false);
    const [usingDemo, setUsingDemo] = useState(false);

    // Zoom & Pan State
    const [scale, setScale] = useState(1);
    const [position, setPosition] = useState({ x: 0, y: 0 });
    const [isDragging, setIsDragging] = useState(false);
    const [dragStart, setDragStart] = useState({ x: 0, y: 0 });
    const containerRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!voucherId) return;

        const fetchFiles = async () => {
            setLoading(true);
            try {
                const { files: linkedFiles, isDemo } = await autoAssociationApi.getLinkedFiles(voucherId);
                setUsingDemo(Boolean(isDemo));
                setFiles(linkedFiles);
                // Auto-select first file of active tab if available
                const firstFile = linkedFiles.find(f => f.type === activeTab);
                if (firstFile) setSelectedFile(firstFile);
                else setSelectedFile(linkedFiles[0] || null);
            } catch (error) {
                console.error('Failed to fetch linked files', error);
                setFiles([]);
                setSelectedFile(null);
            } finally {
                setLoading(false);
            }
        };

        fetchFiles();
    }, [voucherId]);

    // Update selected file when tab changes
    useEffect(() => {
        const firstFile = files.find(f => f.type === activeTab);
        setSelectedFile(firstFile || null);
        // Reset zoom/pan on file change
        resetZoom();
    }, [activeTab, files]);

    const resetZoom = () => {
        setScale(1);
        setPosition({ x: 0, y: 0 });
    };

    const handleWheel = (e: React.WheelEvent) => {
        // Direct zoom without modifier keys for "Map-like" experience
        const delta = e.deltaY > 0 ? 0.9 : 1.1;
        setScale(s => Math.min(Math.max(0.5, s * delta), 4));
    };

    const handleMouseDown = (e: React.MouseEvent) => {
        setIsDragging(true);
        setDragStart({ x: e.clientX - position.x, y: e.clientY - position.y });
    };

    const handleMouseMove = (e: React.MouseEvent) => {
        if (isDragging) {
            setPosition({
                x: e.clientX - dragStart.x,
                y: e.clientY - dragStart.y
            });
        }
    };

    const handleMouseUp = () => {
        setIsDragging(false);
    };

    // Mock OCR Coordinates for "X-Ray" Highlighting
    const getHighlightStyle = (field: string) => {
        if (!highlightField || highlightField !== field) return {};

        // Mock coordinates based on field ID
        // In real app, this comes from OCR result: { x, y, w, h }
        const styles: Record<string, React.CSSProperties> = {
            'total_amount': { top: '82%', left: '75%', width: '15%', height: '4%', border: '2px solid red', backgroundColor: 'rgba(255, 0, 0, 0.1)' },
            'debit_1': { top: '82%', left: '75%', width: '15%', height: '4%', border: '2px solid red', backgroundColor: 'rgba(255, 0, 0, 0.1)' }, // Maps to total amount for demo
            'credit_2': { top: '82%', left: '75%', width: '15%', height: '4%', border: '2px solid red', backgroundColor: 'rgba(255, 0, 0, 0.1)' }, // Maps to total amount for demo
        };

        return styles[field] ? { ...styles[field], position: 'absolute', zIndex: 10, borderRadius: '4px', boxShadow: '0 0 10px rgba(255,0,0,0.5)' } : {};
    };

    const renderPreview = () => {
        if (!selectedFile) {
            return (
                <div className="h-full flex flex-col items-center justify-center text-slate-400 bg-slate-50 rounded-lg border border-dashed border-slate-300 m-4">
                    <Paperclip size={48} className="mb-4 opacity-20" />
                    <p>暂无该类型文件</p>
                </div>
            );
        }

        const renderMockContent = () => {
            // Wrapper for Zoom/Pan
            return (
                <div
                    className="relative origin-center transition-transform duration-75 ease-out"
                    style={{
                        transform: `translate(${position.x}px, ${position.y}px) scale(${scale})`,
                        cursor: isDragging ? 'grabbing' : 'grab'
                    }}
                    onMouseDown={handleMouseDown}
                    onMouseMove={handleMouseMove}
                    onMouseUp={handleMouseUp}
                    onMouseLeave={handleMouseUp}
                >
                    {/* OCR Highlights Layer */}
                    {highlightField && (
                        <div className="absolute inset-0 pointer-events-none z-20">
                            {/* Dynamic Highlight Box */}
                            <div style={getHighlightStyle(highlightField)} className="animate-pulse" />
                        </div>
                    )}

                    {selectedFile.type === 'contract' && (
                        <div className="bg-white p-8 shadow-sm min-h-[800px] w-[595px] mx-auto my-4 text-sm relative select-none">
                            <h1 className="text-xl font-bold text-center mb-8">采购合同</h1>
                            <div className="space-y-4 mb-8">
                                <p><strong>甲方（采购方）：</strong> 纽带科技（上海）有限公司</p>
                                <p><strong>乙方（供货方）：</strong> 阿里云计算有限公司</p>
                                <p><strong>合同编号：</strong> C-202511-002</p>
                            </div>
                            <div className="space-y-4 mb-8 text-slate-600 leading-relaxed">
                                <div>
                                    <h3 className="font-bold text-slate-800 mb-1">第一条 采购标的</h3>
                                    <p>甲方向乙方采购以下产品：高性能计算服务器（型号：ECS-G7），数量：10台，单价：10,500.00元。</p>
                                </div>
                                <div>
                                    <h3 className="font-bold text-slate-800 mb-1">第二条 合同金额</h3>
                                    <p>合同总金额为人民币（大写）：壹拾万零伍仟元整（¥105,000.00）。</p>
                                </div>
                                <div>
                                    <h3 className="font-bold text-slate-800 mb-1">第三条 交付与验收</h3>
                                    <p>乙方应于合同签订后5个工作日内交付货物。甲方应在收到货物后3个工作日内完成验收。</p>
                                </div>
                            </div>
                            <div className="flex justify-between mt-20">
                                <div>
                                    <p>甲方代表（签字）：</p>
                                    <div className="font-serif italic text-xl mt-4">张三</div>
                                </div>
                                <div className="relative">
                                    <p>乙方代表（签字）：</p>
                                    <div className="font-serif italic text-xl mt-4">李四</div>
                                    <div className="absolute top-0 left-4 w-24 h-24 border-2 border-red-500 rounded-full flex items-center justify-center text-red-500 rotate-[-15deg] opacity-80 pointer-events-none">
                                        <span className="text-xs font-bold">合同专用章</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {selectedFile.type === 'invoice' && (
                        // Specific mock for Procurement Invoice
                        selectedFile.name.includes('设备采购发票') ? (
                            <div className="bg-white p-6 shadow-sm w-[600px] mx-auto my-4 text-xs relative select-none">
                                <div className="border border-amber-200 p-4 relative overflow-hidden">
                                    <div className="text-center mb-6">
                                        <h2 className="text-lg font-bold text-amber-600">增值税专用发票</h2>
                                        <div className="w-full h-px bg-amber-600 mt-1"></div>
                                        <div className="w-full h-px bg-amber-600 mt-0.5"></div>
                                    </div>
                                    <div className="flex justify-between mb-4">
                                        <div className="space-y-1">
                                            <p><span className="text-slate-500">购买方名称：</span> 纽带科技（上海）有限公司</p>
                                            <p><span className="text-slate-500">统一社会信用代码：</span> 91310000XXXXXXXXXX</p>
                                        </div>
                                        <div className="space-y-1 text-right">
                                            <p><span className="text-slate-500">发票代码：</span> 3300192130</p>
                                            <p><span className="text-slate-500">发票号码：</span> 12345678</p>
                                            <p><span className="text-slate-500">开票日期：</span> 2025年11月20日</p>
                                        </div>
                                    </div>
                                    <table className="w-full border-collapse border border-amber-200 mb-4">
                                        <thead className="bg-amber-50 text-amber-700">
                                            <tr>
                                                <th className="border border-amber-200 p-1 text-left">货物或应税劳务名称</th>
                                                <th className="border border-amber-200 p-1 text-right">金额</th>
                                                <th className="border border-amber-200 p-1 text-right">税率</th>
                                                <th className="border border-amber-200 p-1 text-right">税额</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td className="border border-amber-200 p-2">信息技术服务*服务器硬件</td>
                                                <td className="border border-amber-200 p-2 text-right">92,920.35</td>
                                                <td className="border border-amber-200 p-2 text-right">13%</td>
                                                <td className="border border-amber-200 p-2 text-right">12,079.65</td>
                                            </tr>
                                        </tbody>
                                    </table>
                                    <div className="flex justify-between items-center border-t border-amber-200 pt-2">
                                        <p>价税合计（大写）：壹拾万零伍仟元整</p>
                                        <p>（小写）¥105,000.00</p>
                                    </div>
                                    <div className="mt-4 flex justify-between text-slate-500">
                                        <p>销售方名称：阿里云计算有限公司</p>
                                        <p>备注：项目编号 ND-2025-HW-001</p>
                                    </div>
                                    <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-32 h-20 border-2 border-red-500 rounded-full flex items-center justify-center text-red-500 rotate-[-15deg] opacity-60 pointer-events-none">
                                        <span className="text-sm font-bold">发票专用章</span>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="bg-white p-6 shadow-sm w-[600px] mx-auto my-4 text-xs relative select-none">
                                <div className="border border-amber-200 p-4 relative overflow-hidden">
                                    <div className="text-center mb-6">
                                        <h2 className="text-lg font-bold text-amber-600">增值税电子普通发票</h2>
                                        <div className="w-full h-px bg-amber-600 mt-1"></div>
                                        <div className="w-full h-px bg-amber-600 mt-0.5"></div>
                                    </div>
                                    <div className="flex justify-between mb-4">
                                        <div className="space-y-1">
                                            <p><span className="text-slate-500">购买方名称：</span> 纽带科技（上海）有限公司</p>
                                            <p><span className="text-slate-500">统一社会信用代码：</span> 91310000XXXXXXXXXX</p>
                                        </div>
                                        <div className="space-y-1 text-right">
                                            <p><span className="text-slate-500">发票代码：</span> 031002000111</p>
                                            <p><span className="text-slate-500">发票号码：</span> 25312000</p>
                                            <p><span className="text-slate-500">开票日期：</span> 2025年11月07日</p>
                                        </div>
                                    </div>
                                    <table className="w-full border-collapse border border-amber-200 mb-4">
                                        <thead className="bg-amber-50 text-amber-700">
                                            <tr>
                                                <th className="border border-amber-200 p-1 text-left">货物或应税劳务名称</th>
                                                <th className="border border-amber-200 p-1 text-right">金额</th>
                                                <th className="border border-amber-200 p-1 text-right">税率</th>
                                                <th className="border border-amber-200 p-1 text-right">税额</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <tr>
                                                <td className="border border-amber-200 p-2">客运服务费*差旅费</td>
                                                <td className="border border-amber-200 p-2 text-right">943.40</td>
                                                <td className="border border-amber-200 p-2 text-right">6%</td>
                                                <td className="border border-amber-200 p-2 text-right">56.60</td>
                                            </tr>
                                        </tbody>
                                    </table>
                                    <div className="flex justify-between items-center border-t border-amber-200 pt-2">
                                        <p>价税合计（大写）：壹仟元整</p>
                                        <p>（小写）¥1,000.00</p>
                                    </div>
                                    <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-32 h-20 border-2 border-red-500 rounded-full flex items-center justify-center text-red-500 rotate-[-15deg] opacity-60 pointer-events-none">
                                        <span className="text-sm font-bold">发票专用章</span>
                                    </div>
                                </div>
                            </div>
                        )
                    )}

                    {selectedFile.type === 'bank_slip' && (
                        // Specific mock for Procurement Bank Slip
                        selectedFile.name.includes('设备款支付') ? (
                            <div className="bg-white p-6 shadow-sm w-[600px] mx-auto my-4 text-sm relative select-none">
                                <div className="border-2 border-slate-800 p-4">
                                    <h2 className="text-lg font-bold text-center mb-6">电子回单</h2>
                                    <div className="grid grid-cols-2 gap-4 mb-4">
                                        <div>
                                            <p className="text-slate-500 text-xs">付款人户名</p>
                                            <p className="font-medium">纽带科技（上海）有限公司</p>
                                            <p className="text-slate-500 text-xs mt-1">付款人账号</p>
                                            <p className="font-mono">6225 **** **** 8888</p>
                                        </div>
                                        <div>
                                            <p className="text-slate-500 text-xs">收款人户名</p>
                                            <p className="font-medium">阿里云计算有限公司</p>
                                            <p className="text-slate-500 text-xs mt-1">收款人账号</p>
                                            <p className="font-mono">5719 **** **** 6688</p>
                                        </div>
                                    </div>
                                    <div className="border-t border-dashed border-slate-300 my-4"></div>
                                    <div className="flex justify-between items-center">
                                        <div>
                                            <p className="text-slate-500 text-xs">交易金额</p>
                                            <p className="text-xl font-bold font-mono">¥105,000.00</p>
                                            <p className="text-slate-500 text-xs mt-1">大写：壹拾万零伍仟元整</p>
                                        </div>
                                        <div>
                                            <p className="text-slate-500 text-xs">交易时间</p>
                                            <p>2025-11-21 14:20:00</p>
                                        </div>
                                    </div>
                                    <div className="mt-4 text-xs text-slate-500">
                                        <p>摘要：付设备采购款 (合同号 C-202511-002)</p>
                                    </div>
                                    <div className="absolute bottom-8 right-8 w-24 h-24 border-2 border-red-500 rounded-full flex items-center justify-center text-red-500 rotate-[-15deg] opacity-80 pointer-events-none">
                                        <span className="text-xs font-bold">业务专用章</span>
                                    </div>
                                </div>
                            </div>
                        ) : (
                            <div className="bg-white p-6 shadow-sm w-[600px] mx-auto my-4 text-sm relative select-none">
                                <div className="border-2 border-slate-800 p-4">
                                    <h2 className="text-lg font-bold text-center mb-6">电子回单</h2>
                                    <div className="grid grid-cols-2 gap-4 mb-4">
                                        <div>
                                            <p className="text-slate-500 text-xs">付款人户名</p>
                                            <p className="font-medium">纽带科技（上海）有限公司</p>
                                            <p className="text-slate-500 text-xs mt-1">付款人账号</p>
                                            <p className="font-mono">6225 **** **** 8888</p>
                                        </div>
                                        <div>
                                            <p className="text-slate-500 text-xs">收款人户名</p>
                                            <p className="font-medium">张三</p>
                                            <p className="text-slate-500 text-xs mt-1">收款人账号</p>
                                            <p className="font-mono">6214 **** **** 1234</p>
                                        </div>
                                    </div>
                                    <div className="border-t border-dashed border-slate-300 my-4"></div>
                                    <div className="flex justify-between items-center">
                                        <div>
                                            <p className="text-slate-500 text-xs">交易金额</p>
                                            <p className="text-xl font-bold font-mono">¥1,000.00</p>
                                            <p className="text-slate-500 text-xs mt-1">大写：壹仟元整</p>
                                        </div>
                                        <div>
                                            <p className="text-slate-500 text-xs">交易时间</p>
                                            <p>2025-11-08 10:30:00</p>
                                        </div>
                                    </div>
                                    <div className="absolute bottom-8 right-8 w-24 h-24 border-2 border-red-500 rounded-full flex items-center justify-center text-red-500 rotate-[-15deg] opacity-80 pointer-events-none">
                                        <span className="text-xs font-bold">业务专用章</span>
                                    </div>
                                </div>
                            </div>
                        )
                    )}

                    {selectedFile.type === 'other' && (
                        // Specific mock for Qualification Certificate
                        selectedFile.name.includes('资质证明') ? (
                            <div className="bg-white p-8 shadow-sm w-[595px] mx-auto my-4 text-sm relative min-h-[842px] border border-slate-200 select-none">
                                <div className="border-4 border-double border-amber-600 h-full p-8 relative">
                                    <h1 className="text-3xl font-bold text-center text-amber-800 mb-12 font-serif">营业执照</h1>
                                    <div className="space-y-6 font-serif text-slate-800">
                                        <p><span className="font-bold text-slate-500 w-32 inline-block">统一社会信用代码：</span> 91330106XXXXXXXXXX</p>
                                        <p><span className="font-bold text-slate-500 w-32 inline-block">名称：</span> 阿里云计算有限公司</p>
                                        <p><span className="font-bold text-slate-500 w-32 inline-block">类型：</span> 有限责任公司（自然人投资或控股）</p>
                                        <p><span className="font-bold text-slate-500 w-32 inline-block">法定代表人：</span> 张勇</p>
                                        <p><span className="font-bold text-slate-500 w-32 inline-block">经营范围：</span> 云计算技术服务；数据处理服务；计算机软件开发...</p>
                                        <p><span className="font-bold text-slate-500 w-32 inline-block">注册资本：</span> 壹拾亿元整</p>
                                        <p><span className="font-bold text-slate-500 w-32 inline-block">成立日期：</span> 2009年09月10日</p>
                                        <p><span className="font-bold text-slate-500 w-32 inline-block">营业期限：</span> 2009年09月10日 至 长期</p>
                                    </div>
                                    <div className="absolute bottom-16 right-16 text-center">
                                        <p className="font-serif font-bold text-amber-800 mb-4">登记机关</p>
                                        <div className="w-32 h-32 border-2 border-red-500 rounded-full flex items-center justify-center text-red-500 rotate-[-15deg] opacity-80 mx-auto">
                                            <span className="text-xs font-bold">杭州市西湖区<br />市场监督管理局</span>
                                        </div>
                                        <p className="text-xs mt-2">2024年01月01日</p>
                                    </div>
                                </div>
                            </div>
                        ) : selectedFile.name.includes('中标通知书') ? (
                            <div className="bg-white p-12 shadow-sm w-[595px] mx-auto my-4 text-sm relative min-h-[842px] select-none">
                                <h1 className="text-2xl font-bold text-center text-red-600 mb-2">中标通知书</h1>
                                <div className="w-full h-0.5 bg-red-600 mb-1"></div>
                                <div className="w-full h-0.5 bg-red-600 mb-8"></div>

                                <div className="space-y-6 leading-relaxed text-slate-800">
                                    <p className="font-bold">阿里云计算有限公司：</p>
                                    <p className="indent-8">
                                        我司（纽带科技（上海）有限公司）组织的 <u>高性能计算服务器采购项目</u>（项目编号：ND-2025-HW-001），经评标委员会评审，确定贵司为中标单位。
                                    </p>
                                    <div className="bg-slate-50 p-6 border border-slate-200 rounded">
                                        <p><strong>中标内容：</strong> 高性能计算服务器（ECS-G7）</p>
                                        <p className="mt-2"><strong>中标金额：</strong> ¥105,000.00（壹拾万零伍仟元整）</p>
                                        <p className="mt-2"><strong>供货周期：</strong> 签订合同后5个工作日</p>
                                    </div>
                                    <p className="indent-8">
                                        请贵司在收到本通知书后3日内，与我司联系签订书面合同。
                                    </p>
                                    <p className="text-right mt-12">特此通知。</p>
                                </div>

                                <div className="absolute bottom-32 right-20 text-center">
                                    <p className="font-bold mb-8">纽带科技（上海）有限公司</p>
                                    <div className="absolute top-0 left-0 w-32 h-32 border-2 border-red-500 rounded-full flex items-center justify-center text-red-500 rotate-[-15deg] opacity-80 pointer-events-none">
                                        <span className="text-xs font-bold">招标专用章</span>
                                    </div>
                                    <p>2025年11月14日</p>
                                </div>
                            </div>
                        ) : (
                            <div className="h-full flex flex-col items-center justify-center bg-slate-100">
                                <div className="w-[500px] h-[700px] bg-white shadow-lg flex flex-col border border-slate-200 relative">
                                    <div className="h-12 bg-slate-50 border-b border-slate-200 flex items-center px-4 justify-between">
                                        <span className="text-sm font-medium text-slate-600">{selectedFile.name}</span>
                                        <div className="flex gap-2">
                                            <div className="w-3 h-3 rounded-full bg-red-400"></div>
                                            <div className="w-3 h-3 rounded-full bg-yellow-400"></div>
                                            <div className="w-3 h-3 rounded-full bg-green-400"></div>
                                        </div>
                                    </div>
                                    <div className="flex-1 p-8 flex flex-col items-center justify-center">
                                        {selectedFile.name.endsWith('.pdf') ? (
                                            <div className="text-center">
                                                <FileText size={64} className="text-red-500 mx-auto mb-4" />
                                                <h3 className="text-lg font-medium text-slate-800 mb-2">PDF 文档预览</h3>
                                                <p className="text-slate-500 text-sm">此区域将集成 PDF 阅读器组件</p>
                                            </div>
                                        ) : (
                                            <div className="text-center">
                                                <ImageIcon size={64} className="text-blue-500 mx-auto mb-4" />
                                                <h3 className="text-lg font-medium text-slate-800 mb-2">图片预览</h3>
                                                <p className="text-slate-500 text-sm">此区域将显示图片内容</p>
                                            </div>
                                        )}
                                    </div>
                                    <div className="absolute bottom-4 right-4 text-xs text-slate-300 font-mono">
                                        FILE_ID: {selectedFile.id}
                                    </div>
                                </div>
                            </div>
                        )
                    )}
                </div>
            );
        };

        return (
            <div className="h-full flex flex-col" ref={containerRef}>
                <div className="flex items-center justify-between p-3 bg-slate-100 border-b border-slate-200">
                    <div className="flex items-center gap-2 truncate">
                        <FileText size={16} className="text-slate-500" />
                        <span className="text-sm font-medium text-slate-700 truncate">{selectedFile.name}</span>
                    </div>
                    <div className="flex items-center gap-2">
                        <div className="flex items-center bg-white rounded-lg border border-slate-200 mr-4">
                            <button onClick={() => setScale(s => Math.max(0.5, s - 0.1))} className="p-1.5 hover:bg-slate-50 text-slate-600" title="缩小">
                                <ZoomOut size={16} />
                            </button>
                            <span className="text-xs w-12 text-center font-mono text-slate-500">{Math.round(scale * 100)}%</span>
                            <button onClick={() => setScale(s => Math.min(4, s + 0.1))} className="p-1.5 hover:bg-slate-50 text-slate-600" title="放大">
                                <ZoomIn size={16} />
                            </button>
                            <div className="w-px h-4 bg-slate-200 mx-1"></div>
                            <button onClick={resetZoom} className="p-1.5 hover:bg-slate-50 text-slate-600" title="重置">
                                <RotateCcw size={16} />
                            </button>
                        </div>
                        <button className="p-1.5 hover:bg-white rounded text-slate-500 hover:text-primary-600 transition-colors" title="全屏查看">
                            <ExternalLink size={16} />
                        </button>
                        <button className="p-1.5 hover:bg-white rounded text-slate-500 hover:text-primary-600 transition-colors" title="下载">
                            <Download size={16} />
                        </button>
                    </div>
                </div>
                <div className="flex-1 bg-slate-200 overflow-hidden relative cursor-grab active:cursor-grabbing" onWheel={handleWheel}>
                    {renderMockContent()}
                </div>
            </div>
        );
    };

    if (!voucherId) {
        return (
            <div className="h-full flex items-center justify-center text-slate-400 bg-slate-50/50 border-l border-slate-200">
                <div className="text-center">
                    <Eye size={48} className="mx-auto mb-4 opacity-20" />
                    <p>选择凭证以查看关联证据</p>
                </div>
            </div>
        );
    }

    return (
        <div className="h-full flex flex-col bg-white border-l border-slate-200">
            {/* Tabs */}
            <div className="flex border-b border-slate-200 bg-slate-50">
                {[
                    { id: 'invoice', label: '原始凭证' },
                    { id: 'bank_slip', label: '银行回单' },
                    { id: 'contract', label: '合同文件' },
                    { id: 'other', label: '附件' }
                ].map(tab => (
                    <button
                        key={tab.id}
                        onClick={() => setActiveTab(tab.id as any)}
                        className={`flex-1 py-3 text-sm font-medium border-b-2 transition-colors ${activeTab === tab.id
                            ? 'border-primary-500 text-primary-700 bg-white'
                            : 'border-transparent text-slate-500 hover:text-slate-700 hover:bg-slate-100'
                            }`}
                    >
                        {tab.label}
                    </button>
                ))}
            </div>
            <div className="flex items-center justify-between px-3 py-2 bg-slate-50 border-b border-slate-200 text-xs text-slate-500">
                <span>关联文件 {files.length} 个</span>
                {usingDemo && <span className="px-2 py-0.5 bg-amber-50 text-amber-600 rounded border border-amber-100">演示预览</span>}
            </div>

            {/* File List (if multiple) */}
            {files.filter(f => f.type === activeTab).length > 1 && (
                <div className="p-2 bg-slate-50 border-b border-slate-200 flex gap-2 overflow-x-auto">
                    {files.filter(f => f.type === activeTab).map(file => (
                        <button
                            key={file.id}
                            onClick={() => setSelectedFile(file)}
                            className={`px-3 py-1.5 rounded text-xs border whitespace-nowrap transition-colors ${selectedFile?.id === file.id
                                ? 'bg-white border-primary-200 text-primary-700 shadow-sm'
                                : 'bg-slate-100 border-transparent text-slate-600 hover:bg-white hover:border-slate-200'
                                }`}
                        >
                            {file.name}
                        </button>
                    ))}
                </div>
            )}

            {/* Preview Area */}
            <div className="flex-1 overflow-hidden">
                {loading ? (
                    <div className="h-full flex items-center justify-center text-slate-500">
                        <div className="animate-pulse">加载关联文件中...</div>
                    </div>
                ) : (
                    renderPreview()
                )}
            </div>
        </div>
    );
};
