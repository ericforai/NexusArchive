import React, { useState, useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { TopBar } from './components/TopBar';
import { Dashboard } from './components/Dashboard';
import { LoginView } from './components/LoginView';
import { AdminLayout } from './components/admin/AdminLayout';
import { ArchiveListView } from './components/ArchiveListView';
import { StatsView } from './components/StatsView';
import { SettingsView } from './components/SettingsView';
import { RelationshipQueryView } from './components/RelationshipQueryView';
import { OCRProcessingView } from './components/OCRProcessingView';
import { WarehouseView } from './components/WarehouseView';
import { DestructionView } from './components/DestructionView';
import { ArchiveApprovalView } from './components/ArchiveApprovalView';
import { RelationshipView } from './components/RelationshipView';
import { OpenAppraisalView } from './components/OpenAppraisalView';
import { OpenInventoryView } from './components/OpenInventoryView';
import { DestructionRepositoryView } from './components/DestructionRepositoryView';
import { BorrowingView } from './components/BorrowingView';
import { ViewState, ModuleConfig } from './types';
import {
  PRE_ARCHIVE_POOL_CONFIG,
  PRE_ARCHIVE_LINK_CONFIG,
  COLLECTION_ONLINE_CONFIG,
  COLLECTION_SCAN_CONFIG,
  COLLECTION_CONFIG,
  ARCHIVE_VIEW_CONFIG,
  ARCHIVE_BOX_CONFIG,
  ACCOUNTING_VOUCHER_CONFIG,
  ACCOUNTING_LEDGER_CONFIG,
  FINANCIAL_REPORT_CONFIG,
  OTHER_ACCOUNTING_MATERIALS_CONFIG,
  BORROWING_CONFIG,
  QUERY_CONFIG,
  GENERIC_CONFIG,
  WAREHOUSE_RACK_CONFIG,
  WAREHOUSE_ENV_CONFIG
} from './constants';

const App: React.FC = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [activeView, setActiveView] = useState<ViewState>(ViewState.PORTAL);
  const [activeSubItem, setActiveSubItem] = useState<string>('');

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      setIsAuthenticated(true);
    }
  }, []);

  const handleLoginSuccess = (user: any) => {
    setIsAuthenticated(true);
  };

  const handleNavigate = (view: ViewState, subItem: string = '') => {
    setActiveView(view);
    setActiveSubItem(subItem);
  };

  if (!isAuthenticated) {
    return <LoginView onLoginSuccess={handleLoginSuccess} />;
  }

  if (activeView === ViewState.ADMIN) {
    return <AdminLayout onExit={() => setActiveView(ViewState.PORTAL)} />;
  }

  const getModuleConfig = (): ModuleConfig => {
    // Pre-Archive
    if (activeView === ViewState.PRE_ARCHIVE) {
      if (activeSubItem === '凭证关联') return PRE_ARCHIVE_LINK_CONFIG;
      return PRE_ARCHIVE_POOL_CONFIG;
    }

    // Collection
    if (activeView === ViewState.COLLECTION) {
      if (activeSubItem === '在线接收') return COLLECTION_ONLINE_CONFIG;
      if (activeSubItem === '扫描集成') return COLLECTION_SCAN_CONFIG;
      return COLLECTION_CONFIG;
    }

    // Archive Management
    if (activeView === ViewState.ARCHIVE_MGMT) {
      if (activeSubItem === '档案装盒') return ARCHIVE_BOX_CONFIG;
      if (activeSubItem === '会计凭证') return ACCOUNTING_VOUCHER_CONFIG;
      if (activeSubItem === '会计账簿') return ACCOUNTING_LEDGER_CONFIG;
      if (activeSubItem === '财务报告') return FINANCIAL_REPORT_CONFIG;
      if (activeSubItem === '其他会计资料') return OTHER_ACCOUNTING_MATERIALS_CONFIG;
      // Note: '销毁鉴定' uses a custom view, not ArchiveListView
      return ARCHIVE_VIEW_CONFIG;
    }

    // Query
    if (activeView === ViewState.QUERY) {
      return QUERY_CONFIG;
    }

    // Borrowing
    if (activeView === ViewState.BORROWING) {
      return BORROWING_CONFIG;
    }

    // Warehouse fallback (though it uses custom view)
    if (activeView === ViewState.WAREHOUSE) {
      if (activeSubItem === '温湿度监控') return WAREHOUSE_ENV_CONFIG;
      return WAREHOUSE_RACK_CONFIG;
    }

    return GENERIC_CONFIG;
  };

  const renderContent = () => {
    switch (activeView) {
      case ViewState.PORTAL:
        return <Dashboard onNavigate={handleNavigate} />;

      case ViewState.PRE_ARCHIVE:
        if (activeSubItem === 'OCR识别') return <OCRProcessingView />;
        return <ArchiveListView title="预归档库" subTitle={activeSubItem || '电子凭证池'} config={getModuleConfig()} />;

      case ViewState.COLLECTION:
        return <ArchiveListView title="资料收集" subTitle={activeSubItem || '概览'} config={getModuleConfig()} />;

      case ViewState.ARCHIVE_MGMT:
        if (activeSubItem === '归档审批') return <ArchiveApprovalView />;
        if (activeSubItem === '关联查看') return <RelationshipView />;
        if (activeSubItem === '开放鉴定') return <OpenAppraisalView />;
        if (activeSubItem === '开放清册') return <OpenInventoryView />;
        if (activeSubItem === '销毁鉴定') return <DestructionView />;
        if (activeSubItem === '销毁库') return <DestructionRepositoryView />;
        return <ArchiveListView title="档案管理" subTitle={activeSubItem || '归档查看'} config={getModuleConfig()} />;

      case ViewState.QUERY:
        if (activeSubItem === '穿透联查') return <RelationshipQueryView />;
        return <ArchiveListView title="档案查询" subTitle={activeSubItem || '全文检索'} config={getModuleConfig()} />;

      case ViewState.BORROWING:
        return <BorrowingView />;

      case ViewState.WAREHOUSE:
        return <WarehouseView />;

      case ViewState.STATS:
        return <StatsView drillDown={activeSubItem} onNavigate={handleNavigate} />;

      case ViewState.SETTINGS:
        return <SettingsView onNavigate={handleNavigate} />;

      default:
        return <Dashboard onNavigate={handleNavigate} />;
    }
  };

  return (
    <div className="flex h-screen bg-slate-50 font-sans text-slate-900">
      <Sidebar
        activeView={activeView}
        setActiveView={setActiveView}
        activeSubItem={activeSubItem}
        setActiveSubItem={setActiveSubItem}
      />

      <div className="flex-1 flex flex-col min-w-0 overflow-hidden">
        <TopBar />
        <main className="flex-1 overflow-y-auto scroll-smooth p-0">
          {renderContent()}
        </main>
      </div>
    </div>
  );
};

export default App;