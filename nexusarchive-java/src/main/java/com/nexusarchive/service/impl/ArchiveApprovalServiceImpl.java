package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.mapper.ArchiveApprovalMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.ArchiveApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 档案审批服务实现
 */
@Service
@RequiredArgsConstructor
public class ArchiveApprovalServiceImpl implements ArchiveApprovalService {

    private final ArchiveApprovalMapper approvalMapper;
    private final ArchiveMapper archiveMapper;
    private final com.nexusarchive.mapper.ArcFileContentMapper arcFileContentMapper;

    @Override
    @Transactional
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
    public void approveArchive(String id, String approverId, String approverName, String comment) {
        ArchiveApproval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new RuntimeException("Approval record not found");
        }

        if (!"PENDING".equals(approval.getStatus())) {
            throw new RuntimeException("Only pending approvals can be processed");
        }

        // Update approval record
        approval.setStatus("APPROVED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApprovalComment(comment);
        approval.setApprovalTime(LocalDateTime.now());
        approval.setLastModifiedTime(LocalDateTime.now()); // Manual update
        approvalMapper.updateById(approval);

        // Update archive status
        Archive archive = archiveMapper.selectById(approval.getArchiveId());
        if (archive != null) {
            archive.setStatus("ARCHIVED");
            archive.setLastModifiedTime(LocalDateTime.now()); // Manual update
            archiveMapper.updateById(archive); // Ensure updated_at is set
            
            // Sync status to ArcFileContent
            QueryWrapper<com.nexusarchive.entity.ArcFileContent> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("archival_code", archive.getArchiveCode());
            java.util.List<com.nexusarchive.entity.ArcFileContent> files = arcFileContentMapper.selectList(queryWrapper);
            
            for (com.nexusarchive.entity.ArcFileContent file : files) {
                file.setPreArchiveStatus("ARCHIVED");
                file.setArchivedTime(LocalDateTime.now());
                arcFileContentMapper.updateById(file);
            }
        }
    }

    @Override
    @Transactional
    public void rejectArchive(String id, String approverId, String approverName, String comment) {
        ArchiveApproval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new RuntimeException("Approval record not found");
        }

        if (!"PENDING".equals(approval.getStatus())) {
            throw new RuntimeException("Only pending approvals can be processed");
        }

        // Update approval record
        approval.setStatus("REJECTED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApprovalComment(comment);
        approval.setApprovalTime(LocalDateTime.now());
        approval.setLastModifiedTime(LocalDateTime.now());
        approvalMapper.updateById(approval);

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
                file.setPreArchiveStatus("PENDING_ARCHIVE"); 
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
        QueryWrapper<ArchiveApproval> queryWrapper = new QueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("created_at");
        return approvalMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public ArchiveApproval getApprovalById(String id) {
        return approvalMapper.selectById(id);
    }
}
