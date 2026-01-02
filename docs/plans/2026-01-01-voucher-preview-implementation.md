# 凭证预览组件实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标:** 构建一个可复用的凭证预览组件，支持左侧元数据展示和右侧格式化凭证渲染，用于全景视图、会计档案、电子凭证池等多个场景。

**架构:** 组合式组件架构，拆分为 VoucherMetadata（左侧元数据）和 VoucherPreviewTabs（右侧标签容器），右侧包含 VoucherPreviewCanvas（会计凭证CSS渲染）和 OriginalDocumentPreview（PDF预览）。组件可独立使用也可组合使用。

**技术栈:** React 19, TypeScript 5.8, Tailwind CSS, Ant Design 6, Vitest

---

## 前置准备

### Task 0: 环境验证

**Step 1: 验证前端开发环境运行**

Run: `cd /Users/user/nexusarchive && npm run dev`
Expected: Vite dev server starts on http://localhost:5173

**Step 2: 验证类型定义存在**

Run: `ls -la src/types.ts`
Expected: File exists

**Step 3: 查看 VoucherDTO 数据结构**

Read: `src/types.ts` or confirm via existing usage in `src/pages/panorama/VoucherDetailCard.tsx`
Expected: VoucherDTO interface with voucherId, voucherNo, entries, etc.

---

## Phase 1: VoucherPreviewCanvas - 核心凭证渲染

### Task 1: 创建组件目录结构

**Files:**
- Create: `src/components/voucher/`
- Create: `src/components/voucher/index.ts`
- Create: `src/components/voucher/styles.ts`

**Step 1: 创建组件目录**

Run: `mkdir -p src/components/voucher`
Expected: Directory created

**Step 2: 创建入口文件 index.ts**

```typescript
// src/components/voucher/index.ts
export { VoucherPreviewCanvas } from './VoucherPreviewCanvas';
export { VoucherMetadata } from './VoucherMetadata';
export { VoucherPreviewTabs } from './VoucherPreviewTabs';
export { OriginalDocumentPreview } from './OriginalDocumentPreview';
export { VoucherPreview } from './VoucherPreview';
```

**Step 3: 创建共享样式文件 styles.ts**

```typescript
// src/components/voucher/styles.ts
import { CSSProperties } from 'react';

// 凭证表格样式
export const voucherTableStyles: Record<string, CSSProperties> = {
  container: {
    border: '1px solid #e5e7eb',
    borderRadius: '4px',
    backgroundColor: '#ffffff',
    fontFamily: 'ui-sans-serif, system-ui, sans-serif',
  },
  header: {
    padding: '12px 16px',
    borderBottom: '1px solid #e5e7eb',
    backgroundColor: '#f9fafb',
  },
  headerRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: '16px',
    fontSize: '14px',
  },
  headerItem: {
    display: 'flex',
    gap: '4px',
  },
  headerLabel: {
    color: '#6b7280',
    fontSize: '12px',
  },
  headerValue: {
    color: '#1f2937',
    fontWeight: 500,
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
    fontSize: '13px',
  },
  tableHead: {
    backgroundColor: '#f9fafb',
    borderBottom: '1px solid #e5e7eb',
  },
  tableHeadCell: {
    padding: '8px 12px',
    textAlign: 'left',
    fontWeight: 500,
    color: '#6b7280',
    fontSize: '12px',
  },
  tableHeadCellRight: {
    padding: '8px 12px',
    textAlign: 'right',
    fontWeight: 500,
    color: '#6b7280',
    fontSize: '12px',
  },
  tableRow: {
    borderBottom: '1px solid #f3f4f6',
  },
  tableCell: {
    padding: '8px 12px',
    color: '#374151',
  },
  tableCellRight: {
    padding: '8px 12px',
    textAlign: 'right',
    fontFamily: 'ui-monospace, monospace',
    color: '#374151',
  },
  tableFoot: {
    backgroundColor: '#f9fafb',
    borderTop: '1px solid #e5e7eb',
  },
  tableFootCell: {
    padding: '8px 12px',
    textAlign: 'right',
    fontWeight: 600,
    color: '#1f2937',
  },
  signatureSection: {
    padding: '12px 16px',
    borderTop: '1px solid #e5e7eb',
    display: 'flex',
    gap: '24px',
    fontSize: '12px',
  },
  signatureItem: {
    display: 'flex',
    gap: '4px',
  },
  signatureLabel: {
    color: '#6b7280',
  },
  signatureValue: {
    color: '#1f2937',
  },
  // 紧凑模式覆盖
  compact: {
    fontSize: '12px',
  },
  compactPadding: {
    padding: '8px 12px',
  },
};

// 数字格式化
export const formatCurrency = (amount: number | string | undefined | null): string => {
  if (amount === undefined || amount === null || amount === '') return '-';
  const num = typeof amount === 'string' ? parseFloat(amount) : amount;
  if (isNaN(num)) return '-';
  return `¥ ${num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
};

