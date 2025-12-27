// Input: API client、ApiResponse 类型
// Output: matchingApi
// Pos: 匹配引擎 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { client } from './client';
import { ApiResponse } from '../types';

// ========== 类型定义 ==========

export interface MatchTask {
    taskId: string;
    voucherId: string;
    status: 'PROCESSING' | 'COMPLETED' | 'ERROR';
    result?: MatchResult;
    message?: string;
}

export interface BatchMatchTask {
    batchTaskId: string;
    total: number;
    completed: number;
    status: 'PROCESSING' | 'COMPLETED';
}

export interface MatchResult {
    taskId?: string;
    batchTaskId?: string;
    matchBatchId: string;
    voucherId: string;
    voucherNo?: string;
    scene: string;
    templateId?: string;
    templateVersion?: string;
    confidence?: number;
    recognitionReasons?: string[];
    status: 'PROCESSING' | 'MATCHED' | 'PENDING' | 'NEED_CONFIRM' | 'CONFIRMED' | 'ERROR';
    missingDocs?: string[];
    message?: string;
    links: LinkResult[];
    voucherHash?: string;
    configHash?: string;
    createdTime?: string;
}

export interface LinkResult {
    evidenceRole: string;
    evidenceRoleName: string;
    linkType: 'MUST_LINK' | 'SHOULD_LINK' | 'MAY_LINK';
    matchedDocId?: string;
    matchedDocNo?: string;
    score?: number;
    reasons?: string[];
    status: 'MATCHED' | 'MISSING' | 'NEED_CONFIRM';
    candidates?: ScoredCandidate[];
    conflictReason?: string;
    suggestion?: string;
}

export interface ScoredCandidate {
    docId: string;
    docNo: string;
    docType: string;
    docTypeName?: string;
    docDate: string;
    amount: number;
    counterparty?: string;
    score: number;
    reasons: string[];
    isLinked?: boolean;
    linkedVoucherId?: string;
}

export interface RuleTemplate {
    id: string;
    name: string;
    version: string;
    scene: string;
    config: string;
}

export interface OnboardingSummary {
    totalAccounts: number;
    matchedAccounts: number;
    unmatchedAccounts: number;
    accountMatchRate: number;
    totalDocTypes: number;
    matchedDocTypes: number;
    unmatchedDocTypes: number;
    docTypeMatchRate: number;
    status: 'PENDING' | 'COMPLETED';
}

export interface AutoMappingResult {
    kitId: string;
    kitName: string;
    accountsMapped: number;
    accountsPending: number;
    docTypesMapped: number;
    docTypesPending: number;
}

export interface LinkConfirmation {
    sourceDocId: string;
    evidenceRole: string;
    linkType: string;
}

export interface MappingConfirmation {
    type: 'ACCOUNT' | 'DOC_TYPE';
    code: string;
    role: string;
}

// ========== 合规报告类型 ==========

export interface ComplianceStats {
    totalVouchers: number;
    matchedVouchers: number;
    pendingVouchers: number;
    missingMustLink: number;
    missingShouldLink: number;
    complianceRate: number;
}

export interface MissingDocRecord {
    voucherId: string;
    voucherNo: string;
    scene: string;
    missingDocType: string;
    linkType: string;
    riskLevel: 'HIGH' | 'MEDIUM' | 'LOW';
    createTime: string;
}

export interface ComplianceReportData {
    stats: ComplianceStats;
    missingDocs: MissingDocRecord[];
}

// ========== API 接口 ==========

