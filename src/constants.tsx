import {
  LayoutDashboard,
  FileInput,
  Layers,
  Archive,
  Search,
  BookOpenCheck,
  Warehouse,
  BarChart3,
  Settings,
  FileText,
  CheckCircle2,
  AlertTriangle,
  Clock,
  PanelLeft,
  Flame
} from 'lucide-react';
import { NavItem, ViewState, ArchiveStat, Notification, ModuleConfig } from './types';

export const NAV_ITEMS: NavItem[] = [
  { id: ViewState.PORTAL, label: '档案门户', icon: LayoutDashboard, permission: 'nav:portal' },
  { id: ViewState.PANORAMA, label: '全景视图', icon: PanelLeft, permission: 'nav:panorama' },
  { id: ViewState.PRE_ARCHIVE, label: '预归档库', icon: Layers, permission: 'nav:pre_archive', subItems: ['电子凭证池', 'OCR识别', '凭证关联', '异常数据'] },
  { id: ViewState.COLLECTION, label: '资料收集', icon: FileInput, permission: 'nav:collection', subItems: ['在线接收', '扫描集成', '批量上传'] },
  {
    id: ViewState.ARCHIVE_MGMT,
    label: '档案管理',
    icon: Archive,
    permission: 'nav:archive_mgmt',
    subItems: ['会计凭证', '会计账簿', '财务报告', '档案装盒', '档案组卷', '归档审批', '开放鉴定', '销毁鉴定']
  },
  { id: ViewState.QUERY, label: '档案查询', icon: Search, permission: 'nav:query', subItems: ['穿透联查', '全文检索'] },
  { id: ViewState.BORROWING, label: '档案借阅', icon: BookOpenCheck, permission: 'nav:borrowing', subItems: ['借阅审批', '水印预览'] },
  { id: ViewState.DESTRUCTION, label: '档案销毁', icon: Flame, permission: 'nav:destruction' },
  { id: ViewState.WAREHOUSE, label: '库房管理', icon: Warehouse, permission: 'nav:warehouse', subItems: ['密集架控制', '温湿度监控'] },
  { id: ViewState.STATS, label: '数据统计', icon: BarChart3, permission: 'nav:stats' },
  { id: ViewState.SETTINGS, label: '系统设置', icon: Settings, permission: 'nav:settings', subItems: ['基础设置', '用户管理', '角色权限', '组织架构', '全宗管理', '安全合规', '审计日志'] },
];

export const MOCK_STATS: ArchiveStat[] = [
  { label: '本月归档量', value: '12,845', trend: 12.5, trendLabel: 'vs 上月', icon: FileText, color: 'bg-blue-500' },
  { label: 'OCR 识别率', value: '99.8%', trend: 0.4, trendLabel: '精度提升', icon: CheckCircle2, color: 'bg-emerald-500' },
  { label: '四性检测异常', value: '23', trend: -5, trendLabel: '待处理', icon: AlertTriangle, color: 'bg-amber-500' },
  { label: '借阅申请', value: '8', trend: 2, trendLabel: '今日新增', icon: Clock, color: 'bg-purple-500' },
];

export const NOTIFICATIONS: Notification[] = [
  { id: '1', title: 'OCR识别完成：10月进项发票批次', time: '10分钟前', type: 'success' },
  { id: '2', title: '四性检测预警：凭证 PZ-003 缺少附件', time: '30分钟前', type: 'warning' },
  { id: '3', title: '新的借阅申请待审批', time: '1小时前', type: 'info' },
];