// 日期格式化
export const formatDate = (date: string | undefined | null): string => {
  if (!date) return '-';
  try {
    const d = new Date(date);
    if (isNaN(d.getTime())) return '-';
    return d.toISOString().split('T')[0];
  } catch {
    return '-';
  }
};

// 数字转中文大写
export const numberToChinese = (num: number | string | undefined | null): string => {
  if (num === undefined || num === null || num === '') return '零元整';
  const n = typeof num === 'string' ? parseFloat(num) : num;
  if (isNaN(n)) return '零元整';
  if (n === 0) return '零元整';

  const digits = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'];
  const units = ['', '拾', '佰', '仟', '万', '拾', '佰', '仟', '亿'];

  const intPart = Math.floor(n);
  const decPart = Math.round((n - intPart) * 100);

  let result = '';

  // 整数部分转中文
  if (intPart > 0) {
    const intStr = intPart.toString();
    let lastZero = false;
    for (let i = 0; i < intStr.length; i++) {
      const digit = parseInt(intStr[i]);
      const pos = intStr.length - i - 1;
      if (digit === 0) {
        if (!lastZero && pos !== 0 && pos !== 4 && pos !== 8) {
          result += digits[0];
          lastZero = true;
        }
      } else {
        result += digits[digit] + units[pos];
        lastZero = false;
      }
    }
    result += '元';
  } else {
    result = '零元';
  }

  // 小数部分
  if (decPart > 0) {
    const jiao = Math.floor(decPart / 10);
    const fen = decPart % 10;
    if (jiao > 0) result += digits[jiao] + '角';
    if (fen > 0) result += digits[fen] + '分';
  } else {
    result += '整';
  }

  return result;
};
```

**Step 4: 提交初始结构**

Run: `git add src/components/voucher/ && git commit -m "feat(voucher): 创建凭证预览组件目录结构和共享样式"

---

### Task 2: 实现 VoucherPreviewCanvas 基础结构

**Files:**
- Create: `src/components/voucher/VoucherPreviewCanvas.tsx`

**Step 1: 创建组件文件**

