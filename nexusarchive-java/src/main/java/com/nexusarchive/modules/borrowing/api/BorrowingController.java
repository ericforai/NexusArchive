// Input: MyBatis-Plus、Jakarta EE、Lombok、Spring Security、等
// Output: BorrowingController 类
// Pos: borrowing/api
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.modules.borrowing.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingApprovalRequest;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingCreateRequest;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingDto;
import com.nexusarchive.modules.borrowing.app.BorrowingFacade;
import com.nexusarchive.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/borrowing")
@RequiredArgsConstructor
public class BorrowingController {

    private final BorrowingFacade borrowingFacade;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('borrowing:create','nav:all') or hasRole('business_user') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CREATE", resourceType = "BORROWING", description = "创建借阅申请")
    public Result<BorrowingDto> createBorrowing(@Valid @RequestBody BorrowingCreateRequest borrowing, HttpServletRequest request) {
        String userId = resolveUserId(request);
        String userName = resolveUserName();
        return Result.success(borrowingFacade.createBorrowing(borrowing, userId, userName));
    }

    @GetMapping
    // 允许所有已认证用户访问，数据隔离由 BorrowingScopePolicyImpl 处理
    // 没有全宗权限的用户只能查看自己的借阅记录
    public Result<Page<BorrowingDto>> getBorrowings(
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
        return Result.success(borrowingFacade.getBorrowings(page, limit, status, userId));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('borrowing:approve','borrowing:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "APPROVE", resourceType = "BORROWING", description = "审批借阅申请")
    public Result<BorrowingDto> approveBorrowing(@PathVariable String id,
                                                 @Valid @RequestBody(required = false) BorrowingApprovalRequest approvalRequest) {
        if (approvalRequest == null) {
            throw new BusinessException(ErrorCode.BORROW_APPROVAL_PARAMS_CANNOT_BE_EMPTY);
        }
        BorrowingDto updated = borrowingFacade.approveBorrowing(id, approvalRequest);
        return Result.success(updated);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyAuthority('borrowing:return','borrowing:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "RETURN", resourceType = "BORROWING", description = "归还借阅档案")
    public Result<Void> returnArchive(@PathVariable String id) {
        borrowingFacade.returnArchive(id);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('borrowing:cancel','borrowing:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "CANCEL", resourceType = "BORROWING", description = "取消借阅申请")
    public Result<Void> cancelBorrowing(@PathVariable String id) {
        borrowingFacade.cancelBorrowing(id);
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
        throw new BusinessException(ErrorCode.CANNOT_GET_CURRENT_USER);
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
}
