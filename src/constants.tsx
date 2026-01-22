// Input: lucide-react 图标、路由路径常量与前端类型定义
// Output: 导航/模块配置常量集合
// Pos: 前端菜单与模块配置注册表
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import {
  LayoutDashboard,
  FileInput,
  Layers,
  Archive,
  Search,
  BarChart3,
  Settings,
  FileText,
  PanelLeft,
  Cloud,
  Server,
} from 'lucide-react';
import { ROUTE_PATHS } from './routes/paths';
import { NavItem, ViewState, AppNotification } from './types';

export const NAV_ITEMS: NavItem[] = [
  { id: ViewState.PORTAL, label: '档案门户', icon: LayoutDashboard, permission: 'nav:portal' },
  { id: ViewState.PANORAMA, label: '全景视图', icon: PanelLeft, permission: 'nav:panorama' },
  {
    id: ViewState.PRE_ARCHIVE,
    label: '预归档库',
    icon: Layers,
    permission: 'nav:pre_archive',
    children: [
      { id: '记账凭证库', label: '记账凭证库', path: '记账凭证库' },
      {
        id: '单据池',
        label: '单据池',
        path: '单据池',
        children: [
          // 发票类 (INVOICE)
          { id: '单据池:纸质发票', label: '纸质发票', path: '单据池:纸质发票' },
          { id: '单据池:增值税电子发票', label: '增值税电子发票', path: '单据池:增值税电子发票' },
          { id: '单据池:数电发票', label: '数电发票', path: '单据池:数电发票' },
          { id: '单据池:数电票（铁路）', label: '数电票（铁路）', path: '单据池:数电票（铁路）' },
          { id: '单据池:数电票（航空）', label: '数电票（航空）', path: '单据池:数电票（航空）' },
          { id: '单据池:数电票（财政）', label: '数电票（财政）', path: '单据池:数电票（财政）' },
          // 银行类 (BANK)
          { id: '单据池:银行回单', label: '银行回单', path: '单据池:银行回单' },
          { id: '单据池:银行对账单', label: '银行对账单', path: '单据池:银行对账单' },
          // 单据类 (DOCUMENT)
          { id: '单据池:付款单', label: '付款单', path: '单据池:付款单' },
          { id: '单据池:收款单', label: '收款单', path: '单据池:收款单' },
          { id: '单据池:收款单据（收据）', label: '收款单据（收据）', path: '单据池:收款单据（收据）' },
          { id: '单据池:工资单', label: '工资单', path: '单据池:工资单' },
          // 合同类 (CONTRACT)
          { id: '单据池:合同', label: '合同', path: '单据池:合同' },
          { id: '单据池:协议', label: '协议', path: '单据池:协议' },
          // 其他类 (OTHER)
          { id: '单据池:其他', label: '其他', path: '单据池:其他' }
        ]
      },
      // { id: 'OCR识别', label: 'OCR识别', path: 'OCR识别' }, // 待开发：后端 OCR 服务未实现，详见 docs/plans/2026-01-06-ocr-service-design.md
      { id: '会计账簿', label: '会计账簿', path: '预归档:会计账簿' },
      { id: '财务报告', label: '财务报告', path: '预归档:财务报告' },
      { id: '凭证关联', label: '凭证关联', path: '凭证关联' },
      { id: '其他会计资料', label: '其他会计资料', path: '其他会计资料' },
      { id: '异常数据', label: '异常数据', path: '异常数据' }
    ]
  },
  {
    id: ViewState.COLLECTION,
    label: '资料收集',
    icon: FileInput,
    permission: 'nav:collection',
    children: [
      { id: '在线接收', label: '在线接收', path: '在线接收' },
      { id: '扫描集成', label: '扫描集成', path: '扫描集成' },
      { id: '批量上传', label: '批量上传', path: '批量上传' }
    ]
  },
  // --- 1. 会计档案库 (Repository) ---
  {
    id: ViewState.ACCOUNT_ARCHIVES,
    label: '会计档案',
    icon: Archive,
    permission: 'nav:repository',
    children: [
      // Level 2 - 会计凭证（含子菜单）
      {
        id: '会计凭证',
        label: '会计凭证',
        children: [
          // 原始凭证及其子分类（与记账凭证并列）
          {
            id: '原始凭证',
            label: '原始凭证',
            path: '原始凭证',
            children: [
              { id: '销售订单', label: '销售订单', path: '销售订单' },
              { id: '出库单', label: '出库单', path: '出库单' },
              { id: '采购订单', label: '采购订单', path: '采购订单' },
              { id: '入库单', label: '入库单', path: '入库单' },
              { id: '付款申请单', label: '付款申请单', path: '付款申请单' },
              { id: '报销单', label: '报销单', path: '报销单' },
              { id: '普通发票', label: '普通发票', path: '普通发票' },
              { id: '增值税专票', label: '增值税专票', path: '增值税专票' },
              { id: '银行回单', label: '银行回单', path: '银行回单' },
              { id: '银行对账单', label: '银行对账单', path: '银行对账单' },
              { id: '合同协议', label: '合同协议', path: '合同协议' }
            ]
          },
          // 记账凭证（与原始凭证并列，都是会计凭证的子项）
          { id: '记账凭证', label: '记账凭证', path: '记账凭证' }
        ]
      },
      {
        id: '会计账簿',
        label: '会计账簿',
        children: [
          { id: '总账', label: '总账', path: '总账' },
          { id: '明细账', label: '明细账', path: '明细账' },
          { id: '现金日记账', label: '现金日记账', path: '现金日记账' },
          { id: '银行存款日记账', label: '银行存款日记账', path: '银行存款日记账' },
          { id: '固定资产卡片', label: '固定资产卡片', path: '固定资产卡片' },
          { id: '其他辅助账簿', label: '其他辅助账簿', path: '其他辅助账簿' }
        ]
      },
      {
        id: '财务报告',
        label: '财务报告',
        children: [
          { id: '月度财务报告', label: '月度财务报告', path: '月度财务报告' },
          { id: '季度财务报告', label: '季度财务报告', path: '季度财务报告' },
          { id: '半年度财务报告', label: '半年度财务报告', path: '半年度财务报告' },
          { id: '年度财务报告', label: '年度财务报告', path: '年度财务报告' },
          { id: '专项财务报告', label: '专项财务报告', path: '专项财务报告' }
        ]
      },
      {
        id: '其他会计资料',
        label: '其他会计资料',
        children: [
          { id: '银行存款余额调节表', label: '银行存款余额调节表', path: '银行存款余额调节表' },
          { id: '银行对账单', label: '银行对账单', path: '银行对账单' },
          { id: '纳税申报表', label: '纳税申报表', path: '纳税申报表' },
          { id: '会计档案移交清册', label: '会计档案移交清册', path: '会计档案移交清册' },
          { id: '会计档案保管清册', label: '会计档案保管清册', path: '会计档案保管清册' },
          { id: '会计档案销毁清册', label: '会计档案销毁清册', path: '会计档案销毁清册' },
          { id: '会计档案鉴定意见书', label: '会计档案鉴定意见书', path: '会计档案鉴定意见书' }
        ]
      }
    ]
  },
  // --- 2. 档案作业 (Operations) ---
  {
    id: ViewState.ARCHIVE_OPS,
    label: '档案作业',
    icon: FileText, // Change Icon to distinguish
    permission: 'nav:operations',
    children: [
      { id: '档案装盒', label: '档案装盒', path: '档案装盒' },
      { id: '档案组卷', label: '档案组卷', path: '档案组卷' },
      { id: '归档审批', label: '归档审批', path: '归档审批' },
      { id: '归档批次', label: '归档批次', path: '归档批次' },
      { id: '开放鉴定', label: '开放鉴定', path: '开放鉴定' },
      { id: '销毁鉴定', label: '销毁鉴定', path: '销毁鉴定' }
    ]
  },
  {
    id: ViewState.ARCHIVE_UTILIZATION,
    label: '档案利用',
    icon: Search,
    permission: 'nav:utilization',
    children: [
      { id: '全文检索', label: '全文检索', path: '全文检索' },
      { id: '穿透联查', label: '穿透联查', path: '穿透联查' },
      { id: '借阅申请', label: '借阅申请', path: '借阅申请' }
    ]
  },
  // DESTRUCTION removed as per user request (redundant with Operations)
  // WAREHOUSE removed as per expert review (Out of Scope)
  // MATCHING removed as per user request (integrated into Pre-Archive > Voucher Link)
  { id: ViewState.STATS, label: '数据统计', icon: BarChart3, permission: 'nav:stats' },
  {
    id: ViewState.SETTINGS,
    label: '系统设置',
    icon: Settings,
    permission: 'nav:settings',
    children: [
      { id: 'settings-basic', label: '基础配置', path: ROUTE_PATHS.SETTINGS_BASIC },
      { id: 'settings-org', label: '组织管理', path: ROUTE_PATHS.SETTINGS_ORG },
      { id: 'settings-user', label: '用户权限', path: ROUTE_PATHS.SETTINGS_USERS },
      { id: 'settings-ops', label: '系统运维', path: ROUTE_PATHS.SETTINGS_INTEGRATION },
    ]
  },
];

