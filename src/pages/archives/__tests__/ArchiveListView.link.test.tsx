import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ArchiveListView } from '../ArchiveListView';
import type { ArchiveListController } from '@/features/archives';
import type { GenericRow } from '@/types';

const LINK_CONFIG = {
  columns: [
    { key: 'voucherNo', header: '记账凭证号', type: 'text' },
    { key: 'amount', header: '金额', type: 'money' },
    { key: 'date', header: '业务日期', type: 'date' },
    { key: 'invoiceCount', header: '关联发票数', type: 'text' },
    { key: 'contractNo', header: '关联合同', type: 'text' },
    { key: 'matchScore', header: '匹配度', type: 'progress' },
    { key: 'autoLink', header: '关联方式', type: 'text' },
    { key: 'status', header: '关联状态', type: 'status' },
  ],
  data: [],
};

function createController(rows: GenericRow[]): ArchiveListController {
  return {
    mode: {
      routeKey: 'link',
      title: '预归档库',
      subTitle: '凭证关联',
      config: LINK_CONFIG as any,
      isPoolView: false,
      isLinkingView: true,
      categoryCode: 'AC01',
      defaultStatus: 'draft,MATCH_PENDING,MATCHED',
    },
    query: {
      searchTerm: '',
      setSearchTerm: vi.fn(),
      statusFilter: '',
      setStatusFilter: vi.fn(),
      orgFilter: '',
      setOrgFilter: vi.fn(),
      orgOptions: [],
      subTypeFilter: '',
      setSubTypeFilter: vi.fn(),
    },
    page: {
      currentPage: 1,
      pageInfo: { total: rows.length, page: 1, pageSize: 10 },
      setCurrentPage: vi.fn(),
    },
    data: {
      rows,
      isLoading: false,
      errorMessage: null,
      pageInfo: { total: rows.length, page: 1, pageSize: 10 },
      setRows: vi.fn(),
      setIsLoading: vi.fn(),
      setErrorMessage: vi.fn(),
      setPageInfo: vi.fn(),
      setCurrentPage: vi.fn(),
    },
    selection: {
      selectedIds: [],
      toggle: vi.fn(),
      toggleAll: vi.fn(),
      clear: vi.fn(),
      allSelected: false,
    },
    pool: {
      statusFilter: null,
      setStatusFilter: vi.fn(),
      statusStats: {},
      refreshStats: vi.fn(async () => {}),
    },
    ui: {
      toast: { visible: false, message: '', type: 'success' },
      showToast: vi.fn(),
    },
    actions: {
      reload: vi.fn(async () => {}),
      exportCsv: vi.fn(),
    },
  };
}

const baseActions = {
  executeArchiving: vi.fn(),
  isArchiving: false,
  handleBatchDelete: vi.fn(),
  handleAipExport: vi.fn(),
  isExporting: false,
};

describe('ArchiveListView link route regression', () => {
  it('link 页面遇到脏数据时不应崩溃，并显示兜底值', () => {
    const controller = createController([
      {
        id: 'row-1',
        voucherNo: '记-1001',
        amount: 123.45,
        date: '2026-03-03',
        invoiceCount: 2,
        contractNo: { bad: 'object' } as any,
        matchScore: 85,
        autoLink: 'AUTO',
        status: '待匹配',
      },
    ]);

    render(
      <MemoryRouter>
        <ArchiveListView
          routeConfig="link"
          controller={controller}
          actions={baseActions as any}
        />
      </MemoryRouter>
    );

    expect(screen.getByText('预归档库')).toBeInTheDocument();
    expect(screen.getByText('凭证关联')).toBeInTheDocument();
    expect(screen.getByText('85%')).toBeInTheDocument();
    expect(screen.getByText('-', { selector: 'span' })).toBeInTheDocument();
  });
});
