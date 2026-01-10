// Input: React、lucide-react 图标
// Output: BorrowStatusTag 组件
// Pos: src/components/borrowing
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import {
    CheckCircle,
    Clock,
    RotateCcw,
    XCircle,
    Ban,
    AlertTriangle,
    Archive
} from 'lucide-react';

export interface BorrowStatusTagProps {
    status: string;
    showComment?: boolean;
    comment?: string;
}

const STATUS_META: Record<
    string,
    { label: string; color: string; icon: React.ReactNode }
> = {
    APPROVED: {
        label: '已通过',
        color: 'bg-emerald-50 text-emerald-700 border-emerald-200',
        icon: <CheckCircle size={12} />
    },
    PENDING: {
        label: '待审批',
        color: 'bg-amber-50 text-amber-700 border-amber-200',
        icon: <Clock size={12} />
    },
    RETURNED: {
        label: '已归还',
        color: 'bg-slate-100 text-slate-700 border-slate-200',
        icon: <RotateCcw size={12} />
    },
    REJECTED: {
        label: '已拒绝',
        color: 'bg-rose-50 text-rose-700 border-rose-200',
        icon: <XCircle size={12} />
    },
    CANCELLED: {
        label: '已取消',
        color: 'bg-slate-100 text-slate-500 border-slate-200',
        icon: <Ban size={12} />
    },
    BORROWED: {
        label: '已借出',
        color: 'bg-blue-50 text-blue-700 border-blue-200',
        icon: <Archive size={12} />
    },
    OVERDUE: {
        label: '已逾期',
        color: 'bg-red-50 text-red-700 border-red-200',
        icon: <AlertTriangle size={12} />
    },
    LOST: {
        label: '已丢失',
        color: 'bg-purple-50 text-purple-700 border-purple-200',
        icon: <XCircle size={12} />
    }
};

/**
 * 借阅状态标签组件
 */
export const BorrowStatusTag: React.FC<BorrowStatusTagProps> = ({
    status,
    showComment = false,
    comment
}) => {
    const meta = STATUS_META[status] || {
        label: status || '未知状态',
        color: 'bg-slate-100 text-slate-600 border-slate-200',
        icon: null
    };

    return (
        <div>
            <span
                className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${meta.color}`}
            >
                {meta.icon}
                {meta.label}
            </span>
            {showComment && comment && (
                <div className="text-xs text-slate-400 mt-1 line-clamp-1">
                    {comment}
                </div>
            )}
        </div>
    );
};

export default BorrowStatusTag;
