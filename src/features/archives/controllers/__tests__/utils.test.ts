import { describe, expect, it } from 'vitest';
import { mapArchiveToRow } from '../utils';

describe('mapArchiveToRow', () => {
    it('凭证关联模式应输出数值金额并补齐关联字段兜底值', () => {
        const row = mapArchiveToRow(
            {
                id: 'a-1',
                archiveCode: '记-1',
                amount: 123.45,
                status: 'MATCH_PENDING'
            },
            '凭证关联'
        );

        expect(row.amount).toBe(123.45);
        expect(row.invoiceCount).toBe('-');
        expect(row.contractNo).toBe('-');
    });

    it('凭证关联模式应容忍非法金额字符串', () => {
        const row = mapArchiveToRow(
            {
                id: 'a-2',
                archiveCode: '记-2',
                amount: 'invalid-value',
                status: 'MATCHED'
            },
            '凭证关联'
        );

        expect(row.amount).toBeNull();
    });
});
