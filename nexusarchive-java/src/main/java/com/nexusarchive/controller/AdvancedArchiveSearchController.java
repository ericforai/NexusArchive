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
@Slf4j
@RestController
@RequestMapping("/archive/search")
@RequiredArgsConstructor
@Tag(name = "高级档案检索", description = "支持金额范围、摘要搜索等多条件组合查询")
public class AdvancedArchiveSearchController {
    
    private final AdvancedArchiveSearchService advancedSearchService;
    
    @PostMapping("/advanced")
    @Operation(summary = "高级检索")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:search')")
    public Result<Page<ArchiveSearchResult>> advancedSearch(@Valid @RequestBody AdvancedSearchRequest request) {
        try {
            Page<ArchiveSearchResult> results = advancedSearchService.advancedSearch(request);
            return Result.success(results);
        } catch (Exception e) {
            log.error("高级检索失败", e);
            return Result.fail("检索失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/amount-range")
    @Operation(summary = "按金额范围查询")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:search')")
    public Result<Page<ArchiveSearchResult>> searchByAmountRange(
            @RequestParam String fondsNo,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(defaultValue = "1") Integer page,
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
    @Operation(summary = "按摘要搜索")
    @PreAuthorize("hasAnyAuthority('archive:view', 'archive:search')")
    public Result<Page<ArchiveSearchResult>> searchBySummary(
            @RequestParam String fondsNo,
            @RequestParam String summary,
            @RequestParam(defaultValue = "1") Integer page,
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





