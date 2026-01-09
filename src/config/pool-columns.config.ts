// Input: PreArchiveStatus (10种预处理状态)
// Output: ColumnGroupConfig (4个主列)
// Pos: src/config/pool-columns.config.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 电子凭证池看板列分组配置
 */

// 状态类型定义（项目中已有的状态）
export type PreArchiveStatus =
  | 'DRAFT'              // 草稿
  | 'PENDING_CHECK'      // 待检测
  | 'CHECK_FAILED'       // 检测失败
  | 'PENDING_METADATA'   // 待补录
  | 'MATCH_PENDING'      // 待匹配
  | 'MATCHED'            // 已匹配
  | 'PENDING_ARCHIVE'    // 待归档
  | 'PENDING_APPROVAL'   // 审批中
  | 'ARCHIVING'          // 归档中
  | 'ARCHIVED';          // 已归档

export interface SubStateConfig {
  value: PreArchiveStatus;
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
}

/**
 * 电子凭证池看板列分组配置
 *
 * 将10种预处理状态分组为4个主列
 */
export const POOL_COLUMN_GROUPS: ColumnGroupConfig[] = [
  {
    id: 'pending',
    title: '待处理',
    subStates: [
      { value: 'DRAFT', label: '草稿' },
      { value: 'PENDING_CHECK', label: '待检测' },
    ],
    actions: [
      { key: 'recheck', label: '重新检测' },
      { key: 'delete', label: '删除', danger: true },
    ],
  },
  {
    id: 'needs-attention',
    title: '需要处理',
    subStates: [
      { value: 'CHECK_FAILED', label: '检测失败' },
      { value: 'PENDING_METADATA', label: '待补录' },
    ],
    actions: [
      { key: 'edit-metadata', label: '编辑元数据' },
      { key: 'retry-check', label: '重试检测' },
      { key: 'delete', label: '删除', danger: true },
    ],
  },
  {
    id: 'ready',
    title: '准备就绪',
    subStates: [
      { value: 'MATCH_PENDING', label: '待匹配' },
      { value: 'MATCHED', label: '已匹配' },
    ],
    actions: [
      { key: 'smart-match', label: '智能匹配' },
      { key: 'manual-link', label: '手动关联' },
      { key: 'move-to-archive', label: '移入待归档' },
    ],
  },
  {
    id: 'processing',
    title: '处理中',
    subStates: [
      { value: 'PENDING_ARCHIVE', label: '待归档' },
      { value: 'PENDING_APPROVAL', label: '审批中' },
      { value: 'ARCHIVING', label: '归档中' },
      { value: 'ARCHIVED', label: '已归档' },
    ],
    actions: [
      { key: 'view-detail', label: '查看详情' },
      { key: 'cancel-archive', label: '取消归档', danger: true },
      { key: 'batch-approve', label: '批量审批' },
    ],
  },
];

/**
 * 状态到列的映射表
 */
const STATE_TO_COLUMN_MAP = new Map<PreArchiveStatus, string>(
  POOL_COLUMN_GROUPS.flatMap(group =>
    group.subStates.map(sub => [sub.value, group.id] as [PreArchiveStatus, string])
  )
);

/**
 * 状态到标签的映射表
 */
const STATE_TO_LABEL_MAP = new Map<PreArchiveStatus, string>(
  POOL_COLUMN_GROUPS.flatMap(group =>
    group.subStates.map(sub => [sub.value, sub.label] as [PreArchiveStatus, string])
  )
);

/**
 * 根据状态获取所属列配置
 */
export function getColumnByState(state: PreArchiveStatus): ColumnGroupConfig | null {
  const columnId = STATE_TO_COLUMN_MAP.get(state);
  return POOL_COLUMN_GROUPS.find(g => g.id === columnId) || null;
}

/**
 * 获取状态的显示标签
 */
export function getSubStateLabel(state: PreArchiveStatus): string {
  return STATE_TO_LABEL_MAP.get(state) || state;
}

/**
 * 获取指定列的所有状态值
 */
export function getColumnStates(columnId: string): PreArchiveStatus[] {
  const column = POOL_COLUMN_GROUPS.find(g => g.id === columnId);
  return column?.subStates.map(s => s.value) || [];
}