```typescript
// src/components/voucher/VoucherPreviewCanvas.tsx
import React, { useMemo } from 'react';
import { voucherTableStyles, formatCurrency, numberToChinese, formatDate } from './styles';

// VoucherDTO 类型定义（如果 types.ts 中没有，需要先添加）
interface VoucherEntryDTO {
  lineNo?: number;
  summary?: string;
  accountCode?: string;
  accountName?: string;
  debit?: number | string;
  credit?: number | string;
}

interface VoucherDTO {
  voucherId: string;
  voucherNo: string;
  voucherWord?: string;
  voucherDate?: string;
  orgName?: string;
  accountPeriod?: string;
  accbookCode?: string;
  summary?: string;
  debitTotal?: number | string;
  creditTotal?: number | string;
  attachmentCount?: number;
  creator?: string;
  auditor?: string;
  poster?: string;
  status?: string;
  entries?: VoucherEntryDTO[];
}

interface VoucherPreviewCanvasProps {
  data: VoucherDTO;
  compact?: boolean;
  showSignature?: boolean;
}

export const VoucherPreviewCanvas: React.FC<VoucherPreviewCanvasProps> = React.memo(({
  data,
  compact = false,
  showSignature = true,
}) => {
  // 计算合计
  const { totalDebit, totalCredit, chineseAmount } = useMemo(() => {
    let debitSum = 0;
    let creditSum = 0;

    if (data.entries && data.entries.length > 0) {
      data.entries.forEach(entry => {
        const debit = typeof entry.debit === 'string' ? parseFloat(entry.debit) || 0 : (entry.debit || 0);
        const credit = typeof entry.credit === 'string' ? parseFloat(entry.credit) || 0 : (entry.credit || 0);
        debitSum += debit;
        creditSum += credit;
      });
    }

    // 如果没有分录或合计为0，使用凭证总金额
    if (debitSum === 0 && creditSum === 0) {
      const total = typeof data.debitTotal === 'string' ? parseFloat(data.debitTotal) || 0 : (data.debitTotal || 0);
      debitSum = total;
      creditSum = total;
    }

    return {
      totalDebit: debitSum,
      totalCredit: creditSum,
      chineseAmount: numberToChinese(debitSum),
    };
  }, [data.entries, data.debitTotal]);

  // 格式化凭证号
  const voucherNumber = useMemo(() => {
    const word = data.voucherWord || '';
    const no = data.voucherNo || '';
    return word && no ? `${word}-${no}` : (no || '-');
  }, [data.voucherWord, data.voucherNo]);

  // 格式化日期
  const displayDate = useMemo(() => {
    return formatDate(data.voucherDate);
  }, [data.voucherDate]);

  return (
    <div style={{
      ...voucherTableStyles.container,
      fontSize: compact ? '12px' : '13px',
    }}>
      {/* 表头区 */}
      <div style={{
        ...voucherTableStyles.header,
        ...compact && voucherTableStyles.compactPadding,
      }}>
        <div style={voucherTableStyles.headerRow}>
          <div style={voucherTableStyles.headerItem}>
            <span style={voucherTableStyles.headerLabel}>核算单位:</span>
            <span style={voucherTableStyles.headerValue}>{data.orgName || '-'}</span>
          </div>
          <div style={voucherTableStyles.headerItem}>
            <span style={voucherTableStyles.headerLabel}>日期:</span>
            <span style={voucherTableStyles.headerValue}>{displayDate}</span>
          </div>
          <div style={voucherTableStyles.headerItem}>
            <span style={voucherTableStyles.headerLabel}>凭证号:</span>
            <span style={voucherTableStyles.headerValue}>{voucherNumber}</span>
          </div>
        </div>
      </div>

      {/* 分录表格区 */}
      <table style={voucherTableStyles.table}>
        <thead style={voucherTableStyles.tableHead}>
          <tr>
            <th style={{ ...voucherTableStyles.tableHeadCell, width: '25%' }}>摘要</th>
            <th style={{ ...voucherTableStyles.tableHeadCell, width: '35%' }}>科目</th>
            <th style={{ ...voucherTableStyles.tableHeadCellRight, width: '20%' }}>借方</th>
            <th style={{ ...voucherTableStyles.tableHeadCellRight, width: '20%' }}>贷方</th>
          </tr>
        </thead>
        <tbody>
          {data.entries && data.entries.length > 0 ? (
            data.entries.map((entry, index) => (
              <tr key={entry.lineNo || index} style={voucherTableStyles.tableRow}>
                <td style={voucherTableStyles.tableCell}>{entry.summary || '-'}</td>
                <td style={voucherTableStyles.tableCell}>
                  <div>
                    <div>{entry.accountName || '-'}</div>
                    {entry.accountCode && (
                      <div style={{ fontSize: '11px', color: '#9ca3af' }}>{entry.accountCode}</div>
                    )}
                  </div>
                </td>
                <td style={voucherTableStyles.tableCellRight}>{formatCurrency(entry.debit)}</td>
                <td style={voucherTableStyles.tableCellRight}>{formatCurrency(entry.credit)}</td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={4} style={{ ...voucherTableStyles.tableCell, textAlign: 'center', color: '#9ca3af' }}>
                暂无分录数据
              </td>
            </tr>
          )}
        </tbody>
        <tfoot style={voucherTableStyles.tableFoot}>
          <tr>
            <td colSpan={2} style={{ ...voucherTableStyles.tableFootCell, textAlign: 'right' }}>
              合计
            </td>
            <td style={voucherTableStyles.tableFootCell}>{formatCurrency(totalDebit)}</td>
            <td style={voucherTableStyles.tableFootCell}>{formatCurrency(totalCredit)}</td>
          </tr>
        </tfoot>
      </table>

      {/* 合计大写 */}
      <div style={{
        padding: compact ? '8px 12px' : '12px 16px',
        borderTop: '1px solid #e5e7eb',
        fontSize: '12px',
        color: '#6b7280',
      }}>
        大写: {chineseAmount}
      </div>

      {/* 签章区 */}
      {showSignature && (
        <div style={{
          ...voucherTableStyles.signatureSection,
          ...compact && voucherTableStyles.compactPadding,
        }}>
          <div style={voucherTableStyles.signatureItem}>
            <span style={voucherTableStyles.signatureLabel}>制单人:</span>
            <span style={voucherTableStyles.signatureValue}>{data.creator || '-'}</span>
          </div>
          <div style={voucherTableStyles.signatureItem}>
            <span style={voucherTableStyles.signatureLabel}>审核人:</span>
            <span style={voucherTableStyles.signatureValue}>{data.auditor || '-'}</span>
          </div>
          <div style={voucherTableStyles.signatureItem}>
            <span style={voucherTableStyles.signatureLabel}>记账人:</span>
            <span style={voucherTableStyles.signatureValue}>{data.poster || '-'}</span>
          </div>
        </div>
      )}
    </div>
  );
});

VoucherPreviewCanvas.displayName = 'VoucherPreviewCanvas';
```

**Step 2: 提交组件实现**

Run: `git add src/components/voucher/VoucherPreviewCanvas.tsx && git commit -m "feat(voucher): 实现 VoucherPreviewCanvas 会计凭证渲染组件"

---

### Task 3: 创建 VoucherPreviewCanvas 单元测试

**Files:**
- Create: `src/components/voucher/__tests__/VoucherPreviewCanvas.test.tsx`

**Step 1: 创建测试文件**

```typescript
// src/components/voucher/__tests__/VoucherPreviewCanvas.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { VoucherPreviewCanvas } from '../VoucherPreviewCanvas';

