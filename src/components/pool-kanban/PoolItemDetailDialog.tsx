// src/components/pool-kanban/PoolItemDetailDialog.tsx
// Input: PoolItem object and visibility state
// Output: Modal dialog displaying pool item details
// Pos: src/components/pool-kanban/PoolItemDetailDialog.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { memo } from 'react';
import { Modal, Descriptions, Badge, Tag } from 'antd';
import { FileText, Calendar, DollarSign, Building } from 'lucide-react';
import type { PoolItem } from '@/api/pool';
import { getSubStateLabel } from '@/config/pool-columns.config';

export interface PoolItemDetailDialogProps {
  item: PoolItem | null;
  open: boolean;
  onClose: () => void;
}

/**
 * 电子凭证池项目详情对话框
 */
export const PoolItemDetailDialog = memo<PoolItemDetailDialogProps>(({
  item,
  open,
  onClose,
}) => {
  if (!item) return null;

  const statusLabel = getSubStateLabel(item.status as any);

  return (
    <Modal
      title="凭证详情"
      open={open}
      onCancel={onClose}
      footer={null}
      width={600}
    >
      <Descriptions column={1} bordered>
        <Descriptions.Item label="凭证编号">{item.code || '-'}</Descriptions.Item>
        <Descriptions.Item label="摘要">{item.summary || '-'}</Descriptions.Item>
        <Descriptions.Item label="状态">
          <Badge status="processing" text={statusLabel} />
        </Descriptions.Item>
        <Descriptions.Item label="文件名">{item.fileName || '-'}</Descriptions.Item>
        {item.amount && (
          <Descriptions.Item label="金额">
            ¥{Number(item.amount).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}
          </Descriptions.Item>
        )}
        {item.docDate && (
          <Descriptions.Item label="业务日期">{item.docDate}</Descriptions.Item>
        )}
        {item.date && (
          <Descriptions.Item label="凭证日期">{item.date}</Descriptions.Item>
        )}
        {item.source && (
          <Descriptions.Item label="来源">{item.source}</Descriptions.Item>
        )}
        {item.type && (
          <Descriptions.Item label="类型">
            <Tag>{item.type}</Tag>
          </Descriptions.Item>
        )}
        <Descriptions.Item label="ID">{item.id}</Descriptions.Item>
      </Descriptions>
    </Modal>
  );
});

PoolItemDetailDialog.displayName = 'PoolItemDetailDialog';
