// Input: Spring Web、FondsHistoryService
// Output: FondsHistoryController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.FondsHistoryDetail;
import com.nexusarchive.service.FondsHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 全宗沿革控制器
 *
 * PRD 来源: Section 1.1 - 全宗沿革可追溯
 */
@Tag(name = "全宗沿革管理", description = """
    全宗迁移、合并、分立、重命名接口。

    **功能说明:**
    - 记录全宗的变更历史
    - 支持全宗迁移、合并、分立、重命名操作
    - 确保全宗沿革可追溯

    **沿革类型:**
    - MIGRATE: 全宗迁移（数据从一个全宗迁移到另一个）
    - MERGE: 全宗合并（多个全宗合并为一个）
    - SPLIT: 全宗分立（一个全宗拆分为多个）
    - RENAME: 全宗重命名（全宗编号变更）

    **业务规则:**
    - 所有沿革操作需记录审批单号
    - 操作需要指定生效日期
    - 操作人信息会被记录
    - 沿革记录不可删除，仅可查询

    **使用场景:**
    - 企业重组导致全宗调整
    - 法人合并导致全宗合并
    - 法人分立导致全宗拆分
    - 全宗编号规范调整

    **权限要求:**
    - fonds:view: 查看全宗沿革权限
    - fonds:manage: 管理全宗沿革权限
    - SYS_ADMIN: 系统管理员
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/fonds-history")
@RequiredArgsConstructor
public class FondsHistoryController {

    private final FondsHistoryService fondsHistoryService;

    @PostMapping("/migrate")
    @Operation(
        summary = "全宗迁移",
        description = """
            将一个全宗的数据迁移到另一个全宗。

            **请求参数:**
            - fromFondsNo: 源全宗号（必填）
            - toFondsNo: 目标全宗号（必填）
            - effectiveDate: 生效日期（必填，格式：YYYY-MM-DD）
            - reason: 迁移原因（必填）
            - approvalTicketId: 审批单号（可选）

            **业务规则:**
            - 源全宗和目标全宗必须存在
            - 目标全宗不能与源全宗相同
            - 迁移后源全宗的数据会转移到目标全宗

            **使用场景:**
            - 档案从一个全宗转移到另一个全宗
            - 组织架构调整导致的档案迁移
            """,
        operationId = "migrateFonds",
        tags = {"全宗沿革管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "迁移成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('fonds:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, String>> migrateFonds(
            @Parameter(description = "源全宗号", required = true, example = "F001")
            @RequestParam String fromFondsNo,
            @Parameter(description = "目标全宗号", required = true, example = "F002")
            @RequestParam String toFondsNo,
            @Parameter(description = "生效日期", required = true, example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @Parameter(description = "迁移原因", required = true, example = "组织架构调整")
            @RequestParam String reason,
            @Parameter(description = "审批单号")
            @RequestParam(required = false) String approvalTicketId,
            @RequestHeader("X-User-Id") String operatorId) {

        try {
            String historyId = fondsHistoryService.migrateFonds(
                fromFondsNo, toFondsNo, effectiveDate, reason, approvalTicketId, operatorId);
            return Result.success(Map.of("historyId", historyId));
        } catch (Exception e) {
            log.error("全宗迁移失败: fromFondsNo={}, toFondsNo={}", fromFondsNo, toFondsNo, e);
            return Result.fail("全宗迁移失败: " + e.getMessage());
        }
    }

    @PostMapping("/merge")
    @Operation(
        summary = "全宗合并",
        description = """
            将多个源全宗合并到一个目标全宗。

            **请求参数:**
            - sourceFondsNos: 源全宗号列表（必填，至少 2 个）
            - targetFondsNo: 目标全宗号（必填）
            - effectiveDate: 生效日期（必填，格式：YYYY-MM-DD）
            - reason: 合并原因（必填）
            - approvalTicketId: 审批单号（可选）

            **业务规则:**
            - 至少需要 2 个源全宗
            - 目标全宗不能在源全宗列表中
            - 合并后源全宗的数据会转移到目标全宗
            - 为每个源全宗创建一条沿革记录

            **使用场景:**
            - 多个子公司合并为一个集团公司
            - 业务整合导致的档案合并
            """,
        operationId = "mergeFonds",
        tags = {"全宗沿革管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "合并成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('fonds:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, Object>> mergeFonds(
            @Parameter(description = "源全宗号列表", required = true, example = "[\"F001\", \"F002\"]")
            @RequestParam List<String> sourceFondsNos,
            @Parameter(description = "目标全宗号", required = true, example = "F003")
            @RequestParam String targetFondsNo,
            @Parameter(description = "生效日期", required = true, example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @Parameter(description = "合并原因", required = true, example = "集团公司重组")
            @RequestParam String reason,
            @Parameter(description = "审批单号")
            @RequestParam(required = false) String approvalTicketId,
            @RequestHeader("X-User-Id") String operatorId) {

        try {
            List<String> historyIds = fondsHistoryService.mergeFonds(
                sourceFondsNos, targetFondsNo, effectiveDate, reason, approvalTicketId, operatorId);
            return Result.success(Map.of("historyIds", historyIds));
        } catch (Exception e) {
            log.error("全宗合并失败: sourceFondsNos={}, targetFondsNo={}", sourceFondsNos, targetFondsNo, e);
            return Result.fail("全宗合并失败: " + e.getMessage());
        }
    }

    @PostMapping("/split")
    @Operation(
        summary = "全宗分立",
        description = """
            将一个源全宗拆分为多个新全宗。

            **请求参数:**
            - sourceFondsNo: 源全宗号（必填）
            - newFondsNos: 新全宗号列表（必填，至少 2 个）
            - effectiveDate: 生效日期（必填，格式：YYYY-MM-DD）
            - reason: 分立原因（必填）
            - approvalTicketId: 审批单号（可选）

            **业务规则:**
            - 至少需要创建 2 个新全宗
            - 源全宗不能在新全宗列表中
            - 分立后源全宗的数据会分配到新全宗
            - 为每个新全宗创建一条沿革记录

            **使用场景:**
            - 一个集团公司拆分为多个子公司
            - 业务拆分导致的档案分立
            """,
        operationId = "splitFonds",
        tags = {"全宗沿革管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "分立成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('fonds:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, Object>> splitFonds(
            @Parameter(description = "源全宗号", required = true, example = "F001")
            @RequestParam String sourceFondsNo,
            @Parameter(description = "新全宗号列表", required = true, example = "[\"F002\", \"F003\"]")
            @RequestParam List<String> newFondsNos,
            @Parameter(description = "生效日期", required = true, example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @Parameter(description = "分立原因", required = true, example = "业务拆分")
            @RequestParam String reason,
            @Parameter(description = "审批单号")
            @RequestParam(required = false) String approvalTicketId,
            @RequestHeader("X-User-Id") String operatorId) {

        try {
            List<String> historyIds = fondsHistoryService.splitFonds(
                sourceFondsNo, newFondsNos, effectiveDate, reason, approvalTicketId, operatorId);
            return Result.success(Map.of("historyIds", historyIds));
        } catch (Exception e) {
            log.error("全宗分立失败: sourceFondsNo={}, newFondsNos={}", sourceFondsNo, newFondsNos, e);
            return Result.fail("全宗分立失败: " + e.getMessage());
        }
    }

    @PostMapping("/rename")
    @Operation(
        summary = "全宗重命名",
        description = """
            将一个全宗编号重命名为新的编号。

            **请求参数:**
            - oldFondsNo: 原全宗号（必填）
            - newFondsNo: 新全宗号（必填）
            - effectiveDate: 生效日期（必填，格式：YYYY-MM-DD）
            - reason: 重命名原因（必填）
            - approvalTicketId: 审批单号（可选）

            **业务规则:**
            - 新全宗号必须未被使用
            - 新全宗号不能与原全宗号相同
            - 重命名后所有关联数据会更新为新全宗号

            **使用场景:**
            - 全宗编号规范调整
            - 全宗编号冲突解决
            - 命名规则变更
            """,
        operationId = "renameFonds",
        tags = {"全宗沿革管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "重命名成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或新编号已被使用"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('fonds:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, String>> renameFonds(
            @Parameter(description = "原全宗号", required = true, example = "F001")
            @RequestParam String oldFondsNo,
            @Parameter(description = "新全宗号", required = true, example = "F001-NEW")
            @RequestParam String newFondsNo,
            @Parameter(description = "生效日期", required = true, example = "2025-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @Parameter(description = "重命名原因", required = true, example = "编号规范调整")
            @RequestParam String reason,
            @Parameter(description = "审批单号")
            @RequestParam(required = false) String approvalTicketId,
            @RequestHeader("X-User-Id") String operatorId) {

        try {
            String historyId = fondsHistoryService.renameFonds(
                oldFondsNo, newFondsNo, effectiveDate, reason, approvalTicketId, operatorId);
            return Result.success(Map.of("historyId", historyId));
        } catch (Exception e) {
            log.error("全宗重命名失败: oldFondsNo={}, newFondsNo={}", oldFondsNo, newFondsNo, e);
            return Result.fail("全宗重命名失败: " + e.getMessage());
        }
    }

    @GetMapping("/{fondsNo}")
    @Operation(
        summary = "查询全宗沿革历史",
        description = """
            查询指定全宗的沿革变更历史。

            **路径参数:**
            - fondsNo: 全宗号

            **返回数据包括:**
            - id: 历史记录ID
            - fondsNo: 全宗号
            - changeType: 变更类型（MIGRATE/MERGE/SPLIT/RENAME）
            - effectiveDate: 生效日期
            - reason: 变更原因
            - operatorId: 操作人
            - createdAt: 创建时间
            - relatedFonds: 关联的全宗列表

            **使用场景:**
            - 查看全宗变更历史
            - 沿革追溯
            - 审计查询
            """,
        operationId = "getFondsHistory",
        tags = {"全宗沿革管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasAnyAuthority('fonds:view', 'fonds:manage')")
    public Result<List<FondsHistoryDetail>> getFondsHistory(
            @Parameter(description = "全宗号", required = true, example = "F001")
            @PathVariable String fondsNo) {
        try {
            List<FondsHistoryDetail> history = fondsHistoryService.getFondsHistory(fondsNo);
            return Result.success(history);
        } catch (Exception e) {
            log.error("查询全宗沿革历史失败: fondsNo={}", fondsNo, e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }
}
