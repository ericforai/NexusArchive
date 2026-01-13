// src/components/voucher/VoucherPreviewCanvas.tsx
// Input: React、共享样式工具函数
// Output: VoucherPreviewCanvas 会计凭证渲染组件
// Pos: 凭证预览功能组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useMemo } from 'react';
import { voucherTableStyles, formatCurrency, numberToChinese, formatDate } from './styles';
import type { VoucherDTO, VoucherEntryDTO } from './types';

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
  // Debug: Log received data
  console.log('[VoucherPreviewCanvas] Rendered with:', {
    voucherId: data.voucherId,
    voucherNo: data.voucherNo,
    entriesCount: data.entries?.length || 0,
    debitTotal: data.debitTotal,
    creditTotal: data.creditTotal
  });

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
            <th style={{ ...voucherTableStyles.tableHeadCell, width: '22%' }}>摘要</th>
            <th style={{ ...voucherTableStyles.tableHeadCell, width: '30%' }}>科目</th>
            <th style={{ ...voucherTableStyles.tableHeadCellRight, width: '13%' }}>借方</th>
            <th style={{ ...voucherTableStyles.tableHeadCellRight, width: '13%' }}>贷方</th>
            <th style={{ ...voucherTableStyles.tableHeadCell, width: '12%' }}>币种</th>
            <th style={{ ...voucherTableStyles.tableHeadCellRight, width: '10%' }}>原币</th>
          </tr>
        </thead>
        <tbody>
          {data.entries && data.entries.length > 0 ? (
            data.entries.map((entry, index) => {
              // 判断是否有外币（非本位币）
              const hasForeignCurrency = entry.currencyCode && entry.currencyCode !== 'CNY';
              // 获取原币金额
              const originalDebit = entry.debitOriginal ? Number(entry.debitOriginal) : 0;
              const originalCredit = entry.creditOriginal ? Number(entry.creditOriginal) : 0;

              return (
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
                  <td style={{ ...voucherTableStyles.tableCell, fontSize: '12px' }}>
                    {entry.currencyName || entry.currencyCode || '-'}
                  </td>
                  <td style={{ ...voucherTableStyles.tableCellRight, fontSize: '12px' }}>
                    {hasForeignCurrency ? (
                      <span>
                        {formatCurrency(originalDebit || originalCredit)}
                      </span>
                    ) : '-'}
                  </td>
                </tr>
              );
            })
          ) : (
            <tr>
              <td colSpan={6} style={{ ...voucherTableStyles.tableCell, textAlign: 'center', color: '#9ca3af' }}>
                暂无分录数据
              </td>
            </tr>
          )}
        </tbody>
        <tfoot style={voucherTableStyles.tableFoot}>
          <tr>
            <td colSpan={3} style={{ ...voucherTableStyles.tableFootCell, textAlign: 'right' }}>
              合计
            </td>
            <td style={voucherTableStyles.tableFootCell}>{formatCurrency(totalDebit)}</td>
            <td style={voucherTableStyles.tableFootCell}></td>
            <td style={voucherTableStyles.tableFootCell}></td>
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

// 自定义比较函数，只在 voucherId 变化时重新渲染
export default React.memo(VoucherPreviewCanvas, (prevProps, nextProps) => {
  return prevProps.data.voucherId === nextProps.data.voucherId &&
         prevProps.compact === nextProps.compact &&
         prevProps.showSignature === nextProps.showSignature;
});
