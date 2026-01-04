// Input: MyBatis-Plus、Lombok、Spring Framework、Spring Security、等
// Output: ArchiveApprovalController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.service.ArchiveApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nexusarchive.security.CustomUserDetails;
import com.nexusarchive.annotation.ArchivalAudit;

import jakarta.validation.Valid;

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
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
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
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CREATE_APPROVAL", resourceType = "ARCHIVE_APPROVAL", description = "创建归档审批申请")
    public Result<ArchiveApproval> createApproval(@Valid @RequestBody ArchiveApproval approval,
                                                  @AuthenticationPrincipal CustomUserDetails user) {
        if (user != null && approval.getApplicantId() == null) {
            approval.setApplicantId(user.getId());
            approval.setApplicantName(user.getFullName());
        }
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
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "APPROVE_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "批准归档申请")
    public Result<Void> approveArchive(@Valid @RequestBody ApprovalRequest request,
                                       @AuthenticationPrincipal CustomUserDetails user) {
        String approverId = user != null ? user.getId() : "system";
        String approverName = user != null ? user.getFullName() : "system";
        approvalService.approveArchive(
            request.getId(),
            approverId,
            approverName,
            request.getComment()
        );
        return Result.success(null);
    }

    /**
     * 拒绝归档
     */
    @PostMapping("/reject")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "REJECT_ARCHIVE", resourceType = "ARCHIVE_APPROVAL", description = "拒绝归档申请")
    public Result<Void> rejectArchive(@Valid @RequestBody ApprovalRequest request,
                                      @AuthenticationPrincipal CustomUserDetails user) {
        String approverId = user != null ? user.getId() : "system";
        String approverName = user != null ? user.getFullName() : "system";
        approvalService.rejectArchive(
            request.getId(),
            approverId,
            approverName,
            request.getComment()
        );
        return Result.success(null);
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
