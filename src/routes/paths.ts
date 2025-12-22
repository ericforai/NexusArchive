// Input: 无外部依赖
// Output: ROUTE_PATHS 与 SUBITEM_TO_PATH 常量
// Pos: 路由路径常量定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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

    // New: Archive Operations Path (Separated from Repository)
    ARCHIVE_OPS: '/system/operations',
    ARCHIVE_BOXING: '/system/operations/boxing',
    ARCHIVE_VOLUME: '/system/operations/volume',
    ARCHIVE_APPROVAL: '/system/operations/approval',
    ARCHIVE_OPEN_APPRAISAL: '/system/operations/open-appraisal',
    ARCHIVE_DESTRUCTION: '/system/operations/destruction',

    ARCHIVE_OPEN_APPRAISAL: '/system/operations/open-appraisal',
    ARCHIVE_DESTRUCTION: '/system/operations/destruction',

    // New: Archive Utilization (Query + Borrowing)
    ARCHIVE_UTILIZATION: '/system/utilization',
    QUERY: '/system/utilization/query',
    QUERY_RELATIONSHIP: '/system/utilization/relationship',
    BORROWING: '/system/utilization/borrowing',

    DESTRUCTION: '/system/destruction',
    // WAREHOUSE removed
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

    // Debug Routes
    TEST_PAYMENT_FILE: '/debug/payment-file',
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

    // --- 会计档案 (Level 1: ACCOUNT_ARCHIVES) ---
    // Level 2: 会计凭证
    '会计凭证': ROUTE_PATHS.ARCHIVE_VOUCHERS,

    // Level 2: 会计账簿 -> Level 3
    '总账': ROUTE_PATHS.ARCHIVE_LEDGERS + '?type=GENERAL_LEDGER',
    '明细账': ROUTE_PATHS.ARCHIVE_LEDGERS + '?type=SUBSIDIARY_LEDGER',
    '现金日记账': ROUTE_PATHS.ARCHIVE_LEDGERS + '?type=CASH_JOURNAL',
    '银行存款日记账': ROUTE_PATHS.ARCHIVE_LEDGERS + '?type=BANK_JOURNAL',
    '固定资产卡片': ROUTE_PATHS.ARCHIVE_LEDGERS + '?type=FIXED_ASSETS_CARD',
    '其他辅助账簿': ROUTE_PATHS.ARCHIVE_LEDGERS + '?type=OTHER_BOOKS',

    // Level 2: 财务报告 -> Level 3
    '月度财务报告': ROUTE_PATHS.ARCHIVE_REPORTS + '?type=MONTHLY',
    '季度财务报告': ROUTE_PATHS.ARCHIVE_REPORTS + '?type=QUARTERLY',
    '半年度财务报告': ROUTE_PATHS.ARCHIVE_REPORTS + '?type=SEMI_ANNUAL',
    '年度财务报告': ROUTE_PATHS.ARCHIVE_REPORTS + '?type=ANNUAL',
    '专项财务报告': ROUTE_PATHS.ARCHIVE_REPORTS + '?type=SPECIAL',

    // Level 2: 其他会计资料 -> Level 3
    '银行存款余额调节表': ROUTE_PATHS.ARCHIVE_OTHER + '?type=BANK_RECONCILIATION',
    '银行对账单': ROUTE_PATHS.ARCHIVE_OTHER + '?type=BANK_STATEMENT',
    '纳税申报表': ROUTE_PATHS.ARCHIVE_OTHER + '?type=TAX_RETURN',
    '会计档案移交清册': ROUTE_PATHS.ARCHIVE_OTHER + '?type=HANDOVER_REGISTER',
    '会计档案保管清册': ROUTE_PATHS.ARCHIVE_OTHER + '?type=CUSTODY_REGISTER',
    '会计档案销毁清册': ROUTE_PATHS.ARCHIVE_OTHER + '?type=DESTRUCTION_REGISTER',
    '会计档案鉴定意见书': ROUTE_PATHS.ARCHIVE_OTHER + '?type=APPRAISAL_OPINION',

    // --- 档案作业 (Level 1: ARCHIVE_OPS) ---
    '档案装盒': ROUTE_PATHS.ARCHIVE_BOXING,
    '档案组卷': ROUTE_PATHS.ARCHIVE_VOLUME,
    '归档审批': ROUTE_PATHS.ARCHIVE_APPROVAL,
    '开放鉴定': ROUTE_PATHS.ARCHIVE_OPEN_APPRAISAL,
    '销毁鉴定': ROUTE_PATHS.ARCHIVE_DESTRUCTION,

    // 档案利用 (ARCHIVE_UTILIZATION)
    '穿透联查': ROUTE_PATHS.QUERY_RELATIONSHIP,
    '全文检索': ROUTE_PATHS.QUERY,
    '借阅申请': ROUTE_PATHS.BORROWING,

    // 库房管理 - Removed
    // '密集架控制', '温湿度监控' removed

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
