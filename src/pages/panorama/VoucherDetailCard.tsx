// Input: React、lucide-react 图标、本地模块 api/autoAssociation、api/archives
// Output: React 组件 VoucherDetailCard
// Pos: src/pages/panorama/VoucherDetailCard.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useEffect, useState } from 'react';
import { ShieldCheck, AlertTriangle, FileText, CreditCard, Calendar, Hash, Info } from 'lucide-react';
import { autoAssociationApi, ComplianceStatus } from '../../api/autoAssociation';
import { archivesApi, Archive } from '../../api/archives';
import { originalVoucherApi, OriginalVoucher } from '../../api/originalVoucher';

interface VoucherDetailCardProps {
    voucherId: string;
    compact?: boolean;
    onFieldHover?: (fieldId: string | null) => void;
    activeField?: string | null;
    sourceType?: 'ARCHIVE' | 'ORIGINAL' | null;
    hideEntries?: boolean;
}

interface VoucherEntry {
    id: string;
    summary: string;
    subjectCode: string;
    subjectName: string;
    debit: string;
    credit: string;
    auxInfo?: string; // 辅助核算信息 (客户/供应商/项目等)
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
            auxInfo: entry.aux_info || entry.auxInfo || undefined, // 辅助核算
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

export const VoucherDetailCard: React.FC<VoucherDetailCardProps> = ({ voucherId, compact = false, onFieldHover, activeField, sourceType, hideEntries = false }) => {
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
            amount: (archive.amount !== null && archive.amount !== undefined) ? `¥ ${Number(archive.amount).toLocaleString()}` : '-',
            creator,
            status: archive.status || '未知',
            entries
        };
    };

    const mapOriginalToVoucher = (ov: OriginalVoucher): VoucherData => {
        return {
            id: ov.id,
            code: ov.voucherNo,
            summary: ov.summary || '原始凭证',
            date: ov.businessDate || (ov as any).createdTime?.split('T')[0] || '',
            amount: (ov.amount !== null && ov.amount !== undefined) ? `¥ ${Number(ov.amount).toLocaleString()}` : '-',
            creator: ov.creator || (ov as any).createdBy || '上传',
            status: ov.archiveStatus || 'DRAFT',
            entries: [] // 原始凭证暂无分录，待 OCR/关联后产生
        };
    };

    useEffect(() => {
        if (!voucherId) return;

        const loadData = async () => {
            setLoading(true);
            setError(null);
            try {
                if (sourceType === 'ORIGINAL') {
                    // 直接获取原始凭证
                    const ov = await originalVoucherApi.getOriginalVoucher(voucherId);
                    if (ov) {
                        setVoucher(mapOriginalToVoucher(ov));
                        // 原始凭证暂无合规性检查
                        setCompliance(null);
                    } else {
                        setError('未找到原始凭证');
                    }
                } else {
                    // 默认或明确为 ARCHIVE，尝试获取归档档案
                    const res = await archivesApi.getArchiveById(voucherId);
                    if (res.code === 200 && res.data) {
                        setVoucher(mapArchiveToVoucher(res.data as Archive));

                        // 获取合规性状态 (仅对正式档案)
                        try {
                            const status = await autoAssociationApi.getComplianceStatus(voucherId);
                            setCompliance(status);
                        } catch {
                            setCompliance(null);
                        }
                    } else if (sourceType === null) {
                        // 如果类型未知且档案接口失败，降级尝试原始凭证
                        const ov = await originalVoucherApi.getOriginalVoucher(voucherId);
                        if (ov) {
                            setVoucher(mapOriginalToVoucher(ov));
                        } else {
                            setError('未找到档案或原始凭证');
                        }
                    } else {
                        setError('未找到该档案');
                    }
                }
            } catch (err: any) {
                console.error("VoucherDetailCard load error:", err);
                setError('加载数据失败');
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, [voucherId, sourceType]);

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

    const amountDisplay = typeof voucher.amount === 'string' && voucher.amount !== '-' ? voucher.amount.replace('¥ ', '') : '--';
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
                        className={`flex items-center gap-2 text-slate-600 cursor-pointer rounded px-1 -ml-1 transition-colors ${activeField === 'total_amount' ? 'bg-yellow-100 ring-2 ring-yellow-400' : 'hover:bg-yellow-50'
                            }`}
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
            {!hideEntries && (
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
                                        <tr
                                            key={entry.id}
                                            className={`transition-colors ${activeField === `entry_${entry.id}` || activeField === `tax_${entry.id}` // Support tax linking
                                                ? 'bg-yellow-50 ring-2 ring-yellow-400 ring-inset z-10 relative'
                                                : 'hover:bg-slate-50'
                                                }`}
                                        >
                                            <td className="p-3 text-slate-600 truncate max-w-[100px]" title={entry.summary}>
                                                {entry.summary}
                                            </td>
                                            <td className="p-3">
                                                <div className="flex flex-col">
                                                    <span className="font-medium text-slate-700">
                                                        {entry.subjectName}
                                                        {entry.auxInfo && (
                                                            <span className="text-primary-600">
                                                                —{entry.auxInfo.replace(/^(客户|供应商|项目|部门|员工)[：:]\s*/i, '')}
                                                            </span>
                                                        )}
                                                    </span>
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
                                <tr className={activeField === 'total_amount' ? 'bg-yellow-100' : ''}>
                                    <td colSpan={2} className="p-3 text-right">合计</td>
                                    <td className="p-3 text-right font-mono">{totalDebit > 0 ? totalDebit.toLocaleString() : amountDisplay}</td>
                                    <td className="p-3 text-right font-mono">{totalCredit > 0 ? totalCredit.toLocaleString() : amountDisplay}</td>
                                </tr>
                            </tfoot>
                        </table>
                    </div>
                </div>
            )}
        </div>
    );
};
