// Input: BatchStats, Ant Design
// Output: 批次统计卡片组件
// Pos: src/pages/operations/archive-batch/components/BatchStats.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { Card, Row, Col, Statistic } from 'antd';
import type { BatchStats } from '@/api/archiveBatch';

/**
 * 批次统计卡片组件 Props
 */
export interface BatchStatsProps {
    stats: BatchStats;
}

/**
 * 批次统计卡片组件
 *
 * 展示批次总数和各状态统计
 */
export function BatchStats({ stats }: BatchStatsProps) {
    return (
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
                        styles={{ content: { color: '#999' } }}
                    />
                </Card>
            </Col>
            <Col span={4}>
                <Card>
                    <Statistic
                        title="待审批"
                        value={stats.byStatus?.VALIDATING || 0}
                        styles={{ content: { color: '#1890ff' } }}
                    />
                </Card>
            </Col>
            <Col span={4}>
                <Card>
                    <Statistic
                        title="待归档"
                        value={stats.byStatus?.APPROVED || 0}
                        styles={{ content: { color: '#faad14' } }}
                    />
                </Card>
            </Col>
            <Col span={4}>
                <Card>
                    <Statistic
                        title="已归档"
                        value={stats.byStatus?.ARCHIVED || 0}
                        styles={{ content: { color: '#52c41a' } }}
                    />
                </Card>
            </Col>
            <Col span={4}>
                <Card>
                    <Statistic
                        title="已驳回"
                        value={stats.byStatus?.REJECTED || 0}
                        styles={{ content: { color: '#ff4d4f' } }}
                    />
                </Card>
            </Col>
        </Row>
    );
}

export default BatchStats;
