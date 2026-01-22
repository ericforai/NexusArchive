// Input: Spring Framework、Java 标准库、本地模块
// Output: ArchiveSubmitBatchController REST 控制器
// Pos: 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ArchiveSubmitBatch;
import com.nexusarchive.entity.ArchiveBatchItem;
import com.nexusarchive.service.ArchiveSubmitBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 归档批次管理控制器
 *
 * 提供从预归档库到正式档案库的批量归档 API。
 */
@RestController
@RequestMapping("/archive-batch")
@RequiredArgsConstructor
@Tag(name = "归档批次管理", description = "归档批次创建、审批、执行等操作")
public class ArchiveSubmitBatchController {

    private final ArchiveSubmitBatchService batchService;

    // ========== 批次管理 ==========

    @PostMapping
    @Operation(summary = "创建归档批次")
    public Result<ArchiveSubmitBatch> createBatch(
            @Valid @RequestBody CreateBatchRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String userId = getUserId(user);
        ArchiveSubmitBatch batch = batchService.createBatch(
                request.getFondsId(),
                request.getPeriodStart(),
                request.getPeriodEnd(),
                userId
        );
        return Result.success(batch);
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "获取批次详情")
    public Result<ArchiveSubmitBatch> getBatch(@PathVariable Long batchId) {
        ArchiveSubmitBatch batch = batchService.getBatch(batchId);
        if (batch == null) {
            return Result.error(404, "批次不存在");
        }
        return Result.success(batch);
    }

    @GetMapping
    @Operation(summary = "分页查询批次")
    public Result<IPage<ArchiveSubmitBatch>> listBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String fondsId,
            @RequestParam(required = false) String status
    ) {
        Page<ArchiveSubmitBatch> pageParam = new Page<>(page, size);
        IPage<ArchiveSubmitBatch> result = batchService.listBatches(pageParam, fondsId, status);
        return Result.success(result);
    }

    @DeleteMapping("/{batchId}")
    @Operation(summary = "删除批次")
    public Result<Void> deleteBatch(@PathVariable Long batchId) {
        batchService.deleteBatch(batchId);
        return Result.success(null);
    }

    // ========== 批次条目管理 ==========

    @PostMapping("/{batchId}/vouchers")
    @Operation(summary = "添加凭证到批次")
    public Result<Integer> addVouchers(
            @PathVariable Long batchId,
            @RequestBody List<String> voucherIds
    ) {
        int added = batchService.addVouchersToBatch(batchId, voucherIds);
        return Result.success(added);
    }

    @PostMapping("/{batchId}/docs")
    @Operation(summary = "添加单据到批次")
    public Result<Integer> addDocs(
            @PathVariable Long batchId,
            @RequestBody List<String> docIds
    ) {
        int added = batchService.addDocsToBatch(batchId, docIds);
        return Result.success(added);
    }

    @DeleteMapping("/{batchId}/items/{itemId}")
    @Operation(summary = "从批次移除条目")
    public Result<Void> removeItem(
            @PathVariable Long batchId,
            @PathVariable Long itemId
    ) {
        batchService.removeItemFromBatch(batchId, itemId);
        return Result.success(null);
    }

    @GetMapping("/{batchId}/items")
    @Operation(summary = "获取批次条目列表")
    public Result<List<ArchiveBatchItem>> getItems(
            @PathVariable Long batchId,
            @RequestParam(required = false) String itemType
    ) {
        List<ArchiveBatchItem> items;
        if (itemType != null && !itemType.isEmpty()) {
            items = batchService.getBatchItemsByType(batchId, itemType);
        } else {
            items = batchService.getBatchItems(batchId);
        }
        return Result.success(items);
    }

    // ========== 归档流程 ==========

    @PostMapping("/{batchId}/submit")
    @Operation(summary = "提交批次进行校验")
    public Result<ArchiveSubmitBatch> submitBatch(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserDetails user
    ) {
        String userId = getUserId(user);
        ArchiveSubmitBatch batch = batchService.submitBatch(batchId, userId);
        return Result.success(batch);
    }

    @PostMapping("/{batchId}/validate")
    @Operation(summary = "执行批次校验")
    public Result<Map<String, Object>> validateBatch(@PathVariable Long batchId) {
        Map<String, Object> report = batchService.validateBatch(batchId);
        return Result.success(report);
    }

    @PostMapping("/{batchId}/approve")
    @Operation(summary = "审批通过")
    public Result<ArchiveSubmitBatch> approveBatch(
            @PathVariable Long batchId,
            @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String userId = getUserId(user);
        String comment = request != null ? request.getComment() : null;
        ArchiveSubmitBatch batch = batchService.approveBatch(batchId, userId, comment);
        return Result.success(batch);
    }

    @PostMapping("/{batchId}/reject")
    @Operation(summary = "审批驳回")
    public Result<ArchiveSubmitBatch> rejectBatch(
            @PathVariable Long batchId,
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        String userId = getUserId(user);
        ArchiveSubmitBatch batch = batchService.rejectBatch(batchId, userId, request.getComment());
        return Result.success(batch);
    }

    @PostMapping("/{batchId}/archive")
    @Operation(summary = "执行归档")
    public Result<ArchiveSubmitBatch> executeBatchArchive(
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserDetails user
    ) {
        String userId = getUserId(user);
        ArchiveSubmitBatch batch = batchService.executeBatchArchive(batchId, userId);
        return Result.success(batch);
    }

    // ========== 四性检测 ==========

    @PostMapping("/{batchId}/integrity-check")
    @Operation(summary = "执行四性检测")
    public Result<Map<String, Object>> runIntegrityCheck(@PathVariable Long batchId) {
        Map<String, Object> report = batchService.runIntegrityCheck(batchId);
        return Result.success(report);
    }

    // ========== 统计 ==========

    @GetMapping("/stats")
    @Operation(summary = "获取批次统计")
    public Result<Map<String, Object>> getStats(@RequestParam(required = false) String fondsId) {
        Map<String, Object> stats = batchService.getBatchStats(fondsId);
        return Result.success(stats);
    }

    // ========== 辅助方法 ==========

    private String getUserId(UserDetails user) {
        // Return username as String ID
        return user.getUsername();
    }

    // ========== 请求 DTO ==========

    @Data
    public static class CreateBatchRequest {
        @NotNull(message = "全宗ID不能为空")
        private String fondsId;

        @NotNull(message = "期间起始日期不能为空")
        private LocalDate periodStart;

        @NotNull(message = "期间结束日期不能为空")
        private LocalDate periodEnd;
    }

    @Data
    public static class ApprovalRequest {
        private String comment;
    }
}
