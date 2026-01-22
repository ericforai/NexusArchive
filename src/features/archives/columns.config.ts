/**
 * Archive Feature - Table Column Configurations
 * 
 * This file contains the column definitions for different archive views.
 * It is located within the features/archives directory to maintain modularity.
 */
import { ModuleConfig } from '../../types';

// 1.1 记账凭证库
export const PRE_ARCHIVE_POOL_CONFIG: ModuleConfig = {
    columns: [
        { key: 'voucherWord', header: '凭证号', type: 'text' },
        { key: 'source', header: '来源系统', type: 'text' },
        { key: 'type', header: '单据类型', type: 'text' },
        { key: 'docDate', header: '业务日期', type: 'date' },
        { key: 'date', header: '入池时间', type: 'datetime' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};

// 1.3 凭证关联
export const PRE_ARCHIVE_LINK_CONFIG: ModuleConfig = {
    columns: [
        { key: 'voucherNo', header: '记账凭证号', type: 'text' },
        { key: 'amount', header: '金额', type: 'money' },
        { key: 'date', header: '业务日期', type: 'date' },
        { key: 'invoiceCount', header: '关联发票数', type: 'text' },
        { key: 'contractNo', header: '关联合同', type: 'text' },
        { key: 'matchScore', header: '匹配度', type: 'progress' },
        { key: 'autoLink', header: '关联方式', type: 'text' },
        { key: 'status', header: '关联状态', type: 'status' },
    ],
    data: []
};

// 3.1 会计凭证
export const ACCOUNTING_VOUCHER_CONFIG: ModuleConfig = {
    columns: [
        { key: 'voucherNo', header: '凭证号', type: 'text' },
        { key: 'type', header: '凭证类型', type: 'text' },
        { key: 'entity', header: '核算主体', type: 'text' },
        { key: 'period', header: '期间', type: 'text' },
        { key: 'subject', header: '摘要/题名', type: 'text' },
        { key: 'amount', header: '金额', type: 'money' },
        { key: 'date', header: '制单日期', type: 'date' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};

// 3.2 会计账簿
export const ACCOUNTING_LEDGER_CONFIG: ModuleConfig = {
    columns: [
        { key: 'ledgerNo', header: '账簿编号', type: 'text' },
        { key: 'type', header: '账簿类型', type: 'text' },
        { key: 'entity', header: '核算主体', type: 'text' },
        { key: 'year', header: '年度', type: 'text' },
        { key: 'period', header: '期间', type: 'text' },
        { key: 'subject', header: '名称/科目', type: 'text' },
        { key: 'pageCount', header: '页数', type: 'text' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};

// 3.3 财务报告
export const FINANCIAL_REPORT_CONFIG: ModuleConfig = {
    columns: [
        { key: 'reportNo', header: '报告编号', type: 'text' },
        { key: 'type', header: '报告类型', type: 'text' },
        { key: 'year', header: '年度', type: 'text' },
        { key: 'unit', header: '会计单位', type: 'text' },
        { key: 'title', header: '报告名称', type: 'text' },
        { key: 'period', header: '报告期间', type: 'text' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};

// 3.4 其他会计资料
export const OTHER_ACCOUNTING_MATERIALS_CONFIG: ModuleConfig = {
    columns: [
        { key: 'materialNo', header: '资料编号', type: 'text' },
        { key: 'type', header: '资料类型', type: 'text' },
        { key: 'year', header: '年度', type: 'text' },
        { key: 'title', header: '题名', type: 'text' },
        { key: 'date', header: '日期', type: 'date' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};

// 3.5 档案装盒
export const ARCHIVE_BOX_CONFIG: ModuleConfig = {
    columns: [
        { key: 'boxCode', header: '盒条码', type: 'text' },
        { key: 'spec', header: '规格', type: 'text' },
        { key: 'thickness', header: '盒宽', type: 'text' },
        { key: 'docCount', header: '装卷数量', type: 'text' },
        { key: 'fullness', header: '装载率', type: 'progress' },
        { key: 'printStatus', header: '标签打印', type: 'status' },
    ],
    data: []
};

// 5 全文检索
export const QUERY_CONFIG: ModuleConfig = {
    columns: [
        { key: 'archiveCode', header: '档号', type: 'text' },
        { key: 'title', header: '题名', type: 'text' },
        { key: 'category', header: '门类', type: 'text' },
        { key: 'year', header: '年度', type: 'text' },
        { key: 'retention', header: '保管期限', type: 'text' },
        { key: 'security', header: '密级', type: 'status' },
    ],
    data: []
};

// 2.1 扫描集成（资料收集）
export const SCAN_CONFIG: ModuleConfig = {
    columns: [
        { key: 'title', header: '题名', type: 'text' },
        { key: 'category', header: '门类', type: 'status' },
        { key: 'orgName', header: '组织机构', type: 'text' },
        { key: 'year', header: '年度', type: 'text' },
        { key: 'period', header: '期间', type: 'text' },
        { key: 'date', header: '日期', type: 'date' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};

// 默认兜底（归档查看 - 支持全类型档案通用显示）
export const GENERIC_CONFIG: ModuleConfig = {
    columns: [
        { key: 'archivalCode', header: '档号', type: 'text' },
        { key: 'title', header: '题名', type: 'text' },
        { key: 'category', header: '门类', type: 'text' },
        { key: 'orgName', header: '组织机构', type: 'text' },
        { key: 'year', header: '年度', type: 'text' },
        { key: 'period', header: '保管期限', type: 'text' },
        { key: 'date', header: '业务日期', type: 'date' },
        { key: 'status', header: '状态', type: 'status' },
    ],
    data: []
};