export const RECENT_DOCS = [
  { id: '1', code: 'VOU-202311-001', name: '11月服务器采购报销', type: '报销单', amount: '¥ 45,200.00', date: '2023-11-03', complianceScore: 98, status: '已归档' },
  { id: '2', code: 'INV-202311-089', name: '阿里云季度服务费', type: '进项发票', amount: '¥ 12,800.00', date: '2023-11-02', complianceScore: 100, status: '已归档' },
  { id: '3', code: 'CON-202310-056', name: '年度审计服务合同', type: '合同', amount: '¥ 150,000.00', date: '2023-10-30', complianceScore: 85, status: '处理中' },
  { id: '4', code: 'VOU-202310-992', name: '研发部团建费用', type: '报销单', amount: '¥ 5,600.00', date: '2023-10-28', complianceScore: 65, status: '审计失败' },
  { id: '5', code: 'REP-2023-Q3', name: '2023第三季度财报', type: '财务报告', amount: '-', date: '2023-10-15', complianceScore: 100, status: '已归档' },
];

// Mock Data for Destruction View
export const DESTRUCTION_CANDIDATES = [
  { id: '1', code: 'QZ-2013-KJ-0012', title: '2013年1月原始凭证', type: '会计凭证', retention: '10年', expireDate: '2023-01-01', aiSuggestion: '销毁', risk: '低' },
  { id: '2', code: 'QZ-2013-KJ-0015', title: '2013年1月银行回单', type: '会计凭证', retention: '10年', expireDate: '2023-01-01', aiSuggestion: '销毁', risk: '低' },
  { id: '3', code: 'HT-2013-9982', title: '2013年设备采购合同', type: '合同协议', retention: '10年', expireDate: '2023-05-20', aiSuggestion: '延期', risk: '中(涉诉)' },
  { id: '4', code: 'QZ-2013-BB-001', title: '2013年度纳税申报表', type: '税务资料', retention: '10年', expireDate: '2023-06-30', aiSuggestion: '销毁', risk: '低' },
];

export const DESTRUCTION_BATCHES = [
  { id: 'B-20231101', date: '2023-11-01', count: '45 卷', creator: '李档案', approver: '王总', status: '监销中', progress: 66 },
  { id: 'B-20231015', date: '2023-10-15', count: '120 卷', creator: '李档案', approver: '王总', status: '已销毁', progress: 100 },
];

// --- 1. PRE_ARCHIVE SUB-CONFIGS ---

// 1.1 电子凭证池
export const PRE_ARCHIVE_POOL_CONFIG: ModuleConfig = {
  columns: [
    { key: 'businessDocNo', header: '业务单据号', type: 'text' },
    { key: 'code', header: '系统流水号', type: 'text' },
    { key: 'source', header: '来源系统', type: 'text' },
    { key: 'type', header: '单据类型', type: 'text' },
    { key: 'amount', header: '金额', type: 'money' },
    { key: 'date', header: '入池时间', type: 'date' },
    { key: 'status', header: '解析状态', type: 'status' },
  ],
  data: [
    { id: '1', businessDocNo: 'JZ-202311-0051', code: 'POOL-20231101-001', source: 'SAP ERP', type: '记账凭证', amount: '¥ 12,000.00', date: '2023-11-01 09:00', status: '待处理' },
    { id: '2', businessDocNo: 'BX-202311-0023', code: 'POOL-20231101-002', source: '费控系统', type: '差旅报销单', amount: '¥ 3,450.00', date: '2023-11-01 09:05', status: '待处理' },
    { id: '3', businessDocNo: 'YH-202311-0089', code: 'POOL-20231101-003', source: '资金系统', type: '银行回单', amount: '¥ 150,000.00', date: '2023-11-01 09:12', status: '处理中' },
    { id: '4', businessDocNo: '-', code: 'POOL-20231101-004', source: '影像系统', type: '电子发票', amount: '¥ 500.00', date: '2023-11-01 09:15', status: '已完成' },
  ]
};

