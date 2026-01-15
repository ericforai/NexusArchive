// Input: MyBatis-Plus、Spring Framework、Lombok、Java 标准库、等
// Output: ArchiveApprovalServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.dto.approval.BatchApprovalRequest;
import com.nexusarchive.dto.approval.BatchApprovalResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.mapper.ArchiveApprovalMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.ArchiveApprovalService;
import com.nexusarchive.service.PreArchiveSubmitService;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Lazy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 档案审批服务实现
 */
@Slf4j
@Service
public class ArchiveApprovalServiceImpl implements ArchiveApprovalService {

    private final ArchiveApprovalMapper approvalMapper;
    private final ArchiveMapper archiveMapper;
    private final com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper;
    private final PreArchiveSubmitService preArchiveSubmitService;

    public ArchiveApprovalServiceImpl(
            ArchiveApprovalMapper approvalMapper,
            ArchiveMapper archiveMapper,
            com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper,
            @Lazy PreArchiveSubmitService preArchiveSubmitService) {
        this.approvalMapper = approvalMapper;
        this.archiveMapper = archiveMapper;
        this.arcFileContentMapper = arcFileContentMapper;
        this.preArchiveSubmitService = preArchiveSubmitService;
    }

    @Override
    @Transactional
    @ArchivalAudit(operationType = "CREATE_APPROVAL", resourceType = "ARCHIVE_APPROVAL", description = "创建归档审批申请")
    public ArchiveApproval createApproval(ArchiveApproval approval) {
        // 验证档案是否存在
        Archive archive = archiveMapper.selectById(approval.getArchiveId());
        if (archive == null) {
            throw new RuntimeException("Archive not found: " + approval.getArchiveId());
        }

        // 设置冗余字段
        approval.setArchiveCode(archive.getArchiveCode());
        approval.setArchiveTitle(archive.getTitle());
        approval.setStatus("PENDING");

        approvalMapper.insert(approval);
        return approval;
    }

    @Override
    @Transactional
    @ArchivalAudit(operationType = "APPROVE_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "批准归档申请")
    public void approveArchive(String id, String approverId, String approverName, String comment) {
        ArchiveApproval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new RuntimeException("Approval record not found");
        }

        // 防重锁：仅允许 PENDING -> APPROVED 的单次状态跳转
        approval.setStatus("APPROVED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApprovalComment(comment);
        approval.setApprovalTime(LocalDateTime.now());
        approval.setLastModifiedTime(LocalDateTime.now()); // Manual update

        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ArchiveApproval> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.eq("id", id).eq("status", "PENDING");
        int updated = approvalMapper.update(approval, wrapper);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approval already processed");
        }

        // Update archive status and trigger locking/conversion
        Archive archive = archiveMapper.selectById(approval.getArchiveId());
        if (archive != null) {
            // Call the complete archival process (OFD conversion, Signing, Locking)
            preArchiveSubmitService.completeArchival(archive.getId());
        }
    }

    @Override
    @Transactional
    @ArchivalAudit(operationType = "REJECT_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "拒绝归档申请")
    public void rejectArchive(String id, String approverId, String approverName, String comment) {
        ArchiveApproval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new RuntimeException("Approval record not found");
        }

        // 防重锁：仅允许 PENDING -> REJECTED 的单次状态跳转
        approval.setStatus("REJECTED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApprovalComment(comment);
        approval.setApprovalTime(LocalDateTime.now());
        approval.setLastModifiedTime(LocalDateTime.now());

        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<ArchiveApproval> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.eq("id", id).eq("status", "PENDING");
        int updated = approvalMapper.update(approval, wrapper);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approval already processed");
        }

        // Update archive status
        Archive archive = archiveMapper.selectById(approval.getArchiveId());
        if (archive != null) {
            archive.setStatus("REJECTED");
            archive.setLastModifiedTime(LocalDateTime.now());
            archiveMapper.updateById(archive);
            
            // Sync status to ArcFileContent (Back to PENDING_METADATA or PENDING_ARCHIVE?)
            // If rejected, user needs to fix issue. Let's send back to PENDING_ARCHIVE so they can re-submit if it was just a mistake,
            // or they can edit metadata then re-submit. PENDING_ARCHIVE is safest.
            QueryWrapper<com.nexusarchive.entity.ArcFileContent> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("archival_code", archive.getArchiveCode());
            java.util.List<com.nexusarchive.entity.ArcFileContent> files = arcFileContentMapper.selectList(queryWrapper);
            
            for (com.nexusarchive.entity.ArcFileContent file : files) {
                file.setPreArchiveStatus("READY_TO_ARCHIVE"); 
                // Don't clear archival_code yet, as it might be reused or they might verify again.
                // Actually, if rejected, the Archive record is marked rejected. 
                // The user might create a NEW application.
                // Keeping it linked for history is okay.
                arcFileContentMapper.updateById(file);
            }
        }
    }

