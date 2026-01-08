// Input: React 组件类型定义
// Output: ProductWebsite 页面静态数据配置
// Pos: src/pages/product-website/data/sections.ts

import {
  Shield,
  CheckCircle2,
  Zap,
  FileText,
  TrendingUp,
  Database,
  Search,
  Activity,
  Layers,
  Key,
  AlertTriangle,
  XCircle,
  Check,
  Lock,
  Globe,
  ChevronRight,
  Server,
  Cpu,
} from 'lucide-react';
import { type ReactNode } from 'react';

/** 导航栏配置 */
export const NAV_CONFIG = {
  brandName: 'DigiVoucher',
  brandSuffix: '数凭档案',
  ctaButton: '立即体验',
} as const;

/** Hero 区域配置 */
export const HERO_SECTION = {
  badge: {
    text: '符合 DA/T 94-2022 国家标准',
    icon: Shield,
  },
  title: ['让每一张凭证都成为', '合法的数字资产'] as const,
  subtitle: '告别纸质档案库房，开启单套制归档新时代。\n专为大型企业打造，从 ERP 到档案库，实现全链路自动化、无纸化、合规化。',
  buttons: [
    { text: '立即体验', primary: true },
    { text: '预约专家顾问', primary: false },
  ] as const,
};

/** 痛点对比数据 */
export interface PainPointItem {
  pain: { title: string; desc: string };
  gain: { title: string; desc: string; icon: ReactNode };
}

export const PAIN_POINTS: PainPointItem[] = [
  {
    pain: { title: '查找困难', desc: '翻箱倒柜找凭证，耗时耗力，效率极低' },
    gain: { title: '秒级检索', desc: '亿级数据毫秒响应，支持穿透式联查', icon: <Search className="w-5 h-5 text-cyan-400" /> },
  },
  {
    pain: { title: '风险高企', desc: '面临发霉、虫蛀、火灾等物理损毁风险' },
    gain: { title: '永久安全', desc: '四性检测 + 异地备份 + 国密加密，数据万无一失', icon: <Shield className="w-5 h-5 text-cyan-400" /> },
  },
  {
    pain: { title: '成本惊人', desc: '库房租金、打印耗材、人工管理费用高昂' },
    gain: { title: '零成本存储', desc: '无纸化存储，节省 90% 以上物理空间与耗材', icon: <Database className="w-5 h-5 text-cyan-400" /> },
  },
  {
    pain: { title: '审计繁琐', desc: '审计进场需搬运大量案卷，配合工作量大' },
    gain: { title: '远程审计', desc: '授权账号在线调阅，轻松应对内外部审计', icon: <Globe className="w-5 h-5 text-cyan-400" /> },
  },
];

/** 数据价值统计 */
export const DATA_STATS = [
  { val: '90%', label: '存储成本降低', icon: <TrendingUp className="w-5 h-5 text-green-400" /> },
  { val: '100%', label: '单套制合规', icon: <CheckCircle2 className="w-5 h-5 text-cyan-400" /> },
  { val: '10x', label: '归档效率提升', icon: <Zap className="w-5 h-5 text-amber-400" /> },
  { val: '0', label: '纸质凭证打印', icon: <FileText className="w-5 h-5 text-slate-400" /> },
] as const;

/** 核心特性 */
export interface FeatureItem {
  icon: ReactNode;
  title: string;
  desc: string;
}

export const CORE_FEATURES: FeatureItem[] = [
  {
    icon: <Search className="w-8 h-8 text-cyan-400" />,
    title: '智能 OCR 识别',
    desc: '高精度识别发票、银行回单，自动提取关键元数据，准确率高达 99.9%。',
  },
  {
    icon: <Activity className="w-8 h-8 text-amber-400" />,
    title: '四性检测引擎',
    desc: '自动进行真实性、完整性、可用性、安全性检测，确保归档数据合规。',
  },
  {
    icon: <Layers className="w-8 h-8 text-purple-400" />,
    title: '穿透式联查',
    desc: '打破数据孤岛，实现从报表到账簿、凭证、原始单据的全链路穿透。',
  },
];

