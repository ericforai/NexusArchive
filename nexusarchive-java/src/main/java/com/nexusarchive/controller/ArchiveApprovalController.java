package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.service.ArchiveApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 档案审批控制器
 */
@RestController
@RequestMapping("/archive-approval")
@RequiredArgsConstructor
public class ArchiveApprovalController {

    private final ArchiveApprovalService approvalService;

    /**
     * 获取审批列表
     */
    @GetMapping("/list")
    public Result<Page<ArchiveApproval>> getApprovalList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status) {
        Page<ArchiveApproval> result = approvalService.getApprovalList(page, limit, status);
        return Result.success(result);
    }

    /**
     * 获取审批详情
     */
    @GetMapping("/{id}")
    public Result<ArchiveApproval> getApprovalById(@PathVariable String id) {
        ArchiveApproval approval = approvalService.getApprovalById(id);
        if (approval == null) {
            return Result.error(404, "Approval record not found");
        }
        return Result.success(approval);
    }

    /**
     * 创建审批申请
     */
    @PostMapping("/create")
    public Result<ArchiveApproval> createApproval(@RequestBody ArchiveApproval approval) {
        try {
            ArchiveApproval created = approvalService.createApproval(approval);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 批准归档
     */
    @PostMapping("/approve")
    public Result<Void> approveArchive(@RequestBody ApprovalRequest request) {
        try {
            approvalService.approveArchive(
                request.getId(),
                request.getApproverId(),
                request.getApproverName(),
                request.getComment()
            );
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 拒绝归档
     */
    @PostMapping("/reject")
    public Result<Void> rejectArchive(@RequestBody ApprovalRequest request) {
        try {
            approvalService.rejectArchive(
                request.getId(),
                request.getApproverId(),
                request.getApproverName(),
                request.getComment()
            );
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 审批请求DTO
     */
    @lombok.Data
    public static class ApprovalRequest {
        private String id;
        private String approverId;
        private String approverName;
        private String comment;
    }
}
