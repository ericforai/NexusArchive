// Input: API client 与 ApiResponse 类型
// Output: autoAssociationApi（增强版演示图谱/方向视图与节点级附件回退）
// Pos: 自动关联/匹配 API 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
    type: 'contract' | 'invoice' | 'voucher' | 'receipt' | 'report' | 'ledger' | 'payment' | 'reimbursement' | 'application' | 'other';
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

export interface RelationDirectionalView {
    upstream: string[];
    downstream: string[];
    layers: Record<string, number>;
    mainline?: string[];
}

export interface RelationGraph {
    centerId: string;
    nodes: RelationGraphNode[];
    edges: RelationGraphEdge[];
    directionalView?: RelationDirectionalView;
    /** 原始查询档案ID（如果发生了自动转换） */
    originalQueryId?: string;
    /** 是否自动转换（true表示以原始档案为中心时自动找到凭证） */
    autoRedirected?: boolean;
    /** 转换提示信息 */
    redirectMessage?: string;
}

interface DemoScenario {
    aliases: string[];
    defaultCenterId: string;
    nodes: RelationGraphNode[];
    edges: RelationGraphEdge[];
    directionalView?: RelationDirectionalView;
    filesByNodeId: Record<string, LinkedFile[]>;
}

const toPublicAssetUrl = (path: string): string => {
    if (/^https?:\/\//.test(path)) return path;
    if (typeof window !== 'undefined' && path.startsWith('/')) {
        return `${window.location.origin}${path}`;
    }
    return path;
};

const DEMO_PDF_URL = toPublicAssetUrl('/dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.pdf');
const DEMO_REIMB_FORM_URL = toPublicAssetUrl('/demo/reimb-form.pdf');
const DEMO_REIMB_INVOICE_001_URL = toPublicAssetUrl('/demo/reimb-invoice-001.pdf');
const DEMO_REIMB_INVOICE_002_URL = toPublicAssetUrl('/demo/reimb-invoice-002.pdf');
const DEMO_REIMB_INVOICE_003_URL = toPublicAssetUrl('/demo/reimb-invoice-003.pdf');
const DEMO_REIMB_INVOICE_004_URL = toPublicAssetUrl('/demo/reimb-invoice-004.pdf');
const DEMO_REIMB_RECEIPT_URL = toPublicAssetUrl('/demo/reimb-receipt.pdf');

const normalizeArchiveKey = (value?: string): string => (value || '').trim().toLowerCase();

const buildDirectionalViewFromEdges = (centerId: string, edges: RelationGraphEdge[]): RelationDirectionalView => {
    if (!centerId || edges.length === 0) {
        return { upstream: [], downstream: [], layers: {}, mainline: [] };
    }

    const outgoing = new Map<string, string[]>();
    const incoming = new Map<string, string[]>();
    edges.forEach((edge) => {
        const out = outgoing.get(edge.from) || [];
        out.push(edge.to);
        outgoing.set(edge.from, out);

        const inn = incoming.get(edge.to) || [];
        inn.push(edge.from);
        incoming.set(edge.to, inn);
    });

    const bfs = (adj: Map<string, string[]>) => {
        const depth = new Map<string, number>([[centerId, 0]]);
        const queue: string[] = [centerId];
        while (queue.length > 0) {
            const current = queue.shift();
            if (!current) continue;
            const currentDepth = depth.get(current) || 0;
            for (const next of adj.get(current) || []) {
                if (depth.has(next)) continue;
                depth.set(next, currentDepth + 1);
                queue.push(next);
            }
        }
        return depth;
    };

    const upstreamDepth = bfs(incoming);
    const downstreamDepth = bfs(outgoing);

    const layers: Record<string, number> = {};
    const upstream = new Set<string>();
    const downstream = new Set<string>();
    const allIds = new Set<string>([
        ...Array.from(upstreamDepth.keys()),
        ...Array.from(downstreamDepth.keys())
    ]);
    allIds.delete(centerId);

    allIds.forEach((id) => {
        const up = upstreamDepth.get(id);
        const down = downstreamDepth.get(id);
        if (up !== undefined && down !== undefined) {
            if (up <= down) {
                upstream.add(id);
                layers[id] = up;
            } else {
                downstream.add(id);
                layers[id] = down;
            }
            return;
        }
        if (up !== undefined) {
            upstream.add(id);
            layers[id] = up;
            return;
        }
        if (down !== undefined) {
            downstream.add(id);
            layers[id] = down;
        }
    });

    const sortByLayerThenId = (ids: Set<string>) =>
        Array.from(ids).sort((a, b) => {
            const layerCmp = (layers[a] || 1) - (layers[b] || 1);
            if (layerCmp !== 0) return layerCmp;
            return a.localeCompare(b);
        });

    return {
        upstream: sortByLayerThenId(upstream),
        downstream: sortByLayerThenId(downstream),
        layers,
        mainline: []
    };
};

const RELATIONSHIP_DEMO_SCENARIOS: DemoScenario[] = [
    {
        aliases: ['demo', 'sample', 'jz-2025-01-001', 'demo-reimb-jz-001'],
        defaultCenterId: 'demo-reimb-jz-001',
        nodes: [
            { id: 'demo-reimb-sq-001', type: 'application', code: 'SQ-2025-01-001', name: '出差申请单-张三-北京出差', date: '2025-01-05', status: '已审批' },
            { id: 'demo-reimb-payapp-001', type: 'application', code: 'SQFK-2025-01-001', name: '付款申请单-差旅报销', amount: '¥ 3,280.00', date: '2025-01-11', status: '已审批' },
            { id: 'demo-reimb-ht-001', type: 'contract', code: 'HT-2025-01-001', name: '差旅服务框架合同', amount: '¥ 20,000.00', date: '2025-01-02', status: '生效中' },
            { id: 'demo-reimb-bx-001', type: 'reimbursement', code: 'BX-2025-01-001', name: '差旅费报销单-张三', amount: '¥ 3,280.00', date: '2025-01-10', status: '已归档' },
            { id: 'demo-reimb-fp-001', type: 'invoice', code: 'FP-2025-01-001', name: '高铁票发票-北京南至上海虹桥', amount: '¥ 553.00', date: '2025-01-06', status: '已验真' },
            { id: 'demo-reimb-fp-002', type: 'invoice', code: 'FP-2025-01-002', name: '酒店住宿费发票-北京希尔顿酒店', amount: '¥ 1,200.00', date: '2025-01-07', status: '已验真' },
            { id: 'demo-reimb-fp-003', type: 'invoice', code: 'FP-2025-01-003', name: '餐饮费发票-北京全聚德', amount: '¥ 450.00', date: '2025-01-08', status: '已验真' },
            { id: 'demo-reimb-fp-004', type: 'invoice', code: 'FP-2025-01-004', name: '出租车发票-上海强生出租汽车', amount: '¥ 87.00', date: '2025-01-09', status: '已验真' },
            { id: 'demo-reimb-jz-001', type: 'voucher', code: 'JZ-2025-01-001', name: '记账凭证-差旅费报销', amount: '¥ 3,280.00', date: '2025-01-12', status: '已过账' },
            { id: 'demo-reimb-fk-001', type: 'payment', code: 'FK-2025-01-001', name: '付款单-差旅费报销', amount: '¥ 3,280.00', date: '2025-01-12', status: '已支付' },
            { id: 'demo-reimb-hd-001', type: 'receipt', code: 'HD-2025-01-001', name: '银行回单-招商银行转账', amount: '¥ 3,280.00', date: '2025-01-12', status: '已回执' },
            { id: 'demo-reimb-bb-001', type: 'report', code: 'BB-2025-01', name: '2025年1月科目余额表', date: '2025-01-31', status: '已归档' }
        ],
        edges: [
            { from: 'demo-reimb-sq-001', to: 'demo-reimb-bx-001', relationType: 'BASIS', description: '申请依据' },
            { from: 'demo-reimb-fp-001', to: 'demo-reimb-bx-001', relationType: 'ORIGINAL_VOUCHER', description: '交通费原始凭证' },
            { from: 'demo-reimb-fp-002', to: 'demo-reimb-bx-001', relationType: 'ORIGINAL_VOUCHER', description: '住宿费原始凭证' },
            { from: 'demo-reimb-fp-003', to: 'demo-reimb-bx-001', relationType: 'ORIGINAL_VOUCHER', description: '餐饮费原始凭证' },
            { from: 'demo-reimb-fp-004', to: 'demo-reimb-bx-001', relationType: 'ORIGINAL_VOUCHER', description: '交通费原始凭证' },
            { from: 'demo-reimb-bx-001', to: 'demo-reimb-jz-001', relationType: 'BASIS', description: '报销依据' },
            { from: 'demo-reimb-sq-001', to: 'demo-reimb-jz-001', relationType: 'BASIS', description: '业务依据' },
            { from: 'demo-reimb-fp-001', to: 'demo-reimb-jz-001', relationType: 'ORIGINAL_VOUCHER', description: '原始凭证' },
            { from: 'demo-reimb-fp-002', to: 'demo-reimb-jz-001', relationType: 'ORIGINAL_VOUCHER', description: '原始凭证' },
            { from: 'demo-reimb-fp-003', to: 'demo-reimb-jz-001', relationType: 'ORIGINAL_VOUCHER', description: '原始凭证' },
            { from: 'demo-reimb-fp-004', to: 'demo-reimb-jz-001', relationType: 'ORIGINAL_VOUCHER', description: '原始凭证' },
            { from: 'demo-reimb-jz-001', to: 'demo-reimb-fk-001', relationType: 'CASH_FLOW', description: '费用支付' },
            { from: 'demo-reimb-fk-001', to: 'demo-reimb-payapp-001', relationType: 'BASIS', description: '付款申请单' },
            { from: 'demo-reimb-payapp-001', to: 'demo-reimb-ht-001', relationType: 'BASIS', description: '合同依据' },
            { from: 'demo-reimb-ht-001', to: 'demo-reimb-fp-001', relationType: 'BASIS', description: '合同开票' },
            { from: 'demo-reimb-fp-001', to: 'demo-reimb-hd-001', relationType: 'CASH_FLOW', description: '发票对应回单' },
            { from: 'demo-reimb-fk-001', to: 'demo-reimb-hd-001', relationType: 'CASH_FLOW', description: '银行转账' },
            { from: 'demo-reimb-jz-001', to: 'demo-reimb-bb-001', relationType: 'ARCHIVE', description: '报表归档' }
        ],
        filesByNodeId: {
            'demo-reimb-sq-001': [
                { id: 'demo-file-sq-001', name: '出差申请单_SQ-2025-01-001.pdf', type: 'other', url: DEMO_REIMB_FORM_URL, uploadDate: '2025-01-05', size: '105KB' }
            ],
            'demo-reimb-bx-001': [
                { id: 'demo-file-bx-001', name: '差旅费报销单_BX-2025-01-001.pdf', type: 'other', url: DEMO_REIMB_FORM_URL, uploadDate: '2025-01-10', size: '105KB' }
            ],
            'demo-reimb-fp-001': [
                { id: 'demo-file-invoice-001', name: '高铁票发票_FP-2025-01-001.pdf', type: 'invoice', url: DEMO_REIMB_INVOICE_001_URL, uploadDate: '2025-01-06', size: '99KB' }
            ],
            'demo-reimb-fp-002': [
                { id: 'demo-file-invoice-002', name: '酒店住宿费发票_FP-2025-01-002.pdf', type: 'invoice', url: DEMO_REIMB_INVOICE_002_URL, uploadDate: '2025-01-07', size: '99KB' }
            ],
            'demo-reimb-fp-003': [
                { id: 'demo-file-invoice-003', name: '餐饮费发票_FP-2025-01-003.pdf', type: 'invoice', url: DEMO_REIMB_INVOICE_003_URL, uploadDate: '2025-01-08', size: '99KB' }
            ],
            'demo-reimb-fp-004': [
                { id: 'demo-file-invoice-004', name: '出租车发票_FP-2025-01-004.pdf', type: 'invoice', url: DEMO_REIMB_INVOICE_004_URL, uploadDate: '2025-01-09', size: '25KB' }
            ],
            'demo-reimb-jz-001': [
                { id: 'demo-file-voucher-001', name: '记账凭证_JZ-2025-01-001.pdf', type: 'other', url: DEMO_PDF_URL, uploadDate: '2025-01-12', size: '102KB' }
            ],
            'demo-reimb-payapp-001': [
                { id: 'demo-file-payapp-001', name: '付款申请单_SQFK-2025-01-001.pdf', type: 'other', url: DEMO_REIMB_FORM_URL, uploadDate: '2025-01-11', size: '105KB' }
            ],
            'demo-reimb-fk-001': [
                { id: 'demo-file-payment-001', name: '付款单_FK-2025-01-001.pdf', type: 'bank_slip', url: DEMO_REIMB_RECEIPT_URL, uploadDate: '2025-01-12', size: '99KB' }
            ],
            'demo-reimb-hd-001': [
                { id: 'demo-file-receipt-001', name: '银行回单_HD-2025-01-001.pdf', type: 'bank_slip', url: DEMO_REIMB_RECEIPT_URL, uploadDate: '2025-01-12', size: '99KB' }
            ]
        }
    },
    {
        aliases: ['purchase-demo', 'jz-2025-12-001', 'demo-purchase-jz-001'],
        defaultCenterId: 'demo-purchase-jz-001',
        nodes: [
            { id: 'demo-purchase-sq-001', type: 'application', code: 'SQ-2025-12-001', name: '付款申请单-设备采购', amount: '¥ 450,000.00', date: '2025-02-21', status: '已审批' },
            { id: 'demo-purchase-ht-001', type: 'contract', code: 'HT-2025-02-001', name: '服务器采购合同-阿里云', amount: '¥ 450,000.00', date: '2025-02-15', status: '生效中' },
            { id: 'demo-purchase-fp-001', type: 'invoice', code: 'FP-2025-02-001', name: '服务器采购发票-阿里云', amount: '¥ 450,000.00', date: '2025-02-20', status: '已验真' },
            { id: 'demo-purchase-jz-001', type: 'voucher', code: 'JZ-2025-12-001', name: '记账凭证-设备采购', amount: '¥ 450,000.00', date: '2025-02-20', status: '已过账' },
            { id: 'demo-purchase-fk-001', type: 'payment', code: 'FK-2025-02-001', name: '付款单-设备采购款', amount: '¥ 450,000.00', date: '2025-02-22', status: '已支付' },
            { id: 'demo-purchase-hd-001', type: 'receipt', code: 'HD-2025-02-001', name: '银行回单-招商银行转账', amount: '¥ 450,000.00', date: '2025-02-22', status: '已回执' },
            { id: 'demo-purchase-bb-001', type: 'report', code: 'BB-2025-02', name: '2025年2月固定资产汇总表', date: '2025-02-28', status: '已归档' }
        ],
        edges: [
            { from: 'demo-purchase-ht-001', to: 'demo-purchase-fp-001', relationType: 'BASIS', description: '合同依据' },
            { from: 'demo-purchase-fp-001', to: 'demo-purchase-jz-001', relationType: 'ORIGINAL_VOUCHER', description: '原始凭证' },
            { from: 'demo-purchase-jz-001', to: 'demo-purchase-fk-001', relationType: 'CASH_FLOW', description: '资金流向' },
            { from: 'demo-purchase-fk-001', to: 'demo-purchase-sq-001', relationType: 'BASIS', description: '付款申请单' },
            { from: 'demo-purchase-sq-001', to: 'demo-purchase-ht-001', relationType: 'BASIS', description: '申请依据合同' },
            { from: 'demo-purchase-fp-001', to: 'demo-purchase-hd-001', relationType: 'CASH_FLOW', description: '发票对应回单' },
            { from: 'demo-purchase-fk-001', to: 'demo-purchase-hd-001', relationType: 'CASH_FLOW', description: '银行转账' },
            { from: 'demo-purchase-jz-001', to: 'demo-purchase-bb-001', relationType: 'ARCHIVE', description: '报表归档' }
        ],
        filesByNodeId: {
            'demo-purchase-jz-001': [
                { id: 'demo-file-purchase-voucher-001', name: '记账凭证_JZ-2025-12-001.pdf', type: 'other', url: DEMO_PDF_URL, uploadDate: '2025-02-20', size: '182KB' }
            ],
            'demo-purchase-fp-001': [
                { id: 'demo-file-purchase-invoice-001', name: '采购发票_FP-2025-02-001.pdf', type: 'invoice', url: DEMO_PDF_URL, uploadDate: '2025-02-20', size: '165KB' }
            ],
            'demo-purchase-hd-001': [
                { id: 'demo-file-purchase-receipt-001', name: '银行回单_HD-2025-02-001.pdf', type: 'bank_slip', url: DEMO_PDF_URL, uploadDate: '2025-02-22', size: '96KB' }
            ]
        }
    }
];

const resolveDemoScenario = (archiveId: string): { scenario: DemoScenario; centerId: string } => {
    const normalized = normalizeArchiveKey(archiveId);

    for (const scenario of RELATIONSHIP_DEMO_SCENARIOS) {
        const matchedNode = scenario.nodes.find(node =>
            normalizeArchiveKey(node.id) === normalized || normalizeArchiveKey(node.code) === normalized
        );
        if (matchedNode) {
            return { scenario, centerId: matchedNode.id };
        }

        const matchedAlias = scenario.aliases.some(alias => normalizeArchiveKey(alias) === normalized);
        if (matchedAlias) {
            return { scenario, centerId: scenario.defaultCenterId };
        }
    }

    const defaultScenario = RELATIONSHIP_DEMO_SCENARIOS[0];
    return { scenario: defaultScenario, centerId: defaultScenario.defaultCenterId };
};

/**
 * 默认关联文件数据（当后端无数据时使用）
 */
const getDefaultFiles = (archiveId: string): LinkedFile[] => {
    const { scenario, centerId } = resolveDemoScenario(archiveId);
    return scenario.filesByNodeId[centerId] || [];
};

/**
 * 默认关系图谱数据（当后端无数据时使用）
 */
const getDefaultGraph = (archiveId: string): RelationGraph => {
    const { scenario, centerId } = resolveDemoScenario(archiveId);
    return {
        centerId,
        nodes: scenario.nodes,
        edges: scenario.edges,
        directionalView: scenario.directionalView || buildDirectionalViewFromEdges(centerId, scenario.edges)
    };
};

export const autoAssociationApi = {
    getLinkedFiles: async (voucherId: string): Promise<{ files: LinkedFile[] }> => {
        try {
            const response = await client.get<ApiResponse<LinkedFile[]>>(`/relations/${voucherId}/files`);
            if (response.data.code === 200 && response.data.data && response.data.data.length > 0) {
                return { files: response.data.data };
            }
        } catch (error: any) {
            console.warn('Failed to fetch linked files', error);
            if (error.response?.status === 401 || error.response?.status === 403) {
                throw error;
            }
        }
        // 无数据时返回默认数据
        return { files: getDefaultFiles(voucherId) };
    },

    getComplianceStatus: async (voucherId: string): Promise<ComplianceStatus> => {
        try {
            const response = await client.get<ApiResponse<ComplianceStatus>>(`/relations/${voucherId}/compliance`);
            if (response.data.code === 200 && response.data.data) {
                return response.data.data;
            }
        } catch (error: any) {
            console.warn('Failed to fetch compliance status', error);
            if (error.response?.status === 401 || error.response?.status === 403) {
                throw error;
            }
        }
        // 无数据时返回默认数据
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
        } catch (error: any) {
            // 401/403 未认证/无权限时抛出错误，让前端处理
            if (error.response?.status === 401 || error.response?.status === 403) {
                throw error;
            }
            // 404 或其他错误静默处理，返回默认图谱
            if (error.response?.status !== 404) {
                console.warn('Failed to fetch relation graph', error);
            }
        }
        // 无数据时返回默认数据
        return getDefaultGraph(archiveId);
    }
};
