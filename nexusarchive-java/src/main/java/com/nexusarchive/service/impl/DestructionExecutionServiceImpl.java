// Input: DestructionExecutionService, DestructionMapper, ArchiveMapper, FileStorageService, DestructionValidationService, AuditLogService
// Output: DestructionExecutionServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.constants.OperationResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveAttachment;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.exception.DestructionNotAllowedException;
import com.nexusarchive.mapper.ArchiveAttachmentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.DestructionMapper;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 销毁执行服务实现
 * 
 * 实现要点：
 * 1. 权限校验：需 Archivist 角色 + DESTRUCTION_APPROVED 状态
 * 2. 事务一致性：清册写入 → 元数据更新（事务内）→ 物理文件删除（事务提交后）
 * 3. 默认软删除，硬删除需额外审批
 * 4. 记录文件删除审计日志
 * [P1-FIX] 分离事务边界，确保元数据回滚时物理文件未被删除
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DestructionExecutionServiceImpl implements DestructionExecutionService {
    
    private final DestructionMapper destructionMapper;
    private final ArchiveMapper archiveMapper;
    private final ArchiveAttachmentMapper archiveAttachmentMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final FileStorageService fileStorageService;
    private final DestructionValidationService destructionValidationService;
    private final AuditLogService auditLogService;
    private final DestructionLogService destructionLogService; // 下一个任务实现
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DestructionExecutionResult executeDestruction(String destructionId, String executorId, 
                                                          DestructionMode mode) {
        // 1. 查询销毁申请
        Destruction destruction = destructionMapper.selectById(destructionId);
        if (destruction == null) {
            throw new IllegalArgumentException("销毁申请不存在: " + destructionId);
        }
        
        // 2. 验证状态：必须是 DESTRUCTION_APPROVED
        if (!"DESTRUCTION_APPROVED".equals(destruction.getStatus())) {
            throw new IllegalStateException(
                String.format("销毁申请状态为 %s，无法执行销毁。必须先通过审批。", destruction.getStatus()));
        }
        
        // 3. 权限校验（TODO: 实现角色校验，当前先跳过）
        // if (!hasArchivistRole(executorId)) {
        //     throw new AccessDeniedException("执行人需具备 Archivist 角色");
        // }
        
        // 4. 解析档案ID列表
        List<String> archiveIds;
        try {
            archiveIds = objectMapper.readValue(
                destruction.getArchiveIds(), 
                new TypeReference<List<String>>() {}
            );
        } catch (Exception e) {
            throw new RuntimeException("解析档案ID列表失败: " + e.getMessage(), e);
        }
        
        if (archiveIds == null || archiveIds.isEmpty()) {
            throw new IllegalArgumentException("待销毁档案列表为空");
        }
        
        // 5. 获取全宗号（从第一个档案获取）
        String fondsNo = null;
        if (!archiveIds.isEmpty()) {
            Archive firstArchive = archiveMapper.selectById(archiveIds.get(0));
            if (firstArchive != null) {
                fondsNo = firstArchive.getFondsNo();
            }
        }
        
        // 6. 校验档案是否可销毁（在借校验、冻结校验等）
        try {
            destructionValidationService.validateDestructionEligibility(archiveIds, fondsNo);
        } catch (DestructionNotAllowedException e) {
            throw e;
        }
        
        // 7. 生成 TraceID
        String traceId = UUID.randomUUID().toString().replace("-", "");
        
        // 8. 执行销毁（[P1-FIX] 两阶段策略：事务内更新元数据，事务后执行物理删除）
        int destroyedCount = 0;
        DestructionMode actualMode = mode != null ? mode : DestructionMode.SOFT_DELETE; // 默认软删除
        List<PendingFileDeletion> allPendingDeletions = new ArrayList<>();
        
        for (String archiveId : archiveIds) {
            try {
                List<PendingFileDeletion> pendingDeletions = destroyArchiveMetadata(
                        archiveId, destructionId, executorId, traceId);
                allPendingDeletions.addAll(pendingDeletions);
                destroyedCount++;
            } catch (Exception e) {
                log.error("销毁档案失败: {}", archiveId, e);
                // 事务回滚，确保数据一致性（此时物理文件未被删除）
                throw new RuntimeException("销毁档案失败: " + archiveId + ", " + e.getMessage(), e);
            }
        }
        
        // 9. [P1-FIX] 事务提交后执行物理删除
        // 使用 TransactionSynchronizationManager 注册事务同步回调
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executePhysicalDeletions(allPendingDeletions, actualMode);
                }
            }
        );
        
        // 10. 更新销毁申请状态
        destruction.setStatus("EXECUTED");
        destruction.setExecutionTime(LocalDateTime.now());
        destructionMapper.updateById(destruction);
        
        // 11. 记录审计日志
        SysAuditLog destructionAudit = new SysAuditLog();
        destructionAudit.setUserId(executorId);
        destructionAudit.setUsername("系统");
        destructionAudit.setAction("DESTRUCTION_EXECUTE");
        destructionAudit.setResourceType("DESTRUCTION");
        destructionAudit.setResourceId(destructionId);
        destructionAudit.setOperationResult(OperationResult.SUCCESS);
        destructionAudit.setDetails(String.format(
                "执行销毁，档案数量: %d, 模式: %s, TraceID: %s", destroyedCount, actualMode, traceId));
        destructionAudit.setTraceId(traceId);
        auditLogService.log(destructionAudit);
        
        // 12. 返回结果
        DestructionExecutionResult result = new DestructionExecutionResult();
        result.setDestroyedCount(destroyedCount);
        result.setTraceId(traceId);
        result.setMode(actualMode);
        
        log.info("销毁执行完成，销毁申请ID: {}, 执行人: {}, 销毁数量: {}, TraceID: {}", 
                destructionId, executorId, destroyedCount, traceId);
        
        return result;
    }
    
    /**
     * 销毁单个档案 - 仅更新元数据（在事务内执行）
     * [P1-FIX] 分离事务边界：此方法仅处理元数据，不执行物理删除
     * @return 待删除的文件路径列表
     */
    private List<PendingFileDeletion> destroyArchiveMetadata(String archiveId, String destructionId, 
                                                              String executorId, String traceId) {
        // 1. 查询档案
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new IllegalArgumentException("档案不存在: " + archiveId);
        }
        
        // 2. 写入销毁清册记录（必须先写入，确保事务一致性）
        destructionLogService.logDestruction(archive, destructionId, executorId, traceId);
        
        // 3. 更新档案状态为 DESTROYED
        LambdaUpdateWrapper<Archive> updateWrapper = new LambdaUpdateWrapper<Archive>()
                .eq(Archive::getId, archiveId)
                .set(Archive::getDestructionStatus, "DESTROYED");
        
        archiveMapper.update(null, updateWrapper);
        
        // 4. 收集待删除文件列表（不立即删除）
        List<PendingFileDeletion> pendingDeletions = collectPendingFileDeletions(archiveId, executorId, traceId);
        
        log.debug("档案元数据销毁完成: {}, 待删除文件数: {}", archive.getArchiveCode(), pendingDeletions.size());
        return pendingDeletions;
    }
    
    /**
     * 收集待删除文件列表
     */
    private List<PendingFileDeletion> collectPendingFileDeletions(String archiveId, String executorId, String traceId) {
        List<PendingFileDeletion> pendingDeletions = new ArrayList<>();
        
        LambdaQueryWrapper<ArchiveAttachment> queryWrapper = new LambdaQueryWrapper<ArchiveAttachment>()
                .eq(ArchiveAttachment::getArchiveId, archiveId);
        
        List<ArchiveAttachment> attachments = archiveAttachmentMapper.selectList(queryWrapper);
        
        for (ArchiveAttachment attachment : attachments) {
            String fileId = attachment.getFileId();
            String storagePath = getFileStoragePath(fileId);
            if (storagePath != null) {
                pendingDeletions.add(new PendingFileDeletion(fileId, storagePath, executorId, traceId));
            }
        }
        
        return pendingDeletions;
    }
    
    /**
     * 执行物理文件删除（在事务提交后调用）
     * [P1-FIX] 此方法不在事务内执行，确保元数据已正确提交
     */
    private void executePhysicalDeletions(List<PendingFileDeletion> pendingDeletions, DestructionMode mode) {
        for (PendingFileDeletion pending : pendingDeletions) {
            try {
                FileStorageService.FileInfo fileInfo = fileStorageService.getFileInfo(pending.storagePath());
                
                boolean deleted = false;
                if (mode == DestructionMode.SOFT_DELETE) {
                    deleted = fileStorageService.softDelete(pending.storagePath());
                } else if (mode == DestructionMode.HARD_DELETE) {
                    deleted = fileStorageService.hardDelete(pending.storagePath());
                }
                
                // 记录文件删除审计日志
                if (deleted && fileInfo != null) {
                    SysAuditLog fileAudit = new SysAuditLog();
                    fileAudit.setUserId(pending.executorId());
                    fileAudit.setUsername("系统");
                    fileAudit.setAction("FILE_DELETE");
                    fileAudit.setResourceType("FILE");
                    fileAudit.setResourceId(pending.fileId());
                    fileAudit.setOperationResult(OperationResult.SUCCESS);
                    fileAudit.setDetails(String.format(
                            "删除文件: %s, 大小: %d, 模式: %s, TraceID: %s",
                            pending.storagePath(), fileInfo.getSize(), mode, pending.traceId()));
                    fileAudit.setTraceId(pending.traceId());
                    auditLogService.log(fileAudit);
                }
            } catch (Exception e) {
                // 物理删除失败仅记录日志，不影响整体流程（元数据已标记为DESTROYED）
                log.error("物理文件删除失败: {}", pending.storagePath(), e);
            }
        }
    }
    
    /**
     * 待删除文件记录
     */
    private record PendingFileDeletion(String fileId, String storagePath, String executorId, String traceId) {}
    
    /**
     * 获取文件存储路径
     */
    private String getFileStoragePath(String fileId) {
        ArcFileContent fileContent = arcFileContentMapper.selectById(fileId);
        if (fileContent != null) {
            return fileContent.getStoragePath();
        }
        return null;
    }
}