    @Override
    public Page<ArchiveApproval> getApprovalList(int page, int limit, String status) {
        Page<ArchiveApproval> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<ArchiveApproval> queryWrapper = new LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(ArchiveApproval::getStatus, status);
        }

        queryWrapper.orderByDesc(ArchiveApproval::getCreatedTime);
        return approvalMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public ArchiveApproval getApprovalById(String id) {
        return approvalMapper.selectById(id);
    }

    @Override
    // 移除外层 @Transactional，让内部 approveArchive() 方法独立处理事务
    @ArchivalAudit(operationType = "BATCH_APPROVE_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "批量批准归档申请")
    public BatchApprovalResponse batchApprove(BatchApprovalRequest request) {
        BatchApprovalResponse response = new BatchApprovalResponse();

        // 构建跳过ID集合（如果存在）
        Set<String> skipIdSet = request.getSkipIds() != null
                ? new HashSet<>(request.getSkipIds())
                : new HashSet<>();

        // 遍历所有ID进行处理
        for (String id : request.getIds()) {
            // 跳过标记为跳过的记录
            if (skipIdSet.contains(id)) {
                log.info("Skipping approval record: {}", id);
                continue;
            }

            try {
                // 调用单个审批方法
                approveArchive(
                        id,
                        request.getApproverId(),
                        request.getApproverName(),
                        request.getComment()
                );
                response.incrementSuccess();
                log.debug("Successfully approved archive: {}", id);
            } catch (Exception e) {
                // 记录失败，继续处理下一条
                response.addError(id, e.getMessage());
                log.warn("Failed to approve archive {}: {}", id, e.getMessage(), e);
            }
        }

        log.info("Batch approval completed: {} succeeded, {} failed",
                response.getSuccessCount(), response.getFailed());

        return response;
    }

    @Override
    // 移除外层 @Transactional，让内部 rejectArchive() 方法独立处理事务
    @ArchivalAudit(operationType = "BATCH_REJECT_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "批量拒绝归档申请")
    public BatchApprovalResponse batchReject(BatchApprovalRequest request) {
        BatchApprovalResponse response = new BatchApprovalResponse();

        // 构建跳过ID集合（如果存在）
        Set<String> skipIdSet = request.getSkipIds() != null
                ? new HashSet<>(request.getSkipIds())
                : new HashSet<>();

        // 遍历所有ID进行处理
        for (String id : request.getIds()) {
            // 跳过标记为跳过的记录
            if (skipIdSet.contains(id)) {
                log.info("Skipping approval record: {}", id);
                continue;
            }

            try {
                // 调用单个拒绝方法
                rejectArchive(
                        id,
                        request.getApproverId(),
                        request.getApproverName(),
                        request.getComment()
                );
                response.incrementSuccess();
                log.debug("Successfully rejected archive: {}", id);
            } catch (Exception e) {
                // 记录失败，继续处理下一条
                response.addError(id, e.getMessage());
                log.warn("Failed to reject archive {}: {}", id, e.getMessage(), e);
            }
        }

        log.info("Batch rejection completed: {} succeeded, {} failed",
                response.getSuccessCount(), response.getFailed());

        return response;
    }
}