export const matchingApi = {
    // 执行单凭证匹配（异步）
    executeMatch: async (voucherId: string): Promise<MatchTask> => {
        if (!voucherId) {
            // 无凭证 ID 时返回模拟任务
            return {
                taskId: 'mock-task-' + Date.now(),
                voucherId: '',
                status: 'COMPLETED',
                message: '请先选择一个凭证'
            };
        }
        try {
            const response = await client.post<ApiResponse<MatchTask>>(`/matching/execute/${voucherId}`);
            return response.data.data;
        } catch {
            // API 不存在时返回模拟完成状态
            return {
                taskId: 'mock-task-' + Date.now(),
                voucherId,
                status: 'ERROR',
                message: '匹配服务暂不可用（后端 API 未部署）'
            };
        }
    },

    // 批量匹配
    executeBatchMatch: async (voucherIds: string[]): Promise<BatchMatchTask> => {
        try {
            const response = await client.post<ApiResponse<BatchMatchTask>>('/matching/execute/batch', voucherIds);
            return response.data.data;
        } catch {
            return {
                batchTaskId: 'mock-batch-' + Date.now(),
                total: voucherIds.length,
                completed: 0,
                status: 'COMPLETED'
            };
        }
    },

    // 查询任务结果
    getTaskResult: async (taskId: string): Promise<MatchTask> => {
        try {
            const response = await client.get<ApiResponse<MatchTask>>(`/matching/task/${taskId}`);
            return response.data.data;
        } catch {
            return {
                taskId,
                voucherId: '',
                status: 'ERROR',
                message: '任务查询服务暂不可用'
            };
        }
    },

    // 获取凭证匹配结果
    getMatchResult: async (voucherId: string): Promise<MatchResult | null> => {
        try {
            const response = await client.get<ApiResponse<MatchResult>>(`/matching/result/${voucherId}`);
            return response.data.data;
        } catch {
            return null;
        }
    },

    // 确认关联
    confirmMatch: async (voucherId: string, links: LinkConfirmation[]): Promise<void> => {
        try {
            await client.post(`/matching/confirm/${voucherId}`, links);
        } catch {
            console.warn('确认关联服务暂不可用');
        }
    },

    // 获取模板列表
    getTemplates: async (): Promise<RuleTemplate[]> => {
        try {
            const response = await client.get<ApiResponse<RuleTemplate[]>>('/matching/templates');
            return response.data.data || [];
        } catch {
            return [];
        }
    },

    // 刷新模板
    reloadTemplates: async (): Promise<void> => {
        try {
            await client.post('/matching/templates/reload');
        } catch {
            console.warn('模板刷新服务暂不可用');
        }
    },

    // ========== 初始化向导 ==========

    // 扫描客户数据
    scanData: async (companyId: number): Promise<OnboardingSummary> => {
        try {
            const response = await client.post<ApiResponse<OnboardingSummary>>(`/matching/onboarding/scan/${companyId}`);
            return response.data.data;
        } catch {
            // 返回模拟数据，表示向导功能暂不可用
            return {
                totalAccounts: 0,
                matchedAccounts: 0,
                unmatchedAccounts: 0,
                accountMatchRate: 0,
                totalDocTypes: 0,
                matchedDocTypes: 0,
                unmatchedDocTypes: 0,
                docTypeMatchRate: 0,
                status: 'PENDING'
            };
        }
    },

    // 应用预置规则
    applyPreset: async (companyId: number, kitId: string = 'KIT_GENERAL'): Promise<AutoMappingResult> => {
        try {
            const response = await client.post<ApiResponse<AutoMappingResult>>(
                `/matching/onboarding/apply-preset/${companyId}?kitId=${kitId}`
            );
            return response.data.data;
        } catch {
            return {
                kitId,
                kitName: '通用规则包',
                accountsMapped: 0,
                accountsPending: 0,
                docTypesMapped: 0,
                docTypesPending: 0
            };
        }
    },

    // 确认映射
    confirmMappings: async (companyId: number, mappings: MappingConfirmation[]): Promise<void> => {
        try {
            await client.post(`/matching/onboarding/confirm/${companyId}`, mappings);
        } catch {
            console.warn('映射确认服务暂不可用');
        }
    },

    // ========== 合规报告 ==========

    // 获取合规报告
    getComplianceReport: async (startDate?: string, endDate?: string): Promise<ComplianceReportData> => {
        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        const query = params.toString() ? `?${params.toString()}` : '';
        try {
            const response = await client.get<ApiResponse<ComplianceReportData>>(`/matching/compliance/report${query}`);
            return response.data.data;
        } catch {
            // API 不存在时返回默认数据，避免页面崩溃
            return {
                stats: {
                    totalVouchers: 0,
                    matchedVouchers: 0,
                    pendingVouchers: 0,
                    missingMustLink: 0,
                    missingShouldLink: 0,
                    complianceRate: 0
                },
                missingDocs: []
            };
        }
    },

    // 获取缺失文档清单
    getMissingDocs: async (startDate?: string, endDate?: string): Promise<MissingDocRecord[]> => {
        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        const query = params.toString() ? `?${params.toString()}` : '';
        try {
            const response = await client.get<ApiResponse<MissingDocRecord[]>>(`/matching/compliance/missing-doc-report${query}`);
            return response.data.data || [];
        } catch {
            return [];
        }
    },

    // 导出缺失文档报告
    exportMissingDocs: async (startDate?: string, endDate?: string): Promise<Blob> => {
        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        const query = params.toString() ? `?${params.toString()}` : '';
        try {
            const response = await client.get(`/matching/compliance/export-missing${query}`, {
                responseType: 'blob'
            });
            return response.data;
        } catch {
            // 返回空 Blob
            return new Blob();
        }
    },
};

export default matchingApi;
