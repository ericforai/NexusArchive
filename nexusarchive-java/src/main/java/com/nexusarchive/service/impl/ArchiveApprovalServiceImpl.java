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

        // 更新审批记录
        approval.setStatus("APPROVED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApprovalComment(comment);
        approval.setApprovalTime(LocalDateTime.now());
        approvalMapper.updateById(approval);

        // 更新档案状态为已归档
        Archive archive = archiveMapper.selectById(approval.getArchiveId());
        if (archive != null) {
            archive.setStatus("ARCHIVED");
            archiveMapper.updateById(archive);
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

        // 更新审批记录
        approval.setStatus("REJECTED");
        approval.setApproverId(approverId);
        approval.setApproverName(approverName);
        approval.setApprovalComment(comment);
        approval.setApprovalTime(LocalDateTime.now());
        approvalMapper.updateById(approval);

        // 更新档案状态为已拒绝
        Archive archive = archiveMapper.selectById(approval.getArchiveId());
        if (archive != null) {
            archive.setStatus("REJECTED");
            archiveMapper.updateById(archive);
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
