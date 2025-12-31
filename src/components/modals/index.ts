// Input: Modal 组件
// Output: 统一导出
// Pos: src/components/modals/index.ts

export { BaseModal } from './BaseModal';
export { ConfirmModal } from './ConfirmModal';
export { FormModal } from './FormModal';
export { DetailModal } from './DetailModal';

export type { BaseModalProps } from './BaseModal';
export type { ConfirmModalProps, ConfirmVariant } from './ConfirmModal';
export type { FormModalProps } from './FormModal';
export type { DetailModalProps } from './DetailModal';
