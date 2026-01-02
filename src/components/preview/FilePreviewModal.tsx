// Input: React, BaseModal, SmartFilePreview
// Output: FilePreviewModal 组件
// Pos: 通用复用组件 - 文件预览 Modal

import React from 'react';
import { BaseModal } from '../modals/BaseModal';
import { SmartFilePreview, SmartFilePreviewProps } from './SmartFilePreview';

export interface FilePreviewModalProps extends Omit<SmartFilePreviewProps, 'className'> {
  /** 是否打开 Modal */
  isOpen: boolean;
  /** 关闭回调 */
  onClose: () => void;
  /** Modal 最大宽度 */
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | 'full';
}

/**
 * 文件预览 Modal
 * <p>
 * 基于 BaseModal 封装的文件预览弹窗，支持多文件切换、缩放、旋转等功能
 * 自动调用 previewApi 获取文件内容
 * </p>
 */
export function FilePreviewModal({
  isOpen,
  onClose,
  maxWidth = '4xl',
  files,
  currentFileId,
  onFileChange,
  showToolbar = true,
  showFileNav = true,
  ...previewParams
}: FilePreviewModalProps) {
  return (
    <BaseModal
      isOpen={isOpen}
      onClose={onClose}
      maxWidth={maxWidth}
      showCloseButton={true}
      closeOnBackdropClick={true}
      className="overflow-hidden"
    >
      {/* 预览区域 - 占满整个 Modal 内容区 */}
      <div className="h-[80vh] min-h-[500px]">
        <SmartFilePreview
          files={files}
          currentFileId={currentFileId}
          onFileChange={onFileChange}
          showToolbar={showToolbar}
          showFileNav={showFileNav}
          {...previewParams}
        />
      </div>
    </BaseModal>
  );
}

export default FilePreviewModal;
