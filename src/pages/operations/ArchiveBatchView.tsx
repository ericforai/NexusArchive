// Input: React、Ant Design、归档批次 API、批量操作组件
// Output: ArchiveBatchView 组件（集成批量审批功能）
// Pos: src/pages/operations/ArchiveBatchView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import {
    Card, Table, Button, Space, Tag, Modal, Form, DatePicker,
    message, Spin, Empty, Descriptions, Alert, Tabs, Tooltip,
    Popconfirm, Input, Select, Row, Col, Statistic
} from 'antd';
import {
    PlusOutlined, CheckCircleOutlined, CloseCircleOutlined,
    PlayCircleOutlined, EyeOutlined, DeleteOutlined,
    SafetyCertificateOutlined, ReloadOutlined,
    PlusCircleOutlined
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import {
    archiveBatchApi,
    ArchiveBatch,
    ArchiveBatchItem,
    BatchStatus,
    IntegrityReport
} from '../../api/archiveBatch';
import { useFondsStore } from '../../store/useFondsStore';
import { useAuthStore } from '../../store/useAuthStore';
import {
    BatchOperationBar,
    BatchApprovalDialog,
    BatchResultModal,
    useBatchSelection,
    type ApprovalRecord,
    type BatchError
} from '../../components/operations';

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

const ArchiveBatchView: React.FC = () => {
    // 全宗状态
    const currentFonds = useFondsStore((state) => state.currentFonds);

    // 当前用户
    const user = useAuthStore((state) => state.user);

    // 批量选择状态
    const {
        selectedIds,
        rowSelection,
        clearSelection,
        selectAll,
        getSelectedCount
    } = useBatchSelection();

    // 状态
    const [loading, setLoading] = useState(false);
    const [batches, setBatches] = useState<ArchiveBatch[]>([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(1);
    const [pageSize, setPageSize] = useState(10);
    const [statusFilter, setStatusFilter] = useState<BatchStatus | undefined>();

    // 弹窗状态
    const [createModalVisible, setCreateModalVisible] = useState(false);
    const [detailModalVisible, setDetailModalVisible] = useState(false);
    const [approvalModalVisible, setApprovalModalVisible] = useState(false);
    const [selectedBatch, setSelectedBatch] = useState<ArchiveBatch | null>(null);
    const [batchItems, setBatchItems] = useState<ArchiveBatchItem[]>([]);
    const [integrityReport, setIntegrityReport] = useState<IntegrityReport | null>(null);
    const [approvalAction, setApprovalAction] = useState<'approve' | 'reject'>('approve');
    const [approvalComment, setApprovalComment] = useState('');

    // 添加凭证弹窗
    const [addVoucherModalVisible, setAddVoucherModalVisible] = useState(false);
    const [availableVouchers, setAvailableVouchers] = useState<any[]>([]);
    const [selectedVoucherIds, setSelectedVoucherIds] = useState<number[]>([]);
    const [loadingVouchers, setLoadingVouchers] = useState(false);

    // 统计
    const [stats, setStats] = useState<{ total: number; byStatus: Record<string, number> }>({
        total: 0,
        byStatus: {}
    });

    // 批量操作状态
    const [batchDialogOpen, setBatchDialogOpen] = useState(false);
    const [batchResultOpen, setBatchResultOpen] = useState(false);
    const [batchAction, setBatchAction] = useState<'approve' | 'reject'>('approve');
    const [batchResult, setBatchResult] = useState({ success: 0, failed: 0, errors: [] as BatchError[] });
    const [batchProcessing, setBatchProcessing] = useState(false);

    const [form] = Form.useForm();

    // 加载批次列表
    const loadBatches = useCallback(async () => {
        setLoading(true);
        try {
            const result = await archiveBatchApi.listBatches(page, pageSize, undefined, statusFilter);
            setBatches(result.records);
            setTotal(result.total);
        } catch {
            message.error('加载批次列表失败');
        } finally {
            setLoading(false);
        }
    }, [page, pageSize, statusFilter]);

    // 加载统计
    const loadStats = useCallback(async () => {
        try {
            const data = await archiveBatchApi.getStats();
            setStats(data);
        } catch {
            // 忽略统计加载失败
        }
    }, []);

    useEffect(() => {
        loadBatches();
        loadStats();
    }, [loadBatches, loadStats]);

    // 创建批次
    const handleCreate = async (values: { period: [dayjs.Dayjs, dayjs.Dayjs] }) => {
        try {
            await archiveBatchApi.createBatch({
                fondsId: currentFonds?.id ? parseInt(currentFonds.id) : 1,
                periodStart: values.period[0].format('YYYY-MM-DD'),
                periodEnd: values.period[1].format('YYYY-MM-DD'),
            });
            message.success('创建成功');
            setCreateModalVisible(false);
            form.resetFields();
            loadBatches();
            loadStats();
        } catch (err: any) {
            message.error(err.message || '创建失败');
        }
    };

    // 查看详情
    const handleViewDetail = async (batch: ArchiveBatch) => {
        setSelectedBatch(batch);
        setDetailModalVisible(true);

        try {
            const items = await archiveBatchApi.getItems(batch.id);
            setBatchItems(items);

            if (batch.integrityReport) {
                setIntegrityReport(batch.integrityReport);
            }
        } catch {
            message.error('加载批次详情失败');
        }
    };

    // 提交校验
    const handleSubmit = async (batchId: number) => {
        try {
            await archiveBatchApi.submitBatch(batchId);
            message.success('提交成功，正在校验');
            loadBatches();
        } catch (err: any) {
            message.error(err.message || '提交失败');
        }
    };

    // 打开审批弹窗
    const openApprovalModal = (batch: ArchiveBatch, action: 'approve' | 'reject') => {
        setSelectedBatch(batch);
        setApprovalAction(action);
        setApprovalComment('');
        setApprovalModalVisible(true);
    };

    // 执行审批
    const handleApproval = async () => {
        if (!selectedBatch) return;

        try {
            if (approvalAction === 'approve') {
                await archiveBatchApi.approveBatch(selectedBatch.id, approvalComment);
                message.success('审批通过');
            } else {
                if (!approvalComment.trim()) {
                    message.error('请填写驳回原因');
                    return;
                }
                await archiveBatchApi.rejectBatch(selectedBatch.id, approvalComment);
                message.success('已驳回');
            }
            setApprovalModalVisible(false);
            loadBatches();
            loadStats();
        } catch (err: any) {
            message.error(err.message || '操作失败');
        }
    };

    // 执行归档
    const handleArchive = async (batchId: number) => {
        try {
            await archiveBatchApi.executeBatchArchive(batchId);
            message.success('归档完成');
            loadBatches();
            loadStats();
        } catch (err: any) {
            message.error(err.message || '归档失败');
        }
    };

    // 执行四性检测
    const handleIntegrityCheck = async (batchId: number) => {
        try {
            const report = await archiveBatchApi.runIntegrityCheck(batchId);
            setIntegrityReport(report);
            message.success('四性检测完成');
        } catch (err: any) {
            message.error(err.message || '检测失败');
        }
    };

    // 删除批次
    const handleDelete = async (batchId: number) => {
        try {
            await archiveBatchApi.deleteBatch(batchId);
            message.success('删除成功');
            loadBatches();
            loadStats();
        } catch (err: any) {
            message.error(err.message || '删除失败');
        }
    };

    // 加载可选凭证列表（预归档库中待归档的凭证）
    const loadAvailableVouchers = async () => {
        if (!selectedBatch) return;
        setLoadingVouchers(true);
        try {
            // 调用预归档库 API 获取待归档凭证
            const response = await fetch('/api/pool?page=1&size=100&status=PENDING_ARCHIVE');
            const data = await response.json();
            if (data.code === 200) {
                setAvailableVouchers(data.data?.records || data.data || []);
            }
        } catch {
            message.error('加载凭证列表失败');
        } finally {
            setLoadingVouchers(false);
        }
    };

    // 添加选中的凭证到批次
    const handleAddVouchersToBatch = async () => {
        if (!selectedBatch || selectedVoucherIds.length === 0) return;
        try {
            const added = await archiveBatchApi.addVouchers(selectedBatch.id, selectedVoucherIds);
            message.success(`已添加 ${added} 条凭证`);
            setAddVoucherModalVisible(false);
            setSelectedVoucherIds([]);
            // 刷新条目列表
            const items = await archiveBatchApi.getItems(selectedBatch.id);
            setBatchItems(items);
            // 刷新批次信息
            const updatedBatch = await archiveBatchApi.getBatch(selectedBatch.id);
            setSelectedBatch(updatedBatch);
            loadBatches();
        } catch (err: any) {
            message.error(err.message || '添加失败');
        }
    };

    // ========== 批量操作处理函数 ==========

    const handleBatchApprove = () => {
        setBatchAction('approve');
        setBatchDialogOpen(true);
    };

    const handleBatchReject = () => {
        setBatchAction('reject');
        setBatchDialogOpen(true);
    };

    const handleBatchConfirm = async (comment: string, skipIds: number[]) => {
        const selectedIdArray = Array.from(selectedIds).filter(id => !skipIds.includes(id));

        // 添加边界检查
        if (selectedIdArray.length === 0) {
            message.warning('请选择至少一条记录');
            return;
        }

        if (selectedIdArray.length > 100) {
            message.warning('单次最多 100 条，请分批操作');
            return;
        }

        try {
            setBatchProcessing(true);

            const request = {
                ids: selectedIdArray,
                approverId: user?.id || '',
                approverName: user?.realName || user?.username || '',
                comment: comment || (batchAction === 'approve' ? '批量批准' : '批量拒绝')
            };

            const result = batchAction === 'approve'
                ? await archiveBatchApi.batchApprove(request)
                : await archiveBatchApi.batchReject(request);

            setBatchResult({
                success: result.success,
                failed: result.failed,
                errors: result.errors || []
            });

            setBatchDialogOpen(false);
            setBatchResultOpen(true);
            clearSelection();
            loadBatches();
            loadStats();

            if (result.failed === 0) {
                message.success(`批量${batchAction === 'approve' ? '批准' : '拒绝'}成功`);
            } else if (result.success === 0) {
                message.error(`批量${batchAction === 'approve' ? '批准' : '拒绝'}失败`);
            } else {
                message.warning(`部分成功：${result.success}条成功，${result.failed}条失败`);
            }
        } catch (err: any) {
            message.error(err.message || `批量${batchAction === 'approve' ? '批准' : '拒绝'}失败，请重试`);
        } finally {
            setBatchProcessing(false);
        }
    };

    const handleBatchRetry = async (failedIds: number[]) => {
        try {
            setBatchProcessing(true);

            const request = {
                ids: failedIds,
                approverId: user?.id || '',
                approverName: user?.realName || user?.username || '',
                comment: batchAction === 'approve' ? '重试批量批准' : '重试批量拒绝'
            };

            const result = batchAction === 'approve'
                ? await archiveBatchApi.batchApprove(request)
                : await archiveBatchApi.batchReject(request);

            setBatchResult({
                success: result.success,
                failed: result.failed,
                errors: result.errors || []
            });

            loadBatches();
            loadStats();

            if (result.failed === 0) {
                message.success('重试成功');
                setBatchResultOpen(false);
            } else if (result.success === 0) {
                message.error('重试失败');
            } else {
                message.warning(`部分成功：${result.success}条成功，${result.failed}条失败`);
            }
        } catch (err: any) {
            message.error(err.message || '重试失败，请重试');
        } finally {
            setBatchProcessing(false);
        }
    };

    const handleSelectAll = () => {
        const allIds = batches.map(b => b.id);
        const result = selectAll(allIds);
        if (!result.success) {
            message.warning(result.reason || '全选失败');
        }
    };

    // 获取选中的记录列表
    const selectedRecords = useMemo<ApprovalRecord[]>(() => {
        return batches
            .filter(b => selectedIds.has(b.id))
            .map(b => ({
                id: b.id,
                title: b.batchNo,
                code: b.batchNo
            }));
    }, [batches, selectedIds]);

    // 表格列
    const columns: ColumnsType<ArchiveBatch> = [
        {
            title: '批次编号',
            dataIndex: 'batchNo',
            key: 'batchNo',
            width: 140,
            render: (text, record) => (
                <a onClick={() => handleViewDetail(record)}>{text}</a>
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
                            onClick={() => handleViewDetail(record)}
                        />
                    </Tooltip>

                    {record.status === 'PENDING' && (
                        <>
                            <Button
                                type="primary"
                                size="small"
                                onClick={() => handleSubmit(record.id)}
                            >
                                提交校验
                            </Button>
                            <Popconfirm
                                title="确定删除此批次？"
                                onConfirm={() => handleDelete(record.id)}
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
                                onClick={() => openApprovalModal(record, 'approve')}
                            >
                                通过
                            </Button>
                            <Button
                                size="small"
                                danger
                                icon={<CloseCircleOutlined />}
                                onClick={() => openApprovalModal(record, 'reject')}
                            >
                                驳回
                            </Button>
                        </>
                    )}

                    {record.status === 'APPROVED' && (
                        <Popconfirm
                            title="确定执行归档？归档后数据将不可修改。"
                            onConfirm={() => handleArchive(record.id)}
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
        <div className="p-6">
            {/* 统计卡片 */}
            <Row gutter={16} className="mb-6">
                <Col span={4}>
                    <Card>
                        <Statistic title="总批次" value={stats.total} />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic
                            title="待提交"
                            value={stats.byStatus?.PENDING || 0}
                            valueStyle={{ color: '#999' }}
                        />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic
                            title="待审批"
                            value={stats.byStatus?.VALIDATING || 0}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic
                            title="待归档"
                            value={stats.byStatus?.APPROVED || 0}
                            valueStyle={{ color: '#faad14' }}
                        />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic
                            title="已归档"
                            value={stats.byStatus?.ARCHIVED || 0}
                            valueStyle={{ color: '#52c41a' }}
                        />
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic
                            title="已驳回"
                            value={stats.byStatus?.REJECTED || 0}
                            valueStyle={{ color: '#ff4d4f' }}
                        />
                    </Card>
                </Col>
            </Row>

            {/* 批次列表 */}
            <Card
                title="归档批次管理"
                extra={
                    <Space>
                        <Select
                            placeholder="状态筛选"
                            allowClear
                            style={{ width: 120 }}
                            value={statusFilter}
                            onChange={setStatusFilter}
                            options={Object.entries(STATUS_CONFIG).map(([value, config]) => ({
                                value,
                                label: config.text,
                            }))}
                        />
                        <Button icon={<ReloadOutlined />} onClick={loadBatches}>
                            刷新
                        </Button>
                        <Button
                            type="primary"
                            icon={<PlusOutlined />}
                            onClick={() => setCreateModalVisible(true)}
                        >
                            创建批次
                        </Button>
                    </Space>
                }
            >
                {/* 批量操作工具栏 */}
                <BatchOperationBar
                    selectedCount={getSelectedCount()}
                    totalCount={batches.length}
                    onBatchApprove={handleBatchApprove}
                    onBatchReject={handleBatchReject}
                    onSelectAll={handleSelectAll}
                    onClear={clearSelection}
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
                        onChange: (p, s) => {
                            setPage(p);
                            setPageSize(s);
                        },
                    }}
                />
            </Card>

            {/* 创建批次弹窗 */}
            <Modal
                title="创建归档批次"
                open={createModalVisible}
                onCancel={() => setCreateModalVisible(false)}
                onOk={() => form.submit()}
            >
                <Form form={form} layout="vertical" onFinish={handleCreate}>
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
                onCancel={() => setDetailModalVisible(false)}
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
                                                <Alert type="error" message={selectedBatch.errorMessage} />
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
                                                        loadAvailableVouchers();
                                                        setAddVoucherModalVisible(true);
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
                                            message={`检测结果: ${integrityReport.overallResult === 'PASS' ? '通过' : '未通过'}`}
                                            className="mb-4"
                                        />
                                        {integrityReport.checks.map((check, idx) => (
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
                                            onClick={() => handleIntegrityCheck(selectedBatch.id)}
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
                onCancel={() => setApprovalModalVisible(false)}
                onOk={handleApproval}
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
                        onChange={(e) => setApprovalComment(e.target.value)}
                        placeholder={approvalAction === 'approve' ? '请输入审批意见...' : '请输入驳回原因...'}
                    />
                </Form.Item>
            </Modal>

            {/* 添加凭证弹窗 */}
            <Modal
                title="选择凭证添加到批次"
                open={addVoucherModalVisible}
                onCancel={() => {
                    setAddVoucherModalVisible(false);
                    setSelectedVoucherIds([]);
                }}
                onOk={handleAddVouchersToBatch}
                okText="确认添加"
                okButtonProps={{ disabled: selectedVoucherIds.length === 0 }}
                width={700}
            >
                <div className="mb-4">
                    <Alert
                        type="info"
                        message={`已选择 ${selectedVoucherIds.length} 条凭证`}
                        showIcon
                    />
                </div>
                {loadingVouchers ? (
                    <div className="text-center py-8">
                        <Spin tip="加载中..." />
                    </div>
                ) : availableVouchers.length === 0 ? (
                    <Empty description='暂无可添加的凭证（需要状态为"准备归档"的凭证）' />
                ) : (
                    <Table
                        rowSelection={{
                            type: 'checkbox',
                            selectedRowKeys: selectedVoucherIds,
                            onChange: (keys) => setSelectedVoucherIds(keys as number[]),
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
                selectedCount={getSelectedCount()}
                action={batchAction}
                onConfirm={handleBatchConfirm}
                onCancel={() => setBatchDialogOpen(false)}
                selectedRecords={selectedRecords}
                loading={batchProcessing}
            />

            {/* 批量操作结果弹窗 */}
            <BatchResultModal
                visible={batchResultOpen}
                successCount={batchResult.success}
                failedCount={batchResult.failed}
                errors={batchResult.errors}
                onRetry={handleBatchRetry}
                onClose={() => setBatchResultOpen(false)}
                operationType="approval"
                isRetrying={batchProcessing}
            />
        </div>
    );
};

export default ArchiveBatchView;