// 1.2 OCR识别
export const PRE_ARCHIVE_OCR_CONFIG: ModuleConfig = {
  columns: [
    { key: 'fileName', header: '文件名称', type: 'text' },
    { key: 'batchId', header: '识别批次', type: 'text' },
    { key: 'pages', header: '页数', type: 'text' },
    { key: 'confidence', header: '置信度', type: 'progress' },
    { key: 'scanDate', header: '扫描时间', type: 'date' },
    { key: 'status', header: '识别状态', type: 'status' },
  ],
  data: [
    { id: '1', fileName: '2023年10月进项发票扫描件.pdf', batchId: 'OCR-2310-A', pages: '150', confidence: 99, scanDate: '2023-10-31', status: '已完成' },
    { id: '2', fileName: '合同附件-技术服务协议.jpg', batchId: 'OCR-2310-B', pages: '5', confidence: 85, scanDate: '2023-11-01', status: '警告' },
    { id: '3', fileName: '手工记账凭证附件_001.png', batchId: 'OCR-2310-C', pages: '1', confidence: 60, scanDate: '2023-11-01', status: '错误' },
  ]
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
  data: [
    {
      id: '1',
      voucherNo: 'JZ-202311-0051',
      amount: '¥ 12,800.00',
      date: '2023-11-02',
      invoiceCount: '1 张',
      contractNo: 'CON-2023-098',
      matchScore: 100,
      autoLink: '金额+日期精确匹配',
      status: '已关联',
      linkedFileId: '2',
      // Rich Data for Visual Chain
      sourceDocuments: [
        { id: 'inv-1', type: 'invoice', code: 'INV-202311-089', name: '阿里云计算服务费发票', amount: '¥ 12,800.00', date: '2023-11-02', status: '已验真' }
      ],
      supportingDocuments: [
        { id: 'con-1', type: 'contract', code: 'CON-2023-098', name: '年度技术服务协议', amount: '¥ 150,000.00', date: '2023-01-01', status: '有效' },
        { id: 'ord-1', type: 'order', code: 'PO-202311-001', name: '服务采购订单', amount: '¥ 12,800.00', date: '2023-10-28', status: '已审批' }
      ]
    },
    {
      id: '2',
      voucherNo: 'JZ-202311-0052',
      amount: '¥ 45,200.00',
      date: '2023-11-03',
      invoiceCount: '2 张',
      contractNo: '-',
      matchScore: 98,
      autoLink: '金额精确+日期邻近',
      status: '已关联',
      linkedFileId: '1',
      sourceDocuments: [
        { id: 'inv-2', type: 'invoice', code: 'INV-202311-092', name: '服务器采购发票A', amount: '¥ 22,600.00', date: '2023-11-03', status: '已验真' },
        { id: 'inv-3', type: 'invoice', code: 'INV-202311-093', name: '服务器采购发票B', amount: '¥ 22,600.00', date: '2023-11-03', status: '已验真' }
      ],
      supportingDocuments: [
        { id: 'rec-1', type: 'receipt', code: 'REC-202311-005', name: '入库单', amount: '-', date: '2023-11-04', status: '已签字' }
      ]
    },
    {
      id: '3',
      voucherNo: 'JZ-202311-0053',
      amount: '¥ 150,000.00',
      date: '2023-11-01',
      invoiceCount: '1 张',
      contractNo: 'CON-2023-099',
      matchScore: 95,
      autoLink: '合同号关联',
      status: '已关联',
      linkedFileId: '3',
      sourceDocuments: [
        { id: 'bank-1', type: 'receipt', code: 'BK-20231101-003', name: '银行回单', amount: '¥ 150,000.00', date: '2023-11-01', status: '已对账' }
      ],
      supportingDocuments: [
        { id: 'con-2', type: 'contract', code: 'CON-2023-099', name: '装修工程合同', amount: '¥ 500,000.00', date: '2023-09-15', status: '履行中' }
      ]
    },
    {
      id: '4',
      voucherNo: 'JZ-202311-0054',
      amount: '¥ 3,450.00',
      date: '2023-11-05',
      invoiceCount: '3 张',
      contractNo: '-',
      matchScore: 92,
      autoLink: '客商名称匹配',
      status: '已关联',
      linkedFileId: '4',
      sourceDocuments: [
        { id: 'inv-4', type: 'invoice', code: 'INV-202311-101', name: '差旅住宿费', amount: '¥ 1,200.00', date: '2023-11-04', status: '已验真' },
        { id: 'inv-5', type: 'invoice', code: 'INV-202311-102', name: '差旅机票', amount: '¥ 2,000.00', date: '2023-11-04', status: '已验真' },
        { id: 'inv-6', type: 'invoice', code: 'INV-202311-103', name: '市内交通费', amount: '¥ 250.00', date: '2023-11-05', status: '已验真' }
      ],
      supportingDocuments: [
        { id: 'app-1', type: 'other', code: 'TR-202311-001', name: '差旅申请单', amount: '-', date: '2023-10-30', status: '已审批' }
      ]
    },
    { id: '5', voucherNo: 'JZ-202311-0055', amount: '¥ 8,900.00', date: '2023-11-06', invoiceCount: '1 张', contractNo: '-', matchScore: 85, autoLink: '摘要包含票据号', status: '已关联', linkedFileId: '1', sourceDocuments: [], supportingDocuments: [] },
    { id: '6', voucherNo: 'JZ-202311-0056', amount: '¥ 2,300.00', date: '2023-11-07', invoiceCount: '0 张', contractNo: '-', matchScore: 45, autoLink: '部分匹配', status: '待确认', sourceDocuments: [], supportingDocuments: [] },
    { id: '7', voucherNo: 'JZ-202311-0057', amount: '¥ 5,600.00', date: '2023-11-08', invoiceCount: '0 张', contractNo: '-', matchScore: 0, autoLink: '-', status: '未关联', sourceDocuments: [], supportingDocuments: [] },
    { id: '8', voucherNo: 'JZ-202311-0058', amount: '¥ 25,000.00', date: '2023-11-09', invoiceCount: '0 张', contractNo: '-', matchScore: 0, autoLink: '-', status: '未关联', sourceDocuments: [], supportingDocuments: [] },
  ]
};

