// Input: Spring Web、AuthTicketService、AuthTicketApprovalService
// Output: AuthTicketController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.AuthScope;
import com.nexusarchive.dto.AuthTicketDetail;
import com.nexusarchive.service.AuthTicketApprovalService;
import com.nexusarchive.service.AuthTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;

/**
 * 授权票据控制器
 * 
 * PRD 来源: Section 2.4 - 跨全宗访问授权票据
 */
@Slf4j
@RestController
@RequestMapping("/api/auth-ticket")
@RequiredArgsConstructor
@Tag(name = "授权票据管理", description = "跨全宗访问授权票据申请、审批、查询接口")
public class AuthTicketController {
    
    private final AuthTicketService authTicketService;
    private final AuthTicketApprovalService authTicketApprovalService;
    
    @PostMapping("/apply")
    @Operation(summary = "创建授权票据申请")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage')")
    public Result<Map<String, Object>> createAuthTicket(
            @RequestParam String targetFonds,
            @Valid @RequestBody AuthScope scope,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expiresAt,
            @RequestParam String reason,
            @RequestHeader("X-Current-Fonds-No") String sourceFonds,
            @RequestHeader("X-User-Id") String applicantId) {
        
        try {
            String ticketId = authTicketService.createAuthTicket(
                applicantId, sourceFonds, targetFonds, scope, expiresAt, reason);
            
            Map<String, Object> data = new HashMap<>();
            data.put("ticketId", ticketId);
            data.put("status", "PENDING");
            data.put("createdAt", LocalDateTime.now());
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("创建授权票据申请失败", e);
            return Result.error("创建授权票据申请失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{ticketId}")
    @Operation(summary = "查询授权票据详情")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage')")
    public Result<AuthTicketDetail> getAuthTicketDetail(@PathVariable String ticketId) {
        try {
            AuthTicketDetail detail = authTicketService.getAuthTicketDetail(ticketId);
            return Result.success(detail);
        } catch (Exception e) {
            log.error("查询授权票据详情失败: ticketId={}", ticketId, e);
            return Result.error("查询授权票据详情失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{ticketId}/revoke")
    @Operation(summary = "撤销授权票据")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:manage')")
    public Result<Void> revokeAuthTicket(
            @PathVariable String ticketId,
            @RequestParam String reason,
            @RequestHeader("X-User-Id") String operatorId) {
        
        try {
            authTicketService.revokeAuthTicket(ticketId, operatorId, reason);
            return Result.success(null);
        } catch (Exception e) {
            log.error("撤销授权票据失败: ticketId={}", ticketId, e);
            return Result.error("撤销授权票据失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{ticketId}/first-approval")
    @Operation(summary = "第一审批人审批")
    @PreAuthorize("hasAnyAuthority('archive:approve', 'archive:manage')")
    public Result<Void> firstApproval(
            @PathVariable String ticketId,
            @RequestParam String comment,
            @RequestParam boolean approved,
            @RequestHeader("X-User-Id") String approverId,
            @RequestHeader("X-User-Name") String approverName) {
        
        try {
            authTicketApprovalService.firstApproval(ticketId, approverId, approverName, comment, approved);
            return Result.success(null);
        } catch (Exception e) {
            log.error("第一审批失败: ticketId={}", ticketId, e);
            return Result.error("第一审批失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/{ticketId}/second-approval")
    @Operation(summary = "第二审批人审批（复核）")
    @PreAuthorize("hasAnyAuthority('archive:approve', 'archive:manage')")
    public Result<Void> secondApproval(
            @PathVariable String ticketId,
            @RequestParam String comment,
            @RequestParam boolean approved,
            @RequestHeader("X-User-Id") String approverId,
            @RequestHeader("X-User-Name") String approverName) {
        
        try {
            authTicketApprovalService.secondApproval(ticketId, approverId, approverName, comment, approved);
            return Result.success(null);
        } catch (Exception e) {
            log.error("第二审批失败: ticketId={}", ticketId, e);
            return Result.error("第二审批失败: " + e.getMessage());
        }
    }
}





