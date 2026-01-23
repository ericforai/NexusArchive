// Input: React, Lucide Icons, originalVoucher API, route path constants, dev debug logging
// Output: OriginalVoucherListView 组件（支持调试日志）
// Pos: src/pages/archives/OriginalVoucherListView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
    Search, Filter, Plus, Trash2,
    Archive, RefreshCw, MoreHorizontal
} from 'lucide-react';
import {
    getOriginalVouchers,
    getOriginalVoucherTypes,
    deleteOriginalVoucher,
    submitForArchive,
    VOUCHER_CATEGORIES,
    ARCHIVE_STATUS
} from '../../api/originalVoucher';
import { CreateOriginalVoucherDialog } from './CreateOriginalVoucherDialog';
import { VoucherPreviewDrawer } from '../../components/pages';
import { toast } from '../../utils/notificationService';
import { ROUTE_PATHS, SUBITEM_TO_PATH } from '../../routes/paths';
import { useFondsStore } from '../../store/useFondsStore';

// 状态徽章组件
const StatusBadge: React.FC<{ status: string }> = ({ status }) => {
    const statusConfig = ARCHIVE_STATUS.find(s => s.code === status) || {
        name: status === 'ENTRY' ? '录入中' : (status === 'PARSED' ? '已解析' : status),
        color: status === 'PARSED' ? 'green' : 'gray'
    };
    const colorClasses: Record<string, string> = {
        gray: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
        yellow: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/50 dark:text-yellow-400',
        green: 'bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400',
        red: 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-400'
    };

    return (
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${colorClasses[statusConfig.color]}`}>
            {statusConfig.name}
        </span>
    );
};

// 类型徽章组件
const TypeBadge: React.FC<{ category: string; typeName?: string }> = ({ category, typeName }) => {
    const categoryInfo = VOUCHER_CATEGORIES.find(c => c.code === category);
    const colorClasses: Record<string, string> = {
        INVOICE: 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-400',
        BANK: 'bg-purple-100 text-purple-700 dark:bg-purple-900/50 dark:text-purple-400',
        DOCUMENT: 'bg-orange-100 text-orange-700 dark:bg-orange-900/50 dark:text-orange-400',
        CONTRACT: 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/50 dark:text-cyan-400',
        OTHER: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300'
    };

    return (
        <span className={`px-2 py-1 rounded text-xs font-medium ${colorClasses[category] || colorClasses.OTHER}`}>
            {typeName || categoryInfo?.name || category}
        </span>
    );
};

// 格式化金额
const formatAmount = (amount?: number, currency: string = 'CNY'): string => {
    if (amount === null || amount === undefined) return '-';
    return new Intl.NumberFormat('zh-CN', {
        style: 'currency',
        currency: currency === 'CNY' ? 'CNY' : currency
    }).format(amount);
};

// 格式化日期
const formatDate = (dateStr?: string): string => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleDateString('zh-CN');
};

interface OriginalVoucherListViewProps {
    title?: string;
    subTitle?: string;
    onNavigate?: (path: string) => void;
    /** 单据池模式：true = 显示待处理状态，false = 显示已归档状态 */
    poolMode?: boolean;
}

// 单据类型名称映射表（与侧边栏菜单保持一致）
const DOC_TYPE_NAME_MAP: Record<string, string> = {
    // 发票类 (INVOICE)
    'INV_PAPER': '纸质发票',
    'INV_VAT_E': '增值税电子发票',
    'INV_DIGITAL': '数电发票',
    'INV_RAIL': '数电票（铁路）',
    'INV_AIR': '数电票（航空）',
    'INV_GOV': '数电票（财政）',
    // 银行类 (BANK)
    'BANK_RECEIPT': '银行回单',
    'BANK_STATEMENT': '银行对账单',
    // 单据类 (DOCUMENT)
    'DOC_PAYMENT': '付款单',
    'DOC_RECEIPT': '收款单',
    'DOC_RECEIPT_VOUCHER': '收款单据（收据）',
    'DOC_PAYROLL': '工资单',
    // 合同类 (CONTRACT)
    'CONTRACT': '合同',
    'AGREEMENT': '协议',
    // 其他类 (OTHER)
    'OTHER': '其他',
};

const ARCHIVE_ORIGINAL_VOUCHER_TYPE_NAME_MAP: Record<string, string> = Object.entries(SUBITEM_TO_PATH)
    .reduce((acc, [label, path]) => {
        if (!path.startsWith(`${ROUTE_PATHS.ARCHIVE_ORIGINAL_VOUCHERS}?`)) {
            return acc;
        }
        const [, queryString = ''] = path.split('?');
        const type = new URLSearchParams(queryString).get('type');
        if (type) {
            acc[type] = label;
        }
        return acc;
    }, {} as Record<string, string>);

export const OriginalVoucherListView: React.FC<OriginalVoucherListViewProps> = ({
    title = '原始凭证',
    subTitle = '原始凭证管理',
    poolMode = false
}) => {
    const queryClient = useQueryClient();
    const { currentFonds } = useFondsStore();

    const location = useLocation();
    const searchParams = new URLSearchParams(location.search);
    const typeFromUrl = searchParams.get('type');
    const debugEnabled = import.meta.env.DEV && searchParams.has('debug');

    const typeNameFromUrl = typeFromUrl
        ? (DOC_TYPE_NAME_MAP[typeFromUrl] || ARCHIVE_ORIGINAL_VOUCHER_TYPE_NAME_MAP[typeFromUrl])
        : undefined;

    // 根据 URL 参数动态计算标题
    const displayTitle = typeNameFromUrl ? typeNameFromUrl : title;
    const displaySubTitle = typeNameFromUrl ? `${typeNameFromUrl}管理` : subTitle;

    // 筛选状态
    const [page, setPage] = useState(1);
    const [limit] = useState(10);
    const [search, setSearch] = useState('');
    // 预览状态
    const [previewVoucherId, setPreviewVoucherId] = useState<string | null>(null);
    const [previewOpen, setPreviewOpen] = useState(false);
    const [categoryFilter, setCategoryFilter] = useState('');
    const typeFilter = typeFromUrl || '';
    const [statusFilter, setStatusFilter] = useState('');
    const [showFilters, setShowFilters] = useState(false);

    // 新建弹窗状态
    const [showCreateDialog, setShowCreateDialog] = useState(false);

    // 选中行
    const [selectedRows, setSelectedRows] = useState<string[]>([]);

    // 悬停行状态
    const [hoveredRowId, setHoveredRowId] = useState<string | null>(null);

    // 防抖预览
    let previewClickTimer: NodeJS.Timeout | null = null;
    const handlePreviewClick = (voucherId: string) => {
        if (previewClickTimer) return;
        previewClickTimer = setTimeout(() => {
            setPreviewVoucherId(voucherId);
            setPreviewOpen(true);
            previewClickTimer = null;
        }, 200);
    };

    // 查询原始凭证列表
    // 根据 poolMode 选择查询池状态 VS 归档状态
    const poolStatusFilter = poolMode ? 'ENTRY,PARSED,PARSE_FAILED' : 'ARCHIVED';

    const { data: vouchersData, isLoading, refetch } = useQuery({
        queryKey: ['originalVouchers', page, limit, search, categoryFilter, typeFilter, statusFilter, poolMode, currentFonds?.fondsCode],
        queryFn: async () => {
            const params = {
                page,
                limit,
                search: search || undefined,
                category: categoryFilter || undefined,
                type: typeFilter || undefined,
                status: statusFilter || undefined,
                fondsCode: currentFonds?.fondsCode, // 新增：传递当前全宗代码
                poolStatus: poolStatusFilter  // 新增：池状态筛选
            };
            if (debugEnabled) {
                console.debug('[OriginalVouchers] query', {
                    pathname: location.pathname,
                    search: location.search,
                    typeFromUrl,
                    typeFilter,
                    categoryFilter,
                    statusFilter,
                    poolStatusFilter,
                    params
                });
            }
            try {
                return await getOriginalVouchers(params);
            } catch (error) {
                if (debugEnabled) {
                    console.error('[OriginalVouchers] query failed', error);
                }
                throw error;
            }
        }
    });

    // 查询类型列表
    const { data: types } = useQuery({
        queryKey: ['originalVoucherTypes'],
        queryFn: getOriginalVoucherTypes
    });

    // 删除 mutation
    const deleteMutation = useMutation({
        mutationFn: deleteOriginalVoucher,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['originalVouchers'] });
            setSelectedRows([]);
        }
    });

    // 提交归档 mutation
    const submitMutation = useMutation({
        mutationFn: submitForArchive,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['originalVouchers'] });
        }
    });

    // 获取类型名称
    const getTypeName = (typeCode: string): string => {
        const type = types?.find(t => t.typeCode === typeCode);
        return type?.typeName || typeCode;
    };

    // 处理全选
    const handleSelectAll = (checked: boolean) => {
        if (checked) {
            setSelectedRows(vouchersData?.records.map(v => v.id) || []);
        } else {
            setSelectedRows([]);
        }
    };

    // 处理单选
    const handleSelectRow = (id: string, checked: boolean) => {
        if (checked) {
            setSelectedRows([...selectedRows, id]);
        } else {
            setSelectedRows(selectedRows.filter(r => r !== id));
        }
    };

    const vouchers = vouchersData?.records || [];
    const totalPages = vouchersData?.pages || 1;

    return (
        <div className="p-6 space-y-6">
            {/* 页头 */}
            <div className="flex justify-between items-center">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900 dark:text-white">{displayTitle}</h1>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">{displaySubTitle}</p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={() => refetch()}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 transition"
                    >
                        <RefreshCw className="w-4 h-4" />
                        刷新
                    </button>
                    <button
                        onClick={() => setShowCreateDialog(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
                    >
                        <Plus className="w-4 h-4" />
                        新建原始凭证
                    </button>
                </div>
            </div>

            {/* 搜索和筛选 */}
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 p-4">
                <div className="flex gap-4 items-center">
                    {/* 搜索框 */}
                    <div className="flex-1 relative">
                        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
                        <input
                            type="text"
                            placeholder="搜索凭证编号、对方单位、摘要..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="w-full pl-10 pr-4 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                        />
                    </div>

                    {/* 类型筛选 */}
                    <select
                        value={categoryFilter}
                        onChange={(e) => setCategoryFilter(e.target.value)}
                        className="px-4 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white"
                    >
                        <option value="">全部类型</option>
                        {VOUCHER_CATEGORIES.map(cat => (
                            <option key={cat.code} value={cat.code}>{cat.name}</option>
                        ))}
                    </select>

                    {/* 状态筛选 */}
                    <select
                        value={statusFilter}
                        onChange={(e) => setStatusFilter(e.target.value)}
                        className="px-4 py-2 border border-gray-200 dark:border-gray-600 rounded-lg bg-gray-50 dark:bg-gray-700 text-gray-900 dark:text-white"
                    >
                        <option value="">全部状态</option>
                        {ARCHIVE_STATUS.map(status => (
                            <option key={status.code} value={status.code}>{status.name}</option>
                        ))}
                    </select>

                    <button
                        onClick={() => setShowFilters(!showFilters)}
                        className="flex items-center gap-2 px-4 py-2 border border-gray-200 dark:border-gray-600 rounded-lg text-gray-600 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
                    >
                        <Filter className="w-4 h-4" />
                        更多筛选
                    </button>
                </div>
            </div>

            {/* 表格 */}
            <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm text-gray-500 dark:text-gray-400">
                        <thead className="bg-gray-50 dark:bg-gray-700/50 text-gray-700 dark:text-gray-200 font-medium">
                            <tr>
                                <th className="p-4 w-10">
                                    <input
                                        type="checkbox"
                                        className="rounded border-gray-300 dark:border-gray-600"
                                        checked={selectedRows.length === vouchers.length && vouchers.length > 0}
                                        onChange={(e) => handleSelectAll(e.target.checked)}
                                    />
                                </th>
                                <th className="p-4">凭证编号</th>
                                <th className="p-4">类型</th>
                                <th className="p-4">业务日期</th>
                                <th className="p-4">摘要</th>
                                <th className="p-4 text-right">金额</th>
                                <th className="p-4">状态</th>
                                <th className="p-4 text-center">操作</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                            {isLoading ? (
                                <tr>
                                    <td colSpan={8} className="p-8 text-center">
                                        加载中...
                                    </td>
                                </tr>
                            ) : vouchers.length === 0 ? (
                                <tr>
                                    <td colSpan={8} className="p-12 text-center text-gray-400">
                                        <div className="flex flex-col items-center">
                                            <Archive className="w-12 h-12 mb-3 text-gray-300" />
                                            暂无原始凭证数据
                                        </div>
                                    </td>
                                </tr>
                            ) : (
                                vouchers.map((voucher) => (
                                    <tr
                                        key={voucher.id}
                                        className={`
                                            cursor-pointer transition-all duration-200
                                            ${hoveredRowId === voucher.id
                                                ? 'bg-blue-50 dark:bg-blue-900/20 border-l-4 border-l-blue-500 dark:border-l-blue-400'
                                                : 'hover:bg-gray-50 dark:hover:bg-gray-700/50'}
                                        `}
                                        onMouseEnter={() => setHoveredRowId(voucher.id)}
                                        onMouseLeave={() => setHoveredRowId(null)}
                                        onClick={() => handlePreviewClick(voucher.id)}
                                        role="button"
                                        tabIndex={0}
                                        aria-label={`预览凭证 ${voucher.voucherNo}`}
                                        onKeyPress={(e) => {
                                            if (e.key === 'Enter' || e.key === ' ') {
                                                e.preventDefault();
                                                handlePreviewClick(voucher.id);
                                            }
                                        }}
                                    >
                                        <td className="p-4" onClick={(e) => e.stopPropagation()}>
                                            <input
                                                type="checkbox"
                                                className="rounded border-gray-300 dark:border-gray-600"
                                                checked={selectedRows.includes(voucher.id)}
                                                onChange={(e) => handleSelectRow(voucher.id, e.target.checked)}
                                            />
                                        </td>
                                        <td className="p-4 font-medium text-blue-600 dark:text-blue-400">
                                            {voucher.voucherNo}
                                        </td>
                                        <td className="p-4">
                                            <TypeBadge category={voucher.voucherCategory} typeName={getTypeName(voucher.voucherType)} />
                                        </td>
                                        <td className="p-4 text-gray-900 dark:text-white">
                                            {formatDate(voucher.businessDate)}
                                        </td>
                                        <td className="p-4 max-w-xs truncate" title={voucher.summary}>
                                            {voucher.summary || '-'}
                                        </td>
                                        <td className="p-4 text-right">
                                            <span className="font-medium text-gray-900 dark:text-white font-mono">
                                                {formatAmount(voucher.amount, voucher.currency)}
                                            </span>
                                        </td>
                                        <td className="p-4">
                                            <StatusBadge status={voucher.archiveStatus} />
                                        </td>
                                        <td className="p-4" onClick={(e) => e.stopPropagation()}>
                                            <div className="flex items-center justify-center">
                                                <button
                                                    onClick={() => handlePreviewClick(voucher.id)}
                                                    className="px-3 py-1.5 text-xs font-medium text-blue-600 hover:text-blue-700 hover:bg-blue-50 dark:text-blue-400 dark:hover:bg-blue-900/30 rounded-md transition-colors"
                                                >
                                                    查看
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>

                {/* 分页 */}
                {totalPages > 1 && (
                    <div className="p-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-2">
                        <button
                            disabled={page === 1}
                            onClick={() => setPage(p => Math.max(1, p - 1))}
                            className="px-3 py-1 border border-gray-200 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                        >
                            上一页
                        </button>
                        <span className="px-3 py-1 text-gray-500 dark:text-gray-400">
                            {page} / {totalPages}
                        </span>
                        <button
                            disabled={page === totalPages}
                            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                            className="px-3 py-1 border border-gray-200 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-700 disabled:opacity-50"
                        >
                            下一页
                        </button>
                    </div>
                )}
            </div>

            {/* Dialog */}
            <CreateOriginalVoucherDialog
                key={`create-voucher-${showCreateDialog ? 'open' : 'closed'}-${typeFilter || 'all'}-${categoryFilter || 'all'}`}
                isOpen={showCreateDialog}
                onClose={() => setShowCreateDialog(false)}
                initialType={typeFilter}
                category={categoryFilter}
            />

            <VoucherPreviewDrawer
                voucherId={previewVoucherId}
                open={previewOpen}
                onClose={() => {
                    setPreviewOpen(false);
                    setPreviewVoucherId(null);
                }}
            />
        </div>
    );
};

export default OriginalVoucherListView;
