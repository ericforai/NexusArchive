// Input: React、Ant Design、匹配 API
// Output: OnboardingWizard 组件
// Pos: src/pages/matching/OnboardingWizard.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState } from 'react';
import {
    Card, Steps, Button, Progress, Alert,
    Space, Typography, Result, message
} from 'antd';
import {
    ScanOutlined, SettingOutlined, CheckCircleOutlined,
    RocketOutlined
} from '@ant-design/icons';
import { matchingApi, OnboardingSummary, AutoMappingResult } from '../../api/matching';

const { Title, Text } = Typography;

interface OnboardingWizardProps {
    companyId: number;
    onComplete?: () => void;
}

const OnboardingWizard: React.FC<OnboardingWizardProps> = ({
    companyId,
    onComplete
}) => {
    const [currentStep, setCurrentStep] = useState(0);
    const [loading, setLoading] = useState(false);
    const [summary, setSummary] = useState<OnboardingSummary | null>(null);
    const [mappingResult, setMappingResult] = useState<AutoMappingResult | null>(null);

    // Step 1: 扫描数据
    const handleScan = async () => {
        setLoading(true);
        try {
            const result = await matchingApi.scanData(companyId);
            setSummary(result);
            setCurrentStep(1);
            message.success('扫描完成');
        } catch {
            message.error('扫描失败');
        } finally {
            setLoading(false);
        }
    };

    // Step 2: 应用预置规则
    const handleApplyPreset = async () => {
        setLoading(true);
        try {
            const result = await matchingApi.applyPreset(companyId);
            setMappingResult(result);
            setCurrentStep(2);
            message.success('规则应用成功');
        } catch {
            message.error('应用失败');
        } finally {
            setLoading(false);
        }
    };

    // Step 3: 确认
    const handleConfirm = async () => {
        setLoading(true);
        try {
            await matchingApi.confirmMappings(companyId, []);
            setCurrentStep(3);
            message.success('配置完成');
            onComplete?.();
        } catch {
            message.error('确认失败');
        } finally {
            setLoading(false);
        }
    };

    const steps = [
        {
            title: '扫描数据',
            icon: <ScanOutlined />,
            content: (
                <div style={{ textAlign: 'center', padding: '40px 0' }}>
                    <RocketOutlined style={{ fontSize: 64, color: '#1890ff', marginBottom: 24 }} />
                    <Title level={4}>智能初始化向导</Title>
                    <Text type="secondary">
                        系统将自动扫描您的科目和单据类型，匹配预置规则
                    </Text>
                    <div style={{ marginTop: 32 }}>
                        <Button
                            type="primary"
                            size="large"
                            icon={<ScanOutlined />}
                            loading={loading}
                            onClick={handleScan}
                        >
                            开始扫描
                        </Button>
                    </div>
                </div>
            )
        },
        {
            title: '应用规则',
            icon: <SettingOutlined />,
            content: summary && (
                <div>
                    <Alert
                        type="info"
                        message="扫描结果"
                        description={
                            <Space direction="vertical">
                                <Text>发现 {summary.totalAccounts} 个科目，{summary.totalDocTypes} 个单据类型</Text>
                                <div>
                                    <Text>科目匹配率: </Text>
                                    <Progress
                                        percent={Math.round(summary.accountMatchRate * 100)}
                                        size="small"
                                        style={{ width: 200, display: 'inline-block' }}
                                    />
                                </div>
                                <div>
                                    <Text>单据匹配率: </Text>
                                    <Progress
                                        percent={Math.round(summary.docTypeMatchRate * 100)}
                                        size="small"
                                        style={{ width: 200, display: 'inline-block' }}
                                    />
                                </div>
                            </Space>
                        }
                        style={{ marginBottom: 24 }}
                    />
                    <div style={{ textAlign: 'center' }}>
                        <Button
                            type="primary"
                            icon={<SettingOutlined />}
                            loading={loading}
                            onClick={handleApplyPreset}
                        >
                            应用通用行业规则
                        </Button>
                    </div>
                </div>
            )
        },
        {
            title: '确认配置',
            icon: <CheckCircleOutlined />,
            content: mappingResult && (
                <div>
                    <Alert
                        type="success"
                        message={`使用预置包: ${mappingResult.kitName}`}
                        description={
                            <Space direction="vertical">
                                <Text>科目映射: {mappingResult.accountsMapped} 个（待确认: {mappingResult.accountsPending}）</Text>
                                <Text>单据映射: {mappingResult.docTypesMapped} 个（待确认: {mappingResult.docTypesPending}）</Text>
                            </Space>
                        }
                        style={{ marginBottom: 24 }}
                    />
                    <div style={{ textAlign: 'center' }}>
                        <Button
                            type="primary"
                            icon={<CheckCircleOutlined />}
                            loading={loading}
                            onClick={handleConfirm}
                        >
                            确认并完成
                        </Button>
                    </div>
                </div>
            )
        },
        {
            title: '完成',
            icon: <CheckCircleOutlined />,
            content: (
                <Result
                    status="success"
                    title="初始化完成！"
                    subTitle="智能关联规则已配置，现在可以开始使用智能凭证关联功能"
                    extra={
                        <Button type="primary" onClick={onComplete}>
                            开始使用
                        </Button>
                    }
                />
            )
        }
    ];

    return (
        <Card title="智能关联初始化向导">
            <Steps
                current={currentStep}
                items={steps.map(s => ({ title: s.title, icon: s.icon }))}
                style={{ marginBottom: 32 }}
            />
            <div className="wizard-content">
                {steps[currentStep]?.content}
            </div>
        </Card>
    );
};

export default OnboardingWizard;
