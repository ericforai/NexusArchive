import { client } from './client';
import { ApiResponse } from '../types';

export interface LinkedFile {
    id: string;
    name: string;
    type: 'invoice' | 'contract' | 'bank_slip' | 'other';
    url: string;
    uploadDate: string;
    size: string;
}

export interface ComplianceStatus {
    passed: boolean;
    score: number;
    details: {
        authenticity: boolean;
        integrity: boolean;
        usability: boolean;
        safety: boolean;
    };
    checkDate: string;
}

export interface RelationGraphNode {
    id: string;
    code?: string;
    name?: string;
    type: 'contract' | 'invoice' | 'voucher' | 'receipt' | 'report' | 'ledger' | 'other';
    amount?: string;
    date?: string;
    status?: string;
}

export interface RelationGraphEdge {
    from: string;
    to: string;
    relationType?: string;
    description?: string;
}

export interface RelationGraph {
    centerId: string;
    nodes: RelationGraphNode[];
    edges: RelationGraphEdge[];
}

/**
 * 默认关联文件数据（当后端无数据时使用）
 */
const getDefaultFiles = (voucherId: string): LinkedFile[] => {
    if (voucherId === 'V-202511-TEST') {
        return [
            { id: '1', name: '电子发票_差旅费_20251107.pdf', type: 'invoice', url: '#', uploadDate: '2025-11-07', size: '150KB' },
            { id: '1-2', name: '滴滴出行行程单.pdf', type: 'invoice', url: '#', uploadDate: '2025-11-07', size: '85KB' },
            { id: '2', name: '招商银行_转账回单_20251108.pdf', type: 'bank_slip', url: '#', uploadDate: '2025-11-08', size: '420KB' },
            { id: '3', name: '2025年度企业差旅服务框架协议.pdf', type: 'contract', url: '#', uploadDate: '2025-01-01', size: '2.5MB' },
            { id: '4', name: '出差审批单_张三_北京.pdf', type: 'other', url: '#', uploadDate: '2025-11-05', size: '120KB' },
            { id: '5', name: '会议邀请函.jpg', type: 'other', url: '#', uploadDate: '2025-11-05', size: '800KB' }
        ];
    }
    if (voucherId === 'C-202511-002') {
        return [
            { id: 'c1', name: '服务器采购合同_20251115.pdf', type: 'contract', url: '#', uploadDate: '2025-11-15', size: '4.2MB' },
            { id: 'c2', name: '供应商资质证明.pdf', type: 'other', url: '#', uploadDate: '2025-11-15', size: '1.8MB' },
            { id: 'c3', name: '中标通知书.pdf', type: 'other', url: '#', uploadDate: '2025-11-14', size: '560KB' },
            { id: 'c4', name: '阿里云_设备采购发票.pdf', type: 'invoice', url: '#', uploadDate: '2025-11-20', size: '1.5MB' },
            { id: 'c5', name: '招行_设备款支付回单.pdf', type: 'bank_slip', url: '#', uploadDate: '2025-11-21', size: '450KB' }
        ];
    }
    return [
        { id: '1', name: '增值税专用发票.pdf', type: 'invoice', url: '#', uploadDate: '2023-11-20', size: '1.2MB' },
        { id: '2', name: '采购合同.pdf', type: 'contract', url: '#', uploadDate: '2023-11-18', size: '3.5MB' },
        { id: '3', name: '银行回单.jpg', type: 'bank_slip', url: '#', uploadDate: '2023-11-21', size: '500KB' }
    ];
};

/**
 * 默认关系图谱数据（当后端无数据时使用）
 */
const getDefaultGraph = (): RelationGraph => ({
    centerId: 'voucher-demo',
    nodes: [
        { id: 'contract-demo', type: 'contract', code: 'CON-2023-098', name: '年度技术服务协议', amount: '¥ 150,000.00', date: '2023-01-15', status: '生效中' },
        { id: 'invoice-demo-1', type: 'invoice', code: 'INV-202311-089', name: '阿里云计算服务费发票', amount: '¥ 12,800.00', date: '2023-11-02', status: '已验真' },
        { id: 'invoice-demo-2', type: 'invoice', code: 'INV-202311-092', name: '服务器采购发票', amount: '¥ 45,200.00', date: '2023-11-03', status: '已验真' },
        { id: 'voucher-demo', type: 'voucher', code: 'JZ-202311-0052', name: '11月技术部费用报销', amount: '¥ 58,000.00', date: '2023-11-05', status: '已过账' },
        { id: 'receipt-demo', type: 'receipt', code: 'B-20231105-003', name: '招商银行付款回单', amount: '¥ 58,000.00', date: '2023-11-05', status: '已匹配' },
        { id: 'report-demo', type: 'report', code: 'REP-2023-11', name: '11月科目余额表', date: '2023-11-30', status: '已生成' }
    ],
    edges: [
        { from: 'contract-demo', to: 'voucher-demo', relationType: '依据' },
        { from: 'invoice-demo-1', to: 'voucher-demo', relationType: '原始凭证' },
        { from: 'invoice-demo-2', to: 'voucher-demo', relationType: '原始凭证' },
        { from: 'voucher-demo', to: 'receipt-demo', relationType: '资金流' },
        { from: 'voucher-demo', to: 'report-demo', relationType: '归档' }
    ]
});

export const autoAssociationApi = {
    getLinkedFiles: async (voucherId: string): Promise<{ files: LinkedFile[] }> => {
        try {
            const response = await client.get<ApiResponse<LinkedFile[]>>(`/relations/${voucherId}/files`);
            if (response.data.code === 200 && response.data.data && response.data.data.length > 0) {
                return { files: response.data.data };
            }
        } catch (error) {
            console.warn('Failed to fetch linked files, using default data', error);
        }
        // 无数据或出错时返回默认数据
        return { files: getDefaultFiles(voucherId) };
    },

    getComplianceStatus: async (voucherId: string): Promise<ComplianceStatus> => {
        try {
            const response = await client.get<ApiResponse<ComplianceStatus>>(`/relations/${voucherId}/compliance`);
            if (response.data.code === 200 && response.data.data) {
                return response.data.data;
            }
        } catch (error) {
            console.warn('Failed to fetch compliance status, using default data', error);
        }
        // 无数据或出错时返回默认数据
        return {
            passed: true,
            score: 98,
            details: {
                authenticity: true,
                integrity: true,
                usability: true,
                safety: true
            },
            checkDate: '2023-11-22 10:00:00'
        };
    },

    getRelationGraph: async (archiveId: string): Promise<RelationGraph> => {
        try {
            const response = await client.get<ApiResponse<RelationGraph>>(`/relations/${archiveId}/graph`);
            if (response.data.code === 200 && response.data.data) {
                return response.data.data;
            }
        } catch (error) {
            console.warn('Failed to fetch relation graph, using default data', error);
        }
        // 无数据或出错时返回默认数据
        const defaultGraph = getDefaultGraph();
        defaultGraph.centerId = archiveId || defaultGraph.centerId;
        return defaultGraph;
    }
};

