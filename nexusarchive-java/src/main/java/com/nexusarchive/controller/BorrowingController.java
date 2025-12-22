// Input: MyBatis-Plus、Jakarta EE、Lombok、Spring Security、等
// Output: BorrowingController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Borrowing;
import com.nexusarchive.security.CustomUserDetails;
import com.nexusarchive.service.BorrowingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/borrowing")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingService borrowingService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('borrowing:create','nav:all') or hasRole('business_user') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CREATE", resourceType = "BORROWING", description = "创建借阅申请")
    public Result<Borrowing> createBorrowing(@RequestBody Borrowing borrowing, HttpServletRequest request) {
        String userId = resolveUserId(request);
        String userName = resolveUserName();
        return Result.success(borrowingService.createBorrowing(borrowing, userId, userName));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('borrowing:view','borrowing:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<Borrowing>> getBorrowings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean my,
            HttpServletRequest request) {

        String userId = null;
        if (Boolean.TRUE.equals(my)) {
            userId = resolveUserId(request);
        } else {
            // 非管理权限默认仅看本人
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            boolean canManage = authentication != null && authentication.getAuthorities().stream()
                    .anyMatch(a -> "borrowing:manage".equals(a.getAuthority())
                            || "borrowing:approve".equals(a.getAuthority())
                            || "nav:all".equals(a.getAuthority())
                            || a.getAuthority().equals("ROLE_SYSTEM_ADMIN"));
            if (!canManage) {
                userId = resolveUserId(request);
            }
        }
        return Result.success(borrowingService.getBorrowings(page, limit, status, userId));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('borrowing:approve','borrowing:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "APPROVE", resourceType = "BORROWING", description = "审批借阅申请")
    public Result<Borrowing> approveBorrowing(@PathVariable String id,
                                              @RequestBody(required = false) ApprovalRequest approvalRequest) {
        if (approvalRequest == null) {
            throw new BusinessException("审批参数不能为空");
        }
        Borrowing updated = borrowingService.approveBorrowing(id, approvalRequest.isApproved(), approvalRequest.getComment());
        return Result.success(updated);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyAuthority('borrowing:return','borrowing:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "RETURN", resourceType = "BORROWING", description = "归还借阅档案")
    public Result<Void> returnArchive(@PathVariable String id) {
        borrowingService.returnArchive(id);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('borrowing:cancel','borrowing:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CANCEL", resourceType = "BORROWING", description = "取消借阅申请")
    public Result<Void> cancelBorrowing(@PathVariable String id) {
        borrowingService.cancelBorrowing(id);
        return Result.success();
    }

    private String resolveUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr != null) {
            return userIdAttr.toString();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getId();
        }
        throw new BusinessException("无法获取当前登录用户");
    }

    private String resolveUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            if (details.getFullName() != null && !details.getFullName().isBlank()) {
                return details.getFullName();
            }
            return details.getUsername();
        }
        return "未知用户";
    }

    @Data
    public static class ApprovalRequest {
        private boolean approved;
        private String comment;
    }
}
