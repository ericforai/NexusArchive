// src/components/voucher/VoucherPreviewCanvas.tsx
// Input: React、共享样式工具函数
// Output: VoucherPreviewCanvas 会计凭证渲染组件
// Pos: 凭证预览功能组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useMemo } from 'react';
import { voucherTableStyles, formatCurrency, numberToChinese, formatDate } from './styles';

// VoucherDTO 类型定义
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
