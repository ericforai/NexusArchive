// Input: 复杂度快照数据结构
// Output: TypeScript 类型定义
// Pos: src/pages/quality/ 类型定义
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

/**
 * 文件违规详情
 */
export interface FileViolation {
    /** 文件路径（相对于项目根目录） */
    path: string;
    /** 文件总行数 */
    lines: number;
    /** 最大函数行数 */
    maxFunctionLines: number;
    /** 圈复杂度 */
    complexity: number;
    /** 违规规则列表 */
    violations: string[];
}

/**
 * 快照摘要
 */
export interface SnapshotSummary {
    /** 总违规数 */
    total: number;
    /** 高严重度违规数 */
    high: number;
    /** 中严重度违规数 */
    medium: number;
    /** 低严重度违规数 */
    low: number;
}

/**
 * 复杂度快照
 */
export interface ComplexitySnapshot {
    /** 快照时间戳 */
    timestamp: string;
    /** Git commit hash */
    commit: string;
    /** Git 分支名 */
    branch: string;
    /** 违规摘要 */
    summary: SnapshotSummary;
    /** 违规文件列表 */
    files: FileViolation[];
}

/**
 * 历史元数据
 */
export interface HistoryMetadata {
    /** 格式版本 */
    formatVersion: string;
    /** 创建时间 */
    createdAt: string;
    /** 最后更新时间 */
    lastUpdated: string;
}

/**
 * 完整历史数据
 */
export interface ComplexityHistory {
    /** 元数据 */
    metadata: HistoryMetadata;
    /** 快照数组 */
    snapshots: ComplexitySnapshot[];
}

/**
 * 严重程度
 */
export type SeverityLevel = 'high' | 'medium' | 'low';
