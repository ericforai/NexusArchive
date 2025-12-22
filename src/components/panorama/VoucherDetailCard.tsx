// Input: React、lucide-react 图标、本地模块 api/autoAssociation、api/archives
// Output: React 组件 VoucherDetailCard
// Pos: 归档全景子组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { ShieldCheck, AlertTriangle, FileText, CreditCard, Calendar, Hash, Info } from 'lucide-react';
import { autoAssociationApi, ComplianceStatus } from '../../api/autoAssociation';
import { archivesApi, Archive } from '../../api/archives';

interface VoucherDetailCardProps {
    voucherId: string;
    compact?: boolean;
    onFieldHover?: (fieldId: string | null) => void;
}

interface VoucherEntry {
    id: string;
    summary: string;
    subjectCode: string;
    subjectName: string;
    debit: string;
    credit: string;
}

interface VoucherData {
    id: string;
    code: string;
    summary: string;
    date: string;
    amount: string;
    creator: string;
    status: string;
    entries: VoucherEntry[];
}

// 解析 YonSuite 凭证分录数据
const parseCustomMetadata = (metadata: string | undefined): VoucherEntry[] => {
    if (!metadata) return [];

    try {
        const entries = JSON.parse(metadata);
        if (!Array.isArray(entries)) return [];

        return entries.map((entry: any, index: number) => ({
            id: entry.id || String(index + 1),
            summary: entry.description || entry.summary || '无摘要',
            subjectCode: entry.accsubject?.code || entry.subjectCode || '',
            subjectName: entry.accsubject?.name || entry.subjectName || '未知科目',
            debit: entry.debit_org > 0 ? entry.debit_org.toLocaleString() : '-',
            credit: entry.credit_org > 0 ? entry.credit_org.toLocaleString() : '-',
        }));
    } catch (e) {
        console.warn('Failed to parse custom_metadata', e);
        return [];
    }
};

// 从分录中提取摘要作为凭证标题
const extractSummaryFromEntries = (entries: VoucherEntry[], fallbackTitle: string): string => {
    // 如果标题不是哈希值，直接使用
    if (fallbackTitle && !isHash(fallbackTitle)) {
        return fallbackTitle;
    }

    // 从第一条分录摘要中提取
    const firstEntry = entries[0];
    if (firstEntry && firstEntry.summary && firstEntry.summary !== '无摘要') {
        return firstEntry.summary;
    }

    return '会计凭证';
};

// 判断字符串是否为哈希值（长度超过 32 且仅包含十六进制字符）
const isHash = (str: string): boolean => {
    if (!str || str.length < 32) return false;
    return /^[a-f0-9]+$/i.test(str);
};

// 格式化制单人显示
const formatCreator = (creator: string | undefined, orgName: string | undefined): string => {
    if (creator && !isHash(creator)) {
        return creator;
    }
    if (orgName && !isHash(orgName)) {
        return orgName;
    }
    return '系统同步';
};

// 格式化状态显示
const formatStatus = (status: string): { label: string; color: string } => {
    const statusMap: Record<string, { label: string; color: string }> = {
        'archived': { label: '已归档', color: 'bg-emerald-500' },
        'pending': { label: '待处理', color: 'bg-amber-500' },
        'rejected': { label: '已驳回', color: 'bg-red-500' },
        'draft': { label: '草稿', color: 'bg-slate-400' },
    };
    return statusMap[status?.toLowerCase()] || { label: status || '未知', color: 'bg-slate-300' };
};

