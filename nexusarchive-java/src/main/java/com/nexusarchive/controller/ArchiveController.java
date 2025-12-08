package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 档案管理控制器
 */
@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
public class ArchiveController {

    private final ArchiveService archiveService;

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<Archive>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String orgId) {
        return Result.success(archiveService.getArchives(page, limit, search, status, categoryCode, orgId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Archive> get(@PathVariable String id) {
        return Result.success(archiveService.getArchiveById(id));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<java.util.List<Archive>> recent(@RequestParam(defaultValue = "5") int limit) {
        return Result.success(archiveService.getRecentArchives(limit));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Archive> create(@RequestBody Archive archive, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        Archive created = archiveService.createArchive(archive, userId);
        
        // 记录审计日志
        auditLogService.log(userId, null, "create", "archive", created.getId(), 
                "success", "创建档案: " + created.getTitle(), request.getRemoteAddr());
        
        return Result.success(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> update(@PathVariable String id, @RequestBody Archive archive, HttpServletRequest request) {
        archiveService.updateArchive(id, archive);
        
        // 记录审计日志
        String userId = (String) request.getAttribute("userId");
        auditLogService.log(userId, null, "update", "archive", id, 
                "success", "更新档案: " + archive.getTitle(), request.getRemoteAddr());
        
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> delete(@PathVariable String id, HttpServletRequest request) {
        archiveService.deleteArchive(id);
        
        // 记录审计日志
        String userId = (String) request.getAttribute("userId");
        auditLogService.log(userId, null, "delete", "archive", id, 
                "success", "删除档案", request.getRemoteAddr());
        
        return Result.success();
    }
}
