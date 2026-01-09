// Input: 类型定义
// Output: 单据类型选项和默认字段配置
// Pos: MetadataEditModal 常量定义

import type { MetadataFormConfig } from '../MetadataForm';

// 单据类型选项 (《会计档案管理办法》财政部79号令 第6条)
export const VOUCHER_TYPE_OPTIONS = [
    { code: 'AC01', label: '会计凭证', desc: '原始凭证（发票、收据、银行回单等）、记账凭证' },
    { code: 'AC02', label: '会计账簿', desc: '总账、明细账、日记账、固定资产卡片' },
    { code: 'AC03', label: '财务会计报告', desc: '月度/季度/半年度/年度报告' },
    { code: 'AC04', label: '其他会计资料', desc: '银行对账单、纳税申报表、会计档案鉴定意见书等' },
];

// 默认字段配置
export const DEFAULT_FIELDS: MetadataFormConfig[] = [
    {
        name: 'fiscalYear',
        label: '会计年度',
        required: true,
        type: 'text',
        placeholder: '例：2025',
        pattern: '\\d{4}',
    },
    {
        name: 'voucherType',
        label: '单据类型',
        required: true,
        type: 'select',
        options: VOUCHER_TYPE_OPTIONS,
    },
    {
        name: 'creator',
        label: '责任者',
        required: true,
        type: 'text',
        placeholder: '例：财务部 张三',
    },
    {
        name: 'fondsCode',
        label: '全宗号',
        required: false,
        type: 'text',
        placeholder: '例：COMP001',
        helperText: '可选',
    },
    {
        name: 'modifyReason',
        label: '修改原因',
        required: true,
        type: 'textarea',
        placeholder: '例：补充上传发票的分类信息',
        rows: 2,
        helperText: '合规要求',
    },
];

// 获取默认表单数据
export const getDefaultFormData = () => ({
    fiscalYear: new Date().getFullYear().toString(),
    voucherType: 'AC01',
    creator: '',
    fondsCode: '',
    modifyReason: '',
});
