// Input: Spring Web、AdvancedArchiveSearchService
// Output: AdvancedArchiveSearchController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.Result;
import com.nexusarchive.dto.request.AdvancedSearchRequest;
import com.nexusarchive.dto.response.ArchiveSearchResult;
import com.nexusarchive.service.AdvancedArchiveSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import jakarta.validation.Valid;

/**
 * 高级档案检索控制器
 *
 * PRD 来源: Section 2.1 - 高级检索与脱敏
 */
@Tag(name = "高级档案检索", description = """
    高级档案检索接口。

    **功能说明:**
    - 支持多条件组合查询
    - 金额范围查询
    - 摘要关键字搜索
    - 分页结果返回

    **检索条件:**
    - 全宗号 (fondsNo): 必填，数据隔离基础
    - 金额范围 (minAmount/maxAmount): 可选，支持金额区间查询
    - 摘要关键字 (summary): 可选，模糊匹配
    - 日期范围 (startDate/endDate): 可选，按会计期间筛选
    - 凭证号 (voucherNo): 可选，精确匹配

    **使用场景:**
    - 财务审计数据查询
    - 跨年度数据追溯
    - 大额凭证检索
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/archive/search")
@RequiredArgsConstructor
public class AdvancedArchiveSearchController {

    private final AdvancedArchiveSearchService advancedSearchService;

    @PostMapping("/advanced")
    @Operation(
        summary = "高级多条件检索",
        description = """
            执行高级多条件组合检索。

            **请求体:**
            - fondsNo: 全宗号（必填）
            - startDate: 开始日期（可选，格式：YYYY-MM-DD）
            - endDate: 结束日期（可选，格式：YYYY-MM-DD）
            - minAmount: 最小金额（可选）
            - maxAmount: 最大金额（可选）
            - summary: 摘要关键字（可选，模糊匹配）
            - voucherNo: 凭证号（可选，精确匹配）

            **返回数据包括:**
            - records: 检索结果列表
            - total: 总记录数
            - page: 当前页码
            - pageSize: 每页大小

            **使用场景:**
            - 审计人员组合条件检索
            - 跨期间数据查询
            """,
        operationId = "advancedSearch",
        tags = {"高级档案检索"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检索成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:search')")
    public Result<Page<ArchiveSearchResult>> advancedSearch(
            @Parameter(description = "高级检索请求", required = true)
            @Valid @RequestBody AdvancedSearchRequest request) {
        try {
            Page<ArchiveSearchResult> results = advancedSearchService.advancedSearch(request);
            return Result.success(results);
        } catch (Exception e) {
            log.error("高级检索失败", e);
            return Result.fail("检索失败: " + e.getMessage());
        }
    }

    @GetMapping("/amount-range")
    @Operation(
        summary = "按金额范围查询",
        description = """
            根据金额范围查询档案记录。

            **查询参数:**
            - fondsNo: 全宗号（必填）
            - minAmount: 最小金额（可选）
            - maxAmount: 最大金额（可选）
            - page: 页码（从 1 开始，默认 1）
            - pageSize: 每页大小（默认 20）

            **业务规则:**
            - 仅设置 minAmount: 查询大于等于该金额的记录
            - 仅设置 maxAmount: 查询小于等于该金额的记录
            - 同时设置: 查询金额区间内的记录

            **使用场景:**
            - 大额凭证检索
            - 异常金额排查
            """,
        operationId = "searchByAmountRange",
        tags = {"高级档案检索"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:search')")
    public Result<Page<ArchiveSearchResult>> searchByAmountRange(
            @Parameter(description = "全宗号", required = true, example = "F001")
            @RequestParam String fondsNo,
            @Parameter(description = "最小金额", example = "1000.00")
            @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "最大金额", example = "100000.00")
            @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize) {

        try {
            Page<ArchiveSearchResult> results = advancedSearchService.searchByAmountRange(
                fondsNo, minAmount, maxAmount, page, pageSize);
            return Result.success(results);
        } catch (Exception e) {
            log.error("金额范围查询失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/summary")
    @Operation(
        summary = "按摘要关键字搜索",
        description = """
            根据摘要关键字模糊查询档案记录。

            **查询参数:**
            - fondsNo: 全宗号（必填）
            - summary: 摘要关键字（必填，模糊匹配）
            - page: 页码（从 1 开始，默认 1）
            - pageSize: 每页大小（默认 20）

            **匹配规则:**
            - 使用 SQL LIKE 进行模糊匹配
            - 关键字可在摘要任意位置

            **使用场景:**
            - 按业务类型检索（如"报销"、"采购"）
            - 按对方单位检索
            """,
        operationId = "searchBySummary",
        tags = {"高级档案检索"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "摘要关键字不能为空"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:search')")
    public Result<Page<ArchiveSearchResult>> searchBySummary(
            @Parameter(description = "全宗号", required = true, example = "F001")
            @RequestParam String fondsNo,
            @Parameter(description = "摘要关键字", required = true, example = "报销")
            @RequestParam String summary,
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") Integer pageSize) {

        try {
            Page<ArchiveSearchResult> results = advancedSearchService.searchBySummary(
                fondsNo, summary, page, pageSize);
            return Result.success(results);
        } catch (Exception e) {
            log.error("摘要搜索失败", e);
            return Result.fail("搜索失败: " + e.getMessage());
        }
    }
}
