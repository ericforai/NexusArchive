// Input: Spring Web、io.swagger、Jakarta EE、Lombok
// Output: OriginalVoucherController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.OriginalVoucher;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.entity.VoucherRelation;
import com.nexusarchive.entity.OriginalVoucherType;
import com.nexusarchive.service.OriginalVoucherService;
import com.nexusarchive.service.OriginalVoucherService.OriginalVoucherStats;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 原始凭证管理控制器
 * <p>
 * 提供原始凭证的 CRUD、版本控制、关联管理、归档流程等 REST API
 * </p>
 * Reference: DA/T 94-2022, 《原始凭证模块设计规范 V1.0》
 */
@RestController
@RequestMapping("/original-vouchers")
@RequiredArgsConstructor
@Validated
@Tag(name = "Original Voucher", description = "原始凭证管理接口")
public class OriginalVoucherController {

    private final OriginalVoucherService voucherService;

    // ===== 查询接口 =====

    @GetMapping
    @Operation(summary = "分页查询原始凭证", description = "根据条件分页检索原始凭证列表")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Page<OriginalVoucher>> list(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Max(100) int limit,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String search,
            @Parameter(description = "一级类型") @RequestParam(required = false) String category,
            @Parameter(description = "二级类型") @RequestParam(required = false) String type,
            @Parameter(description = "归档状态") @RequestParam(required = false) String status,
            @Parameter(description = "全宗号") @RequestParam(required = false) String fondsCode,
            @Parameter(description = "会计年度") @RequestParam(required = false) String fiscalYear) {
        return Result.success(
                voucherService.getVouchers(page, limit, search, category, type, status, fondsCode, fiscalYear));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取原始凭证详情")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucher> get(@PathVariable String id) {
        return Result.success(voucherService.getById(id));
    }

    @GetMapping("/{id}/files")
    @Operation(summary = "获取原始凭证关联的文件列表")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<OriginalVoucherFile>> getFiles(@PathVariable String id) {
        return Result.success(voucherService.getFiles(id));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "获取版本历史")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<OriginalVoucher>> getVersionHistory(@PathVariable String id) {
        return Result.success(voucherService.getVersionHistory(id));
    }

    @GetMapping("/files/download/{fileId}")
    @Operation(summary = "下载原始凭证文件内容")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @PathVariable String fileId) {
        return voucherService.downloadFile(fileId);
    }

    @GetMapping("/{id}/relations")
    @Operation(summary = "获取关联的记账凭证")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<VoucherRelation>> getRelations(@PathVariable String id) {
        return Result.success(voucherService.getAccountingRelations(id));
    }

    // ===== 创建接口 =====

    @PostMapping
    @Operation(summary = "创建原始凭证")
    @ArchivalAudit(operationType = "CREATE", resourceType = "ORIGINAL_VOUCHER", description = "创建原始凭证")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucher> create(
            @Valid @RequestBody OriginalVoucher voucher,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return Result.success(voucherService.create(voucher, userId));
    }

    // ===== 更新接口 =====

    @PutMapping("/{id}")
    @Operation(summary = "更新原始凭证", description = "已归档的凭证会创建新版本")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "ORIGINAL_VOUCHER", description = "更新原始凭证")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucher> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateVoucherRequest request,
            HttpServletRequest httpRequest) {
        String userId = (String) httpRequest.getAttribute("userId");
        return Result.success(voucherService.update(id, request.voucher(), request.reason(), userId));
    }

    // ===== 删除接口 =====

    @DeleteMapping("/{id}")
    @Operation(summary = "删除原始凭证", description = "逻辑删除，已归档的凭证不允许删除")
    @ArchivalAudit(operationType = "DELETE", resourceType = "ORIGINAL_VOUCHER", description = "删除原始凭证")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> delete(@PathVariable String id, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        voucherService.delete(id, userId);
        return Result.success();
    }

    // ===== 文件管理 =====

    @PostMapping("/{id}/files")
    @Operation(summary = "添加文件到原始凭证")
    @ArchivalAudit(operationType = "ADD_FILE", resourceType = "ORIGINAL_VOUCHER", description = "添加文件")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucherFile> addFile(
            @PathVariable String id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "fileRole", defaultValue = "PRIMARY") String fileRole,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return Result.success(voucherService.addFile(id, file, fileRole, userId));
    }


    // ===== 关联管理 =====

    @PostMapping("/{id}/relations")
    @Operation(summary = "建立与记账凭证的关联")
    @ArchivalAudit(operationType = "CREATE_RELATION", resourceType = "ORIGINAL_VOUCHER", description = "建立关联")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<VoucherRelation> createRelation(
            @PathVariable String id,
            @RequestBody CreateRelationRequest req,
            HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return Result.success(voucherService.createRelation(id, req.accountingVoucherId(), req.description(), userId));
    }

    @DeleteMapping("/relations/{relationId}")
    @Operation(summary = "删除关联关系")
    @ArchivalAudit(operationType = "DELETE_RELATION", resourceType = "ORIGINAL_VOUCHER", description = "删除关联")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> deleteRelation(@PathVariable String relationId) {
        voucherService.deleteRelation(relationId);
        return Result.success();
    }

    // ===== 归档流程 =====

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交归档")
    @ArchivalAudit(operationType = "SUBMIT_ARCHIVE", resourceType = "ORIGINAL_VOUCHER", description = "提交归档")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> submitForArchive(@PathVariable String id, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        voucherService.submitForArchive(id, userId);
        return Result.success();
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认归档")
    @ArchivalAudit(operationType = "CONFIRM_ARCHIVE", resourceType = "ORIGINAL_VOUCHER", description = "确认归档")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<Void> confirmArchive(@PathVariable String id, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        voucherService.confirmArchive(id, userId);
        return Result.success();
    }

    // ===== 类型字典 =====

    @GetMapping("/types")
    @Operation(summary = "获取所有原始凭证类型")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<OriginalVoucherType>> getAllTypes() {
        return Result.success(voucherService.getAllTypes());
    }

    @GetMapping("/types/{categoryCode}")
    @Operation(summary = "按类别获取凭证类型")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<List<OriginalVoucherType>> getTypesByCategory(@PathVariable String categoryCode) {
        return Result.success(voucherService.getTypesByCategory(categoryCode));
    }

    // ===== 统计 =====

    @GetMapping("/stats")
    @Operation(summary = "获取原始凭证统计")
    @PreAuthorize("hasAnyAuthority('archive:read','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    public Result<OriginalVoucherStats> getStats(
            @RequestParam(required = false) String fondsCode,
            @RequestParam(required = false) String fiscalYear) {
        return Result.success(voucherService.getStats(fondsCode, fiscalYear));
    }

    // ===== DTO Records =====

    public record UpdateVoucherRequest(OriginalVoucher voucher, String reason) {
    }

    public record CreateRelationRequest(String accountingVoucherId, String description) {
    }
}
