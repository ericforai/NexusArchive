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

// 默认空配置（所有表格数据应从 API 动态获取）
const DEFAULT_CONFIG: ModuleConfig = { columns: [], data: [] };

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
 * 路由模式对应的显示标题（用于 UI 显示）
 * 注意：表格列配置应通过 TableFilters 组件动态生成，而非硬编码
 */
export const ROUTE_TITLES: Record<ArchiveRouteMode, { title: string; subTitle: string }> = {
    'pool': { title: '预归档库', subTitle: '电子凭证池' },
    'link': { title: '预归档库', subTitle: '凭证关联' },
    'collection': { title: '资料收集', subTitle: '概览' },
    'online': { title: '资料收集', subTitle: '在线接收' },
    'scan': { title: '资料收集', subTitle: '扫描集成' },
    'view': { title: '档案管理', subTitle: '归档查看' },
    'voucher': { title: '档案管理', subTitle: '会计凭证' },
    'ledger': { title: '档案管理', subTitle: '会计账簿' },
    'report': { title: '档案管理', subTitle: '财务报告' },
    'other': { title: '档案管理', subTitle: '其他会计资料' },
    'box': { title: '档案管理', subTitle: '档案装盒' },
    'query': { title: '档案查询', subTitle: '全文检索' },
};

/**
 * 解析路由配置
 *
 * @param routeConfig - 路由模式标识符
 * @returns 配置元数据
 */
export function resolveRouteConfig(routeConfig: ArchiveRouteMode | undefined): RouteConfigMeta | undefined {
    if (!routeConfig) return undefined;
    const meta = ROUTE_TITLES[routeConfig];
    if (!meta) return undefined;
    return { config: DEFAULT_CONFIG, ...meta };
}

/**
 * 默认配置（兜底）
 */
export const DEFAULT_ROUTE_CONFIG: RouteConfigMeta = {
    config: DEFAULT_CONFIG,
    title: '档案列表',
    subTitle: '',
};
