// src/pages/archives/utils/voucherDataParser.ts
/**
 * 凭证数据解析工具
 *
 * 职责：将多种 ERP 格式的 sourceData 解析为统一的 VoucherDTO 格式
 * 变更理由：支持新的 ERP 数据格式
 */

import type { VoucherDTO } from '../../../components/voucher';

export interface VoucherEntryDTO {
  lineNo?: number;
  summary?: string;
  accountCode?: string;
  accountName?: string;
  debit?: number | string;
  credit?: number | string;
}

export interface ParseResult {
  voucherData: VoucherDTO | null;
  error?: string;
}

/**
 * 解析 sourceData 为 VoucherDTO
 * 支持多种 ERP 格式：YonSuite、金蝶、用友等
 */
export function parseVoucherData(sourceData: string, row: any): ParseResult {
  try {
    const data = typeof sourceData === 'string' ? JSON.parse(sourceData) : sourceData;

    // 处理不同的数据格式
    let bodies: any[] = [];
    let headerData: any = data;

    // YonSuite 格式: data.bodies
    if (data.bodies && Array.isArray(data.bodies)) {
      bodies = data.bodies;
    }
    // 嵌套格式: data.data.bodies
    else if (data.data?.bodies && Array.isArray(data.data.bodies)) {
      bodies = data.data.bodies;
      headerData = data.data;
    }
    // 直接是数组
    else if (Array.isArray(data)) {
      bodies = data;
    }
    // data.body 是数组
    else if (data.body && Array.isArray(data.body)) {
      bodies = data.body;
    }

    if (bodies.length === 0) {
      return { voucherData: null, error: 'No bodies found' };
    }

    // 解析分录数据
    const parsedEntries = bodies
      .map((body: any, index: number) => {
        const debit = body.debitOrg || body.debit_original || body.debit_org || body.debit || 0;
        const credit = body.creditOrg || body.credit_original || body.credit_org || body.credit || 0;

        // 获取科目信息
        let accountCode = '';
        let accountName = '';
        if (body.accsubject) {
          if (typeof body.accsubject === 'object') {
            accountCode = body.accsubject.code || '';
            accountName = body.accsubject.name || '';
          } else {
            accountName = body.accsubject || '';
          }
        }

        return {
          lineNo: body.recordNumber || body.recordnumber || index + 1,
          summary: body.description || body.digest || body.summary || '',
          accountCode,
          accountName,
          debit: Number(debit) || 0,
          credit: Number(credit) || 0,
        };
      })
      .filter((e: VoucherEntryDTO) => e.debit! > 0 || e.credit! > 0);

    // 计算合计
    const totalDebit = parsedEntries.reduce((sum: number, e: VoucherEntryDTO) => sum + (Number(e.debit) || 0), 0);
    const totalCredit = parsedEntries.reduce((sum: number, e: VoucherEntryDTO) => sum + (Number(e.credit) || 0), 0);

    // 从 header 数据或 row 获取凭证头信息
    const voucherData: VoucherDTO = {
      voucherId: row.id,
      voucherNo: headerData.voucherNo || headerData.voucherno || row.code || row.id,
      voucherWord: headerData.voucherWord || headerData.voucherword || '记',
      voucherDate: headerData.voucherDate || headerData.voucherdate || row.date || row.docDate || row.createdTime || '',
      orgName: headerData.orgName || headerData.orgname || row.orgName || '',
      summary: headerData.summary || row.title || row.summary || '',
      debitTotal: totalDebit,
      creditTotal: totalCredit,
      creator: headerData.creator || headerData.maker || row.creator || row.createdBy || '',
      auditor: headerData.auditor || headerData.checker || row.auditor || '',
      poster: headerData.poster || headerData.bookkeeper || row.poster || '',
      entries: parsedEntries,
      attachments: row.attachments || [],
      createdTime: row.createdTime || row.date || '',
      id: row.id,
    };

    return { voucherData };
  } catch (e) {
    console.warn('[parseVoucherData] Failed to parse sourceData:', e);
    return { voucherData: null, error: String(e) };
  }
}