// --- 2. COLLECTION SUB-CONFIGS ---

// 2.1 在线接收
export const COLLECTION_ONLINE_CONFIG: ModuleConfig = {
  columns: [
    { key: 'interface', header: '接口名称', type: 'text' },
    { key: 'system', header: '对接系统', type: 'text' },
    { key: 'freq', header: '同步频率', type: 'text' },
    { key: 'lastSync', header: '最后同步', type: 'date' },
    { key: 'count', header: '本次接收', type: 'text' },
    { key: 'status', header: '接口状态', type: 'status' },
  ],
  data: [
    { id: '1', interface: 'SAP_VOUCHER_SYNC', system: 'SAP ERP', freq: '实时', lastSync: '2023-11-03 10:00', count: '12 条', status: '正常' },
    { id: '2', interface: 'FSSC_INVOICE_PULL', system: '费控云', freq: '每小时', lastSync: '2023-11-03 09:00', count: '45 条', status: '正常' },
    { id: '3', interface: 'OA_FLOW_SYNC', system: 'OA系统', freq: '每日', lastSync: '2023-11-02 23:00', count: '0 条', status: '异常' },
  ]
};

// 2.2 扫描集成
export const COLLECTION_SCAN_CONFIG: ModuleConfig = {
  columns: [
    { key: 'device', header: '扫描设备', type: 'text' },
    { key: 'location', header: '设备位置', type: 'text' },
    { key: 'operator', header: '当前操作员', type: 'text' },
    { key: 'todayCount', header: '今日扫描页数', type: 'text' },
    { key: 'status', header: '设备状态', type: 'status' },
    { key: 'queue', header: '上传队列', type: 'text' },
  ],
  data: [
    { id: '1', device: 'Canon DR-G2110 #1', location: '财务部扫描室', operator: '张三', todayCount: '1,205', status: '正常', queue: '空闲' },
    { id: '2', device: 'Canon DR-G2110 #2', location: '档案室', operator: '李四', todayCount: '850', status: '正常', queue: '处理中' },
    { id: '3', device: 'HighSpeed Scan Pro', location: '收发室', operator: '王五', todayCount: '0', status: '离线', queue: '-' },
  ]
};