describe('VoucherPreviewCanvas', () => {
  const mockVoucherData = {
    voucherId: 'test-001',
    voucherNo: '001',
    voucherWord: '记',
    voucherDate: '2025-01-01',
    orgName: '测试公司',
    debitTotal: 1500,
    creditTotal: 1500,
    creator: '张三',
    auditor: '李四',
    poster: '王五',
    entries: [
      {
        lineNo: 1,
        summary: '测试摘要',
        accountCode: '1001',
        accountName: '库存现金',
        debit: 1500,
        credit: 0,
      },
      {
        lineNo: 2,
        summary: '测试摘要2',
        accountCode: '1002',
        accountName: '银行存款',
        debit: 0,
        credit: 1500,
      },
    ],
  };

  it('应该渲染凭证表头信息', () => {
    render(<VoucherPreviewCanvas data={mockVoucherData} />);
    expect(screen.getByText(/测试公司/)).toBeTruthy();
    expect(screen.getByText(/2025-01-01/)).toBeTruthy();
    expect(screen.getByText(/记-001/)).toBeTruthy();
  });

  it('应该渲染分录表格', () => {
    render(<VoucherPreviewCanvas data={mockVoucherData} />);
    expect(screen.getByText(/测试摘要/)).toBeTruthy();
    expect(screen.getByText(/库存现金/)).toBeTruthy();
    expect(screen.getByText(/银行存款/)).toBeTruthy();
  });

  it('应该正确计算并显示合计', () => {
    render(<VoucherPreviewCanvas data={mockVoucherData} />);
    expect(screen.getByText(/¥ 1,500.00/)).toBeTruthy();
    expect(screen.getByText(/壹仟伍佰元整/)).toBeTruthy();
  });

  it('应该显示签章区', () => {
    render(<VoucherPreviewCanvas data={mockVoucherData} showSignature={true} />);
    expect(screen.getByText(/制单人:\s*张三/)).toBeTruthy();
    expect(screen.getByText(/审核人:\s*李四/)).toBeTruthy();
    expect(screen.getByText(/记账人:\s*王五/)).toBeTruthy();
  });

  it('应该支持紧凑模式', () => {
    const { container } = render(<VoucherPreviewCanvas data={mockVoucherData} compact={true} />);
    const canvas = container.querySelector('[style*="font-size"]');
    expect(canvas).toBeTruthy();
  });

  it('应该在无分录时显示占位文本', () => {
    const noEntriesData = { ...mockVoucherData, entries: [] };
    render(<VoucherPreviewCanvas data={noEntriesData} />);
    expect(screen.getByText(/暂无分录数据/)).toBeTruthy();
  });

  it('应该支持 memo 优化', () => {
    const { rerender } = render(<VoucherPreviewCanvas data={mockVoucherData} />);
    const initialRender = screen.getByText(/记-001/);

    rerender(<VoucherPreviewCanvas data={mockVoucherData} />);
    expect(initialRender).toBeTruthy();
  });
});
```

**Step 2: 运行测试验证**

Run: `npm run test -- VoucherPreviewCanvas`
Expected: Tests pass

**Step 3: 提交测试**

Run: `git add src/components/voucher/__tests__/ && git commit -m "test(voucher): 添加 VoucherPreviewCanvas 单元测试"

---

## Phase 2: VoucherMetadata - 元数据展示

### Task 4: 实现 VoucherMetadata 组件

**Files:**
- Create: `src/components/voucher/VoucherMetadata.tsx`

**Step 1: 创建组件文件**

```typescript
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
const DEFAULT_FIELDS: (keyof VoucherDTO | 'custom')[] = [
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
```

**Step 2: 提交组件**

Run: `git add src/components/voucher/VoucherMetadata.tsx && git commit -m "feat(voucher): 实现 VoucherMetadata 元数据展示组件"

---

### Task 5: 创建 VoucherMetadata 单元测试

**Files:**
- Create: `src/components/voucher/__tests__/VoucherMetadata.test.tsx`

**Step 1: 创建测试文件**

```typescript
// src/components/voucher/__tests__/VoucherMetadata.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { VoucherMetadata } from '../VoucherMetadata';

describe('VoucherMetadata', () => {
  const mockVoucherData = {
    id: 'test-storage-id-12345',
    voucherId: 'voucher-001',
    voucherNo: '001',
    voucherWord: '记',
    voucherDate: '2025-01-01',
    debitTotal: 1500,
    createdTime: '2025-01-01T10:00:00',
    attachments: [
      { id: '1', type: 'invoice', name: '发票1.pdf' },
      { id: '2', type: 'contract', name: '合同.pdf' },
      { id: '3', name: '其他发票.pdf' },
    ],
  };

  it('应该渲染业务元数据标题', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getByText(/业务元数据/)).toBeTruthy();
  });

  it('应该显示默认字段', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getByText(/记账凭证号/)).toBeTruthy();
    expect(screen.getByText(/金额/)).toBeTruthy();
    expect(screen.getByText(/业务日期/)).toBeTruthy();
  });

  it('应该正确格式化凭证号', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getByText(/记-001/)).toBeTruthy();
  });

  it('应该正确格式化金额', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getByText(/¥ 1,500.00/)).toBeTruthy();
  });

  it('应该正确计算关联发票数', () => {
    render(<VoucherMetadata data={mockVoucherData} />);
    expect(screen.getByText(/2/)).toBeTruthy();
  });

  it('应该支持紧凑模式', () => {
    const { container } = render(<VoucherMetadata data={mockVoucherData} compact={true} />);
    expect(container.querySelector('.text-sm')).toBeTruthy();
  });

  it('应该支持自定义字段', () => {
    render(<VoucherMetadata data={mockVoucherData} fields={['voucherNo', 'debitTotal']} />);
    expect(screen.getByText(/记账凭证号/)).toBeTruthy();
    expect(screen.getByText(/金额/)).toBeTruthy();
    // 其他字段不应该显示
    expect(screen.queryByText(/业务日期/)).toBeFalsy();
  });
});
```

**Step 2: 运行测试**

Run: `npm run test -- VoucherMetadata`
Expected: Tests pass

**Step 3: 提交测试**

Run: `git add src/components/voucher/__tests__/VoucherMetadata.test.tsx && git commit -m "test(voucher): 添加 VoucherMetadata 单元测试"

