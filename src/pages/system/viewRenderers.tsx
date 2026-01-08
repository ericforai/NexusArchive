// Input: React 组件、ViewState、业务子项
// Output: 视图渲染器函数集合
// Pos: src/pages/system/ 视图渲染逻辑
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { ViewState } from '../../types';
import type { ArchiveRouteMode } from '../../features/archives';

// 页面组件导入
import { Dashboard } from '../../pages/portal/Dashboard';
import { ArchiveListPage } from '../../pages/archives/ArchiveListPage';
import { OCRProcessingView } from '../../pages/pre-archive/OCRProcessingView';
import AbnormalDataView from '../../pages/pre-archive/AbnormalDataView';
import { DestructionView } from '../../pages/operations/DestructionView';
import { ArchiveApprovalView } from '../../pages/operations/ArchiveApprovalView';
import { OpenAppraisalView } from '../../pages/operations/OpenAppraisalView';
import VolumeManagement from '../../pages/operations/VolumeManagement';
import { BorrowingView } from '../../pages/utilization/BorrowingView';
import { ArchivalPanoramaView } from '../../pages/panorama/ArchivalPanoramaView';
import { OnlineReceptionView } from '../../pages/collection/OnlineReceptionView';
import { OriginalVoucherListView } from '../../pages/archives/OriginalVoucherListView';
import { ComplianceReportView } from '../../pages/archives/ComplianceReportView';
import VoucherMatchingView from '../../pages/matching/VoucherMatchingView';
import { StatsView } from '../../pages/stats/StatsView';
import { RelationshipQueryView } from '../../pages/utilization/RelationshipQueryView';

import { DOC_POOL_TYPES, ORIGINAL_VOUCHER_TYPES } from './viewConstants';

// 类型定义
export interface RenderContentParams {
    activeView: ViewState;
    activeSubItem: string;
    activeResourceId: string;
    onNavigate: (view: ViewState, subItem?: string, resourceId?: string) => void;
    navigateSettings: () => void;
}

/**
 * 解析归档路由模式
 */
export const resolveArchiveRouteMode = (activeView: ViewState, activeSubItem: string): ArchiveRouteMode => {
    if (activeView === ViewState.PRE_ARCHIVE) {
        return activeSubItem === '凭证关联' ? 'link' : 'pool';
    }
    if (activeView === ViewState.COLLECTION) {
        return activeSubItem === '扫描集成' ? 'scan' : 'collection';
    }
    if (activeView === ViewState.ACCOUNT_ARCHIVES) {
        if (activeSubItem === '会计凭证') return 'voucher';
        if (activeSubItem === '会计账簿') return 'ledger';
        if (activeSubItem === '财务报告') return 'report';
        if (activeSubItem === '其他会计资料') return 'other';
        return 'view';
    }
    if (activeView === ViewState.ARCHIVE_OPS) {
        return activeSubItem === '档案装盒' ? 'box' : 'view';
    }
    if (activeView === ViewState.ARCHIVE_UTILIZATION || activeView === ViewState.QUERY || activeView === ViewState.BORROWING) {
        return 'query';
    }
    return 'view';
};

/**
 * 渲染预归档视图
 */
const renderPreArchive = (activeSubItem: string, resolveRouteMode: () => ArchiveRouteMode) => {
    if (activeSubItem === 'OCR识别') return <OCRProcessingView />;
    if (activeSubItem === '异常数据') return <AbnormalDataView />;

    // 单据池及其子分类使用 OriginalVoucherListView (Pool Mode)
    if (DOC_POOL_TYPES.includes(activeSubItem as any)) {
        const voucherType = activeSubItem?.includes(':') ? activeSubItem.split(':')[1] : undefined;
        return (
            <OriginalVoucherListView
                title="单据池"
                subTitle={voucherType || '全部单据'}
                poolMode={true}
            />
        );
    }

    return <ArchiveListPage routeConfig={resolveRouteMode()} />;
};

/**
 * 渲染归档收集视图
 */
