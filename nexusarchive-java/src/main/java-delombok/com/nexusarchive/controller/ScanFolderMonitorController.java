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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
 * PRD 来源: 扫描集成模块
 * 提供文件夹监控的 REST API
 */
@Tag(name = "文件夹监控", description = """
    文件夹监控配置管理接口。

    **功能说明:**
    - 查询监控配置列表
    - 添加监控配置
    - 更新监控配置
    - 删除监控配置
    - 切换监控启用状态

    **监控机制:**
    - 使用 Java NIO WatchService 实时监控
    - 支持递归监控子目录
    - 文件创建事件触发自动处理
    - 支持文件类型过滤

    **支持的文件类型:**
    - PDF: 便携式文档格式
    - OFD: 版式文档
    - JPG/PNG/TIFF: 图像格式
    - XML: 结构化数据

    **监控状态:**
    - ACTIVE: 监控中
    - INACTIVE: 已暂停
    - ERROR: 监控异常

    **自动处理规则:**
    - 新文件自动复制到扫描工作区
    - 自动触发 OCR 识别（如配置）
    - 记录文件来源为 folder

    **使用场景:**
    - 扫描仪指定目录监控
    - FTP 接收目录自动处理
    - 共享文件夹文件采集

    **权限要求:**
    - scan:view 查看权限
    - scan:upload 上传权限
    - scan:edit 编辑权限
    - scan:delete 删除权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/scan/folder-monitors")
@Validated
@RequiredArgsConstructor
@Slf4j
public class ScanFolderMonitorController {

    private final FolderMonitorService folderMonitorService;

    /**
     * 获取当前用户的文件夹监控列表
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('scan:view', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取文件夹监控列表",
        description = """
            获取当前用户的所有文件夹监控配置。

            **返回数据包括:**
            - id: 监控配置 ID
            - folderPath: 监控文件夹路径
            - recursive: 是否递归监控子目录
            - fileExtensions: 监控的文件扩展名
            - status: 监控状态
            - autoOcr: 是否自动 OCR 识别
            - lastTriggerTime: 最后触发时间
            - processedFileCount: 已处理文件数

            **业务规则:**
            - 只返回当前用户的配置
            - 按创建时间倒序排列

            **使用场景:**
            - 监控配置列表展示
            - 监控状态查看
            """,
        operationId = "getFolderMonitors",
        tags = {"文件夹监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
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
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('scan:upload', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_MONITOR_ADD", resourceType = "SCAN_FOLDER_MONITOR",
                  description = "添加文件夹监控")
    @Operation(
        summary = "添加文件夹监控",
        description = """
            为当前用户添加新的文件夹监控配置。

            **请求参数:**
            - folderPath: 监控文件夹路径（必填）
            - recursive: 是否递归监控子目录（默认 false）
            - fileExtensions: 监控的文件扩展名（如 pdf,ofd,jpg）
            - autoOcr: 是否自动触发 OCR 识别（默认 false）

            **业务规则:**
            - 路径必须存在且可访问
            - 同一路径不能重复配置
            - 创建后自动启动监控
            - 记录审计日志

            **使用场景:**
            - 新增监控配置
            - 扫描仪输出目录配置
            """,
        operationId = "addFolderMonitor",
        tags = {"文件夹监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "添加成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或路径无效"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<FolderMonitorVO> addFolderMonitor(
            @Valid @RequestBody FolderMonitorRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String userId = user != null ? user.getId() : "system";
        ScanFolderMonitor monitor = folderMonitorService.addMonitor(userId, request);
        return Result.success("监控添加成功", folderMonitorService.toVO(monitor));
    }

    /**
     * 更新文件夹监控
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('scan:edit', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_MONITOR_UPDATE", resourceType = "SCAN_FOLDER_MONITOR",
                  description = "更新文件夹监控")
    @Operation(
        summary = "更新文件夹监控",
        description = """
            更新指定的文件夹监控配置。

            **路径参数:**
            - id: 监控配置 ID

            **请求参数:**
            完整的监控配置对象

            **业务规则:**
            - 只能更新当前用户的配置
            - 更新监控路径会重启监控
            - 记录审计日志

            **使用场景:**
            - 修改监控配置
            - 更新文件类型过滤
            """,
        operationId = "updateFolderMonitor",
        tags = {"文件夹监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    public Result<FolderMonitorVO> updateFolderMonitor(
            @Parameter(description = "监控配置 ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody FolderMonitorRequest request,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String userId = user != null ? user.getId() : "system";
        ScanFolderMonitor monitor = folderMonitorService.updateMonitor(id, userId, request);
        return Result.success("监控更新成功", folderMonitorService.toVO(monitor));
    }

    /**
     * 删除文件夹监控
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('scan:delete', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_MONITOR_DELETE", resourceType = "SCAN_FOLDER_MONITOR",
                  description = "删除文件夹监控")
    @Operation(
        summary = "删除文件夹监控",
        description = """
            删除指定的文件夹监控配置。

            **路径参数:**
            - id: 监控配置 ID

            **业务规则:**
            - 只能删除当前用户的配置
            - 删除前停止监控
            - 删除操作不可逆
            - 记录审计日志

            **使用场景:**
            - 移除不需要的监控
            - 清理无效配置
            """,
        operationId = "deleteFolderMonitor",
        tags = {"文件夹监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    public Result<Void> deleteFolderMonitor(
            @Parameter(description = "监控配置 ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String userId = user != null ? user.getId() : "system";
        folderMonitorService.deleteMonitor(id, userId);
        return Result.success();
    }

    /**
     * 切换监控启用状态
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('scan:edit', 'scan:manage', 'nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SCAN_MONITOR_TOGGLE", resourceType = "SCAN_FOLDER_MONITOR",
                  description = "切换文件夹监控状态")
    @Operation(
        summary = "切换监控状态",
        description = """
            启用或禁用指定的文件夹监控。

            **路径参数:**
            - id: 监控配置 ID

            **业务规则:**
            - ACTIVE → INACTIVE: 停止监控
            - INACTIVE → ACTIVE: 启动监控
            - 只能操作当前用户的配置
            - 记录审计日志

            **使用场景:**
            - 暂停监控
            - 恢复监控
            """,
        operationId = "toggleFolderMonitor",
        tags = {"文件夹监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "切换成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "配置不存在")
    })
    public Result<FolderMonitorVO> toggleMonitor(
            @Parameter(description = "监控配置 ID", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal com.nexusarchive.security.CustomUserDetails user) {
        String userId = user != null ? user.getId() : "system";
        ScanFolderMonitor monitor = folderMonitorService.toggleMonitor(id, userId);
        return Result.success("监控状态已切换", folderMonitorService.toVO(monitor));
    }
}