// 2.3 Generic Collection Fallback
export const COLLECTION_CONFIG: ModuleConfig = {
  columns: [
    { key: 'batchNo', header: '批次号', type: 'text' },
    { key: 'type', header: '资料类型', type: 'text' },
    { key: 'source', header: '来源渠道', type: 'text' },
    { key: 'count', header: '数量', type: 'text' },
    { key: 'date', header: '接收时间', type: 'date' },
    { key: 'status', header: '处理状态', type: 'status' },
  ],
  data: [
    { id: '1', batchNo: 'BATCH-20231104-01', type: '电子发票', source: '邮箱导入', count: '125', date: '2023-11-04 09:30', status: '处理中' },
    { id: '2', batchNo: 'BATCH-20231104-02', type: '银行回单', source: '银企直联', count: '45', date: '2023-11-04 10:15', status: '已完成' },
  ]
};

// --- 3. ARCHIVE_MGMT SUB-CONFIGS ---

// 3.1 归档查看
export const ARCHIVE_VIEW_CONFIG: ModuleConfig = {
  columns: [
    { key: 'archiveNo', header: '全宗号', type: 'text' },
    { key: 'category', header: '门类', type: 'text' },
    { key: 'year', header: '年度', type: 'text' },
    { key: 'period', header: '保管期限', type: 'text' },
    { key: 'title', header: '题名', type: 'text' },
    { key: 'security', header: '密级', type: 'status' },
  ],
  data: [
    { id: '1', archiveNo: 'QZ-2022-KJ-001', category: '会计凭证', year: '2022', period: '30年', title: '2022年1月记账凭证', security: '内部' },
    { id: '2', archiveNo: 'QZ-2022-KJ-002', category: '会计凭证', year: '2022', period: '30年', title: '2022年2月记账凭证', security: '内部' },
    { id: '3', archiveNo: 'QZ-2022-BB-001', category: '财务报表', year: '2022', period: '永久', title: '2022年度财务决算报告', security: '机密' },
  ]
};

// 3.3 会计凭证分类视图
export const ACCOUNTING_VOUCHER_CONFIG: ModuleConfig = {
  columns: [
    { key: 'voucherNo', header: '凭证号', type: 'text' },
    { key: 'entity', header: '会计实体', type: 'text' },
    { key: 'period', header: '期间', type: 'text' },
    { key: 'subject', header: '科目', type: 'text' },
    { key: 'type', header: '凭证类型', type: 'text' },
    { key: 'amount', header: '金额', type: 'money' },
    { key: 'date', header: '日期', type: 'date' },
    { key: 'status', header: '状态', type: 'status' },
  ],
  data: [
    { id: '1', voucherNo: 'JZ-202311-0051', archivalCode: 'COMP001-2023-10Y-FIN-AC01-V0051', entity: '总公司', period: '11月', subject: '1002 银行存款', type: '付款凭证', amount: '¥ 45,200.00', date: '2023-11-03', status: '已归档' },
    { id: '2', voucherNo: 'JZ-202311-0052', archivalCode: 'COMP001-2023-10Y-FIN-AC01-V0052', entity: '分公司A', period: '11月', subject: '5001 主营业务收入', type: '收款凭证', amount: '¥ 125,000.00', date: '2023-11-02', status: '已归档' },
    { id: '3', voucherNo: 'JZ-202311-0053', archivalCode: 'COMP001-2023-10Y-FIN-AC01-V0053', entity: '总公司', period: '11月', subject: '6001 主营业务成本', type: '转账凭证', amount: '¥ 28,500.00', date: '2023-11-01', status: '已归档' },
    { id: '4', voucherNo: 'JZ-202310-0098', archivalCode: 'COMP001-2023-10Y-FIN-AC01-V0098', entity: '分公司B', period: '10月', subject: '2001 短期借款', type: '收款凭证', amount: '¥ 500,000.00', date: '2023-10-28', status: '已归档' },
    { id: '5', voucherNo: 'JZ-202310-0095', archivalCode: 'COMP001-2023-10Y-FIN-AC01-V0095', entity: '总公司', period: '10月', subject: '1001 库存现金', type: '付款凭证', amount: '¥ 5,600.00', date: '2023-10-25', status: '已归档' },
  ]
};

