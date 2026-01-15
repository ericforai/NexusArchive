// Input: Spring Framework、MyBatis-Plus、Java 标准库
// Output: ArchiveSubmitBatchServiceImpl 实现类 (Facade协调器)
// Pos: 服务层实现 - 归档批次服务 Facade
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.ArchiveBatchItem;
import com.nexusarchive.entity.ArchiveSubmitBatch;
import com.nexusarchive.mapper.ArchiveSubmitBatchMapper;
import com.nexusarchive.service.ArchiveSubmitBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 归档提交批次服务实现 (Facade 协调器)
 * <p>
 * 本服务已模块化拆分，委托给专用模块处理：
 * <ul>
 * <li>BatchManager - 批次管理（创建、查询、删除）</li>
 * <li>BatchItemManager - 条目管理（添加、移除、查询）</li>
 * <li>BatchWorkflowService - 工作流程（提交、审批、执行归档）</li>
 * <li>FourNatureChecker - 四性检测（真实性、完整性、可用性、安全性）</li>
 * </ul>
 * </p>
 *
 * @see com.nexusarchive.service.impl.batch.BatchManager
 * @see com.nexusarchive.service.impl.batch.BatchItemManager
 * @see com.nexusarchive.service.impl.batch.BatchWorkflowService
 * @see com.nexusarchive.service.impl.batch.FourNatureChecker
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveSubmitBatchServiceImpl implements ArchiveSubmitBatchService {

    private final ArchiveSubmitBatchMapper batchMapper;

    // 注入专用模块
    private final com.nexusarchive.service.impl.batch.BatchManager batchManager;
    private final com.nexusarchive.service.impl.batch.BatchItemManager batchItemManager;
    private final com.nexusarchive.service.impl.batch.BatchWorkflowService batchWorkflowService;
    private final com.nexusarchive.service.impl.batch.FourNatureChecker fourNatureChecker;

    // ========== 批次管理 ==========

    @Override
    @Transactional
    public ArchiveSubmitBatch createBatch(Long fondsId, LocalDate periodStart, LocalDate periodEnd, Long createdBy) {
        return batchManager.createBatch(fondsId, periodStart, periodEnd, createdBy);
    }

    @Override
    public ArchiveSubmitBatch getBatch(Long batchId) {
        return batchManager.getBatch(batchId);
    }

    @Override
    public IPage<ArchiveSubmitBatch> listBatches(Page<ArchiveSubmitBatch> page, Long fondsId, String status) {
        return batchManager.listBatches(page, fondsId, status);
    }

    @Override
    public List<ArchiveSubmitBatch> listBatchesByFonds(Long fondsId, String status) {
        return batchManager.listBatchesByFonds(fondsId, status);
    }

    @Override
    @Transactional
    public void deleteBatch(Long batchId) {
        batchManager.deleteBatch(batchId);
    }

    // ========== 批次条目管理 ==========

    @Override
    @Transactional
    public int addVouchersToBatch(Long batchId, List<Long> voucherIds) {
        ArchiveSubmitBatch batch = batchManager.getBatch(batchId);
        int added = batchItemManager.addVouchersToBatch(batch, voucherIds);
        if (added > 0) {
            batchMapper.updateById(batch);
        }
        return added;
    }

    @Override
    @Transactional
    public int addDocsToBatch(Long batchId, List<Long> docIds) {
        ArchiveSubmitBatch batch = batchManager.getBatch(batchId);
        int added = batchItemManager.addDocsToBatch(batch, docIds);
        if (added > 0) {
            batchMapper.updateById(batch);
        }
        return added;
    }

    @Override
    @Transactional
    public void removeItemFromBatch(Long batchId, Long itemId) {
        ArchiveSubmitBatch batch = batchManager.getBatch(batchId);
        batchItemManager.removeItemFromBatch(batch, itemId);
        batchMapper.updateById(batch);
    }

    @Override
    public List<ArchiveBatchItem> getBatchItems(Long batchId) {
        return batchItemManager.getBatchItems(batchId);
    }

    @Override
    public List<ArchiveBatchItem> getBatchItemsByType(Long batchId, String itemType) {
        return batchItemManager.getBatchItemsByType(batchId, itemType);
    }

    // ========== 归档流程 ==========

    @Override
    @Transactional
    public ArchiveSubmitBatch submitBatch(Long batchId, Long submittedBy) {
        return batchWorkflowService.submitBatch(batchId, submittedBy);
    }

    @Override
    @Transactional
    public Map<String, Object> validateBatch(Long batchId) {
        return fourNatureChecker.validateBatch(batchId);
    }

    @Override
    @Transactional
    public ArchiveSubmitBatch approveBatch(Long batchId, Long approvedBy, String comment) {
        return batchWorkflowService.approveBatch(batchId, approvedBy, comment);
    }

    @Override
    @Transactional
    public ArchiveSubmitBatch rejectBatch(Long batchId, Long rejectedBy, String comment) {
        return batchWorkflowService.rejectBatch(batchId, rejectedBy, comment);
    }

    @Override
    @Transactional
    public ArchiveSubmitBatch executeBatchArchive(Long batchId, Long archivedBy) {
        return batchWorkflowService.executeBatchArchive(batchId, archivedBy);
    }

    // ========== 四性检测 ==========

    @Override
    @Transactional
    public Map<String, Object> runIntegrityCheck(Long batchId) {
        return fourNatureChecker.runIntegrityCheck(batchId);
    }

    // ========== 统计 ==========

    @Override
    public Map<String, Object> getBatchStats(Long fondsId) {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();

        List<Map<String, Object>> statusCounts = batchMapper.countByStatus(fondsId);
        Map<String, Long> byStatus = new java.util.LinkedHashMap<>();
        long total = 0;

        for (Map<String, Object> row : statusCounts) {
            String status = (String) row.get("status");
            Long count = ((Number) row.get("count")).longValue();
            byStatus.put(status, count);
            total += count;
        }

        stats.put("total", total);
        stats.put("byStatus", byStatus);

        return stats;
    }
}
