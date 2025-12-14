/**
 * 路由路径常量
 * 
 * 独立文件避免循环依赖
 */

/**
 * 所有路由路径常量
 */
export const ROUTE_PATHS = {
    PORTAL: '/system',
    PANORAMA: '/system/panorama',

    PRE_ARCHIVE: '/system/pre-archive',
    PRE_ARCHIVE_POOL: '/system/pre-archive/pool',
    PRE_ARCHIVE_OCR: '/system/pre-archive/ocr',
    PRE_ARCHIVE_LINK: '/system/pre-archive/link',
    PRE_ARCHIVE_ABNORMAL: '/system/pre-archive/abnormal',

    COLLECTION: '/system/collection',
    COLLECTION_ONLINE: '/system/collection/online',
    COLLECTION_SCAN: '/system/collection/scan',

    ARCHIVE: '/system/archive',
    ARCHIVE_VOUCHERS: '/system/archive/vouchers',
    ARCHIVE_LEDGERS: '/system/archive/ledgers',
    ARCHIVE_REPORTS: '/system/archive/reports',
    ARCHIVE_OTHER: '/system/archive/other',
    ARCHIVE_BOXING: '/system/archive/boxing',
    ARCHIVE_VOLUME: '/system/archive/volume',
    ARCHIVE_APPROVAL: '/system/archive/approval',
    ARCHIVE_OPEN_APPRAISAL: '/system/archive/open-appraisal',
    ARCHIVE_DESTRUCTION: '/system/archive/destruction',

    QUERY: '/system/query',
    QUERY_RELATIONSHIP: '/system/query/relationship',

    BORROWING: '/system/borrowing',
    DESTRUCTION: '/system/destruction',
    WAREHOUSE: '/system/warehouse',
    WAREHOUSE_ENV: '/system/warehouse/env',
    STATS: '/system/stats',
    SETTINGS: '/system/settings',
    SETTINGS_BASIC: '/system/settings/basic',
    SETTINGS_USERS: '/system/settings/users',
    SETTINGS_ROLES: '/system/settings/roles',
    SETTINGS_ORG: '/system/settings/org',
    SETTINGS_FONDS: '/system/settings/fonds',
    SETTINGS_SECURITY: '/system/settings/security',
    SETTINGS_INTEGRATION: '/system/settings/integration',
    SETTINGS_AUDIT: '/system/settings/audit',
    ADMIN: '/system/admin',
} as const;

/**
 * 子菜单项到路由路径的映射
 * 用于 Sidebar 中文 subItem 到 URL 的转换
 */
export const SUBITEM_TO_PATH: Record<string, string> = {
    // 预归档库
    '电子凭证池': ROUTE_PATHS.PRE_ARCHIVE_POOL,
    'OCR识别': ROUTE_PATHS.PRE_ARCHIVE_OCR,
    '凭证关联': ROUTE_PATHS.PRE_ARCHIVE_LINK,
    '异常数据': ROUTE_PATHS.PRE_ARCHIVE_ABNORMAL,

    // 资料收集
    '在线接收': ROUTE_PATHS.COLLECTION_ONLINE,
    '扫描集成': ROUTE_PATHS.COLLECTION_SCAN,
    '批量上传': '/system/collection/upload',

    // 档案管理
    '归档查看': ROUTE_PATHS.ARCHIVE,
    '会计凭证': ROUTE_PATHS.ARCHIVE_VOUCHERS,
    '会计账簿': ROUTE_PATHS.ARCHIVE_LEDGERS,
    '财务报告': ROUTE_PATHS.ARCHIVE_REPORTS,
    '其他会计资料': ROUTE_PATHS.ARCHIVE_OTHER,
    '档案装盒': ROUTE_PATHS.ARCHIVE_BOXING,
    '档案组卷': ROUTE_PATHS.ARCHIVE_VOLUME,
    '归档审批': ROUTE_PATHS.ARCHIVE_APPROVAL,
    '开放鉴定': ROUTE_PATHS.ARCHIVE_OPEN_APPRAISAL,
    '销毁鉴定': ROUTE_PATHS.ARCHIVE_DESTRUCTION,

    // 档案查询
    '穿透联查': ROUTE_PATHS.QUERY_RELATIONSHIP,
    '全文检索': ROUTE_PATHS.QUERY,

    // 档案借阅
    '借阅审批': ROUTE_PATHS.BORROWING,

    // 库房管理
    '密集架控制': ROUTE_PATHS.WAREHOUSE,
    '温湿度监控': ROUTE_PATHS.WAREHOUSE_ENV,

    // 系统设置
    '基础设置': ROUTE_PATHS.SETTINGS_BASIC,
    '用户管理': ROUTE_PATHS.SETTINGS_USERS,
    '角色权限': ROUTE_PATHS.SETTINGS_ROLES,
    '组织架构': ROUTE_PATHS.SETTINGS_ORG,
    '全宗管理': ROUTE_PATHS.SETTINGS_FONDS,
    '安全合规': ROUTE_PATHS.SETTINGS_SECURITY,
    '集成中心': ROUTE_PATHS.SETTINGS_INTEGRATION,
    '审计日志': ROUTE_PATHS.SETTINGS_AUDIT,
};
