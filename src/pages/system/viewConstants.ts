// Input: ViewState 枚举、业务分类
// Output: 视图相关常量配置
// Pos: src/pages/system/ 视图常量
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { ViewState } from '../../types';

/**
 * 单据池类型列表
 * 与数据库 sys_original_voucher_type 保持同步
 */
export const DOC_POOL_TYPES = [
    '单据池',
    // 发票类
    '单据池:纸质发票',
    '单据池:增值税电子发票',
    '单据池:数电发票',
    '单据池:数电票（铁路）',
    '单据池:数电票（航空）',
    '单据池:数电票（财政）',
    // 银行类
    '单据池:银行回单',
    '单据池:银行对账单',
    // 单据类
    '单据池:付款单',
    '单据池:收款单',
    '单据池:收款单据（收据）',
    '单据池:工资单',
    // 合同类
    '单据池:合同',
    '单据池:协议',
    // 其他类
    '单据池:其他',
] as const;

/**
 * 原始凭证类型列表
 */
export const ORIGINAL_VOUCHER_TYPES = [
    '原始凭证',
    '销售订单',
    '出库单',
    '采购订单',
    '入库单',
    '付款申请单',
    '报销单',
    '普通发票',
    '增值税专票',
    '银行回单',
    '银行对账单',
    '合同协议',
] as const;

/**
 * URL 到 ViewState 的映射
 */
export const PATH_TO_VIEW: Record<string, ViewState> = {
    '/system/pre-archive': ViewState.PRE_ARCHIVE,
    '/system/collection': ViewState.COLLECTION,
    '/system/archive': ViewState.ACCOUNT_ARCHIVES,
    '/system/operations': ViewState.ARCHIVE_OPS,
    '/system/utilization': ViewState.ARCHIVE_UTILIZATION,
    '/system/stats': ViewState.STATS,
    '/system/settings': ViewState.SETTINGS,
    '/system/panorama': ViewState.PANORAMA,
};

/**
 * 子菜单路径到 subItem 的映射
 */
export const PATH_TO_SUBITEM: Record<string, string> = {};
