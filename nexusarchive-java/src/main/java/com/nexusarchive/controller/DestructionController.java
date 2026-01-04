// Input: MyBatis-Plus、Lombok、Spring Framework、Spring Security、等
// Output: DestructionController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.service.DestructionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.nexusarchive.security.CustomUserDetails;
import com.nexusarchive.annotation.ArchivalAudit;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/destruction")
@RequiredArgsConstructor
public class DestructionController {

    private final DestructionService destructionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CREATE_DESTRUCTION", resourceType = "DESTRUCTION", description = "创建销毁申请")
    public Result<Destruction> createDestruction(@Valid @RequestBody Destruction destruction,
                                                @AuthenticationPrincipal CustomUserDetails user) {
        if (destruction.getApplicantId() == null && user != null) {
            destruction.setApplicantId(user.getId());
            destruction.setApplicantName(user.getFullName());
        }
        return Result.success(destructionService.createDestruction(destruction));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:read','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<Destruction>> getDestructions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status) {
        return Result.success(destructionService.getDestructions(page, limit, status));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "APPROVE_DESTRUCTION", resourceType = "DESTRUCTION", description = "审批销毁")
    public Result<Void> approveDestruction(@PathVariable String id,
                                           @RequestBody(required = false) String comment,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        String approverId = user != null ? user.getId() : "system";
        destructionService.approveDestruction(id, approverId, comment);
        return Result.success();
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "EXECUTE_DESTRUCTION", resourceType = "DESTRUCTION", description = "执行销毁")
    public Result<Void> executeDestruction(@PathVariable String id) {
        destructionService.executeDestruction(id);
        return Result.success();
    }
    @GetMapping("/stats")
    @PreAuthorize("hasAnyAuthority('archive:destruction','archive:read','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<java.util.Map<String, Object>> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        
        // 1. 待鉴定档案 (模拟查询: 真实应查询 acc_archive where retention_period expired)
        stats.put("pendingAppraisal", 0);
        
        // 2. AI 建议销毁
        stats.put("aiSuggested", 0);
        
        // 3. 进行中批次
        stats.put("activeBatches", destructionService.getDestructions(1, 100, "PENDING").getTotal());
        
        // 4. 已安全销毁 (模拟)
        stats.put("safeDestructionCount", 0);
        
        return Result.success(stats);
    }
}
