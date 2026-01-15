// Input: Vitest、@testing-library/react、BatchTable 组件
// Output: BatchTable 虚拟化性能测试
// Pos: src/__tests__/performance/BatchTable.performance.test.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/// <reference types="vitest/globals" />

import React, { useState } from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import dayjs from 'dayjs';

// Mock the complex operations components
vi.mock('@/components/operations', () => ({
    BatchOperationBar: ({ children, ...props }: any) => (
        <div data-mock="BatchOperationBar" {...props}>{children}</div>
    ),
    BatchApprovalDialog: ({ children, ...props }: any) => (
        <div data-mock="BatchApprovalDialog" {...props}>{children}</div>
    ),
    BatchResultModal: ({ children, ...props }: any) => (
        <div data-mock="BatchResultModal" {...props}>{children}</div>
    ),
    useBatchSelection: () => ({
        selectedRowKeys: [],
        onChange: vi.fn(),
        getCheckboxProps: () => ({ disabled: false }),
        clearSelection: vi.fn(),
        selectAll: vi.fn(),
    }),
}));

// Import BatchTable component
import { BatchTable } from '@/pages/operations/archive-batch/components';
import type { ArchiveBatch, BatchStatus } from '@/api/archiveBatch';

// ===== Mock Data Generator =====

/**
 * Generate mock ArchiveBatch data
 */
export function generateMockBatches(count: number): ArchiveBatch[] {
    const statuses: BatchStatus[] = ['PENDING', 'VALIDATING', 'APPROVED', 'ARCHIVED', 'REJECTED', 'FAILED'];

    return Array.from({ length: count }, (_, index) => ({
        id: index + 1,
        batchNo: `BATCH${String(index + 1).padStart(6, '0')}`,
        fondsId: 1,
        periodStart: `2024-${String((index % 12) + 1).padStart(2, '0')}-01`,
        periodEnd: `2024-${String((index % 12) + 1).padStart(2, '0')}-28`,
        scopeType: 'PERIOD' as const,
        status: statuses[index % statuses.length],
        voucherCount: Math.floor(Math.random() * 500) + 10,
        docCount: Math.floor(Math.random() * 1000) + 50,
        fileCount: Math.floor(Math.random() * 1500) + 100,
        totalSizeBytes: Math.floor(Math.random() * 10_000_000_000),
        errorMessage: index % 20 === 0 ? 'Mock error message' : undefined,
        createdTime: dayjs().subtract(index, 'day').toISOString(),
        lastModifiedTime: dayjs().subtract(index, 'day').toISOString(),
    }));
}

/**
 * Mock props generator for BatchTable
 */
export function createMockProps(overrides?: Partial<React.ComponentProps<typeof BatchTable>>) {
    const batches = generateMockBatches(overrides?.batches?.length || 100);

    return {
        batches,
        total: batches.length,
        loading: false,
        page: 1,
        pageSize: 20,
        statusFilter: undefined,
        rowSelection: {
            selectedRowKeys: [],
            onChange: () => { },
            getCheckboxProps: () => ({ disabled: false }),
        },
        selectedCount: 0,
        onPageChange: () => { },
        onRefresh: () => { },
        onOpenCreateModal: () => { },
        onStatusFilterChange: () => { },
        onViewDetail: () => { },
        onSubmit: () => { },
        onDelete: () => { },
        onApprove: () => { },
        onReject: () => { },
        onArchive: () => { },
        onBatchApprove: () => { },
        onBatchReject: () => { },
        onSelectAll: () => { },
        onClearSelection: () => { },
        batchProcessing: false,
        batchDialogOpen: false,
        batchResultOpen: false,
        batchAction: 'approve' as const,
        batchResult: { success: 0, failed: 0, errors: [] },
        selectedRecords: [],
        onBatchConfirm: () => Promise.resolve(),
        onBatchRetry: () => Promise.resolve(),
        onCloseResultDialog: () => { },
        detailModalVisible: false,
        selectedBatch: null,
        batchItems: [],
        integrityReport: null,
        onCloseDetail: () => { },
        onIntegrityCheck: () => { },
        onLoadAvailableVouchers: () => { },
        onOpenAddVoucherModal: () => { },
        addVoucherModalVisible: false,
        availableVouchers: [],
        selectedVoucherIds: [],
        loadingVouchers: false,
        onCloseAddVoucherModal: () => { },
        onVoucherSelectionChange: () => { },
        onAddVouchers: () => { },
        approvalModalVisible: false,
        approvalAction: 'approve' as const,
        approvalComment: '',
        onApprovalCommentChange: () => { },
        onApproval: () => { },
        onCloseApproval: () => { },
        createModalVisible: false,
        form: {
            getFieldDecorator: () => ({}),
            setFieldsValue: () => { },
            getFieldsValue: () => ({}),
            resetFields: () => { },
            validateFields: () => Promise.resolve({}),
            submit: () => { },
        },
        onCloseCreate: () => { },
        onFormSubmit: () => { },
        ...overrides,
    };
}

