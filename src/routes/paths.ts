// Input: 无外部依赖
// Output: ROUTE_PATHS 与 SUBITEM_TO_PATH 常量（原始凭证类型映射对齐）
// Pos: 路由路径常量定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 路由路径常量
 *
 * 独立文件避免循环依赖
 */

export const ROUTE_PATHS = {
    PORTAL: '/system',
    PANORAMA: '/system/panorama',

    PRE_ARCHIVE: '/system/pre-archive',
    PRE_ARCHIVE_POOL: '/system/pre-archive/pool',  // 支持 ?view=list|kanban 参数切换视图
    PRE_ARCHIVE_POOL_KANBAN: '/system/pre-archive/pool/kanban',
    PRE_ARCHIVE_DOC_POOL: '/system/pre-archive/doc-pool',
    PRE_ARCHIVE_OCR: '/system/pre-archive/ocr',
    PRE_ARCHIVE_LINK: '/system/pre-archive/link',
    PRE_ARCHIVE_ABNORMAL: '/system/pre-archive/abnormal',
    PRE_ARCHIVE_OTHER: '/system/pre-archive/other',
    PRE_ARCHIVE_LEDGERS: '/system/pre-archive/ledgers',
    PRE_ARCHIVE_REPORTS: '/system/pre-archive/reports',

    COLLECTION: '/system/collection',
    COLLECTION_ONLINE: '/system/collection/online',
    COLLECTION_SCAN: '/system/collection/scan',

    ARCHIVE: '/system/archive',
    ARCHIVE_VOUCHERS: '/system/archive/vouchers',
    ARCHIVE_LEDGERS: '/system/archive/ledgers',
    ARCHIVE_REPORTS: '/system/archive/reports',
    ARCHIVE_OTHER: '/system/archive/other',

    ARCHIVE_OPS: '/system/operations',
    ARCHIVE_VOLUME: '/system/operations/volume',
    ARCHIVE_APPROVAL: '/system/operations/approval',
    ARCHIVE_OPEN_APPRAISAL: '/system/operations/open-appraisal',
    ARCHIVE_DESTRUCTION: '/system/operations/destruction',
    EXPIRED_ARCHIVES: '/system/operations/expired-archives',
    APPRAISAL_LIST: '/system/operations/appraisal-list',
    DESTRUCTION_APPROVAL: '/system/operations/destruction-approval',
    DESTRUCTION_EXECUTION: '/system/operations/destruction-execution',

    // WAREHOUSE removed as per expert review (Out of Scope)
    // ---
    // 档案装盒 (Archive Box) - Restored per user request
    ARCHIVE_BOX: '/system/operations/boxing',

    // 原始凭证和记账凭证
    ARCHIVE_ORIGINAL_VOUCHERS: '/system/archive/original-vouchers',
    ARCHIVE_ACCOUNTING_VOUCHERS: '/system/archive/accounting-vouchers',

    // New: Archive Utilization (Query + Borrowing)
    ARCHIVE_UTILIZATION: '/system/utilization',
    QUERY: '/system/utilization/query',
    QUERY_RELATIONSHIP: '/system/utilization/relationship',
    BORROWING: '/system/utilization/borrowing',

    DESTRUCTION: '/system/destruction',
    // WAREHOUSE removed
    STATS: '/system/stats',
    QUALITY: '/system/quality',
    SETTINGS: '/system/settings',
    SETTINGS_BASIC: '/system/settings/basic',
    SETTINGS_BASIC_SECURITY: '/system/settings/basic/security',
    SETTINGS_USERS: '/system/settings/users',
    SETTINGS_ROLES: '/system/settings/roles',
    SETTINGS_ORG: '/system/settings/org',
    SETTINGS_ORG_ENTITY: '/system/settings/org/entity',
    SETTINGS_ORG_ARCHITECTURE: '/system/settings/org/architecture',
    SETTINGS_ORG_FONDS: '/system/settings/org/fonds',
    SETTINGS_ORG_POSITIONS: '/system/settings/org/positions',
    SETTINGS_ORG_FONDS_HISTORY: '/system/settings/org/fonds-history',
    SETTINGS_FONDS: '/system/settings/fonds',
    SETTINGS_SECURITY: '/system/settings/security',
    SETTINGS_INTEGRATION: '/system/settings/integration',
    SETTINGS_AUDIT: '/system/settings/audit',
    ADMIN: '/system/admin',
    SETTINGS_DATA_IMPORT: '/system/settings/data-import',
    SETTINGS_MFA: '/system/settings/mfa',
    FREEZE_HOLD: '/system/operations/freeze-hold',
    FREEZE_HOLD_DETAIL: '/system/operations/freeze-hold',

    MATCHING: '/system/matching',
    MATCHING_AUTO: '/system/matching/auto',
    MATCHING_WIZARD: '/system/matching/wizard',
    MATCHING_REPORT: '/system/matching/report',

    TEST_PAYMENT_FILE: '/debug/payment-file',
} as const;

/**
 * 子菜单项到路由路径的映射
 * 用于 Sidebar 中文 subItem 到 URL 的转换
 */
