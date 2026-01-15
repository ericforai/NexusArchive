// Input: SimplifiedPreArchiveStatus enum (5种核心状态)
// Output: ColumnGroupConfig (5个主列)
// Pos: src/config/pool-columns.config.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 电子凭证池看板列分组配置
 */

// 简化的预处理状态枚举（5个核心状态）
export enum SimplifiedPreArchiveStatus {
  PENDING_CHECK = 'PENDING_CHECK',    // 待检测
  NEEDS_ACTION = 'NEEDS_ACTION',      // 待处理
  READY_TO_MATCH = 'READY_TO_MATCH',  // 可匹配
  READY_TO_ARCHIVE = 'READY_TO_ARCHIVE', // 可归档（核心）
  COMPLETED = 'COMPLETED',            // 已完成
}

/**
 * 状态显示配置
 * 定义每个状态的视觉呈现（颜色、图标、标签、描述）
 */
export const STATUS_CONFIG: Record<SimplifiedPreArchiveStatus, {
  color: string;
  icon: string;
  label: string;
  description: string;
}> = {
  [SimplifiedPreArchiveStatus.PENDING_CHECK]: {
    color: '#94a3b8',
    icon: 'circle-dot',
    label: '待检测',
    description: '新导入的凭证，等待四性检测',
  },
  [SimplifiedPreArchiveStatus.NEEDS_ACTION]: {
    color: '#f59e0b',
    icon: 'alert-circle',
    label: '待处理',
    description: '检测失败或需要补全信息',
  },
  [SimplifiedPreArchiveStatus.READY_TO_MATCH]: {
    color: '#3b82f6',
    icon: 'link',
    label: '可匹配',
    description: '可以进行凭证关联',
  },
  [SimplifiedPreArchiveStatus.READY_TO_ARCHIVE]: {
    color: '#10b981',
    icon: 'check-circle',
    label: '可归档',
    description: '已就绪，可以提交归档',
  },
  [SimplifiedPreArchiveStatus.COMPLETED]: {
    color: '#64748b',
    icon: 'check-circle',
    label: '已完成',
    description: '归档流程已完成',
  },
};

export interface SubStateConfig {
  value: SimplifiedPreArchiveStatus;
  label: string;
}

export interface ColumnAction {
  key: string;
  label: string;
  icon?: string;
  danger?: boolean;
}

export interface ColumnGroupConfig {
  id: string;
  title: string;
  subStates: SubStateConfig[];
  actions: ColumnAction[];
  highlight?: boolean; // 标记为重点列（如"可归档"）
}

/**
 * 电子凭证池看板列分组配置
 *
 * 将5种简化状态映射为5个主列
 */
export const POOL_COLUMN_GROUPS: ColumnGroupConfig[] = [
  {
    id: 'pending',
    title: '待检测',
    subStates: [
      { value: SimplifiedPreArchiveStatus.PENDING_CHECK, label: '待检测' },
    ],
    actions: [
      { key: 'recheck', label: '重新检测' },
      { key: 'delete', label: '删除', danger: true },
    ],
  },
  {
    id: 'needs-action',
    title: '待处理',
    subStates: [
      { value: SimplifiedPreArchiveStatus.NEEDS_ACTION, label: '待处理' },
    ],
    actions: [
      { key: 'edit-metadata', label: '编辑元数据' },
      { key: 'retry-check', label: '重试检测' },
      { key: 'delete', label: '删除', danger: true },
    ],
  },
  {
    id: 'ready-to-match',
    title: '可匹配',
    subStates: [
      { value: SimplifiedPreArchiveStatus.READY_TO_MATCH, label: '可匹配' },
    ],
    actions: [
      { key: 'smart-match', label: '智能匹配' },
      { key: 'manual-link', label: '手动关联' },
    ],
  },
  {
    id: 'ready-to-archive',
    title: '可归档',
    subStates: [
      { value: SimplifiedPreArchiveStatus.READY_TO_ARCHIVE, label: '可归档' },
    ],
    highlight: true, // 标记为重点列
    actions: [
      { key: 'batch-archive', label: '批量归档' },
      { key: 'view-detail', label: '查看详情' },
    ],
  },
  {
    id: 'completed',
    title: '已完成',
    subStates: [
      { value: SimplifiedPreArchiveStatus.COMPLETED, label: '已完成' },
    ],
    actions: [
      { key: 'view-detail', label: '查看详情' },
    ],
  },
];

