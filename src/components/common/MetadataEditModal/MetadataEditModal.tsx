// Input: React、lucide-react 图标、react-dom
// Output: React 组件 MetadataEditModal（纯 UI 层）
// Pos: 通用复用组件
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React from 'react';
import { FormModal } from '../../modals/FormModal';
import { MetadataForm } from '../MetadataForm';
// MetadataFormData/MetadataFormConfig 类型由 ../MetadataForm 导出，按需使用
import { DEFAULT_FIELDS } from './constants';
import { useFileDetailLoader } from './useFileDetailLoader';
import { useMetadataSubmit } from './useMetadataSubmit';
import { ModalHeader } from './ModalHeader';
import type { MetadataEditModalProps } from './types';

// ✅ 已移除对 api/client 的依赖（遵循架构边界规则 A）
// API 调用现在通过 props 传入

// Re-export types for convenience
export type { FileDetail, MetadataUpdatePayload, MetadataEditModalProps } from './types';

export const MetadataEditModal: React.FC<MetadataEditModalProps> = ({
    isOpen,
    onClose,
    fileId,
    fileName,
    onSuccess,
    onLoadFileDetail,
    onUpdateMetadata,
    fieldConfig = DEFAULT_FIELDS,
}) => {
    // Load file detail and manage form state
    const { loading, formData, setFormData } = useFileDetailLoader({
        isOpen,
        fileId,
        onLoadFileDetail,
    });

    // Handle form submission with validation
    const { saving, error, handleSubmit } = useMetadataSubmit({
        fileId,
        formData,
        fieldConfig,
        onUpdateMetadata,
        onSuccess,
        onClose,
    });

    const header = <ModalHeader fileName={fileName} />;

    return (
        <FormModal
            isOpen={isOpen}
            onClose={onClose}
            onSubmit={handleSubmit}
            isSubmitting={saving}
            error={error}
            title="元数据补录"
            header={header}
            submitText="保存并重新检测"
        >
            <MetadataForm
                data={formData}
                onChange={setFormData}
                fields={fieldConfig}
                loading={loading}
                error={error}
            />
        </FormModal>
    );
};

export default MetadataEditModal;