/** 四性检测数据 */
export const FOUR_PROPERTIES = [
  {
    id: 'authenticity',
    title: '真实性',
    subtitle: 'Authenticity',
    color: 'cyan' as const,
    icon: Shield,
    items: ['哈希值校验', '数字签名验证', '来源系统追溯'],
  },
  {
    id: 'integrity',
    title: '完整性',
    subtitle: 'Integrity',
    color: 'purple' as const,
    icon: Database,
    items: ['元数据完整', '附件齐全', '关联关系完整'],
  },
  {
    id: 'usability',
    title: '可用性',
    subtitle: 'Usability',
    color: 'emerald' as const,
    icon: FileText,
    items: ['格式可读', '长期保存', '随时调阅'],
  },
  {
    id: 'security',
    title: '安全性',
    subtitle: 'Security',
    color: 'rose' as const,
    icon: Lock,
    items: ['权限控制', '审计日志', '备份恢复'],
  },
] as const;

/** 技术三件套 */
export const TECH_SUITE = [
  {
    title: '哈希值 (SM3/SHA256)',
    subtitle: '防篡改',
    icon: <Key className="w-8 h-8 text-cyan-400" />,
    desc: '为每个文件生成唯一数字指纹,任何微小改动都会导致哈希值完全不同',
    tech: '国密SM3算法',
    gradient: 'from-cyan-500/10 to-blue-500/10',
    border: 'border-cyan-500/30',
  },
  {
    title: 'CA证书 (电子签名)',
    subtitle: '防抵赖 / 定身份',
    icon: <Shield className="w-8 h-8 text-amber-400" />,
    desc: '通过权威CA机构颁发的数字证书,确保签名人身份真实且不可否认',
    tech: 'PKI公钥基础设施',
    gradient: 'from-amber-500/10 to-orange-500/10',
    border: 'border-amber-500/30',
  },
  {
    title: '可靠时间戳 (TSA)',
    subtitle: '定时间',
    icon: <Activity className="w-8 h-8 text-purple-400" />,
    desc: '由国家授时中心提供的可信时间证明,精确记录档案形成时刻',
    tech: 'RFC3161标准',
    gradient: 'from-purple-500/10 to-pink-500/10',
    border: 'border-purple-500/30',
  },
] as const;

/** AIP 包组件 */
export const AIP_COMPONENTS = [
  {
    title: '元数据 (XML)',
    subtitle: 'Metadata',
    color: 'cyan' as const,
    icon: Database,
    items: ['档案编号、题名、日期', '保管期限、密级、全宗号', '业务字段(金额、科目等)'],
  },
  {
    title: '版式文件',
    subtitle: 'Content',
    color: 'purple' as const,
    icon: FileText,
    items: ['OFD格式(国产版式标准)', 'PDF/A(长期保存格式)', '原始附件(发票、回单等)'],
  },
  {
    title: '电子签名',
    subtitle: 'Signature',
    color: 'amber' as const,
    icon: Lock,
    items: ['归档人员数字签名', '可靠时间戳(TSA)', 'SM3哈希值封装'],
  },
] as const;

/** AIP 包结构 */
export const AIP_STRUCTURE = [
  { name: '/Metadata', desc: '→ metadata.xml (DA/T 94标准)', color: 'cyan' as const },
  { name: '/Content', desc: '→ voucher_001.ofd, invoice_001.pdf', color: 'purple' as const },
  { name: '/Signature', desc: '→ signature.p7s, timestamp.tsr', color: 'amber' as const },
  { name: '/Logs', desc: '→ audit_trail.log', color: 'emerald' as const },
] as const;

/** Demo 导航菜单 */
export const DEMO_NAV_ITEMS = ['工作台', '档案收集', '档案管理', '借阅中心', '系统设置'] as const;

/** 统计卡片 */
export const DASHBOARD_STATS = [
  { label: '待归档凭证', val: '1,284', color: 'text-cyan-400', bg: 'bg-cyan-500/10' },
  { label: '本月新增', val: '456', color: 'text-purple-400', bg: 'bg-purple-500/10' },
  { label: '借阅申请', val: '12', color: 'text-amber-400', bg: 'bg-amber-500/10' },
  { label: '存储占用', val: '2.4 TB', color: 'text-emerald-400', bg: 'bg-emerald-500/10' },
] as const;