/**
 * 默认看板筛选状态
 */
export const DEFAULT_DASHBOARD_FILTER = SimplifiedPreArchiveStatus.READY_TO_ARCHIVE;

/**
 * 状态到列的映射表
 */
const STATE_TO_COLUMN_MAP = new Map<SimplifiedPreArchiveStatus, string>(
  POOL_COLUMN_GROUPS.flatMap(group =>
    group.subStates.map(sub => [sub.value, group.id] as [SimplifiedPreArchiveStatus, string])
  )
);

/**
 * 状态到标签的映射表
 */
const STATE_TO_LABEL_MAP = new Map<SimplifiedPreArchiveStatus, string>(
  POOL_COLUMN_GROUPS.flatMap(group =>
    group.subStates.map(sub => [sub.value, sub.label] as [SimplifiedPreArchiveStatus, string])
  )
);

/**
 * 根据状态获取所属列配置
 */
export function getColumnByState(state: SimplifiedPreArchiveStatus): ColumnGroupConfig | null {
  const columnId = STATE_TO_COLUMN_MAP.get(state);
  return POOL_COLUMN_GROUPS.find(g => g.id === columnId) || null;
}

/**
 * 获取状态的显示标签
 */
export function getSubStateLabel(state: SimplifiedPreArchiveStatus): string {
  return STATE_TO_LABEL_MAP.get(state) || state;
}

/**
 * 获取指定列的所有状态值
 */
export function getColumnStates(columnId: string): SimplifiedPreArchiveStatus[] {
  const column = POOL_COLUMN_GROUPS.find(g => g.id === columnId);
  return column?.subStates.map(s => s.value) || [];
}

/**
 * 旧状态代码映射到新简化状态的映射表
 * 用于统一处理后端可能返回的历史状态值
 */
export const LEGACY_STATUS_MAP: Record<string, SimplifiedPreArchiveStatus> = {
  // 待检测
  'PENDING_CHECK': SimplifiedPreArchiveStatus.PENDING_CHECK,
  // 待处理
  'CHECK_FAILED': SimplifiedPreArchiveStatus.NEEDS_ACTION,
  'PENDING_METADATA': SimplifiedPreArchiveStatus.NEEDS_ACTION,
  'NEEDS_ACTION': SimplifiedPreArchiveStatus.NEEDS_ACTION,
  // 可匹配
  'READY_TO_MATCH': SimplifiedPreArchiveStatus.READY_TO_MATCH,
  'MATCHED': SimplifiedPreArchiveStatus.READY_TO_MATCH, // 实际上匹配完了可能也是这个状态，或者流转到下一步
  // 可归档
  'PENDING_ARCHIVE': SimplifiedPreArchiveStatus.READY_TO_ARCHIVE,
  'READY_TO_ARCHIVE': SimplifiedPreArchiveStatus.READY_TO_ARCHIVE,
  // 已完成
  'ARCHIVED': SimplifiedPreArchiveStatus.COMPLETED,
  'COMPLETED': SimplifiedPreArchiveStatus.COMPLETED
};

/**
 * 解析状态：将任意（可能过时的）状态字符串解析为标准的 SimplifiedPreArchiveStatus
 * 如果无法解析，默认返回 NEEDS_ACTION 以引起注意
 */
export function resolveStatus(rawStatus: string): SimplifiedPreArchiveStatus {
  // 1. 如果已经是标准状态，直接返回
  if (Object.values(SimplifiedPreArchiveStatus).includes(rawStatus as SimplifiedPreArchiveStatus)) {
    return rawStatus as SimplifiedPreArchiveStatus;
  }
  // 2. 尝试映射旧状态
  return LEGACY_STATUS_MAP[rawStatus] || SimplifiedPreArchiveStatus.NEEDS_ACTION;
}
