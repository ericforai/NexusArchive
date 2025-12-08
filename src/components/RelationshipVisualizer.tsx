import React from 'react';
import { ArrowRight, FileText, Receipt, FileCheck, CreditCard, Building, FileSignature, AlertCircle, CheckCircle2 } from 'lucide-react';
import { GenericRow, LinkedDocument } from '../types';

interface RelationshipVisualizerProps {
    voucher: GenericRow;
    onDocumentClick?: (doc: LinkedDocument) => void;
}

const DocumentCard: React.FC<{ doc: LinkedDocument; position: 'left' | 'right'; onClick?: (doc: LinkedDocument) => void }> = ({ doc, position, onClick }) => {
    const getIcon = (type: string) => {
        switch (type) {
            case 'invoice': return <Receipt size={16} className="text-blue-500" />;
            case 'contract': return <FileSignature size={16} className="text-purple-500" />;
            case 'receipt': return <CreditCard size={16} className="text-emerald-500" />;
            case 'order': return <FileCheck size={16} className="text-amber-500" />;
            default: return <FileText size={16} className="text-slate-500" />;
        }
    };

    return (
        <div
            onClick={() => onClick?.(doc)}
            className={`relative group flex items-center gap-3 p-3 bg-white border border-slate-200 rounded-xl shadow-sm hover:shadow-md hover:border-primary-200 transition-all cursor-pointer ${position === 'left' ? 'flex-row' : 'flex-row-reverse text-right'}`}
        >
            <div className={`p-2 rounded-lg bg-slate-50 group-hover:bg-white transition-colors`}>
                {getIcon(doc.type)}
            </div>
            <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-0.5 justify-between">
                    <span className="text-xs font-bold text-slate-700 truncate">{doc.name}</span>
                    {doc.status && (
                        <span className="text-[10px] px-1.5 py-0.5 bg-emerald-50 text-emerald-600 rounded-full border border-emerald-100 whitespace-nowrap">
                            {doc.status}
                        </span>
                    )}
                </div>
                <div className="text-xs text-slate-500 font-mono truncate">{doc.code}</div>
                {doc.amount && <div className="text-xs font-medium text-slate-800 mt-1">{doc.amount}</div>}
            </div>

            {/* Connecting Line Endpoint */}
            <div className={`absolute top-1/2 w-3 h-3 bg-white border-2 border-primary-400 rounded-full transform -translate-y-1/2 ${position === 'left' ? '-right-1.5' : '-left-1.5'}`}></div>
        </div>
    );
};

export const RelationshipVisualizer: React.FC<RelationshipVisualizerProps> = ({ voucher, onDocumentClick }) => {
    const sourceDocs = (voucher.sourceDocuments as LinkedDocument[]) || [];
    const supportDocs = (voucher.supportingDocuments as LinkedDocument[]) || [];

    return (
        <div className="w-full h-full bg-slate-50/50 p-6 overflow-auto flex flex-col items-center justify-center min-h-[400px]">
            <div className="flex items-center justify-center gap-16 w-full max-w-5xl relative">

                {/* Left Column: Source Documents */}
                <div className="flex flex-col gap-4 w-1/3 relative z-10">
                    <div className="text-center mb-4">
                        <h4 className="text-sm font-bold text-slate-700 flex items-center justify-center gap-2">
                            <Receipt size={16} /> 原始凭证 ({sourceDocs.length})
                        </h4>
                    </div>
                    {sourceDocs.length > 0 ? (
                        sourceDocs.map((doc, idx) => (
                            <DocumentCard key={idx} doc={doc} position="left" onClick={onDocumentClick} />
                        ))
                    ) : (
                        <div className="p-6 border-2 border-dashed border-slate-200 rounded-xl flex flex-col items-center justify-center text-slate-400">
                            <AlertCircle size={24} className="mb-2" />
                            <span className="text-xs">暂无原始凭证</span>
                        </div>
                    )}
                </div>

                {/* Center Column: Voucher */}
                <div className="flex flex-col items-center z-20 relative">
                    <div className="w-64 bg-white rounded-2xl shadow-xl border-2 border-primary-500 p-6 relative">
                        <div className="absolute -top-3 left-1/2 transform -translate-x-1/2 bg-primary-600 text-white text-xs font-bold px-3 py-1 rounded-full shadow-lg">
                            当前凭证
                        </div>

                        <div className="flex flex-col items-center text-center">
                            <div className="w-16 h-16 bg-primary-50 rounded-2xl flex items-center justify-center mb-4 text-primary-600">
                                <Building size={32} />
                            </div>
                            <h3 className="text-lg font-bold text-slate-800 mb-1">{voucher.voucherNo}</h3>
                            <p className="text-sm text-slate-500 mb-4">{voucher.date}</p>

                            <div className="w-full bg-slate-50 rounded-lg p-3 border border-slate-100">
                                <div className="text-xs text-slate-500 mb-1">凭证金额</div>
                                <div className="text-xl font-bold text-slate-800 font-mono">{voucher.amount}</div>
                            </div>

                            <div className="mt-4 flex items-center gap-2 text-xs text-emerald-600 font-medium bg-emerald-50 px-3 py-1.5 rounded-full border border-emerald-100">
                                <CheckCircle2 size={12} /> 状态: {voucher.status}
                            </div>
                        </div>

                        {/* Connecting Lines (Visual Only - CSS Handles positioning) */}
                        <div className="absolute top-1/2 -left-8 w-8 h-0.5 bg-primary-200"></div>
                        <div className="absolute top-1/2 -right-8 w-8 h-0.5 bg-primary-200"></div>
                    </div>
                </div>

                {/* Right Column: Supporting Documents */}
                <div className="flex flex-col gap-4 w-1/3 relative z-10">
                    <div className="text-center mb-4">
                        <h4 className="text-sm font-bold text-slate-700 flex items-center justify-center gap-2">
                            <FileSignature size={16} /> 依据文件 ({supportDocs.length})
                        </h4>
                    </div>
                    {supportDocs.length > 0 ? (
                        supportDocs.map((doc, idx) => (
                            <DocumentCard key={idx} doc={doc} position="right" onClick={onDocumentClick} />
                        ))
                    ) : (
                        <div className="p-6 border-2 border-dashed border-slate-200 rounded-xl flex flex-col items-center justify-center text-slate-400">
                            <AlertCircle size={24} className="mb-2" />
                            <span className="text-xs">暂无依据文件</span>
                        </div>
                    )}
                </div>

                {/* Background Connecting Lines Layer */}
                <div className="absolute inset-0 z-0 pointer-events-none">
                    {/* This would require SVG for perfect lines, but for now we rely on the layout structure */}
                    <svg className="w-full h-full absolute top-0 left-0 opacity-20">
                        {/* Lines can be drawn here if exact coordinates are known, 
                 but for a responsive flex layout, simple CSS pseudo-elements on cards are often enough or we use a dedicated library like react-flow.
                 For this "Visualizer", the flex gap and pseudo-elements on cards (the dots) create the illusion.
             */}
                    </svg>
                </div>

            </div>
        </div>
    );
};
