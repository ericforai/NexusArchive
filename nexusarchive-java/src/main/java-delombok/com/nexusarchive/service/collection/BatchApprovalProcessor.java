// Input: Spring Framework, Local Services
// Output: BatchApprovalProcessor 类
// Pos: Service Layer

package com.nexusarchive.service.collection;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.mapper.CollectionBatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

/**
 * 批次审批处理器
 *
 * 负责批次批准/拒绝的业务逻辑
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class BatchApprovalProcessor {

    private final CollectionBatchMapper batchMapper;
    private final BatchFondsValidator fondsValidator;
    private final BatchAuditHelper auditHelper;

    /**
     * 批量处理批次（批准或拒绝）
     *
     * @param batchIds 要处理的批次ID列表
     * @param skipBatchIds 要跳过的批次ID列表
     * @param approver 审批操作人
     * @param isApprove true=批准, false=拒绝
     * @return 处理结果摘要
     */
    @Transactional
    public BatchProcessSummary processBatch(
            java.util.List<Long> batchIds,
            java.util.List<Long> skipBatchIds,
            BatchApprover approver,
            boolean isApprove) {

        BatchProcessSummary summary = new BatchProcessSummary();
        Set<Long> skipIdSet = skipBatchIds != null ? new HashSet<>(skipBatchIds) : new HashSet<>();

        for (Long batchId : batchIds) {
            if (skipIdSet.contains(batchId)) {
                log.info("Skipping batch: {}", batchId);
                summary.skipped++;
                continue;
            }

            try {
                if (isApprove) {
                    approveBatch(batchId, approver);
                } else {
                    rejectBatch(batchId, approver);
                }
                summary.success++;
                log.debug("{} batch: {}", isApprove ? "Approved" : "Rejected", batchId);
            } catch (Exception e) {
                summary.addFailure(batchId, e.getMessage());
                log.warn("Failed to process batch {}: {}", batchId, e.getMessage(), e);
            }
        }

        log.info("Batch {} completed: {} succeeded, {} failed, {} skipped",
                isApprove ? "approval" : "rejection", summary.success, summary.failed, summary.skipped);

        return summary;
    }

    /**
     * 批准单个批次归档
     */
    public void approveBatch(Long batchId, BatchApprover approver) {
        approveBatch(batchId, approver.getId(), approver.getName(), approver.getComment());
    }

    /**
     * 批准单个批次归档
     */
    @Transactional
    public void approveBatch(Long batchId, String operatorId, String operatorName, String comment) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }

        // 全宗校验
        fondsValidator.validateFondsAccess(batch);

        // 验证批次状态
        if (!CollectionBatch.STATUS_VALIDATED.equals(batch.getStatus())) {
            throw new IllegalStateException("批次状态不允许归档: " + batch.getStatus());
        }

        // 更新批次状态
        batch.setStatus(CollectionBatch.STATUS_ARCHIVED);
        batch.setLastModifiedTime(LocalDateTime.now());
        batch.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        // 记录审计日志
        auditHelper.logApprove(operatorId, operatorName, batchId, batch.getBatchNo(), comment);

        log.info("批次归档批准成功: batchId={}, batchNo={}", batchId, batch.getBatchNo());
    }

    /**
     * 拒绝单个批次归档
     */
    public void rejectBatch(Long batchId, BatchApprover approver) {
        rejectBatch(batchId, approver.getId(), approver.getName(), approver.getComment());
    }

    /**
     * 拒绝单个批次归档
     */
    @Transactional
    public void rejectBatch(Long batchId, String operatorId, String operatorName, String comment) {
        CollectionBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }

        // 全宗校验
        fondsValidator.validateFondsAccess(batch);

        // 更新批次状态
        batch.setStatus(CollectionBatch.STATUS_FAILED);
        batch.setErrorMessage(comment != null ? comment : "归档申请被拒绝");
        batch.setLastModifiedTime(LocalDateTime.now());
        batch.setCompletedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        // 记录审计日志
        auditHelper.logReject(operatorId, operatorName, batchId, batch.getBatchNo(), comment);

        log.info("批次归档拒绝成功: batchId={}, batchNo={}", batchId, batch.getBatchNo());
    }

    /**
     * 批次处理摘要
     */
    public static class BatchProcessSummary {
        public int success = 0;
        public int failed = 0;
        public int skipped = 0;
        private final java.util.Map<Long, String> failures = new java.util.HashMap<>();

        public void addFailure(Long batchId, String error) {
            failed++;
            failures.put(batchId, error);
        }

        public java.util.Map<Long, String> getFailures() {
            return failures;
        }
    }

    /**
     * 批次审批人信息
     */
    public interface BatchApprover {
        String getId();
        String getName();
        default String getComment() { return null; }
    }
}
