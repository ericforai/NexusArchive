// Input: matchingApi, MatchPreviewRow 类型
// Output: useSmartMatching Hook
// Pos: src/features/archives/useSmartMatching.ts
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { useState, useCallback } from 'react';
import { matchingApi } from '../../api/matching';
import { toast } from 'react-hot-toast';

// 适配 MatchPreviewModal 的数据结构
export interface MatchPreviewRow {
    voucherId: string; // 增加 voucherId 以便确认
    voucherNo?: string;
    date?: string;
    amount?: string;
    matchScore?: number;
    _previewStatus?: 'high' | 'medium';
    _proposedLinks?: Array<{ code: string; docId: string; evidenceRole: string; linkType: string }>;
}

export const useSmartMatching = (onRefresh?: () => void) => {
    const [isMatchPreviewOpen, setIsMatchPreviewOpen] = useState(false);
    const [matchPreviewData, setMatchPreviewData] = useState<MatchPreviewRow[]>([]);
    const [isMatching, setIsMatching] = useState(false);
    const [isConfirming, setIsConfirming] = useState(false);

    // 执行智能匹配
    const handleAutoMatch = useCallback(async (selectedIds: string[]) => {
        if (!selectedIds.length) {
            toast.error('请先选择要匹配的凭证');
            return;
        }

        setIsMatching(true);
        const toastId = toast.loading('正在执行智能匹配...');

        try {
            // 1. 提交批量任务
            await matchingApi.executeBatchMatch(selectedIds);

            // 2. 获取每个凭证的详细结果 (这里简化为并行获取，实际生产可能需要轮询任务状态)
            // 既然是"修复"，我们先假设后端返回得很快，或者我们直接获取 Mock/Real 结果
            const tasks = selectedIds.map(id => matchingApi.getMatchResult(id));
            const results = await Promise.all(tasks);

            // 3. 转换为预览数据
            const previewRows: MatchPreviewRow[] = results
                .filter(Boolean)
                .map(res => {
                    if (!res) return {} as MatchPreviewRow;

                    const topLink = res.links?.[0]; // 取分数最高的一个
                    const score = res.confidence || topLink?.score || 0;
                    const previewStatus: MatchPreviewRow['_previewStatus'] =
                        score >= 90 ? 'high' : (score > 60 ? 'medium' : undefined);

                    return {
                        voucherId: res.voucherId,
                        voucherNo: res.voucherNo,
                        // 暂无日期金额等元数据，需从列表获取或 API 返回。
                        // 由于 API 返回可能不全，这里仅依赖 API 返回的 minimal info
                        amount: '待获取',
                        date: res.createdTime?.substring(0, 10),
                        matchScore: score,
                        _previewStatus: previewStatus,
                        _proposedLinks: res.links?.map(l => ({
                            code: l.matchedDocNo || '未知单据',
                            docId: l.matchedDocId || '',
                            evidenceRole: l.evidenceRole,
                            linkType: l.linkType
                        })) || []
                    };
                })
                .filter(row => row._previewStatus); // 过滤掉无匹配建议的

            if (previewRows.length === 0) {
                toast.success('未发现新的高置信度匹配项', { id: toastId });
                return;
            }

            setMatchPreviewData(previewRows);
            setIsMatchPreviewOpen(true);
            toast.dismiss(toastId);

        } catch (error) {
            console.error('Auto match failed:', error);
            toast.error('智能匹配执行失败', { id: toastId });
        } finally {
            setIsMatching(false);
        }
    }, []);

    // 确认并应用关联
    const handleConfirmLinks = useCallback(async () => {
        setIsConfirming(true);
        const toastId = toast.loading('正在建立关联...');

        try {
            // 过滤出需要确认的行
            const rowsToConfirm = matchPreviewData.filter(r => r._previewStatus);

            // 并行提交确认
            const tasks = rowsToConfirm.map(row => {
                if (!row._proposedLinks?.length) return Promise.resolve();

                // 构建 links payload
                // 假设每个凭证取所有 proposed links
                const linksPayload = row._proposedLinks.map(link => ({
                    sourceDocId: link.docId,
                    evidenceRole: link.evidenceRole,
                    linkType: link.linkType
                }));

                return matchingApi.confirmMatch(row.voucherId, linksPayload);
            });

            await Promise.all(tasks);

            toast.success(`成功关联 ${rowsToConfirm.length} 份凭证`, { id: toastId });
            setIsMatchPreviewOpen(false);

            // 刷新列表
            onRefresh?.();

        } catch (error) {
            console.error('Confirm match failed:', error);
            toast.error('关联确认失败', { id: toastId });
        } finally {
            setIsConfirming(false);
        }
    }, [matchPreviewData, onRefresh]);

    const closeMatchPreview = useCallback(() => {
        setIsMatchPreviewOpen(false);
        setMatchPreviewData([]);
    }, []);

    return {
        isMatchPreviewOpen,
        matchPreviewData,
        isMatching,
        isConfirming,
        handleAutoMatch,
        handleConfirmLinks,
        closeMatchPreview
    };
};
