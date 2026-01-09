// Input: 复杂度数据类型
// Output: API 请求函数
// Pos: src/api/ 质量监控 API
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import axios from 'axios';
import type {
    ComplexityHistory,
    ComplexitySnapshot,
} from '@/pages/quality/types';

const API_BASE = '/api/quality';

/**
 * 获取复杂度历史数据
 */
export async function getComplexityHistory(): Promise<ComplexityHistory> {
    const response = await axios.get<ComplexityHistory>(`${API_BASE}/history`);
    return response.data;
}

/**
 * 生成新的复杂度快照
 */
export async function generateSnapshot(): Promise<ComplexityHistory> {
    const response = await axios.post<ComplexityHistory>(`${API_BASE}/snapshot`);
    return response.data;
}

/**
 * 获取最新快照
 */
export async function getLatestSnapshot(): Promise<ComplexitySnapshot> {
    const response = await axios.get<ComplexitySnapshot>(`${API_BASE}/latest`);
    return response.data;
}
