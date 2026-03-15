// Input: ArchiveReadService、ArchiveWriteService、ArchiveStatus
// Output: ArchiveStateTransitionService 类
// Pos: archivecore/app

package com.nexusarchive.modules.archivecore.app;

import com.nexusarchive.common.enums.ArchiveStatus;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.modules.archivecore.api.dto.ArchiveStatusChangeRequest;
import com.nexusarchive.service.ArchiveReadService;
import com.nexusarchive.service.ArchiveWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 档案状态转换服务实现
 * <p>提供类型安全的状态转换管理，支持乐观锁并发控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveStateTransitionService implements ArchiveStateTransitionFacade {

    private final ArchiveReadService archiveReadService;
    private final ArchiveWriteService archiveWriteService;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void transitionStatus(String archiveId, ArchiveStatusChangeRequest request, String userId) {
        log.info("状态转换请求: archiveId={}, target={}, user={}",
                 archiveId, request.getTargetStatus(), userId);

        // 1. 加载档案
        Archive archive = archiveReadService.getArchiveById(archiveId);
        if (archive == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                "档案不存在: archiveId=" + archiveId);
        }

        // 2. 验证乐观锁版本（如果提供了预期版本）
        if (request.getExpectedVersion() != null
            && !request.getExpectedVersion().equals(archive.getVersion())) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                "版本冲突: 预期版本 " + request.getExpectedVersion()
                + ", 实际版本 " + archive.getVersion());
        }

        // 3. 验证状态转换合法性 - 快速失败
        String statusValue = archive.getStatus();
        // null 或空状态默认视为 DRAFT（兼容旧数据）
        ArchiveStatus currentStatus = (statusValue == null || statusValue.isBlank())
            ? ArchiveStatus.DRAFT
            : ArchiveStatus.fromCode(statusValue);

        if (!currentStatus.canTransitionTo(request.getTargetStatus())) {
            String reason = currentStatus.isTerminal()
                ? "终态不可转换"
                : "请遵循状态转换规则";
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION,
                String.format("非法状态转换: %s -> %s. %s",
                    currentStatus, request.getTargetStatus(), reason));
        }

        // 4. 执行状态转换
        Archive update = new Archive();
        update.setId(archiveId); // MyBatis-Plus 需要 ID 用于 WHERE 条件
        update.setStatus(request.getTargetStatus().getCode()); // 只更新状态字段
        // 注意：不要手动设置 version，让 MyBatis-Plus @Version 自动处理

        try {
            archiveWriteService.updateArchive(archiveId, update);
            log.info("状态转换成功: {} -> {}, version={}", currentStatus, request.getTargetStatus(), archive.getVersion());
        } catch (OptimisticLockingFailureException e) {
            log.warn("乐观锁冲突: archiveId={}", archiveId);
            throw new BusinessException(ErrorCode.VERSION_CONFLICT,
                "档案已被其他用户修改，请刷新后重试");
        }

        // 5. TODO: 记录审计日志（P2 阶段）
        // auditLogService.logStatusChange(archiveId, currentStatus, request.getTargetStatus(), userId, request.getReason());
    }

    @Override
    public int batchTransitionStatus(List<String> archiveIds, ArchiveStatus targetStatus, String userId) {
        if (archiveIds == null || archiveIds.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        int failureCount = 0;
        for (String archiveId : archiveIds) {
            try {
                ArchiveStatusChangeRequest request = new ArchiveStatusChangeRequest();
                request.setTargetStatus(targetStatus);
                // 每个操作在独立事务中执行（REQUIRES_NEW）
                transitionStatus(archiveId, request, userId);
                successCount++;
            } catch (BusinessException e) {
                failureCount++;
                log.warn("批量状态转换失败: archiveId={}, error={}", archiveId, e.getMessage());
                // 继续处理下一个，不中断批量操作
            }
        }
        log.info("批量状态转换完成: 成功={}, 失败={}", successCount, failureCount);
        return successCount;
    }

    @Override
    public boolean canTransition(String archiveId, ArchiveStatus targetStatus) {
        try {
            Archive archive = archiveReadService.getArchiveById(archiveId);
            if (archive == null) {
                return false;
            }
            String statusValue = archive.getStatus();
            if (statusValue == null || statusValue.isBlank()) {
                return false; // 无效状态不允许转换
            }
            ArchiveStatus current = ArchiveStatus.fromCode(statusValue);
            return current.canTransitionTo(targetStatus);
        } catch (Exception e) {
            log.warn("检查状态转换失败: archiveId={}", archiveId, e);
            return false;
        }
    }
}
