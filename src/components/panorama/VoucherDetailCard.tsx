import React, { useEffect, useState } from 'react';
import { ShieldCheck, AlertTriangle, FileText, CreditCard, Calendar, Hash } from 'lucide-react';
import { autoAssociationApi, ComplianceStatus } from '../../api/autoAssociation';
import { archivesApi, Archive } from '../../api/archives';
import { isDemoMode } from '../../utils/env';

interface VoucherDetailCardProps {
    voucherId: string;
    compact?: boolean;
    onFieldHover?: (fieldId: string | null) => void;
}

interface VoucherData {
    id: string;
    code: string;
    summary: string;
    date: string;
    amount: string;
    creator: string;
    status: string;
    entries: {
        id: string;
        summary: string;
        subjectCode: string;
        subjectName: string;
        debit: string;
        credit: string;
    }[];
}

export const VoucherDetailCard: React.FC<VoucherDetailCardProps> = ({ voucherId, compact = false, onFieldHover }) => {
    const [voucher, setVoucher] = useState<VoucherData | null>(null);
    const [compliance, setCompliance] = useState<ComplianceStatus | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [usingDemo, setUsingDemo] = useState(isDemoMode());

    const mapArchiveToVoucher = (archive: Archive): VoucherData => ({
        id: archive.id,
        code: archive.archiveCode || archive.id,
        summary: archive.title || '无题名',
        date: archive.docDate || archive.createdTime || '',
        amount: archive.amount ? `¥ ${Number(archive.amount).toLocaleString()}` : '-',
        creator: archive.creator || archive.orgName || '未知',
        status: archive.status || '未知',
        entries: []
    });

    useEffect(() => {
        if (!voucherId) return;

        const fetchData = async () => {
            setLoading(true);
            setError(null);
            try {
                if (isDemoMode()) {
                    setUsingDemo(true);
                    setVoucher({
                        id: voucherId,
                        code: voucherId.replace('V-', '记-'),
                        summary: '演示凭证',
                        date: '2025-11-15',
                        amount: '¥ 12,800.00',
                        creator: '演示用户',
                        status: '已记账',
                        entries: [
                            { id: '1', summary: '演示分录', subjectCode: '6602', subjectName: '管理费用-办公费', debit: '12,800.00', credit: '0.00' },
                            { id: '2', summary: '演示分录', subjectCode: '1002', subjectName: '银行存款', debit: '0.00', credit: '12,800.00' },
                        ]
                    });
                } else {
                    const res = await archivesApi.getArchiveById(voucherId);
                    if (res.code === 200 && res.data) {
                        setVoucher(mapArchiveToVoucher(res.data as Archive));
                        setUsingDemo(false);
                    } else {
                        throw new Error('凭证数据加载失败');
                    }
                }

                // Fetch compliance status
                const status = await autoAssociationApi.getComplianceStatus(voucherId);
                setCompliance(status);
                if ((status as any)?.isDemo) {
                    setUsingDemo(true);
                }
            } catch (err: any) {
                console.error('Failed to fetch voucher details', err);
                setError(err?.message || '加载凭证详情失败');
                if (isDemoMode()) {
                    setUsingDemo(true);
                    setVoucher({
                        id: voucherId,
                        code: voucherId,
                        summary: '演示凭证',
                        date: '2025-11-15',
                        amount: '¥ 12,800.00',
                        creator: '演示用户',
                        status: '已记账',
                        entries: []
                    });
                }
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [voucherId]);

    const handleMouseEnter = (fieldId: string) => {
        if (onFieldHover) onFieldHover(fieldId);
    };

    const handleMouseLeave = () => {
        if (onFieldHover) onFieldHover(null);
    };

    if (!voucherId) {
        return (
            <div className="h-full flex items-center justify-center text-slate-400 bg-slate-50/50">
                <div className="text-center">
                    <FileText size={48} className="mx-auto mb-4 opacity-20" />
                    <p>请选择左侧凭证查看详情</p>
                </div>
            </div>
        );
    }

    if (loading) {
        return (
            <div className="h-full flex items-center justify-center text-slate-500">
                <div className="animate-pulse">加载中...</div>
            </div>
        );
    }

    if (!voucher) return null;
    const amountDisplay = typeof voucher.amount === 'string' ? voucher.amount.replace('¥ ', '') : (voucher.amount || '--');

    return (
        <div className="h-full flex flex-col bg-white">
            {/* Header */}
            <div className={`${compact ? 'p-4' : 'p-6'} border-b border-slate-100 relative overflow-hidden`}>
                <div className="absolute top-0 right-0 p-4 flex items-center gap-2">
                    {usingDemo && (
                        <span className="px-2 py-1 bg-amber-50 text-amber-600 text-xs rounded-md font-medium border border-amber-100">演示数据</span>
                    )}
                    {compliance?.passed ? (
                        <div className="flex items-center gap-2 px-3 py-1.5 bg-emerald-50 text-emerald-700 rounded-full border border-emerald-100 shadow-sm">
                            <ShieldCheck size={16} />
                            <span className="text-xs font-bold">四性检测通过</span>
                        </div>
                    ) : (
                        <div className="flex items-center gap-2 px-3 py-1.5 bg-amber-50 text-amber-700 rounded-full border border-amber-100 shadow-sm">
                            <AlertTriangle size={16} />
                            <span className="text-xs font-bold">检测未通过</span>
                        </div>
                    )}
                </div>

                <h2 className="text-xl font-bold text-slate-800 mb-2 flex items-center gap-2">
                    <FileText className="text-primary-600" />
                    {voucher.code}
                </h2>
                <p className="text-slate-500 text-sm mb-2">{voucher.summary}</p>
                {error && (
                    <div className="text-xs text-rose-600 flex items-center gap-1">
                        <AlertTriangle size={12} /> {error}
                    </div>
                )}

                <div className={`grid ${compact ? 'grid-cols-1 gap-2' : 'grid-cols-2 gap-4'} text-sm`}>
                    <div className="flex items-center gap-2 text-slate-600">
                        <Calendar size={14} className="text-slate-400" />
                        <span>日期: {voucher.date}</span>
                    </div>
                    <div
                        className="flex items-center gap-2 text-slate-600 cursor-pointer hover:bg-yellow-50 rounded px-1 -ml-1 transition-colors"
                        onMouseEnter={() => handleMouseEnter('total_amount')}
                        onMouseLeave={handleMouseLeave}
                    >
                        <CreditCard size={14} className="text-slate-400" />
                        <span>金额: <span className="font-mono font-bold text-slate-800">{voucher.amount}</span></span>
                    </div>
                    <div className="flex items-center gap-2 text-slate-600">
                        <Hash size={14} className="text-slate-400" />
                        <span>制单人: {voucher.creator}</span>
                    </div>
                    <div className="flex items-center gap-2 text-slate-600">
                        <div className={`w-2 h-2 rounded-full ${voucher.status === '已记账' ? 'bg-emerald-500' : 'bg-slate-300'}`} />
                        <span>状态: {voucher.status}</span>
                    </div>
                </div>
            </div>

            {/* Entries Table */}
            <div className={`flex-1 overflow-y-auto ${compact ? 'p-4' : 'p-6'} bg-slate-50/30`}>
                <h3 className="font-bold text-slate-700 mb-4 text-sm uppercase tracking-wider">会计分录</h3>
                <div className="bg-white border border-slate-200 rounded-lg overflow-hidden shadow-sm">
                    <table className="w-full text-sm text-left">
                        <thead className="bg-slate-50 text-slate-500 font-medium border-b border-slate-200">
                            <tr>
                                <th className="p-3 w-16">摘要</th>
                                <th className="p-3">科目</th>
                                <th className="p-3 text-right w-24">借方</th>
                                <th className="p-3 text-right w-24">贷方</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {voucher.entries.length === 0 && (
                                <tr>
                                    <td colSpan={4} className="p-4 text-center text-slate-400">暂无分录数据</td>
                                </tr>
                            )}
                            {voucher.entries.map(entry => (
                                <tr key={entry.id} className="hover:bg-slate-50">
                                    <td className="p-3 text-slate-600 truncate max-w-[100px]" title={entry.summary}>{entry.summary}</td>
                                    <td className="p-3">
                                        <div className="flex flex-col">
                                            <span className="font-medium text-slate-700">{entry.subjectName}</span>
                                            <span className="text-xs text-slate-400 font-mono">{entry.subjectCode}</span>
                                        </div>
                                    </td>
                                    <td
                                        className="p-3 text-right font-mono text-slate-700 cursor-pointer hover:bg-yellow-50 transition-colors"
                                        onMouseEnter={() => handleMouseEnter(`debit_${entry.id}`)}
                                        onMouseLeave={handleMouseLeave}
                                    >
                                        {entry.debit}
                                    </td>
                                    <td
                                        className="p-3 text-right font-mono text-slate-700 cursor-pointer hover:bg-yellow-50 transition-colors"
                                        onMouseEnter={() => handleMouseEnter(`credit_${entry.id}`)}
                                        onMouseLeave={handleMouseLeave}
                                    >
                                        {entry.credit}
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                        <tfoot className="bg-slate-50 font-bold text-slate-700 border-t border-slate-200">
                            <tr>
                                <td colSpan={2} className="p-3 text-right">合计</td>
                                <td className="p-3 text-right font-mono">{amountDisplay}</td>
                                <td className="p-3 text-right font-mono">{amountDisplay}</td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
    );
};
