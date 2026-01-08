// Input: 路由配置类型定义
// Output: ArchiveRouteMode 类型、路由元数据
// Pos: src/features/archives/routeConfigs.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * Archive Route Configurations
 *
 * 强类型路由配置，确保 routeConfig 拼写错误在编译期被捕获。
 */

import type { ModuleConfig } from '../../types';
import {
    PRE_ARCHIVE_POOL_CONFIG,
    PRE_ARCHIVE_LINK_CONFIG,
    SCAN_CONFIG,
    ACCOUNTING_VOUCHER_CONFIG,
    ACCOUNTING_LEDGER_CONFIG,
    FINANCIAL_REPORT_CONFIG,
    OTHER_ACCOUNTING_MATERIALS_CONFIG,
    ARCHIVE_BOX_CONFIG,
    QUERY_CONFIG,
    GENERIC_CONFIG,
} from './columns.config';

// 默认空配置（所有表格数据应从 API 动态获取）
const DEFAULT_CONFIG: ModuleConfig = GENERIC_CONFIG;

/**
 * 路由模式枚举 - 编译期类型检查
 *
 * 使用 union type 确保任何拼写错误都会导致编译失败。
 */
export type ArchiveRouteMode =
    | 'pool'       // 电子凭证池
    | 'link'       // 凭证关联
    | 'collection' // 资料收集概览
    | 'online'     // 在线接收
    | 'scan'       // 扫描集成
    | 'view'       // 归档查看
    | 'voucher'    // 会计凭证
    | 'ledger'     // 会计账簿
    | 'report'     // 财务报告
    | 'other'      // 其他会计资料
    | 'box'        // 档案装盒
    | 'query';     // 全文检索

/**
 * 路由配置元数据
 */
export interface RouteConfigMeta {
    config: ModuleConfig;
    title: string;
    subTitle: string;
}

/**
 * 路由配置映射表（强类型）
 * 
 * Key 类型为 ArchiveRouteMode，任何不存在的 key 都会编译报错。
 */
export const ROUTE_CONFIG_MAP: Record<ArchiveRouteMode, RouteConfigMeta> = {
    'pool': { config: PRE_ARCHIVE_POOL_CONFIG, title: '预归档库', subTitle: '电子凭证池' },
    'link': { config: PRE_ARCHIVE_LINK_CONFIG, title: '预归档库', subTitle: '凭证关联' },
    'collection': { config: GENERIC_CONFIG, title: '资料收集', subTitle: '概览' },
    'online': { config: GENERIC_CONFIG, title: '资料收集', subTitle: '在线接收' },
    'scan': { config: SCAN_CONFIG, title: '资料收集', subTitle: '扫描集成' },
    'view': { config: GENERIC_CONFIG, title: '档案管理', subTitle: '归档查看' },
    'voucher': { config: ACCOUNTING_VOUCHER_CONFIG, title: '档案管理', subTitle: '会计凭证' },
    'ledger': { config: ACCOUNTING_LEDGER_CONFIG, title: '档案管理', subTitle: '会计账簿' },
    'report': { config: FINANCIAL_REPORT_CONFIG, title: '档案管理', subTitle: '财务报告' },
    'other': { config: OTHER_ACCOUNTING_MATERIALS_CONFIG, title: '档案管理', subTitle: '其他会计资料' },
    'box': { config: ARCHIVE_BOX_CONFIG, title: '档案管理', subTitle: '档案装盒' },
    'query': { config: QUERY_CONFIG, title: '档案查询', subTitle: '全文检索' },
};

/**
 * 解析路由配置
 *
 * @param routeConfig - 路由模式标识符
 * @returns 配置元数据
 */
export function resolveRouteConfig(routeConfig: ArchiveRouteMode | undefined): RouteConfigMeta | undefined {
    if (!routeConfig) return undefined;
    return ROUTE_CONFIG_MAP[routeConfig];
}

/**
 * 默认配置（兜底）
 */
export const DEFAULT_ROUTE_CONFIG: RouteConfigMeta = {
    config: DEFAULT_CONFIG,
    title: '档案列表',
    subTitle: '',
};
