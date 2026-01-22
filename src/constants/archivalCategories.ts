import { Receipt, BookOpen, FileText, Folder } from 'lucide-react';

/**
 * 电子会计档案门类定义 (DA/T 94-2022 标准码)
 * 
 * 门类代码说明：
 * - VOUCHER: 记账凭证 (ERP同步)
 * - AC01: 原始凭证 (发票、收据等)
 * - AC02: 会计账簿 (总账、明细账)
 * - AC03: 财务报告 (月报、年报)
 * - AC04: 其他会计资料 (银行对账单等)
 * 
 * 一旦我被更新，务必更新我的开头注释。
 */

export const ARCHIVE_CATEGORIES = {
    // 记账凭证 - 保留 VOUCHER 作为 ERP 同步的记账凭证
    VOUCHER: {
        value: 'VOUCHER',
        label: '记账凭证',
        icon: Receipt,
        description: 'ERP 同步的记账凭证',
        validationRule: 'API 同步数据自动关联。',
        color: 'blue'
    },
    // AC02 - 会计账簿 (DA/T 94 标准码)
    AC02: {
        value: 'AC02',
        label: '会计账簿',
        icon: BookOpen,
        description: '总账、明细账、日记账、固定资产卡片等',
        validationRule: '必须上传 PDF 格式，需包含年度/期间信息。',
        color: 'green'
    },
    // AC03 - 财务报告 (DA/T 94 标准码)
    AC03: {
        value: 'AC03',
        label: '财务报告',
        icon: FileText,
        description: '月报、季报、半年报、年报',
        validationRule: '必须上传 PDF/OFD 格式。',
        color: 'orange'
    },
    // AC04 - 其他会计资料 (DA/T 94 标准码)
    AC04: {
        value: 'AC04',
        label: '其他资料',
        icon: Folder,
        description: '银行对账单、纳税申报表等',
        validationRule: '支持多种常见文档格式。',
        color: 'slate'
    },
} as const;

export type ArchivalCategory = keyof typeof ARCHIVE_CATEGORIES;

export const CATEGORY_OPTIONS = Object.values(ARCHIVE_CATEGORIES).map(cat => ({
    value: cat.value,
    label: cat.label,
    icon: cat.icon,
    description: cat.description,
    validationRule: cat.validationRule,
    color: cat.color
}));
