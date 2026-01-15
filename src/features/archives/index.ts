// Input: archives feature 子模块
// Output: 统一导出
// Pos: src/features/archives/index.ts

export * from './routeConfigs';
export * from './useArchiveActions';
export * from './useArchiveListController';
export * from './useSmartMatching';

// Controllers module (包含 hooks、types、utils)
export * from './controllers';

// Types needed by pages (已通过 controllers 导出，保留向后兼容)
export type { PoolStatusFilter } from './controllers/types';
