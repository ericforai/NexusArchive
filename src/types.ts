// Input: lucide-react 图标类型
// Output: 前端枚举与接口类型集合
// Pos: 前端共享类型定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { LucideIcon } from 'lucide-react';

export enum ViewState {
  PORTAL = 'PORTAL',
  PRE_ARCHIVE = 'PRE_ARCHIVE',
  COLLECTION = 'COLLECTION',
  ARCHIVE_MGMT = 'ARCHIVE_MGMT', // Deprecated? Sending to keep for history or strict refactor? Let's keep for now but add new ones.
  ACCOUNT_ARCHIVES = 'ACCOUNT_ARCHIVES', // New: Repository
  ARCHIVE_OPS = 'ARCHIVE_OPS', // New: Operations
  ARCHIVE_UTILIZATION = 'ARCHIVE_UTILIZATION', // New: Query + Borrowing
  QUERY = 'QUERY', // Keep for backward compat or sub-feature reference? Maybe deprecated but safe to keep.
  BORROWING = 'BORROWING',
  WAREHOUSE = 'WAREHOUSE',
  STATS = 'STATS',
  SETTINGS = 'SETTINGS',
  ADMIN = 'ADMIN',
  PANORAMA = 'PANORAMA',
  LANDING = 'LANDING',
  DESTRUCTION = 'DESTRUCTION',
  ABNORMAL = 'ABNORMAL',
  COMPLIANCE_REPORT = 'COMPLIANCE_REPORT',
  MATCHING = 'MATCHING', // New: 智能匹配
}



export interface NavItem {
  id: string;
  label: string;
  icon?: LucideIcon;
  path?: string; // New: Explicit path or identifier for routing
  permission?: string;
  children?: NavItem[]; // New: Recursive structure
}

export interface ArchiveStat {
  label: string;
  value: string | number;
  trend: number;
  trendLabel: string;
  icon: LucideIcon;
  color: string;
}

export interface Notification {
  id: string;
  title: string;
  time: string;
  type: 'info' | 'warning' | 'success';
}

// Dynamic Table Configuration Types
export type ColumnType = 'text' | 'status' | 'progress' | 'money' | 'date' | 'datetime' | 'tag' | 'action';

export interface TableColumn {
  key: string;
  header: string;
  type: ColumnType;
  width?: string;
}

export interface GenericRow {
  id: string;
  rawStatus?: string; // Stored raw status code for logic checks
  [key: string]: any;
}

export interface ModuleConfig {
  columns: TableColumn[];
  data: GenericRow[];
}

// Association Rule Configuration Types
export interface MatchRule {
  id: string;
  label: string;
  description: string;
  enabled: boolean;
  weight: number;
  type?: 'amount' | 'date' | 'vendor' | 'code' | 'custom';
  config?: Record<string, any>; // Additional rule-specific configuration
}

export interface LinkedDocument {
  id: string;
  type: 'invoice' | 'contract' | 'receipt' | 'order' | 'other';
  code: string;
  name: string;
  amount?: string;
  date?: string;
  status?: string;
  fileUrl?: string;
}

export interface VoucherAssociation {
  id: string;
  voucherNo: string;
  amount: string;
  date: string;
  status: string;

  // Relationships
  sourceDocuments: LinkedDocument[]; // Upstream: Invoices, Receipts
  supportingDocuments: LinkedDocument[]; // Downstream: Contracts, Orders

  // Metadata for UI
  matchScore: number;
  autoLinkMethod: string;
}

export interface AssociationRelation {
  voucherId: string;
  voucherNo: string;
  relatedDocuments: {
    id: string;
    type: 'invoice' | 'contract' | 'receipt' | 'other';
    code: string;
    name: string;
    matchScore: number;
  }[];
  relationType: 'one-to-one' | 'one-to-many' | 'many-to-many';
}

// Scheduled Task Types
export interface ScheduledTask {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  schedule: TaskSchedule;
  taskType: 'auto_match' | 'compliance_check' | 'data_sync' | 'custom';
  config: Record<string, any>;
  lastRun?: string;
  nextRun?: string;
  runCount: number;
  status: 'idle' | 'running' | 'error';
}

export interface TaskSchedule {
  type: 'daily' | 'weekly' | 'monthly' | 'custom' | 'interval';
  time?: string; // HH:mm format
  days?: number[]; // For weekly: [1-7], for monthly: [1-31]
  interval?: number; // Minutes for interval type
  cronExpression?: string; // For custom cron
}

