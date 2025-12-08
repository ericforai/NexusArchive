import { LucideIcon } from 'lucide-react';

export enum ViewState {
  PORTAL = 'PORTAL',
  PRE_ARCHIVE = 'PRE_ARCHIVE',
  COLLECTION = 'COLLECTION',
  ARCHIVE_MGMT = 'ARCHIVE_MGMT',
  QUERY = 'QUERY',
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
}

export interface SubCategory {
  label: string;
  items: string[];
}

export interface NavItem {
  id: ViewState;
  label: string;
  icon: LucideIcon;
  permission?: string;
  subItems?: string[]; // 2级菜单（向后兼容）
  subCategories?: SubCategory[]; // 3级菜单结构
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
export type ColumnType = 'text' | 'status' | 'progress' | 'money' | 'date' | 'action';

export interface TableColumn {
  key: string;
  header: string;
  type: ColumnType;
  width?: string;
}

export interface GenericRow {
  id: string;
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
