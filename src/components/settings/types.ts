// Input: settings UI layer API contracts
// Output: settings UI API interface definitions
// Pos: src/components/settings/types.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import {
    ApiResponse,
    PageResult,
    User,
    Role,
    ErpConfig,
    ErpScenario,
    ErpSubInterface,
    SyncHistory,
    LicenseInfo,
    AuditLog,
    AdminSettingItem,
    AdminSettingUpdate,
    OrgNode,
    OrgImportResult,
    OrgTemplateInfo,
    IntegrationMonitoring,
    IntegrationDiagnosisResult,
    ReconciliationRecord,
    ConnectionTestResult
} from '../../types';

export interface AdminSettingsApi {
    getSettings: () => Promise<ApiResponse<AdminSettingItem[]>>;
    updateSettings: (settings: AdminSettingUpdate[]) => Promise<ApiResponse<void>>;
    getUsers: (params?: { page?: number; limit?: number }) => Promise<ApiResponse<PageResult<User>>>;
    createUser: (data: Partial<User> & { password?: string; roleIds?: string[]; status?: string }) => Promise<ApiResponse<User>>;
    resetPassword: (id: string, password: string) => Promise<ApiResponse<void>>;
    toggleUserStatus: (id: string, status: string) => Promise<ApiResponse<void>>;
    getRoles: (params?: { page?: number; limit?: number }) => Promise<ApiResponse<PageResult<Role>>>;
    createRole: (data: Partial<Role>) => Promise<ApiResponse<Role>>;
    updateRole: (id: string, data: Partial<Role>) => Promise<ApiResponse<void>>;
    deleteRole: (id: string) => Promise<ApiResponse<void>>;
    getPermissions: () => Promise<ApiResponse<Array<Record<string, unknown>>>>;
    listOrg: () => Promise<ApiResponse<OrgNode[]>>;
    getOrgTree: () => Promise<ApiResponse<OrgNode[]>>;
    createOrg: (data: Partial<OrgNode>) => Promise<ApiResponse<OrgNode>>;
    updateOrgOrder: (id: string, orderNum: number) => Promise<ApiResponse<void>>;
    deleteOrg: (id: string) => Promise<ApiResponse<void>>;
    bulkOrg: (items: Array<Partial<OrgNode>>) => Promise<ApiResponse<void>>;
    importOrg: (file: File) => Promise<ApiResponse<OrgImportResult>>;
    downloadOrgTemplate: () => Promise<ApiResponse<OrgTemplateInfo>>;
}

export interface IntegrationSettingsApi {
    getConfigs: () => Promise<ApiResponse<ErpConfig[]>>;
    getScenarios: (configId: number) => Promise<ApiResponse<ErpScenario[]>>;
    getSubInterfaces: (scenarioId: number) => Promise<ApiResponse<ErpSubInterface[]>>;
    getSyncHistory: (scenarioId: number) => Promise<ApiResponse<SyncHistory[]>>;
    updateScenario: (data: Partial<ErpScenario>) => Promise<ApiResponse<void>>;
    toggleSubInterface: (id: number) => Promise<ApiResponse<void>>;
    triggerSync: (id: number) => Promise<ApiResponse<void>>;
    testConnection: (configId: number) => Promise<ApiResponse<ConnectionTestResult>>;
    triggerReconciliation: (params: Record<string, unknown>) => Promise<ApiResponse<ReconciliationRecord>>;
    diagnoseConfig: (id: number) => Promise<ApiResponse<IntegrationDiagnosisResult>>;
    updateScenarioParams: (scenarioId: number, params: Record<string, unknown>) => Promise<ApiResponse<void>>;
    saveConfig: (config: Partial<ErpConfig>) => Promise<ApiResponse<void>>;
    deleteConfig: (id: number) => Promise<ApiResponse<void>>;
    getIntegrationMonitoring: () => Promise<ApiResponse<IntegrationMonitoring>>;
}

export interface LicenseSettingsApi {
    load: (licenseText: string) => Promise<ApiResponse<LicenseInfo>>;
    getCurrent: () => Promise<ApiResponse<LicenseInfo>>;
}

export interface AuditSettingsApi {
    getLogs: (params?: Record<string, unknown>) => Promise<ApiResponse<PageResult<AuditLog>>>;
}