// NOTIFICATIONS 已移除 - 通知数据通过 /api/notifications 获取真实数据
export const NOTIFICATIONS: AppNotification[] = [];

// RECENT_DOCS 已移除 - 最近归档记录已从档案门户移除
export const RECENT_DOCS = [];

// ============================================================================
// SAP 集成接口类型配置
// ============================================================================
// SAP S/4HANA 支持多种集成方式，此处定义四种接口类型作为产品能力预留
// - OData: 已实现的现代化 REST 风格集成
// - RFC/BAPI: 传统 SAP 集成方式（预留）
// - IDoc: 异步批量数据交换（预留）
// - SAP Gateway: 自定义 OData 服务构建（预留）
// ============================================================================

export const SAP_INTERFACE_TYPES = [
  {
    key: 'odata',
    name: 'OData 服务',
    description: '现代化 REST 风格集成，基于 HTTP/JSON',
    status: 'implemented' as const,
    icon: Cloud,
  },
  {
    key: 'rfc_bapi',
    name: 'RFC/BAPI',
    description: '传统 SAP 集成方式，需要 SAP Java Connector',
    status: 'reserved' as const,
    icon: Server,
  },
  {
    key: 'idoc',
    name: 'IDoc',
    description: '异步批量数据交换，类似 EDI 格式',
    status: 'reserved' as const,
    icon: FileText,
  },
  {
    key: 'gateway',
    name: 'SAP Gateway',
    description: '自定义 OData 服务构建',
    status: 'reserved' as const,
    icon: Settings,
  },
] as const;