export interface TaskExecutionHistory {
  id: string;
  taskId: string;
  taskName: string;
  startTime: string;
  endTime?: string;
  status: 'success' | 'failed' | 'running';
  result?: {
    matchedCount?: number;
    errorMessage?: string;
    details?: Record<string, any>;
  };
}

// Notification Enhancement
export interface NotificationRule {
  id: string;
  name: string;
  enabled: boolean;
  trigger: 'task_complete' | 'task_failed' | 'match_threshold' | 'compliance_alert' | 'custom';
  conditions: Record<string, any>;
  channels: ('system' | 'email' | 'sms')[];
  template?: string;
}

export interface EnhancedNotification extends Notification {
  read: boolean;
  category?: 'task' | 'match' | 'compliance' | 'system';
  actionUrl?: string;
  metadata?: Record<string, any>;
}

// Archive Approval Types
export interface ApprovalWorkflow {
  id: string;
  name: string;
  steps: ApprovalStep[];
  enabled: boolean;
}

export interface ApprovalStep {
  id: string;
  name: string;
  approver?: string;
  approverRole?: string;
  order: number;
  required: boolean;
  status: 'pending' | 'approved' | 'rejected' | 'skipped';
  comment?: string;
  timestamp?: string;
}

export interface ArchiveApproval {
  id: string;
  archiveNo: string;
  title: string;
  category: string;
  year: string;
  applicant: string;
  applyDate: string;
  workflowId: string;
  workflow: ApprovalWorkflow;
  status: 'draft' | 'submitted' | 'approving' | 'approved' | 'rejected';
  duplicateCheck: {
    passed: boolean;
    duplicates?: string[];
  };
  complianceCheck: {
    enabled: boolean;
    score?: number;
    reportId?: string;
  };
  currentStep?: number;
}

// Inventory Types
export interface Inventory {
  id: string;
  type: 'open' | 'destruction';
  title: string;
  date: string;
  creator: string;
  approver?: string;
  status: 'draft' | 'approved' | 'completed';
  items: InventoryItem[];
  totalCount: number;
}

export interface InventoryItem {
  id: string;
  archiveNo: string;
  title: string;
  category: string;
  year: string;
  period: string;
}

// Destruction Repository Types
export interface DestructionRecord {
  id: string;
  archiveNo: string;
  title: string;
  category: string;
  destructionDate: string;
  batchId: string;
  status: 'pending' | 'destroyed' | 'recovered';
  canRecover: boolean;
}

// API Response Types
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

// Settings & Integration Types
export interface User {
  id: string;
  username: string;
  fullName: string;
  email?: string;
  phone?: string;
  departmentId?: string;
  status: string;
  roleIds?: string[];
  createdAt?: string;
}

export interface Role {
  id: string;
  name: string;
  code: string;
  roleCategory: string;
  isExclusive: boolean;
  description?: string;
  permissions?: string;
  dataScope?: string;
  type: string;
  createdAt?: string;
}

export interface ErpConfig {
  id: number;
  name: string;
  erpType: string;
  configJson: string;
  isActive: number;
  status?: string;
  createdTime?: string;
  lastModifiedTime?: string;
}

export interface ErpScenario {
  id: number;
  configId: number;
  scenarioKey: string;
  name: string;
  description: string;
  isActive: boolean;
  syncStrategy: 'REALTIME' | 'CRON' | 'MANUAL';
  cronExpression?: string;
  lastSyncTime?: string;
  lastSyncStatus: string;
  lastSyncMsg?: string;
  paramsJson?: string;
}

export interface ErpSubInterface {
  id: number;
  scenarioId: number;
  interfaceKey: string;
  interfaceName: string;
  description?: string;
  isActive: boolean;
  sortOrder: number;
  configJson?: string;
}

export interface SyncHistory {
  id: number;
  scenarioId: number;
  syncStartTime: string;
  syncEndTime?: string;
  status: 'RUNNING' | 'SUCCESS' | 'FAIL';
  totalCount: number;
  successCount: number;
  failCount: number;
  errorMessage?: string;
  syncParams?: string;
  fourNatureSummary?: string;
}

export interface LicenseInfo {
  expireAt: string;
  maxUsers: number;
  nodeLimit: number;
  raw?: string;
}

export interface AuditLog {
  id: string;
  userId: string;
  username: string;
  action: string;
  resourceType: string;
  resourceId?: string;
  operationResult?: string;
  riskLevel?: string;
  details?: string;
  clientIp?: string;
  createdAt?: string;
}

export interface GlobalSearchDTO {
  id: string;
  archiveCode: string;
  title: string;
  matchType: 'ARCHIVE' | 'METADATA';
  matchDetail: string;
  score?: number;
}
