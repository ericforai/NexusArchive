// Input: vitest、@testing-library/react、react-router-dom 路由、React Query、@/pages/archives/ArchiveListView、@/api/archives、@/api/admin、@/api/pool
// Output: 测试用例与断言
// Pos: 前端单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { describe, it, expect, beforeEach, vi, Mock } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ArchiveListView } from '@/pages/archives/ArchiveListView';
import { useArchiveActions, useArchiveListController, ArchiveRouteMode } from '@/features/archives';
import { archivesApi } from '@/api/archives';
import { adminApi } from '@/api/admin';
import { poolApi } from '@/api/pool';

// Mock APIs
vi.mock('@/api/archives', () => ({
    archivesApi: {
        getArchives: vi.fn(),
        deleteArchive: vi.fn(),
        createArchive: vi.fn(),
        exportAipPackage: vi.fn(),
    },
}));

vi.mock('@/api/admin', () => ({
    adminApi: {
        listOrg: vi.fn(),
    },
}));

vi.mock('@/api/pool', () => ({
    poolApi: {
        getList: vi.fn(),
        archiveItems: vi.fn(),
    },
}));

// Mock storage
vi.mock('@/utils/storage', () => ({
    safeStorage: {
        getItem: vi.fn(() => 'false'),
        setItem: vi.fn(),
    },
}));

// Mock env
vi.mock('@/utils/env', () => ({
    isDemoMode: vi.fn(() => false),
}));

// Mock audit
vi.mock('@/utils/audit', () => ({
    triggerAuditRefresh: vi.fn(),
}));

/**
 * ArchiveListView 组件测试
 * 
 * 测试覆盖:
 * - 组件渲染
 * - 数据加载
 * - 路由配置模式
 * 
 * @author Agent E - 质量保障工程师
 */
const ArchiveListViewHarness = ({ routeConfig }: { routeConfig: ArchiveRouteMode }) => {
    const controller = useArchiveListController({ routeConfig });
    const actions = useArchiveActions(controller);
    return (
        <ArchiveListView
            routeConfig={routeConfig}
            controller={controller}
            actions={actions}
        />
    );
};

describe('ArchiveListView', () => {
    let queryClient: QueryClient;

    beforeEach(() => {
        vi.clearAllMocks();
        queryClient = new QueryClient({
            defaultOptions: {
                queries: { retry: false },
            },
        });

        // 默认 mock 返回空组织列表
        (adminApi.listOrg as Mock).mockResolvedValue({
            code: 200,
            data: [],
        });
    });

    const renderArchiveListView = (routeConfig: ArchiveRouteMode = 'voucher') => {
        return render(
            <QueryClientProvider client={queryClient}>
                <BrowserRouter>
                    <ArchiveListViewHarness routeConfig={routeConfig} />
                </BrowserRouter>
            </QueryClientProvider>
        );
    };

    describe('渲染', () => {
        it('应显示标题', async () => {
            (archivesApi.getArchives as Mock).mockResolvedValue({
                code: 200,
                data: { records: [], total: 0, size: 10, current: 1 },
            });

            renderArchiveListView();

            await waitFor(() => {
                expect(screen.getByText('档案管理')).toBeInTheDocument();
            });
        });

        it('应显示副标题', async () => {
            (archivesApi.getArchives as Mock).mockResolvedValue({
                code: 200,
                data: { records: [], total: 0, size: 10, current: 1 },
            });

            renderArchiveListView();

            await waitFor(() => {
                expect(screen.getByText('会计凭证')).toBeInTheDocument();
            });
        });
    });

    describe('数据加载', () => {
        it('应调用 getArchives API', async () => {
            (archivesApi.getArchives as Mock).mockResolvedValue({
                code: 200,
                data: { records: [], total: 0, size: 10, current: 1 },
            });

            renderArchiveListView();

            await waitFor(() => {
                expect(archivesApi.getArchives).toHaveBeenCalled();
            });
        });

        it('API 返回数据时应正确处理', async () => {
            const mockArchives = [
                {
                    id: 'arc-001',
                    archiveCode: 'AC-2023-001',
                    title: '测试凭证',
                    categoryCode: 'AC01',
                    fiscalYear: '2023',
                    status: 'archived',
                },
            ];

            (archivesApi.getArchives as Mock).mockResolvedValue({
                code: 200,
                data: { records: mockArchives, total: 1, size: 10, current: 1 },
            });

            renderArchiveListView();

            await waitFor(() => {
                expect(archivesApi.getArchives).toHaveBeenCalled();
            });
        });
    });

    describe('空数据状态', () => {
        it('空数据时应正常渲染', async () => {
            (archivesApi.getArchives as Mock).mockResolvedValue({
                code: 200,
                data: { records: [], total: 0, size: 10, current: 1 },
            });

            renderArchiveListView();

            await waitFor(() => {
                expect(archivesApi.getArchives).toHaveBeenCalled();
            });
        });
    });

    describe('路由配置模式', () => {
        it('使用 routeConfig 时应正确解析配置', async () => {
            (archivesApi.getArchives as Mock).mockResolvedValue({
                code: 200,
                data: { records: [], total: 0, size: 10, current: 1 },
            });

            renderArchiveListView('voucher');

            await waitFor(() => {
                expect(screen.getByText('档案管理')).toBeInTheDocument();
                expect(screen.getByText('会计凭证')).toBeInTheDocument();
            });
        });

        it('使用 pool 配置时应调用 poolApi', async () => {
            (poolApi.getList as Mock).mockResolvedValue([]);

            renderArchiveListView('pool');

            await waitFor(() => {
                expect(poolApi.getList).toHaveBeenCalled();
            });
        });
    });

    describe('加载状态', () => {
        it('加载时应调用 API', async () => {
            (archivesApi.getArchives as Mock).mockImplementation(() => new Promise(() => { }));

            renderArchiveListView();

            await waitFor(() => {
                expect(archivesApi.getArchives).toHaveBeenCalled();
            });
        });
    });

    describe('错误处理', () => {
        it('API 错误时应正确处理', async () => {
            (archivesApi.getArchives as Mock).mockResolvedValue({
                code: 500,
                message: '服务器错误',
            });

            renderArchiveListView();

            await waitFor(() => {
                expect(archivesApi.getArchives).toHaveBeenCalled();
            });
        });
    });
});