// 3.4 会计账簿分类视图
export const ACCOUNTING_LEDGER_CONFIG: ModuleConfig = {
  columns: [
    { key: 'ledgerNo', header: '账簿编号', type: 'text' },
    { key: 'type', header: '账簿类型', type: 'text' },
    { key: 'entity', header: '会计实体', type: 'text' },
    { key: 'year', header: '会计年度', type: 'text' },
    { key: 'period', header: '期间', type: 'text' },
    { key: 'subject', header: '科目', type: 'text' },
    { key: 'pageCount', header: '页数', type: 'text' },
    { key: 'status', header: '状态', type: 'status' },
  ],
  data: [
    { id: '1', ledgerNo: 'ZB-2023-001', archivalCode: 'COMP001-2023-30Y-FIN-AC02-L001', type: '总账', entity: '总公司', year: '2023', period: '全年', subject: '全部科目', pageCount: '120', status: '已归档' },
    { id: '2', ledgerNo: 'ZB-2023-002', archivalCode: 'COMP001-2023-30Y-FIN-AC02-L002', type: '明细账', entity: '总公司', year: '2023', period: '全年', subject: '1002 银行存款', pageCount: '45', status: '已归档' },
    { id: '3', ledgerNo: 'ZB-2023-003', archivalCode: 'COMP001-2023-30Y-FIN-AC02-L003', type: '固定资产卡片', entity: '总公司', year: '2023', period: '全年', subject: '固定资产', pageCount: '28', status: '已归档' },
    { id: '4', ledgerNo: 'ZB-2023-004', archivalCode: 'COMP001-2023-30Y-FIN-AC02-L004', type: '现金日记账', entity: '总公司', year: '2023', period: '全年', subject: '1001 库存现金', pageCount: '36', status: '已归档' },
    { id: '5', ledgerNo: 'ZB-2023-005', archivalCode: 'COMP001-2023-30Y-FIN-AC02-L005', type: '银行存款日记账', entity: '总公司', year: '2023', period: '全年', subject: '1002 银行存款', pageCount: '52', status: '已归档' },
    { id: '6', ledgerNo: 'ZB-2023-006', archivalCode: 'COMP001-2023-30Y-FIN-AC02-L006', type: '明细账', entity: '分公司A', year: '2023', period: '全年', subject: '5001 主营业务收入', pageCount: '38', status: '已归档' },
  ]
};