---

## Phase 3: VoucherPreviewTabs - 标签容器

### Task 6: 实现 VoucherPreviewTabs 组件

**Files:**
- Create: `src/components/voucher/VoucherPreviewTabs.tsx`

**Step 1: 创建组件文件**

```typescript
// src/components/voucher/VoucherPreviewTabs.tsx
import React, { useState } from 'react';
import { Tabs } from 'antd';
import { VoucherPreviewCanvas } from './VoucherPreviewCanvas';
import { OriginalDocumentPreview } from './OriginalDocumentPreview';

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
  fileName?: string;
  name?: string;
  fileUrl?: string;
  type?: string;
}

interface VoucherDTO {
  voucherId: string;
  voucherNo: string;
  voucherWord?: string;
  voucherDate?: string;
  orgName?: string;
  summary?: string;
  debitTotal?: number | string;
  creditTotal?: number | string;
  creator?: string;
  auditor?: string;
  poster?: string;
  entries?: VoucherEntryDTO[];
}

interface VoucherPreviewTabsProps {
  voucherData: VoucherDTO;
  attachments?: AttachmentDTO[];
  defaultTab?: 'voucher' | 'attachments';
}

export const VoucherPreviewTabs: React.FC<VoucherPreviewTabsProps> = ({
  voucherData,
  attachments = [],
  defaultTab = 'voucher',
}) => {
  const [activeTab, setActiveTab] = useState(defaultTab);

  const tabItems = [
    {
      key: 'voucher',
      label: '会计凭证',
      children: <VoucherPreviewCanvas data={voucherData} />,
    },
    {
      key: 'attachments',
      label: `关联附件${attachments.length > 0 ? ` (${attachments.length})` : ''}`,
      children: <OriginalDocumentPreview files={attachments} />,
    },
  ];

  return (
    <div className="h-full bg-white">
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={tabItems}
        className="voucher-preview-tabs"
      />
    </div>
  );
};
```

**Step 2: 提交组件**

Run: `git add src/components/voucher/VoucherPreviewTabs.tsx && git commit -m "feat(voucher): 实现 VoucherPreviewTabs 标签容器组件"

---

### Task 7: 实现 OriginalDocumentPreview PDF预览组件

**Files:**
- Create: `src/components/voucher/OriginalDocumentPreview.tsx`

**Step 1: 创建组件文件**

```typescript
// src/components/voucher/OriginalDocumentPreview.tsx
import React, { useState, useMemo } from 'react';
import { FileText, Download } from 'lucide-react';

interface AttachmentDTO {
  id: string;
  fileName?: string;
  name?: string;
  fileUrl?: string;
  type?: string;
}

interface OriginalDocumentPreviewProps {
  files: AttachmentDTO[];
  defaultFileIndex?: number;
}

