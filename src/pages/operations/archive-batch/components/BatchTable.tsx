// Input: ArchiveBatch, handlers, Ant Design（Spin tip 嵌套）
// Output: 批次表格组件
// Pos: src/pages/operations/archive-batch/components/BatchTable.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import {
    Card, Table, Button, Space, Tag, Tooltip, Popconfirm,
    Select, Modal, Form, DatePicker, Tabs, Descriptions,
    Alert, Empty, Spin, Input
} from 'antd';
import {
    PlusOutlined, CheckCircleOutlined, CloseCircleOutlined,
    PlayCircleOutlined, EyeOutlined, DeleteOutlined,
    SafetyCertificateOutlined, ReloadOutlined, PlusCircleOutlined
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
    ArchiveBatch,
    ArchiveBatchItem,
    BatchStatus,
    IntegrityReport
} from '@/api/archiveBatch';
import {
    BatchOperationBar,
    BatchApprovalDialog,
    BatchResultModal
} from '@/components/operations';

const { RangePicker } = DatePicker;
const { TextArea } = Input;

// 状态配置
const STATUS_CONFIG: Record<BatchStatus, { color: string; text: string }> = {
    PENDING: { color: 'default', text: '待提交' },
    VALIDATING: { color: 'processing', text: '校验中' },
    APPROVED: { color: 'success', text: '已审批' },
    ARCHIVED: { color: 'green', text: '已归档' },
    REJECTED: { color: 'error', text: '已驳回' },
    FAILED: { color: 'error', text: '失败' },
};

/**
 * 批次表格组件 Props
 */
export interface BatchTableProps {
    // 数据
    batches: ArchiveBatch[];
    total: number;
    loading: boolean;
    page: number;
    pageSize: number;
    statusFilter?: BatchStatus;

    // 行选择
    rowSelection: any;
    selectedCount: number;

    // 回调
    onPageChange: (page: number, pageSize: number) => void;
    onRefresh: () => void;
    onOpenCreateModal: () => void;
    onStatusFilterChange: (status?: BatchStatus) => void;

    // 行操作
    onViewDetail: (batch: ArchiveBatch) => void;
    onSubmit: (batchId: number) => void;
    onDelete: (batchId: number) => void;
    onApprove: (batch: ArchiveBatch) => void;
    onReject: (batch: ArchiveBatch) => void;
    onArchive: (batchId: number) => void;

    // 批量操作
    onBatchApprove: () => void;
    onBatchReject: () => void;
    onSelectAll: () => void;
    onClearSelection: () => void;
    batchProcessing: boolean;
    batchDialogOpen: boolean;
    batchResultOpen: boolean;
    batchAction: 'approve' | 'reject';
    batchResult: { success: number; failed: number; errors: any[] };
    selectedRecords: any[];
    onBatchConfirm: (comment: string, skipIds: string[]) => Promise<void>;
    onBatchRetry: (failedIds: string[]) => Promise<void>;
    onCloseResultDialog: () => void;

    // 详情弹窗数据
    detailModalVisible: boolean;
    selectedBatch: ArchiveBatch | null;
    batchItems: ArchiveBatchItem[];
    integrityReport: IntegrityReport | null;

    // 详情弹窗操作
    onCloseDetail: () => void;
    onIntegrityCheck: (batchId: number) => void;
    onLoadAvailableVouchers: () => void;
    onOpenAddVoucherModal: () => void;
    addVoucherModalVisible: boolean;
    availableVouchers: any[];
    selectedVoucherIds: number[];
    loadingVouchers: boolean;
    onCloseAddVoucherModal: () => void;
    onVoucherSelectionChange: (ids: number[]) => void;
    onAddVouchers: () => void;

    // 审批弹窗
    approvalModalVisible: boolean;
    approvalAction: 'approve' | 'reject';
    approvalComment: string;
    onApprovalCommentChange: (comment: string) => void;
    onApproval: () => void;
    onCloseApproval: () => void;

    // 创建弹窗
    createModalVisible: boolean;
    form: any;
    onCloseCreate: () => void;
    onFormSubmit: (values: { period: [dayjs.Dayjs, dayjs.Dayjs] }) => void;
}

/**
 * 批次表格组件
 *
 * 包含表格、工具栏和所有弹窗
 */