// ===== Performance Metrics =====

interface PerformanceMetrics {
    renderTime: number;        // Time in ms
    reRenderTime: number;      // Time in ms for re-render
    domNodes: number;          // Number of DOM nodes
    memoryUsage: number;       // Estimated memory in bytes
}

/**
 * Measure render performance
 */
function measureRenderPerformance(renderFn: () => any): PerformanceMetrics {
    // Force garbage collection if available
    if (global.gc) {
        global.gc();
    }

    const startTime = performance.now();

    // Execute render
    const { container } = renderFn();

    const endTime = performance.now();
    const renderTime = endTime - startTime;

    // Count DOM nodes
    const domNodes = container.querySelectorAll('*').length;

    // Estimate memory usage (rough approximation)
    const memoryUsage = domNodes * 200; // Assume ~200 bytes per DOM node

    return {
        renderTime,
        reRenderTime: 0,
        domNodes,
        memoryUsage,
    };
}

/**
 * Measure re-render performance
 */
/**
 * measureReRenderPerformance 已移除，因为它未被使用。
 */

/**
 * Count rendered table rows
 */
function countRenderedRows(container: HTMLElement): number {
    const tableBody = container.querySelector('.ant-table-tbody');
    if (!tableBody) return 0;
    return tableBody.querySelectorAll('tr').length;
}

/**
 * Get performance report for analysis
 */
function getPerformanceReport(metrics: PerformanceMetrics, dataSize: number): string {
    return `
Data Size: ${dataSize} records
Render Time: ${metrics.renderTime.toFixed(2)} ms
DOM Nodes: ${metrics.domNodes}
Estimated Memory: ${(metrics.memoryUsage / 1024 / 1024).toFixed(2)} MB
Render Efficiency: ${(dataSize / metrics.renderTime).toFixed(2)} records/ms
`;
}

// ===== Test Suites =====