export const VoucherDetailCard: React.FC<VoucherDetailCardProps> = ({ voucherId, compact = false, onFieldHover }) => {
    const [voucher, setVoucher] = useState<VoucherData | null>(null);
    const [compliance, setCompliance] = useState<ComplianceStatus | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const mapArchiveToVoucher = (archive: Archive): VoucherData => {
        const entries = parseCustomMetadata(archive.customMetadata);
        const summary = extractSummaryFromEntries(entries, archive.title || '');
        const creator = formatCreator(archive.creator, archive.orgName);

        return {
            id: archive.id,
            code: archive.archiveCode || archive.id,
            summary,
            date: archive.docDate || archive.createdTime || '',
            amount: archive.amount ? `¥ ${Number(archive.amount).toLocaleString()}` : '-',
            creator,
            status: archive.status || '未知',
            entries
        };
    };

    useEffect(() => {
        if (!voucherId) return;

        const fetchData = async () => {
            setLoading(true);
            setError(null);
            try {
                const res = await archivesApi.getArchiveById(voucherId);
                if (res.code === 200 && res.data) {
                    setVoucher(mapArchiveToVoucher(res.data as Archive));
                } else {
                    setError('未找到凭证数据');
                    setVoucher(null);
                }

                // Fetch compliance status
                try {
                    const status = await autoAssociationApi.getComplianceStatus(voucherId);
                    setCompliance(status);
                } catch {
                    // 四性检测接口失败时设置默认状态
                    setCompliance(null);
                }
            } catch (err: any) {
                console.warn('Failed to fetch voucher details', err);
                setError('加载凭证数据失败');
                setVoucher(null);
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

    if (error || !voucher) {
        return (
            <div className="h-full flex items-center justify-center text-slate-400 bg-slate-50/50">
                <div className="text-center">
                    <AlertTriangle size={48} className="mx-auto mb-4 text-amber-400" />
                    <p>{error || '暂无数据'}</p>
                </div>
            </div>
        );
    }

    const amountDisplay = typeof voucher.amount === 'string' ? voucher.amount.replace('¥ ', '') : (voucher.amount || '--');
    const statusInfo = formatStatus(voucher.status);

    // 计算合计借方/贷方
    const totalDebit = voucher.entries.reduce((sum, e) => {
        const val = parseFloat(e.debit.replace(/,/g, '')) || 0;
        return sum + val;
    }, 0);
    const totalCredit = voucher.entries.reduce((sum, e) => {
        const val = parseFloat(e.credit.replace(/,/g, '')) || 0;
        return sum + val;
    }, 0);

    return (
        <div className="h-full flex flex-col bg-white">
            {/* Header */}
            <div className={`${compact ? 'p-4' : 'p-6'} border-b border-slate-100 relative overflow-hidden`}>
                {/* 四性检测状态 */}
                <div className="absolute top-0 right-0 p-4">
                    {compliance === null ? (
                        <div className="flex items-center gap-2 px-3 py-1.5 bg-slate-50 text-slate-500 rounded-full border border-slate-200 shadow-sm">
                            <Info size={16} />
                            <span className="text-xs font-medium">待检测</span>
                        </div>
                    ) : compliance.passed ? (
                        <div className="flex items-center gap-2 px-3 py-1.5 bg-emerald-50 text-emerald-700 rounded-full border border-emerald-100 shadow-sm">
                            <ShieldCheck size={16} />
                            <span className="text-xs font-bold">四性检测通过</span>
                        </div>
                    ) : (
                        <div className="group relative">
                            <div className="flex items-center gap-2 px-3 py-1.5 bg-amber-50 text-amber-700 rounded-full border border-amber-100 shadow-sm cursor-help">
                                <AlertTriangle size={16} />
                                <span className="text-xs font-bold">四性检测未通过</span>
                            </div>
                            {/* 悬浮显示检测详情 */}
                            <div className="absolute right-0 top-full mt-2 w-64 p-3 bg-white border border-slate-200 rounded-lg shadow-lg opacity-0 group-hover:opacity-100 transition-opacity z-10">
                                <p className="text-xs font-bold text-slate-700 mb-2">未通过项目：</p>
                                <ul className="text-xs text-slate-600 space-y-1">
                                    {compliance.details?.authenticity === false && <li>• 真实性检测：哈希值不匹配</li>}
                                    {compliance.details?.integrity === false && <li>• 完整性检测：元数据不完整</li>}
                                    {compliance.details?.usability === false && <li>• 可用性检测：文件格式异常</li>}
                                    {compliance.details?.safety === false && <li>• 安全性检测：存在风险</li>}
                                    {(!compliance.details || (compliance.details.authenticity !== false && compliance.details.integrity !== false && compliance.details.usability !== false && compliance.details.safety !== false)) && (
                                        <li className="text-slate-400">暂无详细信息</li>
                                    )}
                                </ul>
                            </div>
                        </div>
                    )}
                </div>

                <h2 className="text-xl font-bold text-slate-800 mb-2 flex items-center gap-2 pr-32">
                    <FileText className="text-primary-600 flex-shrink-0" />
                    <span className="truncate">{voucher.code}</span>
                </h2>
                <p className="text-slate-600 text-sm mb-3 font-medium">{voucher.summary}</p>

                <div className={`grid ${compact ? 'grid-cols-1 gap-2' : 'grid-cols-2 gap-4'} text-sm`}>
                    <div className="flex items-center gap-2 text-slate-600">
                        <Calendar size={14} className="text-slate-400" />
                        <span>日期: {voucher.date || '-'}</span>
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
                        <div className={`w-2 h-2 rounded-full ${statusInfo.color}`} />
                        <span>状态: {statusInfo.label}</span>
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
                                <th className="p-3 w-28">摘要</th>
                                <th className="p-3">科目</th>
                                <th className="p-3 text-right w-24">借方</th>
                                <th className="p-3 text-right w-24">贷方</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100">
                            {voucher.entries.length === 0 ? (
                                <tr>
                                    <td colSpan={4} className="p-4 text-center text-slate-400">
                                        暂无分录数据（该凭证可能缺少元数据）
                                    </td>
                                </tr>
                            ) : (
                                voucher.entries.map(entry => (
                                    <tr key={entry.id} className="hover:bg-slate-50">
                                        <td className="p-3 text-slate-600 truncate max-w-[100px]" title={entry.summary}>
                                            {entry.summary}
                                        </td>
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
                                ))
                            )}
                        </tbody>
                        <tfoot className="bg-slate-50 font-bold text-slate-700 border-t border-slate-200">
                            <tr>
                                <td colSpan={2} className="p-3 text-right">合计</td>
                                <td className="p-3 text-right font-mono">{totalDebit > 0 ? totalDebit.toLocaleString() : amountDisplay}</td>
                                <td className="p-3 text-right font-mono">{totalCredit > 0 ? totalCredit.toLocaleString() : amountDisplay}</td>
                            </tr>
                        </tfoot>
                    </table>
                </div>
            </div>
        </div>
    );
};