// SAP 接口类型状态定义
export const SAP_INTERFACE_STATUS = {
  implemented: 'implemented',   // 已实现
  reserved: 'reserved',         // 产品能力预留
  planned: 'planned',           // 计划中
  deprecated: 'deprecated',     // 已废弃
} as const;

// ============================================================================
// MOCK 数据清理说明
// ============================================================================
// 以下配置常量已移除（未被实际使用）：
// - PRE_ARCHIVE_POOL_CONFIG, PRE_ARCHIVE_OCR_CONFIG, PRE_ARCHIVE_LINK_CONFIG
// - COLLECTION_ONLINE_CONFIG, COLLECTION_SCAN_CONFIG, COLLECTION_CONFIG
// - ARCHIVE_VIEW_CONFIG, ACCOUNTING_VOUCHER_CONFIG, ACCOUNTING_LEDGER_CONFIG
// - FINANCIAL_REPORT_CONFIG, OTHER_ACCOUNTING_MATERIALS_CONFIG, ARCHIVE_BOX_CONFIG
// - WAREHOUSE_RACK_CONFIG, WAREHOUSE_ENV_CONFIG
// - BORROWING_CONFIG, QUERY_CONFIG, GENERIC_CONFIG
//
// 如需表格配置，请使用 TableFilters/TablePreview 组件动态生成
// ============================================================================