// 3.5 财务报告分类视图
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
  data: [
    { id: '1', reportNo: 'REP-2023-01', archivalCode: 'COMP001-2023-PERM-FIN-AC03-R001', type: '月度报告', year: '2023', unit: '总公司', title: '2023年1月财务月报', period: '2023-01', status: '已归档' },
    { id: '2', reportNo: 'REP-2023-Q1', archivalCode: 'COMP001-2023-PERM-FIN-AC03-R002', type: '季度报告', year: '2023', unit: '总公司', title: '2023年第一季度财务报告', period: '2023-Q1', status: '已归档' },
    { id: '3', reportNo: 'REP-2023-H1', archivalCode: 'COMP001-2023-PERM-FIN-AC03-R003', type: '半年度报告', year: '2023', unit: '总公司', title: '2023年半年度财务报告', period: '2023-H1', status: '已归档' },
    { id: '4', reportNo: 'REP-2023-ANNUAL', archivalCode: 'COMP001-2023-PERM-FIN-AC03-R004', type: '年度报告', year: '2023', unit: '总公司', title: '2023年度财务决算报告', period: '2023', status: '已归档' },
    { id: '5', reportNo: 'REP-2023-10', archivalCode: 'COMP001-2023-PERM-FIN-AC03-R005', type: '月度报告', year: '2023', unit: '分公司A', title: '2023年10月财务月报', period: '2023-10', status: '已归档' },
  ]
};

// 3.6 其他会计资料分类视图
export const OTHER_ACCOUNTING_MATERIALS_CONFIG: ModuleConfig = {
  columns: [
    { key: 'materialNo', header: '资料编号', type: 'text' },
    { key: 'type', header: '资料类型', type: 'text' },
    { key: 'year', header: '年度', type: 'text' },
    { key: 'title', header: '题名', type: 'text' },
    { key: 'date', header: '日期', type: 'date' },
    { key: 'status', header: '状态', type: 'status' },
  ],
  data: [
    { id: '1', materialNo: 'QT-2023-001', type: '税务申报表', year: '2023', title: '2023年10月增值税申报表', date: '2023-11-15', status: '已归档' },
    { id: '2', materialNo: 'QT-2023-002', type: '银行对账单', year: '2023', title: '2023年10月银行对账单', date: '2023-11-01', status: '已归档' },
    { id: '3', materialNo: 'QT-2023-003', type: '银行回单', year: '2023', title: '2023年10月银行回单汇总', date: '2023-10-31', status: '已归档' },
    { id: '4', materialNo: 'QT-2023-004', type: '税务申报表', year: '2023', title: '2023年第三季度企业所得税申报表', date: '2023-10-20', status: '已归档' },
    { id: '5', materialNo: 'QT-2023-005', type: '银行对账单', year: '2023', title: '2023年9月银行对账单', date: '2023-10-01', status: '已归档' },
  ]
};

// 3.2 装盒管理
export const ARCHIVE_BOX_CONFIG: ModuleConfig = {
  columns: [
    { key: 'boxCode', header: '盒条码', type: 'text' },
    { key: 'spec', header: '规格', type: 'text' },
    { key: 'thickness', header: '盒宽', type: 'text' },
    { key: 'docCount', header: '装卷数量', type: 'text' },
    { key: 'fullness', header: '装载率', type: 'progress' },
    { key: 'printStatus', header: '标签打印', type: 'status' },
  ],
  data: [
    { id: '1', boxCode: 'BOX-2023-001', spec: 'A4标准盒', thickness: '4cm', docCount: '5', fullness: 85, printStatus: '已完成' },
    { id: '2', boxCode: 'BOX-2023-002', spec: 'A4标准盒', thickness: '4cm', docCount: '4', fullness: 60, printStatus: '已完成' },
    { id: '3', boxCode: 'BOX-2023-003', spec: 'A4加厚盒', thickness: '6cm', docCount: '0', fullness: 0, printStatus: '待处理' },
  ]
};

// --- 4. WAREHOUSE SUB-CONFIGS ---

// 4.1 密集架控制
export const WAREHOUSE_RACK_CONFIG: ModuleConfig = {
  columns: [
    { key: 'zone', header: '库区', type: 'text' },
    { key: 'group', header: '列组', type: 'text' },
    { key: 'column', header: '列号', type: 'text' },
    { key: 'status', header: '运行状态', type: 'status' },
    { key: 'lock', header: '锁定', type: 'text' },
    { key: 'vent', header: '通风模式', type: 'status' },
  ],
  data: [
    { id: '1', zone: 'A区', group: 'G1', column: 'L-01', status: '闭合', lock: '锁定', vent: '关闭' },
    { id: '2', zone: 'A区', group: 'G1', column: 'L-02', status: '打开', lock: '解锁', vent: '关闭' },
    { id: '3', zone: 'A区', group: 'G1', column: 'L-03', status: '闭合', lock: '锁定', vent: '关闭' },
  ]
};

