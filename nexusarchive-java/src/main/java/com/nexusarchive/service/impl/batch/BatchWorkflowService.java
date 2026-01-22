// Input: MyBatis-Plus、Lombok、Spring Framework
// Output: BatchWorkflowService 类
// Pos: 归档批次服务 - 工作流程层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.batch;

import com.nexusarchive.entity.ArchiveBatchItem;
import com.nexusarchive.entity.ArchiveSubmitBatch;
import com.nexusarchive.entity.PeriodLock;
import com.nexusarchive.mapper.ArchiveBatchItemMapper;
import com.nexusarchive.mapper.ArchiveSubmitBatchMapper;
import com.nexusarchive.mapper.PeriodLockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 批次工作流服务
 * <p>
 * 负责归档批次的提交流程、审批流程、执行归档等操作
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchWorkflowService {

    private final ArchiveSubmitBatchMapper batchMapper;
    private final ArchiveBatchItemMapper itemMapper;
    private final PeriodLockMapper periodLockMapper;
    private final FourNatureChecker fourNatureChecker;

    /**
     * 提交批次
     */
    @Transactional
    public ArchiveSubmitBatch submitBatch(Long batchId, String submittedBy) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!batch.canSubmit()) {
            throw new IllegalStateException("批次状态不允许提交: " + batch.getStatus());
        }

        // 检查是否有条目
        List<ArchiveBatchItem> items = itemMapper.findByBatchId(batchId);
        if (items.isEmpty()) {
            throw new IllegalStateException("批次中没有任何条目，无法提交");
        }

        batch.setStatus(ArchiveSubmitBatch.STATUS_VALIDATING);
        batch.setSubmittedBy(submittedBy);
        batch.setSubmittedAt(LocalDateTime.now());
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        log.info("提交归档批次: {} (提交人: {})", batch.getBatchNo(), submittedBy);

        // 异步执行校验（此处简化为同步）
        fourNatureChecker.validateBatch(batchId);

        return batchMapper.selectById(batchId);
    }

    /**
     * 审批通过批次
     */
    @Transactional
    public ArchiveSubmitBatch approveBatch(Long batchId, String approvedBy, String comment) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!ArchiveSubmitBatch.STATUS_VALIDATING.equals(batch.getStatus())) {
            throw new IllegalStateException("只能审批校验中的批次");
        }

        // 检查是否有失败条目
        int failedCount = itemMapper.countFailedItems(batchId);
        if (failedCount > 0) {
            throw new IllegalStateException("存在 " + failedCount + " 个校验失败的条目，无法审批通过");
        }

        batch.setStatus(ArchiveSubmitBatch.STATUS_APPROVED);
        batch.setApprovedBy(approvedBy);
        batch.setApprovedAt(LocalDateTime.now());
        batch.setApprovalComment(comment);
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        log.info("审批通过归档批次: {} (审批人: {})", batch.getBatchNo(), approvedBy);

        return batch;
    }

    /**
     * 驳回批次
     */
    @Transactional
    public ArchiveSubmitBatch rejectBatch(Long batchId, String rejectedBy, String comment) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!ArchiveSubmitBatch.STATUS_VALIDATING.equals(batch.getStatus())) {
            throw new IllegalStateException("只能驳回校验中的批次");
        }

        batch.setStatus(ArchiveSubmitBatch.STATUS_REJECTED);
        batch.setApprovedBy(rejectedBy);
        batch.setApprovedAt(LocalDateTime.now());
        batch.setApprovalComment(comment);
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        log.info("驳回归档批次: {} (原因: {})", batch.getBatchNo(), comment);

        return batch;
    }

    /**
     * 执行批次归档
     */
    @Transactional
    public ArchiveSubmitBatch executeBatchArchive(Long batchId, String archivedBy) {
        ArchiveSubmitBatch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new IllegalArgumentException("批次不存在: " + batchId);
        }
        if (!batch.canArchive()) {
            throw new IllegalStateException("批次状态不允许执行归档: " + batch.getStatus());
        }

        // 执行四性检测
        Map<String, Object> integrityReport = fourNatureChecker.runIntegrityCheck(batchId);
        batch.setIntegrityReport(integrityReport);

        // 更新所有条目状态为已归档
        itemMapper.updateStatusByBatchId(batchId, ArchiveBatchItem.STATUS_ARCHIVED);

        // 锁定期间
        String startPeriod = batch.getPeriodStart().toString().substring(0, 7);
        String endPeriod = batch.getPeriodEnd().toString().substring(0, 7);

        // 为每个月份创建锁定记录
        LocalDate current = batch.getPeriodStart().withDayOfMonth(1);
        while (!current.isAfter(batch.getPeriodEnd())) {
            String period = current.toString().substring(0, 7);
            PeriodLock existingLock = periodLockMapper.findActiveLockByType(
                    batch.getFondsId(), period, PeriodLock.TYPE_ARCHIVED);

            if (existingLock == null) {
                PeriodLock lock = PeriodLock.builder()
                        .fondsId(batch.getFondsId())
                        .period(period)
                        .lockType(PeriodLock.TYPE_ARCHIVED)
                        .lockedAt(LocalDateTime.now())
                        .lockedBy(archivedBy)
                        .reason("归档批次: " + batch.getBatchNo())
                        .build();
                periodLockMapper.insert(lock);
            }

            current = current.plusMonths(1);
        }

        // 更新批次状态
        batch.setStatus(ArchiveSubmitBatch.STATUS_ARCHIVED);
        batch.setArchivedBy(archivedBy);
        batch.setArchivedAt(LocalDateTime.now());
        batch.setLastModifiedTime(LocalDateTime.now());
        batchMapper.updateById(batch);

        log.info("归档批次执行完成: {} ({} 凭证, {} 单据)",
                batch.getBatchNo(), batch.getVoucherCount(), batch.getDocCount());

        return batch;
    }
}
