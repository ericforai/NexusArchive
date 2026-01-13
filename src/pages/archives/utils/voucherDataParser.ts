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
    console.log('[Debug] Full Source Data:', JSON.stringify(data));

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
    // 简化格式: data.entries 是数组 (用友等系统)
    else if (data.entries && Array.isArray(data.entries)) {
      bodies = data.entries;
    }

    // 即使没有分录数据，也要尝试返回凭证头信息
    // 因为用户仍需查看凭证的基本信息（编号、日期、金额等）



    // 解析分录数据
    if (bodies.length > 0) {
      console.log('[Debug] First Body Entry:', JSON.stringify(bodies[0]));
    }
    const parsedEntries = bodies
      .map((body: any, index: number) => {
        const debit = body.debitOrg || body.debit_original || body.debit_org || body.debit || 0;
        const credit = body.creditOrg || body.credit_original || body.credit_org || body.credit || 0;
        const debitOriginal = body.debitOriginal || body.debit_original || body.amountInTransactionCurrency || 0;
        const creditOriginal = body.creditOriginal || body.credit_original || 0;

        // 获取科目信息 - 支持 YonSuite 原始格式 (accsubject) 和 VoucherDTO 格式 (accountCode/accountName)
        let accountCode = body.accountCode || body.account_code || '';
        let accountName = body.accountName || body.account_name || '';

        // 如果还没有科目信息，尝试从 accsubject 获取 (YonSuite 原始格式)
        if (!accountCode && !accountName && body.accsubject) {
          if (typeof body.accsubject === 'object') {
            accountCode = body.accsubject.code || '';
            accountName = body.accsubject.name || '';
          } else {
            accountName = body.accsubject || '';
          }
        }

        // 获取币种信息 - 支持 YonSuite 原始格式 (currency) 和 VoucherDTO 格式 (currencyCode/currencyName)
        let currencyCode = body.currencyCode || body.currency_code || '';
        let currencyName = body.currencyName || body.currency_name || '';
        let exchangeRate = body.exchangeRate || body.exchange_rate || null;

        // 尝试从 YonSuite 的 currency 对象获取
        if (!currencyCode && !currencyName) {
          if (typeof body.currency === 'object' && body.currency !== null) {
            currencyCode = body.currency.code || '';
            currencyName = body.currency.name || '';
            // 有些版本可能是 id/name
            if (!currencyCode) currencyCode = body.currency.id || '';
          } else if (typeof body.currency === 'string') {
            // 有些情况 currency 直接是字符串 (可能是 code 或 id)
            // 此时尝试从 currency_name 获取名称
            currencyCode = body.currency;
            currencyName = body.currencyName || body.currency_name || '';
          }
        }

        // 尝试从 YonSuite 的 currencyMap 结构获取
        if (!currencyCode && !currencyName && body.currencyMap) {
          const map = body.currencyMap;
          currencyCode = map.code || '';
          currencyName = map.name || '';
        }

        // 尝试从扁平字段获取 (如 currency_id, currency_name)
        if (!currencyCode) currencyCode = body.currency_id || '';
        if (!currencyName) currencyName = body.currency_name || '';

        // 尝试从 oriCurrency / oriCurrencyName 变体获取
        if (!currencyCode) currencyCode = body.oriCurrencyCode || body.oriCurrency || '';
        if (!currencyName) currencyName = body.oriCurrencyName || '';

        // 默认处理: 如果只有 code 没有 name，name = code
        if (currencyCode && !currencyName) currencyName = currencyCode;
        // 如果只有 name 没有 code (比较少见), code = name
        if (!currencyCode && currencyName) currencyCode = currencyName;

        return {
          lineNo: body.recordNumber || body.recordnumber || body.lineNo || index + 1,
          summary: body.description || body.digest || body.summary || '',
          accountCode,
          accountName,
          debit: Number(debit) || 0,
          credit: Number(credit) || 0,
          currencyCode,
          currencyName,
          debitOriginal: Number(debitOriginal) || undefined,
          creditOriginal: Number(creditOriginal) || undefined,
          exchangeRate: exchangeRate ? Number(exchangeRate) : undefined,
        };
      })
      .filter((e: VoucherEntryDTO) => (Number(e.debit) || 0) > 0 || (Number(e.credit) || 0) > 0);

    // 计算合计 - 如果有分录则从分录汇总，否则使用顶层的 debitTotal/creditTotal
    let totalDebit = parsedEntries.reduce((sum: number, e: VoucherEntryDTO) => sum + (Number(e.debit) || 0), 0);
    let totalCredit = parsedEntries.reduce((sum: number, e: VoucherEntryDTO) => sum + (Number(e.credit) || 0), 0);

    // 如果分录合计为 0，尝试从顶层字段获取金额
    if (totalDebit === 0 && totalCredit === 0) {
      totalDebit = Number(headerData.debitTotal) || Number(headerData.debit_total) || 0;
      totalCredit = Number(headerData.creditTotal) || Number(headerData.credit_total) || totalDebit; // 通常借贷相等
    }

    // 从 header 数据或 row 获取凭证头信息
    // 即使没有分录，也要返回凭证基本信息供用户查看

    // 解析凭证号和凭证字，避免重复显示
    // 例如: voucherNo="记-8", voucherWord="记" → 只显示 "记-8"，不显示 "记-记-8"
    let voucherNo = headerData.voucherNo || headerData.voucherno || row.code || row.erpVoucherNo || row.id || '';
    let voucherWord = headerData.voucherWord || headerData.voucherword || row.voucherWord || '';

    // 如果 voucherNo 已经包含凭证字格式（如 "记-8"），且没有单独的 voucherWord，
    // 则解析出凭证字，避免重复组合
    if (!voucherWord && voucherNo && /^[记收付转资产]/.test(voucherNo)) {
      const match = voucherNo.match(/^([记收付转资产])-(.+)$/);
      if (match) {
        voucherWord = match[1];  // 解析出凭证字
        voucherNo = match[2];     // 解析出凭证号
      }
    }
    // 如果仍然没有凭证字，使用默认值
    if (!voucherWord) {
      voucherWord = '记';
    }

    const voucherData: VoucherDTO = {
      voucherId: row.id,
      voucherNo,
      voucherWord,
      voucherDate: headerData.voucherDate || headerData.voucherdate || row.date || row.docDate || row.createdTime || '',
      orgName: headerData.orgName || headerData.orgname || row.orgName || '',
      summary: headerData.summary || row.title || row.summary || '',
      debitTotal: totalDebit,
      creditTotal: totalCredit,
      creator: headerData.creator || headerData.maker || row.creator || row.createdBy || '',
      auditor: headerData.auditor || headerData.checker || row.auditor || '',
      poster: headerData.poster || headerData.bookkeeper || row.poster || '',
      entries: parsedEntries, // 可能是空数组，VoucherPreviewCanvas 会显示"暂无分录数据"
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
