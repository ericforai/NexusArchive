// Input: React、Ant Design、salesOrderApi
// Output: SalesOrderSyncPanel 组件
// Pos: 集成设置 - 销售订单同步

import React, { useState } from 'react';
import { Button, DatePicker, Select, Form, message, Alert, Statistic, Row, Col } from 'antd';
import { RotateCw, CheckCircle, XCircle } from 'lucide-react';
import { salesOrderApi, SalesOrderSyncResult } from '@/api/sales-order';
import dayjs from 'dayjs';

export interface SalesOrderSyncPanelProps {
  className?: string;
}

export const SalesOrderSyncPanel: React.FC<SalesOrderSyncPanelProps> = ({ className }) => {
  const [form] = Form.useForm();
  const [syncing, setSyncing] = useState(false);
  const [result, setResult] = useState<SalesOrderSyncResult | null>(null);

  const handleSync = async () => {
    const values = await form.validateFields();
    setSyncing(true);
    setResult(null);

    try {
      const response = await salesOrderApi.sync({
        dateBegin: values.dateRange?.[0]?.format('YYYY-MM-DD HH:mm:ss'),
        dateEnd: values.dateRange?.[1]?.format('YYYY-MM-DD HH:mm:ss'),
        statusCodes: values.statusCodes,
        agentId: values.agentId,
      });

      setResult(response.data);
      const { success: successCount, failed, skipped } = response.data;
      if (failed > 0) {
        message.warning(`同步完成！成功 ${successCount} 条，失败 ${failed} 条，跳过 ${skipped} 条`);
      } else {
        message.success(`同步完成！成功 ${successCount} 条，跳过 ${skipped} 条`);
      }
    } catch (error: unknown) {
      const errMsg = error instanceof Error ? error.message : '未知错误';
      message.error(`同步失败: ${errMsg}`);
    } finally {
      setSyncing(false);
    }
  };

  return (
    <div className={className}>
      <Form form={form} layout="vertical">
        <Form.Item
          label="日期范围"
          name="dateRange"
          initialValue={[dayjs().subtract(7, 'day'), dayjs()]}
        >
          <DatePicker.RangePicker showTime />
        </Form.Item>

        <Form.Item label="订单状态" name="statusCodes">
          <Select mode="multiple" placeholder="选择订单状态（默认全部）" allowClear>
            <Select.Option value="ENDORDER">已完成</Select.Option>
            <Select.Option value="DELIVERY_PART">部分发货</Select.Option>
            <Select.Option value="CONFIRMORDER">开立</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item>
          <Button
            type="primary"
            icon={<RotateCw size={16} />}
            onClick={handleSync}
            loading={syncing}
          >
            开始同步
          </Button>
        </Form.Item>
      </Form>

      {result && (
        <div className="mt-4">
          <h4>同步结果</h4>
          <Row gutter={16} className="mb-4">
            <Col span={6}>
              <Statistic title="总计" value={result.total} />
            </Col>
            <Col span={6}>
              <Statistic
                title="成功"
                value={result.success}
                styles={{ content: { color: '#52c41a' } }}
                prefix={<CheckCircle size={14} />}
              />
            </Col>
            <Col span={6}>
              <Statistic
                title="失败"
                value={result.failed}
                styles={{ content: { color: result.failed > 0 ? '#ff4d4f' : undefined } }}
                prefix={<XCircle size={14} />}
              />
            </Col>
            <Col span={6}>
              <Statistic title="跳过" value={result.skipped} />
            </Col>
          </Row>

          {result.errors.length > 0 && (
            <Alert
              type="warning"
              title="部分订单同步失败"
              description={
                <ul className="m-0 pl-5">
                  {result.errors.slice(0, 10).map((err, idx) => (
                    <li key={idx}>
                      {err.orderCode}: {err.reason}
                    </li>
                  ))}
                  {result.errors.length > 10 && (
                    <li>...还有 {result.errors.length - 10} 条错误</li>
                  )}
                </ul>
              }
              showIcon
            />
          )}
        </div>
      )}
    </div>
  );
};
