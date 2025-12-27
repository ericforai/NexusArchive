// Input: VoucherMatchingView 组件、URL 参数
// Output: Page 层容器，连接路由与 View 组件
// Pos: src/pages/matching/VoucherMatchingPage.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Voucher Matching Page Container
 * 
 * 职责：
 * 1. 封装懒加载和 Suspense
 * 2. 从 URL 参数获取 voucherId
 * 3. 解耦 routes/index.tsx 与业务组件
 * 
 * 注意：VoucherMatchingView 需要 voucherId 参数。
 * 如果从列表页跳转，应携带 ?id=xxx 参数。
 */
import React, { lazy, Suspense } from 'react';
import { useSearchParams, useParams } from 'react-router-dom';

const VoucherMatchingView = lazy(() => import('./VoucherMatchingView'));

const LoadingFallback = () => (
    <div className="flex items-center justify-center h-full text-slate-600">
        <span className="animate-pulse text-sm">加载中...</span>
    </div>
);

/**
 * 空状态提示组件
 */
const EmptyState = () => (
    <div className="flex flex-col items-center justify-center h-full text-slate-500 py-20">
        <svg className="w-16 h-16 mb-4 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <h3 className="text-lg font-medium text-slate-700 mb-2">请选择凭证</h3>
        <p className="text-sm text-slate-500">请从凭证列表中选择一个凭证以查看匹配详情</p>
    </div>
);

interface VoucherMatchingPageProps {
    voucherId?: string;
}

export function VoucherMatchingPage({ voucherId: propVoucherId }: VoucherMatchingPageProps) {
    // 支持从 URL 参数或 props 获取 voucherId
    const [searchParams] = useSearchParams();
    const { id: paramId } = useParams<{ id: string }>();

    const voucherId = propVoucherId || paramId || searchParams.get('id') || undefined;

    if (!voucherId) {
        return <EmptyState />;
    }

    return (
        <Suspense fallback={<LoadingFallback />}>
            <VoucherMatchingView voucherId={voucherId} />
        </Suspense>
    );
}

export default VoucherMatchingPage;