export function BatchTable({
    batches,
    total,
    loading,
    page,
    pageSize,
    statusFilter,
    rowSelection,
    selectedCount,
    onPageChange,
    onRefresh,
    onOpenCreateModal,
    onStatusFilterChange,
    onViewDetail,
    onSubmit,
    onDelete,
    onApprove,
    onReject,
    onArchive,
    onBatchApprove,
    onBatchReject,
    onSelectAll,
    onClearSelection,
    batchProcessing,
    batchDialogOpen,
    batchResultOpen,
    batchAction,
    batchResult,
    selectedRecords,
    onBatchConfirm,
    onBatchRetry,
    onCloseResultDialog,
    detailModalVisible,
    selectedBatch,
    batchItems,
    integrityReport,
    onCloseDetail,
    onIntegrityCheck,
    onLoadAvailableVouchers,
    onOpenAddVoucherModal,
    addVoucherModalVisible,
    availableVouchers,
    selectedVoucherIds,
    loadingVouchers,
    onCloseAddVoucherModal,
    onVoucherSelectionChange,
    onAddVouchers,
    approvalModalVisible,
    approvalAction,
    approvalComment,
    onApprovalCommentChange,
    onApproval,
    onCloseApproval,
    createModalVisible,
    form,
    onCloseCreate,
    onFormSubmit
}: BatchTableProps) {
    // 表格列
    const columns: ColumnsType<ArchiveBatch> = [
        {
            title: '批次编号',
            dataIndex: 'batchNo',
            key: 'batchNo',
            width: 140,
            render: (text, record) => (
                <a onClick={() => onViewDetail(record)}>{text}</a>
            ),
        },
        {
            title: '归档期间',
            key: 'period',
            width: 200,
            render: (_, record) => (
                <span>
                    {record.periodStart} ~ {record.periodEnd}
                </span>
            ),
        },
        {
            title: '凭证数',
            dataIndex: 'voucherCount',
            key: 'voucherCount',
            width: 80,
            align: 'center',
        },
        {
            title: '单据数',
            dataIndex: 'docCount',
            key: 'docCount',
            width: 80,
            align: 'center',
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 100,
            render: (status: BatchStatus) => {
                const config = STATUS_CONFIG[status];
                return <Tag color={config?.color}>{config?.text || status}</Tag>;
            },
        },
        {
            title: '创建时间',
            dataIndex: 'createdTime',
            key: 'createdTime',
            width: 160,
            render: (text) => dayjs(text).format('YYYY-MM-DD HH:mm'),
        },
        {
            title: '操作',
            key: 'action',
            width: 280,
            render: (_, record) => (
                <Space size="small">
                    <Tooltip title="查看详情">
                        <Button
                            type="text"
                            icon={<EyeOutlined />}
                            onClick={() => onViewDetail(record)}
                        />
                    </Tooltip>

                    {record.status === 'PENDING' && (
                        <>
                            <Button
                                type="primary"
                                size="small"
                                onClick={() => onSubmit(record.id)}
                            >
                                提交校验
                            </Button>
                            <Popconfirm
                                title="确定删除此批次？"
                                onConfirm={() => onDelete(record.id)}
                            >
                                <Button type="text" danger icon={<DeleteOutlined />} />
                            </Popconfirm>
                        </>
                    )}

                    {record.status === 'VALIDATING' && (
                        <>
                            <Button
                                type="primary"
                                size="small"
                                icon={<CheckCircleOutlined />}
                                onClick={() => onApprove(record)}
                            >
                                通过
                            </Button>
                            <Button
                                size="small"
                                danger
                                icon={<CloseCircleOutlined />}
                                onClick={() => onReject(record)}
                            >
                                驳回
                            </Button>
                        </>
                    )}

                    {record.status === 'APPROVED' && (
                        <Popconfirm
                            title="确定执行归档？归档后数据将不可修改。"
                            onConfirm={() => onArchive(record.id)}
                        >
                            <Button
                                type="primary"
                                size="small"
                                icon={<PlayCircleOutlined />}
                            >
                                执行归档
                            </Button>
                        </Popconfirm>
                    )}
                </Space>
            ),
        },
    ];

    // 条目表格列
    const itemColumns: ColumnsType<ArchiveBatchItem> = [
        {
            title: '类型',
            dataIndex: 'itemType',
            key: 'itemType',
            width: 100,
            render: (type) => (
                <Tag color={type === 'VOUCHER' ? 'blue' : 'green'}>
                    {type === 'VOUCHER' ? '凭证' : '单据'}
                </Tag>
            ),
        },
        {
            title: '编号',
            dataIndex: 'refNo',
            key: 'refNo',
        },
        {
            title: '状态',
            dataIndex: 'status',
            key: 'status',
            width: 100,
            render: (status) => {
                const config: Record<string, { color: string; text: string }> = {
                    PENDING: { color: 'default', text: '待校验' },
                    VALIDATED: { color: 'success', text: '已校验' },
                    ARCHIVED: { color: 'green', text: '已归档' },
                    FAILED: { color: 'error', text: '失败' },
                };
                return <Tag color={config[status]?.color}>{config[status]?.text}</Tag>;
            },
        },
    ];

    return (
        <>
            <Card
                title="归档批次管理"
                extra={
                    <Space>
                        <Select
                            placeholder="状态筛选"
                            allowClear
                            style={{ width: 120 }}
                            value={statusFilter}
                            onChange={onStatusFilterChange}
                            options={Object.entries(STATUS_CONFIG).map(([value, config]) => ({
                                value,
                                label: config.text,
                            }))}
                        />
                        <Button icon={<ReloadOutlined />} onClick={onRefresh}>
                            刷新
                        </Button>
                        <Button
                            type="primary"
                            icon={<PlusOutlined />}
                            onClick={onOpenCreateModal}
                        >
                            创建批次
                        </Button>
                    </Space>
                }
            >
                {/* 批量操作工具栏 */}
                <BatchOperationBar
                    selectedCount={selectedCount}
                    totalCount={batches.length}
                    onBatchApprove={onBatchApprove}
                    onBatchReject={onBatchReject}
                    onSelectAll={onSelectAll}
                    onClear={onClearSelection}
                    loading={batchProcessing}
                />

                <Table
                    columns={columns}
                    dataSource={batches}
                    rowKey="id"
                    rowSelection={rowSelection}
                    loading={loading}
                    pagination={{
                        current: page,
                        pageSize,
                        total,
                        showSizeChanger: true,
                        showTotal: (t) => `共 ${t} 条`,
                        onChange: onPageChange,
                    }}
                />
            </Card>

            {/* 创建批次弹窗 */}
            <Modal
                title="创建归档批次"
                open={createModalVisible}
                onCancel={onCloseCreate}
                onOk={() => form.submit()}
            >
                <Form form={form} layout="vertical" onFinish={onFormSubmit}>
                    <Form.Item
                        name="period"
                        label="归档期间"
                        rules={[{ required: true, message: '请选择归档期间' }]}
                    >
                        <RangePicker style={{ width: '100%' }} />
                    </Form.Item>
                </Form>
            </Modal>

            {/* 详情弹窗 */}
            <Modal
                title={`批次详情 - ${selectedBatch?.batchNo}`}
                open={detailModalVisible}
                onCancel={onCloseDetail}
                footer={null}
                width={800}
            >
                {selectedBatch && (
                    <Tabs
                        items={[
                            {
                                key: 'info',
                                label: '基本信息',
                                children: (
                                    <Descriptions column={2} bordered size="small">
                                        <Descriptions.Item label="批次编号">
                                            {selectedBatch.batchNo}
                                        </Descriptions.Item>
                                        <Descriptions.Item label="状态">
                                            <Tag color={STATUS_CONFIG[selectedBatch.status]?.color}>
                                                {STATUS_CONFIG[selectedBatch.status]?.text}
                                            </Tag>
                                        </Descriptions.Item>
                                        <Descriptions.Item label="归档期间" span={2}>
                                            {selectedBatch.periodStart} ~ {selectedBatch.periodEnd}
                                        </Descriptions.Item>
                                        <Descriptions.Item label="凭证数">
                                            {selectedBatch.voucherCount}
                                        </Descriptions.Item>
                                        <Descriptions.Item label="单据数">
                                            {selectedBatch.docCount}
                                        </Descriptions.Item>
                                        <Descriptions.Item label="创建时间" span={2}>
                                            {dayjs(selectedBatch.createdTime).format('YYYY-MM-DD HH:mm:ss')}
                                        </Descriptions.Item>
                                        {selectedBatch.archivedAt && (
                                            <Descriptions.Item label="归档时间" span={2}>
                                                {dayjs(selectedBatch.archivedAt).format('YYYY-MM-DD HH:mm:ss')}
                                            </Descriptions.Item>
                                        )}
                                        {selectedBatch.errorMessage && (
                                            <Descriptions.Item label="错误信息" span={2}>
                                                <Alert type="error" title={selectedBatch.errorMessage} />
                                            </Descriptions.Item>
                                        )}
                                    </Descriptions>
                                ),
                            },
                            {
                                key: 'items',
                                label: `条目列表 (${batchItems.length})`,
                                children: (
                                    <div>
                                        {selectedBatch?.status === 'PENDING' && (
                                            <div className="mb-4">
                                                <Button
                                                    type="primary"
                                                    icon={<PlusCircleOutlined />}
                                                    onClick={() => {
                                                        onLoadAvailableVouchers();
                                                        onOpenAddVoucherModal();
                                                    }}
                                                >
                                                    添加凭证
                                                </Button>
                                            </div>
                                        )}
                                        <Table
                                            columns={itemColumns}
                                            dataSource={batchItems}
                                            rowKey="id"
                                            size="small"
                                            pagination={{ pageSize: 10 }}
                                        />
                                    </div>
                                ),
                            },
                            {
                                key: 'integrity',
                                label: (
                                    <span>
                                        <SafetyCertificateOutlined /> 四性检测
                                    </span>
                                ),
                                children: integrityReport ? (
                                    <div>
                                        <Alert
                                            type={integrityReport.overallResult === 'PASS' ? 'success' : 'error'}
                                            title={`检测结果: ${integrityReport.overallResult === 'PASS' ? '通过' : '未通过'}`}
                                            className="mb-4"
                                        />
                                        {integrityReport.checks.map((check: any, idx: number) => (
                                            <Card key={idx} size="small" className="mb-2">
                                                <Space>
                                                    {check.result === 'PASS' ? (
                                                        <CheckCircleOutlined style={{ color: '#52c41a' }} />
                                                    ) : (
                                                        <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
                                                    )}
                                                    <span>{check.name}</span>
                                                    <Tag color={check.result === 'PASS' ? 'green' : 'red'}>
                                                        {check.result}
                                                    </Tag>
                                                </Space>
                                            </Card>
                                        ))}
                                    </div>
                                ) : (
                                    <Empty description="暂无检测记录">
                                        <Button
                                            type="primary"
                                            onClick={() => onIntegrityCheck(selectedBatch.id)}
                                        >
                                            执行四性检测
                                        </Button>
                                    </Empty>
                                ),
                            },
                        ]}
                    />
                )}
            </Modal>

            {/* 审批弹窗 */}
            <Modal
                title={approvalAction === 'approve' ? '审批通过' : '驳回批次'}
                open={approvalModalVisible}
                onCancel={onCloseApproval}
                onOk={onApproval}
                okText={approvalAction === 'approve' ? '确认通过' : '确认驳回'}
                okButtonProps={{
                    danger: approvalAction === 'reject',
                }}
            >
                <div className="mb-4">
                    批次编号: <strong>{selectedBatch?.batchNo}</strong>
                </div>
                <Form.Item
                    label={approvalAction === 'approve' ? '审批意见（可选）' : '驳回原因'}
                    required={approvalAction === 'reject'}
                >
                    <TextArea
                        rows={4}
                        value={approvalComment}
                        onChange={(e) => onApprovalCommentChange(e.target.value)}
                        placeholder={approvalAction === 'approve' ? '请输入审批意见...' : '请输入驳回原因...'}
                    />
                </Form.Item>
            </Modal>

            {/* 添加凭证弹窗 */}
            <Modal
                title="选择凭证添加到批次"
                open={addVoucherModalVisible}
                onCancel={onCloseAddVoucherModal}
                onOk={onAddVouchers}
                okText="确认添加"
                okButtonProps={{ disabled: selectedVoucherIds.length === 0 }}
                width={700}
            >
                <div className="mb-4">
                    <Alert
                        type="info"
                        title={`已选择 ${selectedVoucherIds.length} 条凭证`}
                        showIcon
                    />
                </div>
                {loadingVouchers ? (
                    <div className="text-center py-8">
                        <Spin tip="加载中...">
                            <div style={{ minHeight: 24 }} />
                        </Spin>
                    </div>
                ) : availableVouchers.length === 0 ? (
                    <Empty description='暂无可添加的凭证（需要状态为"准备归档"的凭证）' />
                ) : (
                    <Table
                        rowSelection={{
                            type: 'checkbox',
                            selectedRowKeys: selectedVoucherIds,
                            onChange: (keys) => onVoucherSelectionChange(keys as number[]),
                        }}
                        columns={[
                            { title: '凭证号', dataIndex: 'erpVoucherNo', key: 'erpVoucherNo' },
                            { title: '摘要', dataIndex: 'summary', key: 'summary', ellipsis: true },
                            { title: '金额', dataIndex: 'amount', key: 'amount' },
                            { title: '日期', dataIndex: 'docDate', key: 'docDate' },
                        ]}
                        dataSource={availableVouchers}
                        rowKey="id"
                        size="small"
                        pagination={{ pageSize: 10 }}
                    />
                )}
            </Modal>

            {/* 批量审批弹窗 */}
            <BatchApprovalDialog
                visible={batchDialogOpen}
                selectedCount={selectedCount}
                action={batchAction}
                onConfirm={onBatchConfirm}
                onCancel={() => {}}
                selectedRecords={selectedRecords}
                loading={batchProcessing}
            />

            {/* 批量操作结果弹窗 */}
            <BatchResultModal
                visible={batchResultOpen}
                successCount={batchResult.success}
                failedCount={batchResult.failed}
                errors={batchResult.errors}
                onRetry={onBatchRetry}
                onClose={onCloseResultDialog}
                operationType="approval"
                isRetrying={batchProcessing}
            />
        </>
    );
}

export default BatchTable;
