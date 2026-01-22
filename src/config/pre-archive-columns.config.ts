
/**
 * Pre-Archive Column Configurations
 *
 * Specialized column definitions for different archive categories in the Pool.
 */
import { ModuleConfig } from '../types';

// Pre-Archive: Financial Reports (AC03)
export const PRE_ARCHIVE_REPORT_CONFIG: ModuleConfig = {
    columns: [
        { key: 'fileName', header: '报告名称', type: 'text' },
        { key: 'voucherType', header: '报告类型', type: 'text' },
        { key: 'fiscalYear', header: '年度', type: 'text' },
        { key: 'period', header: '期间', type: 'text' },
        { key: 'fondsCode', header: '全宗', type: 'text' },
        { key: 'date', header: '入池时间', type: 'datetime' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};

// Pre-Archive: Accounting Ledgers (AC02)
export const PRE_ARCHIVE_LEDGER_CONFIG: ModuleConfig = {
    columns: [
        { key: 'fileName', header: '账簿名称', type: 'text' },
        { key: 'voucherType', header: '账簿类型', type: 'text' },
        { key: 'fiscalYear', header: '年度', type: 'text' },
        { key: 'period', header: '期间', type: 'text' },
        { key: 'fondsCode', header: '全宗', type: 'text' },
        { key: 'source', header: '来源', type: 'text' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};

// Pre-Archive: Other Materials (AC04)
export const PRE_ARCHIVE_OTHER_CONFIG: ModuleConfig = {
    columns: [
        { key: 'fileName', header: '资料名称', type: 'text' },
        { key: 'voucherType', header: '资料类型', type: 'text' },
        { key: 'fiscalYear', header: '年度', type: 'text' },
        { key: 'fondsCode', header: '全宗', type: 'text' },
        { key: 'date', header: '日期', type: 'datetime' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};
