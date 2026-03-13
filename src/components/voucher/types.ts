// src/components/voucher/types.ts
// Input: -
// Output: 凭证预览组件的类型定义
// Pos: 凭证预览功能类型定义

/**
 * 凭证分录数据传输对象
 */
export interface VoucherEntryDTO {
  lineNo?: number;
  summary?: string;
  accountCode?: string;
  accountName?: string;
  debit?: number | string;
  credit?: number | string;
  /** 币种代码 (如: CNY, USD, EUR) */
  currencyCode?: string;
  /** 币种名称 (如: 人民币, 美元, 欧元) */
  currencyName?: string;
  /** 原币借方金额 */
  debitOriginal?: number | string;
  /** 原币贷方金额 */
  creditOriginal?: number | string;
  /** 汇率 (本位币金额 / 原币金额) */
  exchangeRate?: number | string;
}

/**
 * 附件数据传输对象
 */
export interface AttachmentDTO {
  id: string;
  fileName?: string;
  name?: string;
  fileUrl?: string;
  type?: string;
  fileId?: string;
  archiveId?: string;
  previewResourceType?: 'archiveMain' | 'file';
}

/**
 * 凭证数据传输对象
 */
export interface VoucherDTO {
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
  // Additional optional fields for metadata display
  attachments?: AttachmentDTO[];
  createdTime?: string;
  id?: string; // Alias for voucherId in some contexts
}