describe('BatchTable Performance Tests', () => {
    beforeEach(() => {
        // Reset any global state before each test
    });

    afterEach(() => {
        // Cleanup after each test
    });

    describe('Small Dataset (10 records)', () => {
        it('should render 10 rows efficiently', () => {
            const props = createMockProps({ batches: generateMockBatches(10), total: 10 });

            const metrics = measureRenderPerformance(() => render(<BatchTable {...props} />));

            // Performance assertions
            expect(metrics.renderTime).toBeLessThan(500); // Should render in < 500ms

            // Verify data is rendered
            expect(screen.getByText('归档批次管理')).toBeInTheDocument();

            // Log performance data
            console.log(getPerformanceReport(metrics, 10));
        });

        it('should have proportional DOM nodes for small dataset', () => {
            const props = createMockProps({ batches: generateMockBatches(10), total: 10 });
            const { container } = render(<BatchTable {...props} />);

            const domNodes = container.querySelectorAll('*').length;

            // With mock components, DOM nodes are minimal but component renders
            expect(domNodes).toBeGreaterThan(0);
            expect(domNodes).toBeLessThan(500);

            console.log(`Small dataset DOM nodes: ${domNodes}`);
        });
    });

    describe('Medium Dataset (100 records)', () => {
        it('should render 100 rows efficiently', () => {
            const props = createMockProps({ batches: generateMockBatches(100), total: 100 });

            const metrics = measureRenderPerformance(() => render(<BatchTable {...props} />));

            // Performance assertions
            expect(metrics.renderTime).toBeLessThan(1000); // Should render in < 1s

            // Verify component rendered (mock components don't render pagination text)
            expect(screen.getByText('归档批次管理')).toBeInTheDocument();

            console.log(getPerformanceReport(metrics, 100));
        });

        it('should handle status filter change efficiently', () => {
            const props = createMockProps({
                batches: generateMockBatches(100),
                total: 100,
                statusFilter: undefined,
            });

            const { container: _container, rerender } = render(<BatchTable {...props} />);

            // Measure re-render with filter
            const startTime = performance.now();
            rerender(<BatchTable {...props} statusFilter="PENDING" />);
            const endTime = performance.now();

            const reRenderTime = endTime - startTime;

            // Re-render should be fast
            expect(reRenderTime).toBeLessThan(500);

            console.log(`Status filter re-render time: ${reRenderTime.toFixed(2)} ms`);
        });

        it('should handle page change efficiently', () => {
            const props = createMockProps({
                batches: generateMockBatches(100),
                total: 100,
                page: 1,
                pageSize: 20,
            });

            const { container: _container, rerender } = render(<BatchTable {...props} />);

            // Measure re-render on page change
            const startTime = performance.now();
            rerender(<BatchTable {...props} page={2} />);
            const endTime = performance.now();

            const reRenderTime = endTime - startTime;

            // Page change re-render should be fast
            expect(reRenderTime).toBeLessThan(500);

            console.log(`Page change re-render time: ${reRenderTime.toFixed(2)} ms`);
        });
    });

    describe('Large Dataset (1000 records)', () => {
        it('should render 1000 rows efficiently (with pagination)', () => {
            const props = createMockProps({
                batches: generateMockBatches(1000),
                total: 1000,
                page: 1,
                pageSize: 20, // Only 20 visible at a time
            });

            const metrics = measureRenderPerformance(() => render(<BatchTable {...props} />));

            // Performance assertions - with pagination, should still be fast
            expect(metrics.renderTime).toBeLessThan(1000); // Should render in < 1s

            console.log(getPerformanceReport(metrics, 1000));
        });

        it('should maintain reasonable DOM nodes with pagination', () => {
            const props = createMockProps({
                batches: generateMockBatches(1000),
                total: 1000,
                pageSize: 20,
            });

            const { container } = render(<BatchTable {...props} />);

            const domNodes = container.querySelectorAll('*').length;

            // With pagination, DOM nodes should not grow linearly with data
            expect(domNodes).toBeLessThan(1500);

            console.log(`Large dataset (paginated) DOM nodes: ${domNodes}`);
        });

        it('should handle row selection efficiently', () => {
            const selectedKeys = [1, 2, 3, 4, 5];
            const props = createMockProps({
                batches: generateMockBatches(1000),
                total: 1000,
                rowSelection: {
                    selectedRowKeys: selectedKeys,
                    onChange: () => { },
                    getCheckboxProps: () => ({ disabled: false }),
                },
                selectedCount: selectedKeys.length,
            });

            const startTime = performance.now();
            render(<BatchTable {...props} />);
            const endTime = performance.now();

            const renderTime = endTime - startTime;

            // Should handle selection without significant performance impact
            expect(renderTime).toBeLessThan(1000);

            console.log(`Row selection render time: ${renderTime.toFixed(2)} ms`);
        });
    });

    describe('Extra Large Dataset (10000 records)', () => {
        it('should render 10000 rows efficiently (with pagination)', () => {
            const props = createMockProps({
                batches: generateMockBatches(10000),
                total: 10000,
                page: 1,
                pageSize: 20,
            });

            const metrics = measureRenderPerformance(() => render(<BatchTable {...props} />));

            // Even with 10k records, pagination should keep render time reasonable
            expect(metrics.renderTime).toBeLessThan(2000); // Should render in < 2s

            console.log(getPerformanceReport(metrics, 10000));
        });

        it('should maintain constant DOM nodes regardless of dataset size', () => {
            const smallProps = createMockProps({
                batches: generateMockBatches(100),
                total: 100,
                pageSize: 20,
            });

            const largeProps = createMockProps({
                batches: generateMockBatches(10000),
                total: 10000,
                pageSize: 20,
            });

            const { container: smallContainer } = render(<BatchTable {...smallProps} />);
            const smallNodes = smallContainer.querySelectorAll('*').length;

            // Unmount small
            smallContainer.remove();

            const { container: largeContainer } = render(<BatchTable {...largeProps} />);
            const largeNodes = largeContainer.querySelectorAll('*').length;

            // DOM nodes should be similar (within 20% tolerance)
            const nodeRatio = largeNodes / smallNodes;
            expect(nodeRatio).toBeLessThan(1.2); // Less than 20% increase

            console.log(`Small dataset DOM nodes: ${smallNodes}`);
            console.log(`Large dataset DOM nodes: ${largeNodes}`);
            console.log(`Node ratio: ${nodeRatio.toFixed(2)}`);
        });

        it('should handle batch selection operations efficiently', () => {
            const props = createMockProps({
                batches: generateMockBatches(10000),
                total: 10000,
                pageSize: 20,
                rowSelection: {
                    selectedRowKeys: Array.from({ length: 50 }, (_, i) => i + 1),
                    onChange: () => { },
                    getCheckboxProps: () => ({ disabled: false }),
                },
                selectedCount: 50,
            });

            const startTime = performance.now();
            const { container: _container } = render(<BatchTable {...props} />);
            const endTime = performance.now();

            const renderTime = endTime - startTime;

            // Should handle 50 selected rows efficiently
            expect(renderTime).toBeLessThan(1500);

            console.log(`Batch selection (50 rows) render time: ${renderTime.toFixed(2)} ms`);
        });
    });

    describe('Stress Tests', () => {
        it('should handle rapid re-renders without memory leaks', () => {
            const props = createMockProps({
                batches: generateMockBatches(1000),
                total: 1000,
            });

            const { container: _container, rerender, unmount } = render(<BatchTable {...props} />);

            const initialMemory = (performance as any).memory?.usedJSHeapSize || 0;
            const renderTimes: number[] = [];

            // Perform 10 rapid re-renders
            for (let i = 0; i < 10; i++) {
                const start = performance.now();
                rerender(<BatchTable {...props} page={(i % 5) + 1} />);
                const end = performance.now();
                renderTimes.push(end - start);
            }

            const finalMemory = (performance as any).memory?.usedJSHeapSize || 0;
            const avgRenderTime = renderTimes.reduce((a, b) => a + b, 0) / renderTimes.length;
            const memoryGrowth = finalMemory - initialMemory;

            // Average render time should be consistent
            expect(avgRenderTime).toBeLessThan(500);

            // Memory growth should be minimal (less than 5MB)
            expect(memoryGrowth).toBeLessThan(5 * 1024 * 1024);

            console.log(`Average re-render time: ${avgRenderTime.toFixed(2)} ms`);
            console.log(`Memory growth: ${(memoryGrowth / 1024 / 1024).toFixed(2)} MB`);

            unmount();
        });

        it('should handle concurrent state changes efficiently', () => {
            function TestWrapper() {
                const [page, setPage] = useState(1);
                const [statusFilter, setStatusFilter] = useState<BatchStatus | undefined>();

                const props = createMockProps({
                    batches: generateMockBatches(1000),
                    total: 1000,
                    page,
                    statusFilter,
                    onPageChange: setPage,
                    onStatusFilterChange: setStatusFilter,
                });

                return <BatchTable {...props} />;
            }

            const startTime = performance.now();
            const { container: _container } = render(<TestWrapper />);
            const endTime = performance.now();

            const renderTime = endTime - startTime;

            // Initial render should be fast
            expect(renderTime).toBeLessThan(1500);

            console.log(`Concurrent state changes render time: ${renderTime.toFixed(2)} ms`);
        });
    });
});

// ===== Virtualization Validation =====

describe('BatchTable Virtualization Validation', () => {
    it('should only render visible rows when using pagination', () => {
        const pageSize = 20;
        const props = createMockProps({
            batches: generateMockBatches(1000),
            total: 1000,
            pageSize,
            page: 1,
        });

        const { container } = render(<BatchTable {...props} />);

        const renderedRows = countRenderedRows(container);

        // Should render approximately pageSize rows (plus header)
        expect(renderedRows).toBeLessThanOrEqual(pageSize + 5);

        console.log(`Rendered rows: ${renderedRows} (pageSize: ${pageSize})`);
    });

    it('should update visible rows on page change', () => {
        const pageSize = 20;
        const props = createMockProps({
            batches: generateMockBatches(1000),
            total: 1000,
            pageSize,
            page: 1,
        });

        const { container, rerender } = render(<BatchTable {...props} />);

        const firstPageRows = countRenderedRows(container);

        rerender(<BatchTable {...props} page={2} />);

        const secondPageRows = countRenderedRows(container);

        // Row count should remain consistent
        expect(firstPageRows).toBe(secondPageRows);

        console.log(`First page rows: ${firstPageRows}, Second page rows: ${secondPageRows}`);
    });
});
