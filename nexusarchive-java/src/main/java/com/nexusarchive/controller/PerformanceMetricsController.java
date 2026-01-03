// Input: Spring Web、PerformanceMetricsService
// Output: PerformanceMetricsController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.response.PerformanceMetricsReport;
import com.nexusarchive.service.PerformanceMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 性能指标控制器
 * 
 * PRD 来源: Section 7.5 - 非功能指标
 */
@Slf4j
@RestController
@RequestMapping("/api/system/metrics")
@RequiredArgsConstructor
@Tag(name = "性能指标监控", description = "系统性能指标收集和报告")
public class PerformanceMetricsController {
    
    private final PerformanceMetricsService performanceMetricsService;
    
    @GetMapping("/report")
    @Operation(summary = "获取性能指标报告")
    @PreAuthorize("hasAnyAuthority('system:monitor', 'system:admin')")
    public Result<PerformanceMetricsReport> getPerformanceReport(
            @RequestParam(required = false) String fondsNo) {
        try {
            PerformanceMetricsReport report = performanceMetricsService.getPerformanceReport(fondsNo);
            return Result.success(report);
        } catch (Exception e) {
            log.error("获取性能报告失败", e);
            return Result.fail("获取报告失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/current")
    @Operation(summary = "获取当前性能指标快照")
    @PreAuthorize("hasAnyAuthority('system:monitor', 'system:admin')")
    public Result<Map<String, Object>> getCurrentMetrics() {
        try {
            Map<String, Object> metrics = performanceMetricsService.getCurrentMetrics();
            return Result.success(metrics);
        } catch (Exception e) {
            log.error("获取当前指标失败", e);
            return Result.fail("获取指标失败: " + e.getMessage());
        }
    }
}