const renderCollection = (activeSubItem: string, resolveRouteMode: () => ArchiveRouteMode) => {
    if (activeSubItem === '在线接收') return <OnlineReceptionView />;
    if (activeSubItem === '扫描集成') return <OCRProcessingView />;
    return <ArchiveListPage routeConfig={resolveRouteMode()} />;
};

/**
 * 渲染会计档案视图
 */
const renderAccountArchives = (activeSubItem: string, resolveRouteMode: () => ArchiveRouteMode) => {
    // 原始凭证及其子分类使用独立组件
    if (ORIGINAL_VOUCHER_TYPES.includes(activeSubItem as any)) {
        return (
            <OriginalVoucherListView
                title="原始凭证"
                subTitle={activeSubItem === '原始凭证' ? '原始凭证管理' : activeSubItem}
            />
        );
    }

    // 记账凭证和其他子菜单使用通用组件
    return <ArchiveListPage routeConfig={resolveRouteMode()} />;
};

/**
 * 渲染档案操作视图
 */
const renderArchiveOps = (activeSubItem: string, resolveRouteMode: () => ArchiveRouteMode) => {
    if (activeSubItem === '归档审批') return <ArchiveApprovalView />;
    if (activeSubItem === '档案组卷') return <VolumeManagement />;
    if (activeSubItem === '开放鉴定') return <OpenAppraisalView />;
    if (activeSubItem === '销毁鉴定') return <DestructionView />;
    if (activeSubItem === '档案装盒') return <ArchiveListPage routeConfig={resolveRouteMode()} />;
    return <ArchiveListPage routeConfig={resolveRouteMode()} />;
};

/**
 * 渲染档案利用视图
 */
const renderArchiveUtilization = (activeSubItem: string, resolveRouteMode: () => ArchiveRouteMode) => {
    if (activeSubItem === '穿透联查') return <RelationshipQueryView />;
    if (activeSubItem === '借阅申请') return <BorrowingView />;
    return <ArchiveListPage routeConfig={resolveRouteMode()} />;
};

/**
 * 主内容渲染器
 * 根据当前视图和子项渲染对应的页面组件
 */
export const renderContent = (params: RenderContentParams): React.ReactNode => {
    const { activeView, activeSubItem, activeResourceId, onNavigate, navigateSettings } = params;

    // 创建路由模式解析函数
    const resolveRoute = () => resolveArchiveRouteMode(activeView, activeSubItem);

    switch (activeView) {
        case ViewState.PORTAL:
            return <Dashboard onNavigate={onNavigate} />;

        case ViewState.PANORAMA:
            return <ArchivalPanoramaView initialVoucherId={activeResourceId} />;

        case ViewState.PRE_ARCHIVE:
            return renderPreArchive(activeSubItem, resolveRoute);

        case ViewState.COLLECTION:
            return renderCollection(activeSubItem, resolveRoute);

        case ViewState.ACCOUNT_ARCHIVES:
            return renderAccountArchives(activeSubItem, resolveRoute);

        case ViewState.ARCHIVE_OPS:
            return renderArchiveOps(activeSubItem, resolveRoute);

        case ViewState.ARCHIVE_MGMT:
            return <ArchiveListPage routeConfig={resolveRoute()} />;

        case ViewState.ARCHIVE_UTILIZATION:
            return renderArchiveUtilization(activeSubItem, resolveRoute);

        case ViewState.DESTRUCTION:
            return <DestructionView />;

        case ViewState.STATS:
            return <StatsView drillDown={activeSubItem} onNavigate={onNavigate} />;

        case ViewState.SETTINGS:
            // 使用新的路由架构，Settings 页面通过 routes/index.tsx 渲染
            navigateSettings();
            return null;

        case ViewState.COMPLIANCE_REPORT:
            return (
                <ComplianceReportView
                    archiveId={activeResourceId}
                    onBack={() => onNavigate(ViewState.ARCHIVE_MGMT, activeSubItem)}
                />
            );

        case ViewState.MATCHING:
            return <VoucherMatchingView voucherId={activeResourceId || ''} voucherNo={activeSubItem} />;

        default:
            return <Dashboard onNavigate={onNavigate} />;
    }
};