export const SUBITEM_TO_PATH: Record<string, string> = {
    // 预归档库
    '记账凭证库': ROUTE_PATHS.PRE_ARCHIVE_POOL,  // 默认列表视图，支持 ?view=kanban 切换
    '单据池': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL,

    // 'OCR识别': ROUTE_PATHS.PRE_ARCHIVE_OCR, // 待开发
    '凭证关联': ROUTE_PATHS.PRE_ARCHIVE_LINK,
    '异常数据': ROUTE_PATHS.PRE_ARCHIVE_ABNORMAL,
    '其他会计资料': ROUTE_PATHS.PRE_ARCHIVE_OTHER,
    '预归档:会计账簿': ROUTE_PATHS.PRE_ARCHIVE_LEDGERS,
    '预归档:财务报告': ROUTE_PATHS.PRE_ARCHIVE_REPORTS,

    // 单据池 - 15 个分类子菜单（与数据库 sys_original_voucher_type 一致）
    '单据池:纸质发票': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=INV_PAPER',
    '单据池:增值税电子发票': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=INV_VAT_E',
    '单据池:数电发票': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=INV_DIGITAL',
    '单据池:数电票（铁路）': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=INV_RAIL',
    '单据池:数电票（航空）': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=INV_AIR',
    '单据池:数电票（财政）': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=INV_FINANCIAL',
    '单据池:银行回单': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=BANK_RECEIPT',
    '单据池:银行对账单': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=BANK_STATEMENT',
    // 单据类 (DOCUMENT)
    '单据池:付款单': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=DOC_PAYMENT',
    '单据池:收款单': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=DOC_RECEIPT',
    '单据池:收款单据（收据）': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=DOC_RECEIPT_VOUCHER',
    '单据池:工资单': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=DOC_PAYROLL',
    // 合同类 (CONTRACT)
    '单据池:合同': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=CONTRACT',
    '单据池:协议': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=AGREEMENT',
    // 其他类 (OTHER)
    '单据池:其他': ROUTE_PATHS.PRE_ARCHIVE_DOC_POOL + '?type=OTHER',

    // 嵄料收集
    '在线接收': ROUTE_PATHS.COLLECTION_ONLINE,
    '扫描集成': ROUTE_PATHS.COLLECTION_SCAN,

    // --- 会计档案 (Level 1: ACCOUNT_ARCHIVES) ---
    // Level 2: 会计凭证
    '会计凭证': ROUTE_PATHS.ARCHIVE_VOUCHERS,
    // Level 3: 原始凭证、记账凭证
    '原始凭证': ROUTE_PATHS.ARCHIVE_ORIGINAL_VOUCHERS,
    '记账凭证': ROUTE_PATHS.ARCHIVE_ACCOUNTING_VOUCHERS,

    // Level 2: 会计账簿（父级，默认导航到第一个子项）
    '会计账簿': ROUTE_PATHS.ARCHIVE_LEDGERS + '?type=GENERAL_LEDGER',
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
    '纳税申报表': ROUTE_PATHS.ARCHIVE_OTHER + '?type=TAX_RETURN',
    '会计档案移交清册': ROUTE_PATHS.ARCHIVE_OTHER + '?type=HANDOVER_REGISTER',
    '会计档案保管清册': ROUTE_PATHS.ARCHIVE_OTHER + '?type=CUSTODY_REGISTER',
    '会计档案销毁清册': ROUTE_PATHS.ARCHIVE_OTHER + '?type=DESTRUCTION_REGISTER',
    '会计档案鉴定意见书': ROUTE_PATHS.ARCHIVE_OTHER + '?type=APPRAISAL_OPINION',

    // 档案装盒 (Archive Box)
    '档案装盒': ROUTE_PATHS.ARCHIVE_BOX,

    // --- 档案作业 (Level 1: ARCHIVE_OPS) ---
    '档案组卷': ROUTE_PATHS.ARCHIVE_VOLUME,
    '归档审批': ROUTE_PATHS.ARCHIVE_APPROVAL,  // 单条归档审批
    '归档批次': '/system/operations/batch',     // 批次归档管理
    '开放鉴定': ROUTE_PATHS.ARCHIVE_OPEN_APPRAISAL,
    '销毁鉴定': ROUTE_PATHS.ARCHIVE_DESTRUCTION,

    // 档案利用 (ARCHIVE_UTILIZATION)
    '穿透联查': ROUTE_PATHS.QUERY_RELATIONSHIP,
    '全文检索': ROUTE_PATHS.QUERY,
    '借阅申请': ROUTE_PATHS.BORROWING,

    // 库房管理 - Removed
    // '密集架控制', '温湿度监控' removed

    // --- 系统设置 ---
    '基础设置': ROUTE_PATHS.SETTINGS_BASIC,
    '用户管理': ROUTE_PATHS.SETTINGS_USERS,
    '角色权限': ROUTE_PATHS.SETTINGS_ROLES,
    '组织架构': ROUTE_PATHS.SETTINGS_ORG,
    '全宗管理': ROUTE_PATHS.SETTINGS_FONDS,
    '安全合规': ROUTE_PATHS.SETTINGS_SECURITY,
    '集成中心': ROUTE_PATHS.SETTINGS_INTEGRATION,
    '审计日志': ROUTE_PATHS.SETTINGS_AUDIT,
    '数据导入': ROUTE_PATHS.SETTINGS_DATA_IMPORT,
    '多因子认证': ROUTE_PATHS.SETTINGS_MFA,

    ADMIN: '/system/admin',

    // 匹配向导模块（OnboardingWizard 和 ComplianceReport 仍用 withSuspense）
    MATCHING: '/system/matching',
    MATCHING_AUTO: '/system/matching/auto',
    MATCHING_WIZARD: '/system/matching/wizard',
    MATCHING_REPORT: '/system/matching/report',

    // Debug Routes
    TEST_PAYMENT_FILE: '/debug/payment-file',
};
