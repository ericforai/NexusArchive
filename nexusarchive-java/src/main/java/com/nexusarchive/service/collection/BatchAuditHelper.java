// Input: Spring Framework, AuditLogService
// Output: BatchAuditHelper 类
// Pos: Service Layer

package com.nexusarchive.service.collection;

import com.nexusarchive.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 批次审计日志辅助类
 *
 * 提供统一的审计日志记录方法
 */
@Component
@RequiredArgsConstructor
public class BatchAuditHelper {

    private final AuditLogService auditLogService;

    /**
     * 记录批次创建日志
     */
    public void logCreate(Long userId, Long batchId, String batchNo) {
        auditLogService.log(
                String.valueOf(userId),
                String.valueOf(userId),
                "CREATE_BATCH",
                "COLLECTION_BATCH",
                String.valueOf(batchId),
                "SUCCESS",
                "创建上传批次: " + batchNo,
                null
        );
    }

    /**
     * 记录批次完成日志
     */
    public void logComplete(Long userId, Long batchId, String batchNo) {
        auditLogService.log(
                String.valueOf(userId),
                String.valueOf(userId),
                "COMPLETE_BATCH",
                "COLLECTION_BATCH",
                String.valueOf(batchId),
                "SUCCESS",
                "完成批次上传: " + batchNo,
                null
        );
    }

    /**
     * 记录批次取消日志
     */
    public void logCancel(Long userId, Long batchId, String batchNo) {
        auditLogService.log(
                String.valueOf(userId),
                String.valueOf(userId),
                "CANCEL_BATCH",
                "COLLECTION_BATCH",
                String.valueOf(batchId),
                "SUCCESS",
                "取消批次: " + batchNo,
                null
        );
    }

    /**
     * 记录批次批准日志
     */
    public void logApprove(String operatorId, String operatorName, Long batchId, String batchNo, String comment) {
        String operator = operatorId != null ? operatorId : "system";
        auditLogService.log(
                operator,
                operatorName != null ? operatorName : "system",
                "APPROVE_BATCH",
                "COLLECTION_BATCH",
                String.valueOf(batchId),
                "SUCCESS",
                "批准批次归档: " + batchNo + (comment != null ? ", 备注: " + comment : ""),
                null
        );
    }

    /**
     * 记录批次拒绝日志
     */
    public void logReject(String operatorId, String operatorName, Long batchId, String batchNo, String comment) {
        String operator = operatorId != null ? operatorId : "system";
        auditLogService.log(
                operator,
                operatorName != null ? operatorName : "system",
                "REJECT_BATCH",
                "COLLECTION_BATCH",
                String.valueOf(batchId),
                "SUCCESS",
                "拒绝批次归档: " + batchNo + (comment != null ? ", 原因: " + comment : ""),
                null
        );
    }
}
