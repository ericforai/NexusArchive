// src/components/voucher/VoucherMetadata.tsx
import React from 'react';
import { formatCurrency, formatDate } from './styles';

interface VoucherEntryDTO {
  lineNo?: number;
  summary?: string;
  accountCode?: string;
  accountName?: string;
  debit?: number | string;
  credit?: number | string;
}

interface AttachmentDTO {
  id: string;
  type?: string;
  name: string;
}

interface VoucherDTO {
  id: string;
  voucherId?: string;
  voucherNo: string;
  voucherWord?: string;
  voucherDate?: string;
  summary?: string;
  debitTotal?: number | string;
  creditTotal?: number | string;
  createdTime?: string;
  orgName?: string;
  attachments?: AttachmentDTO[];
}

interface MetadataField {
  label: string;
  value: string | number | React.ReactNode;
  type?: 'text' | 'amount' | 'date' | 'link';
}

interface VoucherMetadataProps {
  data: VoucherDTO;
  compact?: boolean;
  fields?: string[];
}

// 默认显示字段配置
const DEFAULT_FIELDS: string[] = [
  'voucherNo',
  'debitTotal',
  'voucherDate',
  'invoiceCount',
  'contract',
  'matchScore',
  'matchMethod',
  'createdTime',
  'storageId',
];

export const VoucherMetadata: React.FC<VoucherMetadataProps> = ({
  data,
  compact = false,
  fields,
}) => {
  // 构建字段列表
  const metadataFields: MetadataField[] = React.useMemo(() => {
    const result: MetadataField[] = [];

    // 记账凭证号
    if (shouldIncludeField('voucherNo')) {
      const voucherNumber = data.voucherWord && data.voucherNo
        ? `${data.voucherWord}-${data.voucherNo}`
        : (data.voucherNo || '-');
      result.push({
        label: '记账凭证号',
        value: voucherNumber,
        type: 'text',
      });
    }

    // 金额
    if (shouldIncludeField('debitTotal')) {
      result.push({
        label: '金额',
        value: formatCurrency(data.debitTotal),
        type: 'amount',
      });
    }

    // 业务日期
    if (shouldIncludeField('voucherDate')) {
      result.push({
        label: '业务日期',
        value: formatDate(data.voucherDate),
        type: 'date',
      });
    }

    // 关联发票数
    if (shouldIncludeField('invoiceCount')) {
      const invoiceCount = data.attachments?.filter(a =>
        a.type?.toLowerCase().includes('invoice') ||
        a.name.toLowerCase().includes('发票')
      ).length || 0;
      result.push({
        label: '关联发票数',
        value: invoiceCount,
        type: 'text',
      });
    }

    // 关联合同（暂时显示占位）
    if (shouldIncludeField('contract')) {
      result.push({
        label: '关联合同',
        value: '-',
        type: 'link',
      });
    }

    // 匹配度（外部注入，暂时显示占位）
    if (shouldIncludeField('matchScore')) {
      result.push({
        label: '匹配度',
        value: '-',
        type: 'text',
      });
    }

    // 关联方式（外部注入，暂时显示占位）
    if (shouldIncludeField('matchMethod')) {
      result.push({
        label: '关联方式',
        value: '-',
        type: 'text',
      });
    }

    // 入池时间
    if (shouldIncludeField('createdTime')) {
      result.push({
        label: '入池时间',
        value: formatDate(data.createdTime),
        type: 'date',
      });
    }

    // 存储ID
    if (shouldIncludeField('storageId')) {
      const storageId = data.id || data.voucherId || '-';
      result.push({
        label: '存储ID',
        value: storageId.length > 20 ? `${storageId.slice(0, 20)}...` : storageId,
        type: 'text',
      });
    }

    return result;

    function shouldIncludeField(fieldName: string): boolean {
      if (!fields) return DEFAULT_FIELDS.includes(fieldName as any);
      return fields.includes(fieldName);
    }
  }, [data, fields]);

  return (
    <div className="bg-white border border-slate-200 rounded-lg overflow-hidden">
      <div className={`px-4 py-3 border-b border-slate-100 bg-slate-50 ${compact ? 'py-2' : ''}`}>
        <h3 className="font-semibold text-slate-700 text-sm">业务元数据</h3>
      </div>
      <div className={`divide-y divide-slate-100 ${compact ? 'text-sm' : ''}`}>
        {metadataFields.map((field, index) => (
          <div
            key={index}
            className={`flex justify-between items-center px-4 ${compact ? 'py-2' : 'py-3'}`}
          >
            <span className="text-slate-500 text-xs uppercase tracking-wide">{field.label}</span>
            <span className={`font-medium ${
              field.type === 'amount' ? 'font-mono text-slate-800' : 'text-slate-700'
            }`}>
              {field.value}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
};
