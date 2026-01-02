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
