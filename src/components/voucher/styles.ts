// src/components/voucher/styles.ts
// Input: React 类型定义
// Output: 凭证表格样式常量和格式化工具函数
// Pos: 凭证预览共享样式
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
