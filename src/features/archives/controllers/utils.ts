/**
 * Archive Controllers - Utility Functions
 */

import { GenericRow } from '../../../types';

const CATEGORY_LABELS: Record<string, string> = {
    AC01: '会计凭证',
    AC02: '会计账簿',
    AC03: '财务报告',
    AC04: '其他会计资料'
};

const STATUS_LABELS: Record<string, string> = {
    draft: '草稿',
    MATCH_PENDING: '待匹配',
    MATCHED: '匹配成功',
    pending: '准备归档',
    archived: '已归档'
};

const PRE_ARCHIVE_STATUS_LABELS: Record<string, { label: string }> = {
    PENDING_CHECK: { label: '待检测' },
    NEEDS_ACTION: { label: '待处理' },
    READY_TO_MATCH: { label: '待匹配' },
    READY_TO_ARCHIVE: { label: '准备归档' },
    COMPLETED: { label: '已归档' },
    PENDING_APPROVAL: { label: '归档审批中' },
    ARCHIVING: { label: '归档中' },
};

export const resolveCategoryLabel = (code?: string) => {
    if (!code) return '档案';
    return CATEGORY_LABELS[code] || code;
};

export const formatStatus = (status?: string) => {
    if (!status) return '-';
    return STATUS_LABELS[status] || PRE_ARCHIVE_STATUS_LABELS[status]?.label || status;
};

export const getSafeDisplayValue = (text: string | undefined | null) => {
    if (!text) return '-';
    const isHash = text.length > 30 && !text.includes(' ') && /^[a-fA-F0-9]+$/.test(text);
    if (isHash) return '密级文档(标题加密)';
    return text;
};

export const mapArchiveToRow = (archive: any, subTitle: string): GenericRow => {
    const categoryLabel = resolveCategoryLabel(archive?.categoryCode);
    const baseDate = archive?.docDate || archive?.createdTime || '';
    const date = baseDate ? String(baseDate).split('T')[0] : '';
    const statusText = formatStatus(archive?.status);

    if (subTitle === '会计凭证' || subTitle === '凭证关联') {
        const amountValue = archive?.amount;
        const amount = typeof amountValue === 'number'
            ? `¥ ${amountValue.toFixed(2)}`
            : amountValue || '-';

        let subjectName = '-';
        try {
            if (archive?.customMetadata) {
                const meta = typeof archive.customMetadata === 'string'
                    ? JSON.parse(archive.customMetadata)
                    : archive.customMetadata;
                if (Array.isArray(meta) && meta.length > 0) {
                    const entry = meta.find((m: any) => m.accsubject && (m.debit_org > 0 || m.debitLocal > 0)) || meta[0];
                    if (entry?.accsubject?.name) {
                        subjectName = entry.accsubject.name;
                    } else if (entry?.description) {
                        subjectName = entry.description;
                    }
                }
            }
            if (subjectName === '-' && archive?.title) {
                subjectName = archive.title;
            }
            subjectName = getSafeDisplayValue(subjectName);
        } catch (e) {
            console.warn('Failed to parse custom metadata for subject', e);
        }

        return {
            id: archive?.id,
            code: archive?.archiveCode,
            voucherNo: archive?.archiveCode,
            archivalCode: archive?.archiveCode,
            entity: archive?.orgName,
            period: archive?.fiscalPeriod || archive?.fiscalYear || '',
            subject: subjectName,
            type: categoryLabel,
            amount,
            date,
            status: statusText,
            matchScore: archive?.matchScore || 0,
            autoLink: archive?.matchMethod || '-',
            rawStatus: archive?.status
        };
    }

    if (subTitle === '会计账簿') {
        return {
            id: archive?.id,
            code: archive?.archiveCode,
            ledgerNo: archive?.archiveCode,
            archivalCode: archive?.archiveCode,
            type: categoryLabel,
            entity: archive?.orgName,
            year: archive?.fiscalYear,
            period: archive?.fiscalPeriod || '全年',
            subject: getSafeDisplayValue(archive?.title),
            pageCount: archive?.pageCount || '-',
            status: statusText,
            rawStatus: archive?.preArchiveStatus || archive?.status
        };
    }

    if (subTitle === '财务报告') {
        return {
            id: archive?.id,
            code: archive?.archiveCode,
            reportNo: archive?.archiveCode,
            archivalCode: archive?.archiveCode,
            type: categoryLabel,
            year: archive?.fiscalYear,
            unit: archive?.orgName,
            title: getSafeDisplayValue(archive?.title),
            period: archive?.fiscalPeriod || '',
            date,
            status: statusText,
            rawStatus: archive?.preArchiveStatus || archive?.status
        };
    }

    return {
        id: archive?.id,
        code: archive?.archiveCode,
        archiveNo: archive?.fondsNo,
        archivalCode: archive?.archiveCode,
        category: categoryLabel,
        year: archive?.fiscalYear,
        period: archive?.retentionPeriod || archive?.fiscalPeriod,
        title: getSafeDisplayValue(archive?.title),
        security: archive?.securityLevel || '-',
        status: statusText,
        orgName: archive?.orgName,
        date,
        rawStatus: archive?.preArchiveStatus || archive?.status
    };
};

