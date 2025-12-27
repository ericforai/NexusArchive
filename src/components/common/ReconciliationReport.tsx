// Input: React、antd、@ant-design/icons
// Output: React 组件 ReconciliationReport
// Pos: 通用复用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { Card, Table, Tag, Statistic, Row, Col, Progress, Alert, Space } from 'antd';
import { CheckCircleOutlined, ExclamationCircleOutlined } from '@ant-design/icons';

interface ReconciliationRecord {
    fiscalYear: string;
    fiscalPeriod: string;
    subjectName: string;
    erpDebitTotal: number;
    erpVoucherCount: number;
    arcDebitTotal: number;
    arcVoucherCount: number;
    attachmentCount: number;
    attachmentMissingCount: number;
    reconStatus: 'SUCCESS' | 'DISCREPANCY' | 'ERROR';
    reconMessage: string;
}

interface Props {
    record: ReconciliationRecord | null;
    loading?: boolean;
}

export const ReconciliationReport: React.FC<Props> = ({ record, loading }) => {
    if (!record) return null;

    const isSuccess = record.reconStatus === 'SUCCESS';
    const metaRate = record.arcVoucherCount > 0
        ? Math.round(((record.arcVoucherCount - record.attachmentMissingCount) / record.arcVoucherCount) * 100)
        : 0;

    return (
        <Card loading={loading} className="reconciliation-report">
            <Space direction="vertical" style={{ width: '100%' }} size="large">
                <Alert
                    message={`对账结论：${isSuccess ? '账凭证一致' : '发现业务差异'}`}
                    description={record.reconMessage}
                    type={isSuccess ? 'success' : 'warning'}
                    showIcon
                    icon={isSuccess ? <CheckCircleOutlined /> : <ExclamationCircleOutlined />}
                />

                <Row gutter={24}>
                    <Col span={8}>
                        <Card size="small" title="财务账（ERP）" bordered={false} style={{ background: '#f0f5ff' }}>
                            <Statistic title="本期合计" value={record.erpDebitTotal} precision={2} prefix="¥" />
                            <Statistic title="凭证总数" value={record.erpVoucherCount} suffix="笔" valueStyle={{ fontSize: 16 }} />
                        </Card>
                    </Col>
                    <Col span={8}>
                        <Card size="small" title="归档凭证" bordered={false} style={{ background: '#f6ffed' }}>
                            <Statistic title="入库合计" value={record.arcDebitTotal} precision={2} prefix="¥" />
                            <Statistic title="存档总数" value={record.arcVoucherCount} suffix="笔" valueStyle={{ fontSize: 16 }} />
                        </Card>
                    </Col>
                    <Col span={8}>
                        <Card size="small" title="原始证据" bordered={false} style={{ background: '#fff7e6' }}>
                            <Statistic title="附件总计" value={record.attachmentCount} suffix="个" />
                            <div style={{ marginTop: 8 }}>
                                <span style={{ fontSize: 12, color: '#8c8c8c' }}>证据覆盖率</span>
                                <Progress percent={metaRate} size="small" status={metaRate < 100 ? 'exception' : 'success'} />
                            </div>
                        </Card>
                    </Col>
                </Row>

                <Table
                    size="small"
                    pagination={false}
                    dataSource={[
                        { key: 'amount', item: '发生额总计', erp: `¥${record.erpDebitTotal.toFixed(2)}`, arc: `¥${record.arcDebitTotal.toFixed(2)}`, status: Math.abs(record.erpDebitTotal - record.arcDebitTotal) < 0.01 },
                        { key: 'count', item: '凭证总笔数', erp: record.erpVoucherCount, arc: record.arcVoucherCount, status: record.erpVoucherCount === record.arcVoucherCount },
                        { key: 'file', item: '原始附件完整性', erp: '-', arc: `${record.arcVoucherCount - record.attachmentMissingCount} / ${record.arcVoucherCount}`, status: record.attachmentMissingCount === 0 },
                    ]}
                    columns={[
                        { title: '对账项目', dataIndex: 'item', key: 'item' },
                        { title: 'ERP数据（账）', dataIndex: 'erp', key: 'erp' },
                        { title: '系统归档（凭证）', dataIndex: 'arc', key: 'arc' },
                        {
                            title: '结果',
                            dataIndex: 'status',
                            key: 'status',
                            render: (status: boolean) => (
                                <Tag color={status ? 'green' : 'red'}>{status ? '一致' : '偏差'}</Tag>
                            )
                        }
                    ]}
                />
            </Space>
        </Card>
    );
};
