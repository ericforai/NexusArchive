// Input: 类型定义
// Output: MetadataEditModal 类型定义
// Pos: MetadataEditModal 类型定义

import type { MetadataFormConfig } from '../MetadataForm';

export interface FileDetail {
    id: string;
    fileName: string;
    fileType: string;
    status: string;
    fiscalYear?: string;
    voucherType?: string;
    creator?: string;
    fondsCode?: string;
}

export interface MetadataUpdatePayload {
    id: string;
    fiscalYear: string;
    voucherType: string;
    creator: string;
    fondsCode?: string;
    modifyReason: string;
}

export interface MetadataEditModalProps {
    isOpen: boolean;
    onClose: () => void;
    fileId: string;
    fileName: string;
    onSuccess?: () => void;
    onLoadFileDetail: (fileId: string) => Promise<FileDetail | null>;
    onUpdateMetadata: (payload: MetadataUpdatePayload) => Promise<{ success: boolean; message?: string }>;
    fieldConfig?: MetadataFormConfig[];
}
