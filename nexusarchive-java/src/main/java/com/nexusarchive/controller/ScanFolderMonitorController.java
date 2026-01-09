// Input: Spring Web, Spring Security, Lombok, Jakarta Validation
// Output: ScanFolderMonitorController (文件夹监控CRUD API)
// Pos: Controller Layer

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.scan.FolderMonitorRequest;
import com.nexusarchive.dto.scan.FolderMonitorVO;
import com.nexusarchive.entity.ScanFolderMonitor;
import com.nexusarchive.service.FolderMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件夹监控控制器
 *
 * 提供文件夹监控的REST API:
 * - 查询监控配置列表
 * - 添加监控配置
 * - 更新监控配置
 * - 删除监控配置
 * - 切换监控启用状态
 */
@RestController
@RequestMapping("/scan/folder-monitors")
@Validated
@RequiredArgsConstructor
@Slf4j
@Tag(name = "文件夹监控", description = "文件夹监控配置管理")
public class ScanFolderMonitorController {

    private final FolderMonitorService folderMonitorService;

    /**
     * 获取当前用户的文件夹监控列表
     *
     * GET /scan/folder-monitors
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('scan:view', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "获取文件夹监控列表", description = "获取当前用户的所有文件夹监控配置")
    public Result<List<FolderMonitorVO>> getFolderMonitors(
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String userId = user != null ? user.getId() : "system";
        List<ScanFolderMonitor> monitors = folderMonitorService.findByUserId(userId);
        List<FolderMonitorVO> vos = monitors.stream()
            .map(folderMonitorService::toVO)
            .toList();
        return Result.success(vos);
    }

    /**
     * 添加文件夹监控
     *
     * POST /scan/folder-monitors
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('scan:upload', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_MONITOR_ADD", resourceType = "SCAN_FOLDER_MONITOR",
                  description = "添加文件夹监控")
    @Operation(summary = "添加文件夹监控", description = "为当前用户添加新的文件夹监控配置")
    public Result<FolderMonitorVO> addFolderMonitor(
            @Valid @RequestBody FolderMonitorRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String userId = user != null ? user.getId() : "system";
        ScanFolderMonitor monitor = folderMonitorService.addMonitor(userId, request);
        return Result.success("监控添加成功", folderMonitorService.toVO(monitor));
    }

    /**
     * 更新文件夹监控
     *
     * PUT /scan/folder-monitors/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('scan:edit', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_MONITOR_UPDATE", resourceType = "SCAN_FOLDER_MONITOR",
                  description = "更新文件夹监控")
    @Operation(summary = "更新文件夹监控", description = "更新指定的文件夹监控配置")
    public Result<FolderMonitorVO> updateFolderMonitor(
            @PathVariable Long id,
            @Valid @RequestBody FolderMonitorRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String userId = user != null ? user.getId() : "system";
        ScanFolderMonitor monitor = folderMonitorService.updateMonitor(id, userId, request);
        return Result.success("监控更新成功", folderMonitorService.toVO(monitor));
    }

    /**
     * 删除文件夹监控
     *
     * DELETE /scan/folder-monitors/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('scan:delete', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_MONITOR_DELETE", resourceType = "SCAN_FOLDER_MONITOR",
                  description = "删除文件夹监控")
    @Operation(summary = "删除文件夹监控", description = "删除指定的文件夹监控配置")
    public Result<Void> deleteFolderMonitor(
            @PathVariable Long id,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String userId = user != null ? user.getId() : "system";
        folderMonitorService.deleteMonitor(id, userId);
        return Result.success();
    }

    /**
     * 切换监控启用状态
     *
     * PATCH /scan/folder-monitors/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('scan:edit', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_MONITOR_TOGGLE", resourceType = "SCAN_FOLDER_MONITOR",
                  description = "切换文件夹监控状态")
    @Operation(summary = "切换监控状态", description = "启用或禁用指定的文件夹监控")
    public Result<FolderMonitorVO> toggleMonitor(
            @PathVariable Long id,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String userId = user != null ? user.getId() : "system";
        ScanFolderMonitor monitor = folderMonitorService.toggleMonitor(id, userId);
        return Result.success("监控状态已切换", folderMonitorService.toVO(monitor));
    }
}
