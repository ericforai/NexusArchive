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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
 * 提供从预归档库到正式档案库的批量归档 API
 */
@Tag(name = "归档批次管理", description = """
    归档批次创建、审批、执行接口。

    **功能说明:**
    - 创建归档批次
    - 批次条目管理（添加凭证、单据）
    - 批次校验和审批流程
    - 四性检测执行
    - 批次统计查询

    **批次状态:**
    - DRAFT: 草稿（编辑中）
    - SUBMITTED: 已提交（待校验）
    - VALIDATED: 已校验（待审批）
    - APPROVED: 已审批（待归档）
    - ARCHIVED: 已归档
    - REJECTED: 已驳回

    **条目类型:**
    - VOUCHER: 凭证
    - DOCUMENT: 单据

    **四性检测 (DA/T 92-2022):**
    - 真实性 (Authenticity): 数字签名验证
    - 完整性 (Integrity): SM3 哈希校验
    - 可用性 (Usability): 格式和可读性验证
    - 安全性 (Safety): 病毒扫描和访问控制

    **归档流程:**
    1. 创建批次 → DRAFT
    2. 添加条目（凭证/单据）
    3. 提交校验 → SUBMITTED
    4. 执行校验 → VALIDATED/REJECTED
    5. 审批 → APPROVED/REJECTED
    6. 执行归档 → ARCHIVED

    **使用场景:**
    - 月度批量归档
    - 预归档数据正式归档
    - 审计前数据准备
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/archive-batch")
@RequiredArgsConstructor
public class ArchiveSubmitBatchController {

    private final ArchiveSubmitBatchService batchService;

    // ========== 批次管理 ==========

    @PostMapping
    @Operation(
        summary = "创建归档批次",
        description = """
            创建新的归档批次。

            **请求体:**
            - fondsId: 全宗 ID（必填）
            - periodStart: 期间起始日期（必填，格式：YYYY-MM-DD）
            - periodEnd: 期间结束日期（必填，格式：YYYY-MM-DD）

            **业务规则:**
            - 同一全宗同一期间只能有一个进行中的批次
            - 期间跨度不能超过一年

            **使用场景:**
            - 月度归档批次创建
            - 指定期间数据归档
            """,
        operationId = "createArchiveBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "409", description = "期间内已存在进行中的批次")
    })
    public Result<ArchiveSubmitBatch> createBatch(
            @Parameter(description = "创建批次请求", required = true)
            @Valid @RequestBody CreateBatchRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        Long userId = getUserId(user);
        ArchiveSubmitBatch batch = batchService.createBatch(
                request.getFondsId(),
                request.getPeriodStart(),
                request.getPeriodEnd(),
                userId
        );
        return Result.success(batch);
    }

    @GetMapping("/{batchId}")
    @Operation(
        summary = "获取批次详情",
        description = """
            查询指定批次的详细信息。

            **路径参数:**
            - batchId: 批次 ID

            **返回数据包括:**
            - id: 批次 ID
            - fondsId: 全宗 ID
            - periodStart: 期间起始
            - periodEnd: 期间结束
            - status: 批次状态
            - itemCount: 条目数量
            - createdAt: 创建时间

            **使用场景:**
            - 批次详情查看
            - 状态确认
            """,
        operationId = "getArchiveBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<ArchiveSubmitBatch> getBatch(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId) {
        ArchiveSubmitBatch batch = batchService.getBatch(batchId);
        if (batch == null) {
            return Result.error(404, "批次不存在");
        }
        return Result.success(batch);
    }

    @GetMapping
    @Operation(
        summary = "分页查询批次",
        description = """
            分页查询归档批次列表。

            **查询参数:**
            - page: 页码（从 1 开始，默认 1）
            - size: 每页大小（默认 20）
            - fondsId: 全宗 ID（可选，用于筛选）
            - status: 状态（可选，用于筛选）

            **返回数据包括:**
            - records: 批次记录列表
            - total: 总记录数
            - page: 当前页码
            - size: 每页大小

            **使用场景:**
            - 批次列表展示
            - 状态筛选查询
            """,
        operationId = "listArchiveBatches",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Result<IPage<ArchiveSubmitBatch>> listBatches(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "全宗ID", example = "1")
            @RequestParam(required = false) Long fondsId,
            @Parameter(description = "状态", example = "DRAFT")
            @RequestParam(required = false) String status
    ) {
        Page<ArchiveSubmitBatch> pageParam = new Page<>(page, size);
        IPage<ArchiveSubmitBatch> result = batchService.listBatches(pageParam, fondsId, status);
        return Result.success(result);
    }

    @DeleteMapping("/{batchId}")
    @Operation(
        summary = "删除批次",
        description = """
            删除指定的归档批次。

            **路径参数:**
            - batchId: 批次 ID

            **业务规则:**
            - 仅草稿状态的批次可删除
            - 已提交的批次不可删除

            **使用场景:**
            - 取消错误的批次
            - 清理草稿数据
            """,
        operationId = "deleteArchiveBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "批次状态不允许删除"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<Void> deleteBatch(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId) {
        batchService.deleteBatch(batchId);
        return Result.success(null);
    }

    // ========== 批次条目管理 ==========

    @PostMapping("/{batchId}/vouchers")
    @Operation(
        summary = "添加凭证到批次",
        description = """
            批量添加凭证到指定批次。

            **路径参数:**
            - batchId: 批次 ID

            **请求体:**
            - 凭证 ID 列表

            **返回数据包括:**
            - added: 成功添加的数量

            **使用场景:**
            - 批量添加凭证
            - 条目内容填充
            """,
        operationId = "addVouchersToBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "添加成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<Integer> addVouchers(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId,
            @Parameter(description = "凭证ID列表", required = true)
            @RequestBody List<Long> voucherIds
    ) {
        int added = batchService.addVouchersToBatch(batchId, voucherIds);
        return Result.success(added);
    }

    @PostMapping("/{batchId}/docs")
    @Operation(
        summary = "添加单据到批次",
        description = """
            批量添加单据到指定批次。

            **路径参数:**
            - batchId: 批次 ID

            **请求体:**
            - 单据 ID 列表

            **返回数据包括:**
            - added: 成功添加的数量

            **使用场景:**
            - 批量添加单据
            - 条目内容填充
            """,
        operationId = "addDocsToBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "添加成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<Integer> addDocs(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId,
            @Parameter(description = "单据ID列表", required = true)
            @RequestBody List<Long> docIds
    ) {
        int added = batchService.addDocsToBatch(batchId, docIds);
        return Result.success(added);
    }

    @DeleteMapping("/{batchId}/items/{itemId}")
    @Operation(
        summary = "从批次移除条目",
        description = """
            从批次中移除指定条目。

            **路径参数:**
            - batchId: 批次 ID
            - itemId: 条目 ID

            **使用场景:**
            - 移除错误条目
            - 调整批次内容
            """,
        operationId = "removeItemFromBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "移除成功"),
        @ApiResponse(responseCode = "404", description = "批次或条目不存在")
    })
    public Result<Void> removeItem(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId,
            @Parameter(description = "条目ID", required = true, example = "1")
            @PathVariable Long itemId
    ) {
        batchService.removeItemFromBatch(batchId, itemId);
        return Result.success(null);
    }

    @GetMapping("/{batchId}/items")
    @Operation(
        summary = "获取批次条目列表",
        description = """
            查询批次中的所有条目。

            **路径参数:**
            - batchId: 批次 ID

            **查询参数:**
            - itemType: 条目类型（可选，VOUCHER/DOCUMENT）

            **返回数据包括:**
            - 条目列表，包含条目详情

            **使用场景:**
            - 查看批次内容
            - 条目类型筛选
            """,
        operationId = "getBatchItems",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<List<ArchiveBatchItem>> getItems(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId,
            @Parameter(description = "条目类型", example = "VOUCHER")
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
    @Operation(
        summary = "提交批次进行校验",
        description = """
            提交批次进入校验流程。

            **路径参数:**
            - batchId: 批次 ID

            **业务规则:**
            - 仅草稿状态可提交
            - 提交后状态变更为 SUBMITTED

            **使用场景:**
            - 条目添加完成后提交
            - 启动校验流程
            """,
        operationId = "submitArchiveBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "提交成功"),
        @ApiResponse(responseCode = "400", description = "批次状态不允许提交"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<ArchiveSubmitBatch> submitBatch(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserDetails user
    ) {
        Long userId = getUserId(user);
        ArchiveSubmitBatch batch = batchService.submitBatch(batchId, userId);
        return Result.success(batch);
    }

    @PostMapping("/{batchId}/validate")
    @Operation(
        summary = "执行批次校验",
        description = """
            对批次执行数据校验。

            **路径参数:**
            - batchId: 批次 ID

            **校验内容:**
            - 条目完整性检查
            - 数据格式验证
            - 业务规则校验
            - 必填字段检查

            **返回数据包括:**
            - valid: 是否通过校验
            - errorCount: 错误数量
            - warningCount: 警告数量
            - errors: 错误列表
            - warnings: 警告列表

            **使用场景:**
            - 归档前数据校验
            - 质量检查
            """,
        operationId = "validateArchiveBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "校验完成"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<Map<String, Object>> validateBatch(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId) {
        Map<String, Object> report = batchService.validateBatch(batchId);
        return Result.success(report);
    }

    @PostMapping("/{batchId}/approve")
    @Operation(
        summary = "审批通过",
        description = """
            审批通过归档批次。

            **路径参数:**
            - batchId: 批次 ID

            **请求体:**
            - comment: 审批意见（可选）

            **业务规则:**
            - 仅已校验状态可审批
            - 审批通过后状态变更为 APPROVED

            **使用场景:**
            - 校验通过后审批
            - 准备归档
            """,
        operationId = "approveArchiveBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "审批成功"),
        @ApiResponse(responseCode = "400", description = "批次状态不允许审批"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<ArchiveSubmitBatch> approveBatch(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId,
            @Parameter(description = "审批请求")
            @RequestBody(required = false) ApprovalRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        Long userId = getUserId(user);
        String comment = request != null ? request.getComment() : null;
        ArchiveSubmitBatch batch = batchService.approveBatch(batchId, userId, comment);
        return Result.success(batch);
    }

    @PostMapping("/{batchId}/reject")
    @Operation(
        summary = "审批驳回",
        description = """
            驳回归档批次。

            **路径参数:**
            - batchId: 批次 ID

            **请求体:**
            - comment: 驳回原因（必填）

            **业务规则:**
            - 驳回后状态变更为 REJECTED
            - 驳回后可重新编辑再提交

            **使用场景:**
            - 校验发现问题驳回
            - 数据不完整驳回
            """,
        operationId = "rejectArchiveBatch",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "驳回成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<ArchiveSubmitBatch> rejectBatch(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId,
            @Parameter(description = "驳回请求", required = true)
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal UserDetails user
    ) {
        Long userId = getUserId(user);
        ArchiveSubmitBatch batch = batchService.rejectBatch(batchId, userId, request.getComment());
        return Result.success(batch);
    }

    @PostMapping("/{batchId}/archive")
    @Operation(
        summary = "执行归档",
        description = """
            执行批次归档操作。

            **路径参数:**
            - batchId: 批次 ID

            **业务规则:**
            - 仅已审批状态可归档
            - 归档后状态变更为 ARCHIVED
            - 归档后数据进入正式档案库

            **使用场景:**
            - 审批通过后归档
            - 月度归档操作
            """,
        operationId = "executeBatchArchive",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "归档成功"),
        @ApiResponse(responseCode = "400", description = "批次状态不允许归档"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<ArchiveSubmitBatch> executeBatchArchive(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId,
            @AuthenticationPrincipal UserDetails user
    ) {
        Long userId = getUserId(user);
        ArchiveSubmitBatch batch = batchService.executeBatchArchive(batchId, userId);
        return Result.success(batch);
    }

    // ========== 四性检测 ==========

    @PostMapping("/{batchId}/integrity-check")
    @Operation(
        summary = "执行四性检测",
        description = """
            对批次执行四性检测（DA/T 92-2022）。

            **路径参数:**
            - batchId: 批次 ID

            **检测内容:**
            - 真实性: 数字签名验证、时间戳校验
            - 完整性: SM3 哈希校验、文件完整性
            - 可用性: 格式验证、可读性检查
            - 安全性: 病毒扫描、访问控制检查

            **返回数据包括:**
            - authentic: 真实性检测结果
            - intact: 完整性检测结果
            - usable: 可用性检测结果
            - safe: 安全性检测结果
            - overall: 综合评级（PASS/FAIL）
            - details: 详细检测报告

            **使用场景:**
            - 归档前质量检测
            - 合规性验证
            - 审计证据准备
            """,
        operationId = "runIntegrityCheck",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检测完成"),
        @ApiResponse(responseCode = "404", description = "批次不存在")
    })
    public Result<Map<String, Object>> runIntegrityCheck(
            @Parameter(description = "批次ID", required = true, example = "1")
            @PathVariable Long batchId) {
        Map<String, Object> report = batchService.runIntegrityCheck(batchId);
        return Result.success(report);
    }

    // ========== 统计 ==========

    @GetMapping("/stats")
    @Operation(
        summary = "获取批次统计",
        description = """
            获取归档批次的统计数据。

            **查询参数:**
            - fondsId: 全宗 ID（可选，不填则统计全部）

            **返回数据包括:**
            - totalCount: 总批次数
            - draftCount: 草稿数量
            - submittedCount: 已提交数量
            - approvedCount: 已批准数量
            - archivedCount: 已归档数量
            - rejectedCount: 已驳回数量
            - totalItems: 总条目数
            - pendingItems: 待处理条目数

            **使用场景:**
            - 统计仪表盘
            - 工作进度概览
            """,
        operationId = "getBatchStats",
        tags = {"归档批次管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Result<Map<String, Object>> getStats(
            @Parameter(description = "全宗ID", example = "1")
            @RequestParam(required = false) Long fondsId) {
        Map<String, Object> stats = batchService.getBatchStats(fondsId);
        return Result.success(stats);
    }

    // ========== 辅助方法 ==========

    private Long getUserId(UserDetails user) {
        // 简化实现：从用户名解析 ID，实际应从 UserDetails 扩展类获取
        try {
            return Long.parseLong(user.getUsername());
        } catch (NumberFormatException e) {
            return 1L; // 默认用户 ID
        }
    }

    // ========== 请求 DTO ==========

    @Data
    public static class CreateBatchRequest {
        @NotNull(message = "全宗ID不能为空")
        private Long fondsId;

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
