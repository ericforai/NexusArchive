// Input: React, legacyImportApi, toast notification service
// Output: Field mapping and template download hook (useFieldMapping)
// Pos: src/pages/admin/LegacyImportPage/hooks/useFieldMapping.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback } from 'react';
import { legacyImportApi } from '../../../../api/legacyImport';
import { toast } from '../../../../utils/notificationService';

/**
 * 模板类型
 */
export type TemplateType = 'csv' | 'excel';

/**
 * 必需字段定义
 */
export interface RequiredField {
    name: string;
    label: string;
    description: string;
    example: string;
    validation: string;
}

/**
 * 可选字段定义
 */
export interface OptionalField {
    name: string;
    label: string;
    description: string;
    example: string;
}

/**
 * 下载状态
 */
export interface DownloadState {
    downloading: boolean;
    templateType: TemplateType | null;
}

/**
 * 必需字段列表（定义在组件外部以避免依赖警告）
 */
const REQUIRED_FIELDS: RequiredField[] = [
    {
        name: 'fonds_no',
        label: '全宗号',
        description: '档案所属的全宗编号',
        example: 'JD-001',
        validation: '字母数字下划线，长度1-50',
    },
    {
        name: 'fonds_name',
        label: '全宗名称',
        description: '档案所属的全宗名称',
        example: '京东集团',
        validation: '长度1-100',
    },
    {
        name: 'archive_year',
        label: '归档年度',
        description: '档案的归档年份',
        example: '2024',
        validation: '有效年份1900-2100',
    },
    {
        name: 'doc_type',
        label: '档案类型',
        description: '档案的分类类型',
        example: '凭证、报表、账簿等',
        validation: '长度1-50',
    },
    {
        name: 'title',
        label: '档案标题',
        description: '档案的标题或名称',
        example: '2024年1月记账凭证',
        validation: '长度1-255',
    },
    {
        name: 'retention_policy_name',
        label: '保管期限名称',
        description: '档案的保管期限',
        example: '永久、30年、10年等',
        validation: '必须是系统中已存在的保管期限名称',
    },
];

/**
 * 可选字段列表（定义在组件外部以避免依赖警告）
 */
const OPTIONAL_FIELDS: OptionalField[] = [
    {
        name: 'entity_name',
        label: '法人实体名称',
        description: '档案所属的法人实体名称',
        example: '京东科技（北京）有限公司',
    },
    {
        name: 'entity_tax_code',
        label: '统一社会信用代码',
        description: '法人实体的统一社会信用代码',
        example: '91110000XXXXXXXXXX',
    },
    {
        name: 'doc_date',
        label: '形成日期',
        description: '档案的形成或签署日期',
        example: '2024-01-15',
    },
    {
        name: 'amount',
        label: '金额',
        description: '档案涉及的金额',
        example: '100000.00',
    },
    {
        name: 'counterparty',
        label: '对方单位',
        description: '交易对方单位名称',
        example: '某某供应商',
    },
    {
        name: 'voucher_no',
        label: '凭证号',
        description: '凭证编号',
        example: '记字第001号',
    },
    {
        name: 'invoice_no',
        label: '发票号',
        description: '发票号码',
        example: '12345678',
    },
];

/**
 * 所有字段（合并后的列表）
 */
const ALL_FIELDS = [...REQUIRED_FIELDS, ...OPTIONAL_FIELDS];

/**
 * 字段映射和模板下载 Hook
 *
 * 负责处理模板下载、字段说明展示等功能
 *
 * @returns 模板下载状态和操作方法
 */
export function useFieldMapping() {
    const [downloading, setDownloading] = useState<DownloadState>({
        downloading: false,
        templateType: null,
    });

    /**
     * 下载 CSV 模板
     */
    const handleDownloadCsvTemplate = useCallback(async () => {
        setDownloading({ downloading: true, templateType: 'csv' });
        try {
            const blob = await legacyImportApi.downloadCsvTemplate();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'legacy-import-template.csv';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            toast.success('CSV模板下载成功');
        } catch (error) {
            console.error('下载CSV模板失败', error);
            toast.error('下载CSV模板失败');
        } finally {
            setDownloading({ downloading: false, templateType: null });
        }
    }, []);

    /**
     * 下载 Excel 模板
     */
    const handleDownloadExcelTemplate = useCallback(async () => {
        setDownloading({ downloading: true, templateType: 'excel' });
        try {
            const blob = await legacyImportApi.downloadExcelTemplate();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'legacy-import-template.xlsx';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
            toast.success('Excel模板下载成功');
        } catch (error) {
            console.error('下载Excel模板失败', error);
            toast.error('下载Excel模板失败');
        } finally {
            setDownloading({ downloading: false, templateType: null });
        }
    }, []);

    /**
     * 根据模板类型下载
     */
    const downloadTemplate = useCallback((templateType: TemplateType) => {
        if (templateType === 'csv') {
            return handleDownloadCsvTemplate();
        }
        return handleDownloadExcelTemplate();
    }, [handleDownloadCsvTemplate, handleDownloadExcelTemplate]);

    /**
     * 检查是否正在下载
     */
    const isDownloading = useCallback((templateType?: TemplateType): boolean => {
        if (templateType) {
            return downloading.downloading && downloading.templateType === templateType;
        }
        return downloading.downloading;
    }, [downloading]);

    /**
     * 获取字段描述
     */
    const getFieldDescription = useCallback((fieldName: string): RequiredField | OptionalField | undefined => {
        return ALL_FIELDS.find(
            field => field.name === fieldName
        );
    }, []);

    /**
     * 检查是否为必需字段
     */
    const isRequiredField = useCallback((fieldName: string): boolean => {
        return REQUIRED_FIELDS.some(field => field.name === fieldName);
    }, []);

    /**
     * 获取所有字段名
     */
    const getAllFieldNames = useCallback((): string[] => {
        return ALL_FIELDS.map(field => field.name);
    }, []);

    /**
     * 获取必需字段名
     */
    const getRequiredFieldNames = useCallback((): string[] => {
        return REQUIRED_FIELDS.map(field => field.name);
    }, []);

    /**
     * 获取可选字段名
     */
    const getOptionalFieldNames = useCallback((): string[] => {
        return OPTIONAL_FIELDS.map(field => field.name);
    }, []);

    return {
        // 状态
        downloading,

        // 字段定义
        REQUIRED_FIELDS,
        OPTIONAL_FIELDS,

        // 操作方法
        downloadTemplate,
        handleDownloadCsvTemplate,
        handleDownloadExcelTemplate,

        // 辅助方法
        isDownloading,
        getFieldDescription,
        isRequiredField,
        getAllFieldNames,
        getRequiredFieldNames,
        getOptionalFieldNames,
    };
}
