// Input: 类型定义
// Output: 全宗类型导出
// Pos: FondsSwitcher 类型定义

/**
 * 全宗类型（本地定义，避免导入 API 层）
 */
export interface Fonds {
    id: string;
    fondsCode: string;
    fondsName: string;
    fondsNo?: string;
    companyName?: string;
    description?: string;
}
