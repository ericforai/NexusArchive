// Input: MyBatis-Plus、io.swagger、Jakarta EE、Lombok、DtoMapper、ArchiveResponse
// Output: ArchiveController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.mapper.DtoMapper;
import com.nexusarchive.dto.response.ArchiveResponse;
import com.nexusarchive.dto.response.ArchiveFileResponse;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;

/**
 * 档案管理控制器
 * <p>
 * Provides REST APIs for managing Electronic Accounting Archives.
 * Compliant with DA/T 94-2022.
 * 所有返回值使用 DTO，避免直接暴露 Entity
 * </p>
 */
@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
@Validated
@Tag(name = "Archive Management", description = "电子会计档案核心管理接口")
public class ArchiveController {

    private final ArchiveService archiveService;
    private final DtoMapper dtoMapper;

    @GetMapping
    @Operation(summary = "分页查询档案", description = "根据条件分页检索档案列表，支持全宗号、档号、标题模糊搜索")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<ArchiveResponse>> list(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "页码必须大于0") int page,

            @Parameter(description = "每页条数", example = "10")
            @RequestParam(defaultValue = "10")
            @Max(value = 100, message = "每页最多显示100条") int limit,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "类别号") @RequestParam(required = false) String categoryCode,
            @Parameter(description = "部门ID") @RequestParam(required = false) String orgId,
            @Parameter(description = "子类型(账簿类型/报表周期/其他类型)") @RequestParam(required = false) String subType,
            @Parameter(description = "唯一业务ID") @RequestParam(required = false) String uniqueBizId,
            @Parameter(description = "全宗号") @RequestParam(required = false) String fondsNo) {
        Page<Archive> entityPage = archiveService.getArchives(page, limit, search, status, categoryCode, orgId, uniqueBizId, subType, fondsNo);
        return Result.success(dtoMapper.toArchiveResponsePage(entityPage));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取档案详情", description = "根据ID获取档案详细信息")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<ArchiveResponse> get(@PathVariable String id) {
        Archive archive = archiveService.getArchiveById(id);
        return Result.success(dtoMapper.toArchiveResponse(archive));
    }

    @GetMapping("/{id}/files")
    @Operation(summary = "获取档案关联文件", description = "获取档案关联的所有文件列表")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<ArchiveFileResponse>> getArchiveFiles(@PathVariable String id) {
        List<com.nexusarchive.entity.ArcFileContent> files = archiveService.getFilesByArchiveId(id);
        return Result.success(dtoMapper.toArchiveFileResponseList(files));
    }

    @GetMapping("/recent")
    @Operation(summary = "获取最近档案", description = "获取最近创建的档案列表")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<ArchiveResponse>> recent(@RequestParam(defaultValue = "5") int limit) {
        List<Archive> archives = archiveService.getRecentArchives(limit);
        return Result.success(dtoMapper.toArchiveResponseList(archives));
    }

    @PostMapping
    @Operation(summary = "创建档案", description = "创建新的电子会计档案")
    @ArchivalAudit(operationType = "CREATE", resourceType = "ARCHIVE", description = "创建新档案")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<ArchiveResponse> create(@Valid @RequestBody Archive archive, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        Archive created = archiveService.createArchive(archive, userId);
        return Result.success(dtoMapper.toArchiveResponse(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新档案", description = "更新现有档案的元数据")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ARCHIVE", description = "更新档案元数据")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> update(
            @PathVariable String id,
            @Valid @RequestBody Archive archive,
            HttpServletRequest request) {
        archiveService.updateArchive(id, archive);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除档案", description = "逻辑删除档案")
    @ArchivalAudit(operationType = "DELETE", resourceType = "ARCHIVE", description = "删除档案")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> delete(@PathVariable String id, HttpServletRequest request) {
        archiveService.deleteArchive(id);
        return Result.success();
    }
}
