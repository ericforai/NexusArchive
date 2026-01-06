// Input: React、Ant Design
// Output: ManualArchiveModal 组件
// Pos: src/pages/archives/components/ManualArchiveModal.tsx - 手动归档弹窗

/**
 * 手动归档弹窗组件
 *
 * 用于处理长期未关联的文件，允许用户补录元数据后直接归档。
 * 符合 DA/T 94-2022 元数据完整性要求。
 */

import React, { useEffect } from 'react';
import { Modal, Form, Input, Select, DatePicker, message, Spin } from 'antd';
import type { Archive } from '@/api/archives';
import { archivesApi } from '@/api/archives';

interface ManualArchiveModalProps {
  visible: boolean;
  archive?: Partial<Archive>;
  onSuccess?: (archive: Archive) => void;
  onCancel?: () => void;
}

interface ManualArchiveFormData {
  title: string;
  voucherNo: string;
  voucherWord?: string;
  docDate?: string;
  amount?: string;
  summary?: string;
}

export const ManualArchiveModal: React.FC<ManualArchiveModalProps> = ({
  visible,
  archive,
  onSuccess,
  onCancel,
}) => {
  const [form] = Form.useForm<ManualArchiveFormData>();
  const [loading, setLoading] = React.useState(false);

  // 当 archive 变化时，更新表单默认值
  useEffect(() => {
    if (archive && visible) {
      form.setFieldsValue({
        title: archive.title || archive.fileName || '',
        voucherNo: archive.erpVoucherNo || '',
        voucherWord: archive.voucherWord || '',
        docDate: archive.docDate,
        amount: archive.amount?.toString(),
        summary: archive.summary || '',
      });
    }
  }, [archive, visible, form]);

  const handleSubmit = async () => {
    if (!archive?.id) {
      message.error('档案ID不存在');
      return;
    }

    try {
      const values = await form.validateFields();
      setLoading(true);

      // 调用更新档案接口
      await archivesApi.updateArchive(archive.id, {
        title: values.title,
        erpVoucherNo: values.voucherNo,
        voucherWord: values.voucherWord,
        docDate: values.docDate,
        amount: values.amount ? parseFloat(values.amount) : undefined,
        summary: values.summary,
        status: 'PENDING_ARCHIVE', // 更新为准备归档状态
      });

      message.success('元数据已更新，可以提交归档');
      onSuccess?.({ ...archive, ...values } as Archive);
      handleCancel();
    } catch (error: any) {
      console.error('手动归档失败:', error);
      message.error(error?.message || '操作失败，请重试');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel?.();
  };

  return (
    <Modal
      title="手动归档"
      open={visible}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      destroyOnClose
    >
      <Spin spinning={loading}>
        <Form
          form={form}
          layout="vertical"
          className="mt-4"
        >
          <Form.Item
            label="题名"
            name="title"
            rules={[{ required: true, message: '请输入题名' }]}
          >
            <Input placeholder="输入档案题名" />
          </Form.Item>

          <div className="grid grid-cols-2 gap-4">
            <Form.Item label="凭证字" name="voucherWord">
              <Select
                placeholder="选择凭证字"
                allowClear
                options={[
                  { label: '记', value: '记' },
                  { label: '收', value: '收' },
                  { label: '付', value: '付' },
                  { label: '转', value: '转' },
                ]}
              />
            </Form.Item>

            <Form.Item label="凭证号" name="voucherNo">
              <Input placeholder="如：001" />
            </Form.Item>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <Form.Item label="业务日期" name="docDate">
              <Input type="date" />
            </Form.Item>

            <Form.Item label="金额" name="amount">
              <Input type="number" placeholder="输入金额" />
            </Form.Item>
          </div>

          <Form.Item label="摘要" name="summary">
            <Input.TextArea
              rows={3}
              placeholder="输入档案摘要"
              maxLength={500}
              showCount
            />
          </Form.Item>

          <div className="bg-amber-50 dark:bg-amber-900/20 p-3 rounded text-sm text-amber-700 dark:text-amber-300">
            <p className="font-medium mb-1">💡 提示：</p>
            <ul className="list-disc list-inside space-y-1 text-xs">
              <li>补录元数据后，档案状态将更新为"准备归档"</li>
              <li>请在提交前确认信息准确无误</li>
              <li>提交后可在归档管理页面查看</li>
            </ul>
          </div>
        </Form>
      </Spin>
    </Modal>
  );
};

export default ManualArchiveModal;