// 4.2 温湿度监控
export const WAREHOUSE_ENV_CONFIG: ModuleConfig = {
  columns: [
    { key: 'sensorId', header: '传感器ID', type: 'text' },
    { key: 'location', header: '安装位置', type: 'text' },
    { key: 'temp', header: '温度(°C)', type: 'text' },
    { key: 'humidity', header: '湿度(%RH)', type: 'text' },
    { key: 'lastUpdate', header: '更新时间', type: 'date' },
    { key: 'battery', header: '电量', type: 'progress' },
    { key: 'alert', header: '告警状态', type: 'status' },
  ],
  data: [
    { id: '1', sensorId: 'S-001', location: 'A区-密集架G1顶部', temp: '22.5', humidity: '45.2', lastUpdate: '2023-11-03 10:35', battery: 90, alert: '正常' },
    { id: '2', sensorId: 'S-002', location: 'A区-密集架G5中部', temp: '22.6', humidity: '46.0', lastUpdate: '2023-11-03 10:35', battery: 85, alert: '正常' },
    { id: '3', sensorId: 'S-003', location: 'B区-门口', temp: '25.1', humidity: '60.5', lastUpdate: '2023-11-03 10:35', battery: 40, alert: '警告' },
  ]
};

// --- 5. OTHER CONFIGS ---

export const BORROWING_CONFIG: ModuleConfig = {
  columns: [
    { key: 'reqId', header: '申请单号', type: 'text' },
    { key: 'applicant', header: '申请人', type: 'text' },
    { key: 'dept', header: '部门', type: 'text' },
    { key: 'count', header: '借阅数量', type: 'text' },
    { key: 'applyDate', header: '申请时间', type: 'date' },
    { key: 'status', header: '审批状态', type: 'status' },
  ],
  data: [
    { id: '1', reqId: 'BR-202311-001', applicant: '张三', dept: '财务部', count: '2 卷', applyDate: '2023-11-03', status: '待审批' },
    { id: '2', reqId: 'BR-202311-002', applicant: '李四', dept: '审计部', count: '15 卷', applyDate: '2023-11-02', status: '已通过' },
    { id: '3', reqId: 'BR-202311-003', applicant: '王五', dept: '内控部', count: '1 卷', applyDate: '2023-11-01', status: '已归还' },
  ]
};

export const QUERY_CONFIG: ModuleConfig = {
  columns: [
    { key: 'archiveCode', header: '档号', type: 'text' },
    { key: 'title', header: '题名', type: 'text' },
    { key: 'category', header: '门类', type: 'text' },
    { key: 'year', header: '年度', type: 'text' },
    { key: 'retention', header: '保管期限', type: 'text' },
    { key: 'security', header: '密级', type: 'status' },
  ],
  data: [
    { id: '1', archiveCode: 'QZ-2022-KJ-0056', title: '2022年12月记账凭证', category: '会计凭证', year: '2022', retention: '30年', security: '内部' },
    { id: '2', archiveCode: 'HT-2023-0034', title: '年度审计服务合同', category: '合同类', year: '2023', retention: '10年', security: '机密' },
    { id: '3', archiveCode: 'WS-2023-0112', title: '税务局检查通知书', category: '文书类', year: '2023', retention: '永久', security: '内部' },
  ]
};

// --- Default / Fallback Configs ---

export const GENERIC_CONFIG: ModuleConfig = {
  columns: [{ key: 'info', header: '信息', type: 'text' }],
  data: []
};
