// Input: MyBatis-Plus、Lombok、Spring Security、Spring Framework、等
// Output: AuditLogController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('audit:view') or hasAuthority('audit_logs') or hasRole('audit_admin') or hasRole('SYSTEM_ADMIN') or hasAuthority('nav:all')")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    public Result<Page<SysAuditLog>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String action
    ) {
        return Result.success(auditLogQueryService.query(page, limit, userId, resourceType, action));
    }
}
