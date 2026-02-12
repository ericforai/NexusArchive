import { describe, it, expect, vi, beforeEach, Mock } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { useState } from 'react';
import { useArchiveDataLoader } from '../useArchiveDataLoader';
import type { ControllerDataInternal, ControllerMode, ControllerPage, ControllerQuery } from '../types';
import type { GenericRow } from '../../../../types';
import { archivesApi } from '../../../../api/archives';

vi.mock('../../../../api/archives', () => ({
  archivesApi: {
    getArchives: vi.fn(),
  },
}));

vi.mock('../../../../api/pool', () => ({
  poolApi: {
    getList: vi.fn(),
    getListByStatus: vi.fn(),
  },
}));

vi.mock('../../../../store/useFondsStore', () => ({
  useFondsStore: (selector: any) =>
    selector({
      _hasHydrated: true,
      currentFonds: { fondsNo: 'BR-GROUP', fondsName: '泊冉集团有限公司' },
    }),
}));

function Harness() {
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [rows, setRows] = useState<GenericRow[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [pageInfo, setPageInfo] = useState({ total: 0, page: 1, pageSize: 10 });

  const mode: ControllerMode = {
    routeKey: 'voucher',
    title: '档案管理',
    subTitle: '会计凭证',
    config: {} as any,
    isPoolView: false,
    isLinkingView: false,
    categoryCode: 'AC01',
    defaultStatus: 'archived',
  };

  const query: ControllerQuery = {
    searchTerm,
    setSearchTerm,
    statusFilter: '',
    setStatusFilter: () => undefined,
    orgFilter: '',
    setOrgFilter: () => undefined,
    orgOptions: [],
    subTypeFilter: '',
    setSubTypeFilter: () => undefined,
  };

  const page: ControllerPage = {
    currentPage,
    setCurrentPage,
    pageInfo: { total: pageInfo.total, page: pageInfo.page, pageSize: pageInfo.pageSize },
  };

  const data: ControllerDataInternal = {
    rows,
    isLoading,
    errorMessage,
    pageInfo,
    setRows,
    setIsLoading,
    setErrorMessage,
    setPageInfo,
    setCurrentPage,
  };

  useArchiveDataLoader({
    mode,
    query,
    page,
    isPoolView: false,
    poolStatusFilter: null,
    categoryFilter: null,
    data,
    showToast: () => undefined,
  });

  return (
    <input
      data-testid="search"
      value={searchTerm}
      onChange={(e) => setSearchTerm(e.target.value)}
    />
  );
}

describe('useArchiveDataLoader', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    (archivesApi.getArchives as Mock).mockResolvedValue({
      code: 200,
      data: { records: [], total: 0, size: 10, current: 1 },
    });
  });

  it('在第1页修改搜索词时应重新加载数据', async () => {
    render(<Harness />);

    await waitFor(() => {
      expect(archivesApi.getArchives).toHaveBeenCalledTimes(1);
    });

    fireEvent.change(screen.getByTestId('search'), { target: { value: '000002' } });

    await waitFor(() => {
      expect(archivesApi.getArchives).toHaveBeenCalledTimes(2);
    });
  });
});

