import { client } from './client';

export interface DynamicBookType {
    label: string; // e.g. "现金日记账"
    value: string; // e.g. "CASH_JOURNAL"
}

// Map backend keys to friendly labels
const BOOK_TYPE_LABELS: Record<string, string> = {
    'GENERAL_LEDGER': '总账',
    'SUBSIDIARY_LEDGER': '明细账',
    'CASH_JOURNAL': '现金日记账',
    'BANK_JOURNAL': '银行存款日记账',
    'FIXED_ASSETS_CARD': '固定资产卡片',
    'OTHER_BOOKS': '其他账簿'
};

export const fetchAccountBookTypes = async (): Promise<string[]> => {
    try {
        const response = await client.get<string[]>('/nav/books');
        // Map backend response (e.g. "CASH_JOURNAL") to UI labels (e.g. "现金日记账")
        // If backend returns raw codes, we map them here. 
        // If backend returns 404/500, return empty.
        const rawTypes = response.data || [];

        // Sort to ensure consistent order: General -> Subsidiary -> Cash -> Bank -> Others
        const priority = ['GENERAL_LEDGER', 'SUBSIDIARY_LEDGER', 'CASH_JOURNAL', 'BANK_JOURNAL', 'FIXED_ASSETS_CARD'];

        const sortedTypes = rawTypes.sort((a: string, b: string) => {
            const idxA = priority.indexOf(a);
            const idxB = priority.indexOf(b);
            // Items in priority list come first
            if (idxA !== -1 && idxB !== -1) return idxA - idxB;
            if (idxA !== -1) return -1;
            if (idxB !== -1) return 1;
            return a.localeCompare(b);
        });

        return sortedTypes.map((t: string) => BOOK_TYPE_LABELS[t] || t);
    } catch (error) {
        console.error('Failed to fetch dynamic book types:', error);
        return [];
    }
};