export const OriginalDocumentPreview: React.FC<OriginalDocumentPreviewProps> = ({
  files,
  defaultFileIndex = 0,
}) => {
  const [selectedIndex, setSelectedIndex] = useState(
    files.length > 0 ? Math.min(defaultFileIndex, files.length - 1) : -1
  );

  const selectedFile = useMemo(() => {
    return selectedIndex >= 0 ? files[selectedIndex] : null;
  }, [selectedIndex, files]);

  // 如果没有文件
  if (files.length === 0) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-slate-400 bg-slate-50">
        <FileText size={48} className="mb-4 opacity-20" />
        <p>暂无关联附件</p>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      {/* 文件列表 */}
      {files.length > 1 && (
        <div className="border-b border-slate-200 p-2 bg-slate-50">
          <div className="flex gap-2 overflow-x-auto">
            {files.map((file, index) => (
              <button
                key={file.id}
                onClick={() => setSelectedIndex(index)}
                className={`px-3 py-1.5 text-sm rounded whitespace-nowrap transition-colors ${
                  selectedIndex === index
                    ? 'bg-blue-50 text-blue-700 border border-blue-200'
                    : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
                }`}
              >
                {file.fileName || file.name || `文件${index + 1}`}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* 预览区域 */}
      <div className="flex-1 overflow-auto bg-slate-100 p-4">
        {selectedFile?.fileUrl ? (
          <div className="h-full flex flex-col">
            {/* iframe PDF 预览 */}
            <iframe
              src={selectedFile.fileUrl}
              title={selectedFile.fileName || selectedFile.name}
              className="flex-1 w-full rounded border-0 bg-white shadow-sm"
            />
          </div>
        ) : (
          <div className="h-full flex flex-col items-center justify-center text-slate-400">
            <FileText size={48} className="mb-4 opacity-20" />
            <p>该文件无法预览</p>
            <a
              href={selectedFile?.fileUrl || '#'}
              download={selectedFile?.fileName || selectedFile?.name}
              className="mt-4 flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              <Download size={16} />
              下载文件
            </a>
          </div>
        )}
      </div>
    </div>
  );
};
```

**Step 2: 提交组件**

Run: `git add src/components/voucher/OriginalDocumentPreview.tsx && git commit -m "feat(voucher): 实现 OriginalDocumentPreview PDF预览组件"

---

### Task 8: 创建标签组件单元测试

**Files:**
- Create: `src/components/voucher/__tests__/VoucherPreviewTabs.test.tsx`

**Step 1: 创建测试文件**

```typescript
// src/components/voucher/__tests__/VoucherPreviewTabs.test.tsx
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { VoucherPreviewTabs } from '../VoucherPreviewTabs';

describe('VoucherPreviewTabs', () => {
  const mockVoucherData = {
    voucherId: 'test-001',
    voucherNo: '001',
    voucherWord: '记',
    entries: [],
  };

  const mockAttachments = [
    { id: '1', fileName: 'invoice1.pdf', fileUrl: '/files/invoice1.pdf' },
    { id: '2', fileName: 'contract.pdf', fileUrl: '/files/contract.pdf' },
  ];

  it('应该渲染标签页', () => {
    render(<VoucherPreviewTabs voucherData={mockVoucherData} />);
    expect(screen.getByText(/会计凭证/)).toBeTruthy();
    expect(screen.getByText(/关联附件/)).toBeTruthy();
  });

  it('应该显示附件数量', () => {
    render(
      <VoucherPreviewTabs voucherData={mockVoucherData} attachments={mockAttachments} />
    );
    expect(screen.getByText(/关联附件 \(2\)/)).toBeTruthy();
  });

  it('应该在无附件时显示空状态', () => {
    render(<VoucherPreviewTabs voucherData={mockVoucherData} attachments={[]} />);
    expect(screen.getByText(/暂无关联附件/)).toBeTruthy();
  });

  it('应该支持切换标签', () => {
    render(
      <VoucherPreviewTabs voucherData={mockVoucherData} attachments={mockAttachments} />
    );
    const attachmentTab = screen.getByText(/关联附件/);
    fireEvent.click(attachmentTab);
    expect(screen.getByText(/invoice1\.pdf/)).toBeTruthy();
  });

  it('应该支持默认选中标签', () => {
    render(
      <VoucherPreviewTabs
        voucherData={mockVoucherData}
        attachments={mockAttachments}
        defaultTab="attachments"
      />
    );
    expect(screen.getByText(/invoice1\.pdf/)).toBeTruthy();
  });
});
```

**Step 2: 运行测试**

Run: `npm run test -- VoucherPreviewTabs`
Expected: Tests pass

**Step 3: 提交测试**

Run: `git add src/components/voucher/__tests__/VoucherPreviewTabs.test.tsx && git commit -m "test(voucher): 添加 VoucherPreviewTabs 和 OriginalDocumentPreview 单元测试"

---

## Phase 4: VoucherPreview - 组合容器

### Task 9: 实现 VoucherPreview 容器组件

**Files:**
- Create: `src/components/voucher/VoucherPreview.tsx`

**Step 1: 创建容器组件**

```typescript
// src/components/voucher/VoucherPreview.tsx
import React from 'react';
import { VoucherMetadata } from './VoucherMetadata';
import { VoucherPreviewTabs } from './VoucherPreviewTabs';

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
  fileName?: string;
  name?: string;
  fileUrl?: string;
  type?: string;
}

interface VoucherDTO {
  id: string;
  voucherId?: string;
  voucherNo: string;
  voucherWord?: string;
  voucherDate?: string;
  orgName?: string;
  summary?: string;
  debitTotal?: number | string;
  creditTotal?: number | string;
  createdTime?: string;
  creator?: string;
  auditor?: string;
  poster?: string;
  entries?: VoucherEntryDTO[];
  attachments?: AttachmentDTO[];
}

type LayoutType = 'horizontal' | 'vertical';
type SizeType = 'compact' | 'normal' | 'large';

interface VoucherPreviewProps {
  data: VoucherDTO;
  attachments?: AttachmentDTO[];
  layout?: LayoutType;
  size?: SizeType;
  defaultTab?: 'voucher' | 'attachments';
}

export const VoucherPreview: React.FC<VoucherPreviewProps> = ({
  data,
  attachments,
  layout = 'horizontal',
  size = 'normal',
  defaultTab = 'voucher',
}) => {
  const isCompact = size === 'compact';
  const isLarge = size === 'large';

  // 水平布局（左右分栏）
  if (layout === 'horizontal') {
    return (
      <div className={`flex gap-4 ${isLarge ? 'h-[600px]' : 'h-[500px]'}`}>
        {/* 左侧元数据 */}
        <div className={isCompact ? 'w-64' : isLarge ? 'w-80' : 'w-72'}>
          <VoucherMetadata data={data} compact={isCompact} />
        </div>

        {/* 右侧预览 */}
        <div className="flex-1 border border-slate-200 rounded-lg overflow-hidden">
          <VoucherPreviewTabs
            voucherData={data}
            attachments={attachments}
            defaultTab={defaultTab}
          />
        </div>
      </div>
    );
  }

  // 垂直布局（上下堆叠）
  return (
    <div className="flex flex-col gap-4">
      {/* 上方元数据 */}
      <VoucherMetadata data={data} compact={isCompact} />

      {/* 下方预览 */}
      <div className="border border-slate-200 rounded-lg overflow-hidden" style={{ minHeight: '400px' }}>
        <VoucherPreviewTabs
          voucherData={data}
          attachments={attachments}
          defaultTab={defaultTab}
        />
      </div>
    </div>
  );
};
```

**Step 2: 更新 index.ts 导出**

```typescript
// src/components/voucher/index.ts
export { VoucherPreviewCanvas } from './VoucherPreviewCanvas';
export { VoucherMetadata } from './VoucherMetadata';
export { VoucherPreviewTabs } from './VoucherPreviewTabs';
export { OriginalDocumentPreview } from './OriginalDocumentPreview';
export { VoucherPreview } from './VoucherPreview';
```

**Step 3: 提交容器组件**

Run: `git add src/components/voucher/VoucherPreview.tsx src/components/voucher/index.ts && git commit -m "feat(voucher): 实现 VoucherPreview 组合容器组件"

---

### Task 10: 创建容器组件测试

**Files:**
- Create: `src/components/voucher/__tests__/VoucherPreview.test.tsx`

**Step 1: 创建测试文件**

```typescript
// src/components/voucher/__tests__/VoucherPreview.test.tsx
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { VoucherPreview } from '../VoucherPreview';

describe('VoucherPreview', () => {
  const mockVoucherData = {
    id: 'test-001',
    voucherNo: '001',
    voucherWord: '记',
    voucherDate: '2025-01-01',
    orgName: '测试公司',
    debitTotal: 1500,
    entries: [],
  };

  it('应该渲染左右分栏布局', () => {
    const { container } = render(
      <VoucherPreview data={mockVoucherData} layout="horizontal" />
    );
    expect(container.querySelector('.flex.gap-4')).toBeTruthy();
  });

  it('应该渲染垂直布局', () => {
    const { container } = render(
      <VoucherPreview data={mockVoucherData} layout="vertical" />
    );
    expect(container.querySelector('.flex.flex-col.gap-4')).toBeTruthy();
  });

  it('应该支持紧凑模式', () => {
    const { container } = render(
      <VoucherPreview data={mockVoucherData} size="compact" />
    );
    expect(container.querySelector('.w-64')).toBeTruthy();
  });

  it('应该支持大尺寸', () => {
    const { container } = render(
      <VoucherPreview data={mockVoucherData} size="large" />
    );
    expect(container.querySelector('.w-80')).toBeTruthy();
  });

  it('应该同时渲染元数据和预览区', () => {
    render(<VoucherPreview data={mockVoucherData} />);
    expect(screen.getByText(/业务元数据/)).toBeTruthy();
    expect(screen.getByText(/会计凭证/)).toBeTruthy();
  });
});
```

**Step 2: 运行测试**

Run: `npm run test -- VoucherPreview`
Expected: Tests pass

**Step 3: 提交测试**

Run: `git add src/components/voucher/__tests__/VoucherPreview.test.tsx && git commit -m "test(voucher): 添加 VoucherPreview 容器组件单元测试"

---

## Phase 5: 集成与优化

### Task 11: 添加性能优化

**Files:**
- Modify: `src/components/voucher/VoucherPreviewCanvas.tsx`

**Step 1: 优化 VoucherPreviewCanvas memo 比较**

```typescript
// 在 VoucherPreviewCanvas.tsx 末尾，更新 memo 配置
VoucherPreviewCanvas.displayName = 'VoucherPreviewCanvas';

// 自定义比较函数，只在 voucherId 变化时重新渲染
export default React.memo(VoucherPreviewCanvas, (prevProps, nextProps) => {
  return prevProps.data.voucherId === nextProps.data.voucherId &&
         prevProps.compact === nextProps.compact &&
         prevProps.showSignature === nextProps.showSignature;
});
```

**Step 2: 提交优化**

Run: `git add src/components/voucher/VoucherPreviewCanvas.tsx && git commit -m "perf(voucher): 添加 VoucherPreviewCanvas 性能优化"

---

### Task 12: 创建集成示例页面

**Files:**
- Create: `src/pages/demo/VoucherPreviewDemo.tsx`

**Step 1: 创建演示页面**

```typescript
// src/pages/demo/VoucherPreviewDemo.tsx
import React from 'react';
import { VoucherPreview, VoucherMetadata, VoucherPreviewCanvas } from '../../components/voucher';

const mockVoucherData = {
  id: 'demo-001',
  voucherId: 'voucher-demo-001',
  voucherNo: '001',
  voucherWord: '记',
  voucherDate: '2025-01-01',
  orgName: '示例公司',
  accountPeriod: '2025-01',
  accbookCode: 'BR01',
  summary: '测试凭证摘要',
  debitTotal: 1500,
  creditTotal: 1500,
  createdTime: '2025-01-01T10:00:00',
  creator: '张三',
  auditor: '李四',
  poster: '王五',
  entries: [
    {
      lineNo: 1,
      summary: '收到货款',
      accountCode: '1001',
      accountName: '库存现金',
      debit: 1500,
      credit: 0,
    },
    {
      lineNo: 2,
      summary: '收到货款',
      accountCode: '1002',
      accountName: '银行存款',
      debit: 0,
      credit: 1500,
    },
  ],
  attachments: [
    { id: '1', fileName: '发票.pdf', fileUrl: '/files/invoice.pdf', type: 'invoice' },
  ],
};

export const VoucherPreviewDemo: React.FC = () => {
  return (
    <div className="p-8 space-y-8">
      <h1 className="text-2xl font-bold">凭证预览组件演示</h1>

      <section>
        <h2 className="text-xl font-semibold mb-4">完整预览（水平布局）</h2>
        <VoucherPreview data={mockVoucherData} layout="horizontal" />
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-4">完整预览（垂直布局）</h2>
        <VoucherPreview data={mockVoucherData} layout="vertical" />
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-4">紧凑模式</h2>
        <VoucherPreview data={mockVoucherData} size="compact" />
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-4">仅元数据</h2>
        <div className="w-72">
          <VoucherMetadata data={mockVoucherData} />
        </div>
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-4">仅凭证渲染</h2>
        <div className="max-w-2xl">
          <VoucherPreviewCanvas data={mockVoucherData} />
        </div>
      </section>
    </div>
  );
};
```

**Step 2: 提交演示页面**

Run: `git add src/pages/demo/VoucherPreviewDemo.tsx && git commit -m "feat(voucher): 添加组件演示页面"

---

### Task 13: 更新设计文档状态

**Files:**
- Modify: `docs/plans/2026-01-01-voucher-preview-component-design.md`

**Step 1: 更新文档状态为已完成**

在文档末尾添加：
```markdown
## 实施状态

- [x] Phase 1: VoucherPreviewCanvas 核心渲染
- [x] Phase 2: VoucherMetadata 元数据展示
- [x] Phase 3: VoucherPreviewTabs 标签切换
- [x] Phase 4: VoucherPreview 组合容器
- [x] Phase 5: 性能优化

**实施日期**: 2026-01-01
**状态**: ✅ 已完成
```

**Step 2: 提交文档更新**

Run: `git add docs/plans/2026-01-01-voucher-preview-component-design.md && git commit -m "docs(voucher): 更新设计文档状态为已完成"

---

## 完成检查清单

### Task 14: 最终验证

**Step 1: 运行所有测试**

Run: `npm run test`
Expected: All tests pass

**Step 2: 检查 TypeScript 编译**

Run: `npm run build`
Expected: No TypeScript errors

**Step 3: 验证组件导出**

Run: `grep -r "export.*Voucher" src/components/voucher/index.ts`
Expected: All components exported

**Step 4: 提交最终代码**

Run: `git add -A && git commit -m "feat(voucher): 完成凭证预览组件实施

- 实现 VoucherPreviewCanvas 会计凭证CSS渲染
- 实现 VoucherMetadata 元数据展示
- 实现 VoucherPreviewTabs 标签切换
- 实现 OriginalDocumentPreview PDF预览
- 实现 VoucherPreview 组合容器
- 添加完整的单元测试
- 添加性能优化（React.memo）
- 添加演示页面

组件可在全景视图、会计档案、电子凭证池等多处复用。"

---

## 总结

此实施计划创建了完整的凭证预览组件体系：

1. **VoucherPreviewCanvas**: 会计凭证CSS表格渲染
2. **VoucherMetadata**: 元数据字段列表展示
3. **VoucherPreviewTabs**: 标签切换容器
4. **OriginalDocumentPreview**: PDF预览
5. **VoucherPreview**: 完整组合容器

所有组件均支持独立使用和组合使用，支持多种布局和尺寸，包含完整的单元测试和性能优化。
