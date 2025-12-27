// Input: React、Ant Design、匹配 API
// Output: VoucherMatchingView 组件
// Pos: src/pages/matching/VoucherMatchingView.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
    Card, Button, Tag, Table, Space, Alert, Progress,
    Modal, message, Spin, Empty
} from 'antd';
import {
    CheckCircleOutlined, ExclamationCircleOutlined,
    ClockCircleOutlined, SyncOutlined, FileSearchOutlined
} from '@ant-design/icons';
import { matchingApi, MatchResult, LinkResult, ScoredCandidate } from '../../api/matching';
import './VoucherMatchingView.css';

interface VoucherMatchingViewProps {
    voucherId?: string; // 可选，页面级使用时可能从 URL 或其他来源获取
    voucherNo?: string;
    onMatchComplete?: (result: MatchResult) => void;
}

const VoucherMatchingView: React.FC<VoucherMatchingViewProps> = ({
    voucherId,
    voucherNo,
    onMatchComplete
}) => {
    const [loading, setLoading] = useState(false);
    const [matching, setMatching] = useState(false);
    const [result, setResult] = useState<MatchResult | null>(null);
    const [candidateModal, setCandidateModal] = useState<{
        visible: boolean;
        link: LinkResult | null;
    }>({ visible: false, link: null });

    // 用于跟踪组件是否已卸载，防止内存泄漏
    const mountedRef = useRef(true);
    const pollingTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    // 清理轮询定时器
    const clearPollingTimeout = useCallback(() => {
        if (pollingTimeoutRef.current) {
            clearTimeout(pollingTimeoutRef.current);
            pollingTimeoutRef.current = null;
        }
    }, []);

    // 组件卸载时清理
    useEffect(() => {
        mountedRef.current = true;
        return () => {
            mountedRef.current = false;
            clearPollingTimeout();
        };
    }, [clearPollingTimeout]);

    // 加载已有匹配结果
    const loadExistingResult = useCallback(async () => {
        if (!mountedRef.current) return;
        if (!voucherId) return;
        setLoading(true);
        try {
            const existingResult = await matchingApi.getMatchResult(voucherId);
            if (!mountedRef.current) return;
            if (existingResult) {
                setResult(existingResult);
            }
        } catch (error) {
            console.error('Failed to load match result:', error);
        } finally {
            if (mountedRef.current) {
                setLoading(false);
            }
        }
    }, [voucherId]);

    useEffect(() => {
        loadExistingResult();
    }, [loadExistingResult]);

    // 执行匹配
    const handleMatch = useCallback(async () => {
        if (!voucherId) {
            message.warning('请先选择一个凭证');
            return;
        }
        setMatching(true);
        clearPollingTimeout();

        try {
            const task = await matchingApi.executeMatch(voucherId);

            // 如果任务已同步完成（或 Mock 返回），直接处理，不进行轮询
            if (task.status === 'COMPLETED' || task.status === 'ERROR') {
                if (task.status === 'ERROR') {
                    message.error(task.message || '匹配失败');
                } else {
                    if (task.message) {
                        message.info(task.message);
                    } else {
                        message.success('匹配完成');
                    }
                    // 如果有结果对象，也可以在这里设置，但目前 executeMatch 只返回 task
                    loadExistingResult();
                }
                setMatching(false);
                return;
            }

            // 轮询等待结果
            const pollResult = async () => {
                if (!mountedRef.current) return;

                try {
                    const taskResult = await matchingApi.getTaskResult(task.taskId);
                    if (!mountedRef.current) return;

                    if (taskResult.status === 'COMPLETED') {
                        if (taskResult.result) {
                            setResult(taskResult.result);
                            onMatchComplete?.(taskResult.result);
                        }
                        message.success('匹配完成');
                        setMatching(false);
                    } else if (taskResult.status === 'ERROR') {
                        message.error(taskResult.message || '匹配失败');
                        setMatching(false);
                    } else {
                        // 继续轮询
                        pollingTimeoutRef.current = setTimeout(pollResult, 1000);
                    }
                } catch {
                    if (mountedRef.current) {
                        message.error('查询任务状态失败');
                        setMatching(false);
                    }
                }
            };

            pollingTimeoutRef.current = setTimeout(pollResult, 500);
        } catch {
            message.error('执行匹配失败');
            setMatching(false);
        }
    }, [voucherId, onMatchComplete, clearPollingTimeout, loadExistingResult]);

    // 确认关联
    const handleConfirm = useCallback(async (link: LinkResult, candidate: ScoredCandidate) => {
        if (!voucherId) return;
        try {
            await matchingApi.confirmMatch(voucherId, [{
                sourceDocId: candidate.docId,
                evidenceRole: link.evidenceRole,
                linkType: link.linkType
            }]);
            message.success('关联确认成功');
            setCandidateModal({ visible: false, link: null });
            loadExistingResult();
        } catch {
            message.error('确认失败');
        }
    }, [voucherId, loadExistingResult]);

    // 渲染状态标签
    const renderStatusTag = (status: string) => {
        const config: Record<string, { color: string; icon: React.ReactNode; text: string }> = {
            MATCHED: { color: 'success', icon: <CheckCircleOutlined />, text: '已匹配' },
            PENDING: { color: 'warning', icon: <ExclamationCircleOutlined />, text: '待补证' },
            NEED_CONFIRM: { color: 'processing', icon: <ClockCircleOutlined />, text: '需确认' },
            MISSING: { color: 'error', icon: <ExclamationCircleOutlined />, text: '缺失' },
            PROCESSING: { color: 'processing', icon: <SyncOutlined spin />, text: '处理中' },
        };
        const cfg = config[status] || { color: 'default', icon: null, text: status };
        return <Tag color={cfg.color} icon={cfg.icon}>{cfg.text}</Tag>;
    };

    // 渲染关联类型标签
    const renderLinkTypeTag = (linkType: string) => {
        const config: Record<string, { color: string; text: string }> = {
            MUST_LINK: { color: 'red', text: '必关联' },
            SHOULD_LINK: { color: 'orange', text: '应关联' },
            MAY_LINK: { color: 'blue', text: '可关联' },
        };
        const cfg = config[linkType] || { color: 'default', text: linkType };
        return <Tag color={cfg.color}>{cfg.text}</Tag>;
    };

    // 渲染单个关联结果
    const renderLinkResult = (link: LinkResult) => (
        <Card
            key={link.evidenceRole}
            size="small"
            title={
                <Space>
                    {link.evidenceRoleName}
                    {renderLinkTypeTag(link.linkType)}
                    {renderStatusTag(link.status)}
                </Space>
            }
            extra={
                link.status === 'NEED_CONFIRM' && link.candidates?.length ? (
                    <Button
                        type="link"
                        onClick={() => setCandidateModal({ visible: true, link })}
                    >
                        选择候选
                    </Button>
                ) : null
            }
        >
            {link.status === 'MATCHED' && (
                <div className="link-matched">
                    <div className="matched-doc">
                        <FileSearchOutlined /> {link.matchedDocNo}
                    </div>
                    <div className="match-score">
                        <Progress
                            percent={link.score || 0}
                            size="small"
                            format={percent => `${percent}分`}
                        />
                    </div>
                    {link.reasons?.length ? (
                        <div className="match-reasons">
                            {link.reasons.map((reason, i) => (
                                <Tag key={i} color="green">{reason}</Tag>
                            ))}
                        </div>
                    ) : null}
                </div>
            )}
            {link.status === 'MISSING' && (
                <Alert
                    type="warning"
                    message={link.suggestion || `缺少${link.evidenceRoleName}`}
                    showIcon
                />
            )}
            {link.status === 'NEED_CONFIRM' && (
                <Alert
                    type="info"
                    message={`有 ${link.candidates?.length || 0} 个候选文档，请选择确认`}
                    showIcon
                />
            )}
        </Card>
    );

    // 渲染候选选择弹窗
    const renderCandidateModal = () => {
        const { link } = candidateModal;
        if (!link) return null;

        const columns = [
            { title: '单据号', dataIndex: 'docNo', key: 'docNo' },
            { title: '类型', dataIndex: 'docTypeName', key: 'docTypeName' },
            { title: '日期', dataIndex: 'docDate', key: 'docDate' },
            {
                title: '金额',
                dataIndex: 'amount',
                key: 'amount',
                render: (v: number) => `¥${v?.toLocaleString() || '-'}`
            },
            { title: '交易对手', dataIndex: 'counterparty', key: 'counterparty' },
            {
                title: '评分',
                dataIndex: 'score',
                key: 'score',
                render: (v: number) => <Progress percent={v} size="small" />
            },
            {
                title: '操作',
                key: 'action',
                render: (_: unknown, record: ScoredCandidate) => (
                    <Button
                        type="primary"
                        size="small"
                        onClick={() => handleConfirm(link, record)}
                    >
                        确认
                    </Button>
                )
            }
        ];

        return (
            <Modal
                title={`选择 ${link.evidenceRoleName}`}
                open={candidateModal.visible}
                onCancel={() => setCandidateModal({ visible: false, link: null })}
                footer={null}
                width={900}
            >
                <Table
                    dataSource={link.candidates}
                    columns={columns}
                    rowKey="docId"
                    size="small"
                    pagination={false}
                />
            </Modal>
        );
    };

    if (loading) {
        return <Spin tip="加载中..." />;
    }

    return (
        <div className="voucher-matching-view">
            <Card
                title={
                    <Space>
                        <FileSearchOutlined />
                        智能关联
                        {voucherNo && <Tag>{voucherNo}</Tag>}
                    </Space>
                }
                extra={
                    <Button
                        type="primary"
                        icon={<SyncOutlined spin={matching} />}
                        loading={matching}
                        onClick={handleMatch}
                    >
                        {result ? '重新匹配' : '开始匹配'}
                    </Button>
                }
            >
                {!result ? (
                    <Empty description='暂无匹配结果，点击「开始匹配」执行智能关联' />
                ) : (
                    <>
                        {/* 匹配摘要 */}
                        <div className="match-summary">
                            <Space size="large">
                                <span>业务场景: <Tag color="blue">{result.scene}</Tag></span>
                                <span>状态: {renderStatusTag(result.status)}</span>
                                {result.confidence && (
                                    <span>置信度: {(result.confidence * 100).toFixed(0)}%</span>
                                )}
                            </Space>
                        </div>

                        {/* 缺失警告 */}
                        {result.missingDocs?.length ? (
                            <Alert
                                type="warning"
                                message={`缺少必关联文档: ${result.missingDocs.join('、')}`}
                                style={{ marginBottom: 16 }}
                                showIcon
                            />
                        ) : null}

                        {/* 关联详情 */}
                        <div className="link-results">
                            {result.links?.map(link => renderLinkResult(link))}
                        </div>
                    </>
                )}
            </Card>

            {renderCandidateModal()}
        </div>
    );
};

export default VoucherMatchingView;
// Re-trigger HMR Fri Dec 26 09:43:26 CST 2025