/** 收集任务 */
export const COLLECTION_TASKS = [
  { name: '2024年12月财务凭证', status: '进行中', count: 234, color: 'text-cyan-400' },
  { name: '2024年11月财务凭证', status: '已完成', count: 456, color: 'text-green-400' },
  { name: '2024年10月财务凭证', status: '已完成', count: 423, color: 'text-green-400' },
] as const;

/** 档案列表 */
export const ARCHIVE_TABLE_DATA = [
  { code: '记-202511-001', type: '出差审批单', amount: '¥2,580.00', status: '待归档' },
  { code: 'ARC-2024-001', type: '记账凭证', amount: '¥125,000', status: '已归档' },
  { code: 'ARC-2024-002', type: '银行回单', amount: '¥89,500', status: '待审批' },
  { code: 'ARC-2024-003', type: '发票', amount: '¥45,200', status: '已归档' },
] as const;

/** 借阅统计 */
export const BORROW_STATS = [
  { label: '待审批', count: 5, color: 'text-amber-400' },
  { label: '借阅中', count: 12, color: 'text-cyan-400' },
  { label: '已归还', count: 234, color: 'text-green-400' },
] as const;

/** 借阅记录 */
export const BORROW_RECORDS = [
  { user: '张三', dept: '财务部', doc: 'ARC-2024-001', date: '2024-12-01', status: '借阅中' },
  { user: '李四', dept: '审计部', doc: 'ARC-2024-045', date: '2024-11-28', status: '待审批' },
] as const;

/** 系统设置项 */
export const SYSTEM_SETTINGS = [
  { title: '用户权限管理', desc: '配置用户角色和访问权限', icon: <Key className="w-5 h-5" /> },
  { title: '归档规则配置', desc: '设置自动归档规则和保管期限', icon: <FileText className="w-5 h-5" /> },
  { title: '系统日志', desc: '查看系统操作日志和审计记录', icon: <Activity className="w-5 h-5" /> },
  { title: '数据备份', desc: '配置自动备份策略', icon: <Database className="w-5 h-5" /> },
] as const;

/** 合规架构层级 */
export const COMPLIANCE_LAYERS = [
  {
    title: '业务应用层',
    subtitle: 'Application Layer',
    color: 'cyan' as const,
    icon: Layers,
    items: ['档案采集', '四性检测', '借阅利用', '鉴定销毁'],
  },
  {
    title: '安全合规层',
    subtitle: 'Security Layer',
    color: 'amber' as const,
    icon: Shield,
    items: [
      { label: '三员分立', desc: '系统/安全/审计管理员', icon: Key },
      { label: 'SM3 哈希摘要', desc: '国密算法防篡改', icon: FileText },
      { label: '电子签名', desc: '法律效力保障', icon: Lock },
    ],
  },
  {
    title: '信创基础设施层',
    subtitle: 'Infrastructure Layer',
    color: 'blue' as const,
    icon: Server,
    items: [
      { label: '麒麟/统信 OS', icon: Cpu },
      { label: '达梦/人大金仓', icon: Database },
      { label: '鲲鹏/海光 CPU', icon: Activity },
    ],
  },
] as const;

/** 信创生态合作伙伴 */
export const XINCHUANG_PARTNERS = [
  { name: '麒麟软件', en: 'KylinSoft', icon: Cpu },
  { name: '统信软件', en: 'UnionTech', icon: Layers },
  { name: '达梦数据库', en: 'Dameng DB', icon: Database },
  { name: '人大金仓', en: 'Kingbase', icon: Database },
  { name: '华为鲲鹏', en: 'Kunpeng', icon: Cpu },
  { name: '中科海光', en: 'Hygon', icon: Cpu },
  { name: '中国电子', en: 'CEC', icon: Shield },
] as const;

/** 页脚链接 */
export const FOOTER_LINKS = ['产品白皮书', '技术文档', '隐私政策', '联系我们'] as const;

/** 归档趋势图数据 */
export const ARCHIVE_TREND_DATA = [30, 45, 35, 60, 50, 75, 65, 80, 70, 90, 85, 95] as const;
