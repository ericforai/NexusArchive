// Input: React、Ant Design（Spin tip 嵌套）、匹配 API
// Output: ComplianceReport 组件
// Pos: src/pages/matching/ComplianceReport.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback } from 'react';
import {
    Card, Table, Tag, Button, Space, Statistic, Row, Col,
    DatePicker, message, Spin, Alert
} from 'antd';
import {
    WarningOutlined, CheckCircleOutlined,
    DownloadOutlined, ReloadOutlined
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import type { Dayjs } from 'dayjs';
import { matchingApi, ComplianceStats, MissingDocRecord } from '../../api/matching';

const { RangePicker } = DatePicker;

const ComplianceReport: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [exporting, setExporting] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);
    const [stats, setStats] = useState<ComplianceStats>({
        totalVouchers: 0,
        matchedVouchers: 0,
        pendingVouchers: 0,
        missingMustLink: 0,
        missingShouldLink: 0,
        complianceRate: 0
    });
    const [missingDocs, setMissingDocs] = useState<MissingDocRecord[]>([]);

    // 加载报告数据
    const loadReport = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const startDate = dateRange?.[0]?.format('YYYY-MM-DD');
            const endDate = dateRange?.[1]?.format('YYYY-MM-DD');
            const report = await matchingApi.getComplianceReport(startDate, endDate);
            setStats(report.stats);
            setMissingDocs(report.missingDocs);
        } catch (err) {
            const errorMessage = err instanceof Error ? err.message : '加载报告失败';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    }, [dateRange]);

    // 初始加载
    useEffect(() => {
        loadReport();
    }, [loadReport]);

    // 日期变更时重新加载
    const handleDateChange = (dates: any, dateStrings: [string, string]) => {
        setDateRange(dates);
    };

    // 导出 Excel
    const handleExport = async () => {
        setExporting(true);
        try {
            const startDate = dateRange?.[0]?.format('YYYY-MM-DD');
            const endDate = dateRange?.[1]?.format('YYYY-MM-DD');
            const blob = await matchingApi.exportMissingDocs(startDate, endDate);

            // 创建下载链接
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `合规报告_${startDate || 'all'}_${endDate || 'all'}.xlsx`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            message.success('导出成功');
        } catch {
            message.error('导出失败');
        } finally {
            setExporting(false);
        }
    };

    const columns: ColumnsType<MissingDocRecord> = [
        {
            title: '凭证号',
            dataIndex: 'voucherNo',
            key: 'voucherNo',
        },
        {
            title: '业务场景',
            dataIndex: 'scene',
            key: 'scene',
            render: (scene: string) => {
                const sceneNames: Record<string, string> = {
                    PAYMENT: '付款',
                    RECEIPT: '收款',
                    REIMBURSEMENT: '报销',
                    PURCHASE_ENTRY: '采购入库',
                    SALES_INVOICE: '销售开票',
                    SALARY_PAYMENT: '工资发放',
                    TAX_PAYMENT: '税费缴纳',
                };
                return sceneNames[scene] || scene;
            }
        },
        {
            title: '缺失单据',
            dataIndex: 'missingDocType',
            key: 'missingDocType',
        },
        {
            title: '关联类型',
            dataIndex: 'linkType',
            key: 'linkType',
            render: (type: string) => (
                <Tag color={type === 'MUST_LINK' ? 'red' : 'orange'}>
                    {type === 'MUST_LINK' ? '必关联' : '应关联'}
                </Tag>
            )
        },
        {
            title: '风险等级',
            dataIndex: 'riskLevel',
            key: 'riskLevel',
            render: (level: string) => {
                const config: Record<string, { color: string; text: string }> = {
                    HIGH: { color: 'red', text: '高' },
                    MEDIUM: { color: 'orange', text: '中' },
                    LOW: { color: 'green', text: '低' },
                };
                const cfg = config[level];
                return <Tag color={cfg?.color}>{cfg?.text}</Tag>;
            }
        },
        {
            title: '凭证日期',
            dataIndex: 'createTime',
            key: 'createTime',
        },
        {
            title: '操作',
            key: 'action',
            render: () => (
                <Button type="link" size="small">补证</Button>
            )
        }
    ];

    if (loading && missingDocs.length === 0) {
        return (
            <Spin tip="加载报告...">
                <div style={{ minHeight: 120 }} />
            </Spin>
        );
    }

    return (
        <div className="compliance-report">
            <Card title="凭证关联合规报告" extra={
                <Space>
                    <RangePicker
                        value={dateRange}
                        onChange={handleDateChange}
                        allowClear
                    />
                    <Button
                        icon={<ReloadOutlined spin={loading} />}
                        onClick={loadReport}
                        loading={loading}
                    >
                        刷新
                    </Button>
                    <Button
                        icon={<DownloadOutlined />}
                        onClick={handleExport}
                        loading={exporting}
                    >
                        导出 Excel
                    </Button>
                </Space>
            }>
                {error && (
                    <Alert
                        type="error"
                        title={error}
                        closable
                        onClose={() => setError(null)}
                        style={{ marginBottom: 16 }}
                    />
                )}

                {/* 统计卡片 */}
                <Row gutter={16} style={{ marginBottom: 24 }}>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="总凭证数"
                                value={stats.totalVouchers}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="已匹配"
                                value={stats.matchedVouchers}
                                styles={{ content: { color: '#3f8600' } }}
                                prefix={<CheckCircleOutlined />}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="待补证"
                                value={stats.pendingVouchers}
                                styles={{ content: { color: '#cf1322' } }}
                                prefix={<WarningOutlined />}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="缺必关联"
                                value={stats.missingMustLink}
                                styles={{ content: { color: '#cf1322' } }}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="缺应关联"
                                value={stats.missingShouldLink}
                                styles={{ content: { color: '#faad14' } }}
                            />
                        </Card>
                    </Col>
                    <Col span={4}>
                        <Card>
                            <Statistic
                                title="合规率"
                                value={stats.complianceRate}
                                suffix="%"
                                styles={{ content: { color: stats.complianceRate >= 95 ? '#3f8600' : '#faad14' } }}
                            />
                        </Card>
                    </Col>
                </Row>

                {/* 缺失清单 */}
                <Card title="待补证清单" size="small">
                    <Table
                        columns={columns}
                        dataSource={missingDocs}
                        rowKey="voucherId"
                        size="small"
                        loading={loading}
                        pagination={{ pageSize: 10 }}
                    />
                </Card>
            </Card>
        </div>
    );
};

export default ComplianceReport;
