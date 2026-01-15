// Input: Spring Web、PerformanceMetricsService
// Output: PerformanceMetricsController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.response.PerformanceMetricsReport;
import com.nexusarchive.service.PerformanceMetricsService;
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

import java.util.Map;

/**
 * 性能指标控制器
 *
 * PRD 来源: Section 7.5 - 非功能指标
 */
@Tag(name = "性能指标监控", description = """
    系统性能指标收集和报告接口。

    **功能说明:**
    - 收集系统性能指标
    - 生成性能报告
    - 提供实时性能快照

    **监控指标:**
    - 响应时间 (Response Time): API 平均响应时间
    - 吞吐量 (Throughput): 每秒处理请求数
    - 错误率 (Error Rate): 请求失败率
    - 并发用户 (Concurrent Users): 当前在线用户数
    - 数据库连接 (DB Connections): 数据库连接池使用情况
    - 缓存命中率 (Cache Hit Rate): Redis 缓存效率
    - 内存使用 (Memory Usage): JVM 堆内存使用情况
    - CPU 使用率 (CPU Usage): 系统 CPU 占用

    **报告类型:**
    - 实时快照: 当前时刻的性能指标
    - 历史报告: 指定时间段的性能趋势

    **使用场景:**
    - 系统运维监控
    - 性能瓶颈分析
    - 容量规划参考
    - SLA 合规检查

    **权限要求:**
    - system:monitor: 系统监控权限
    - system:admin: 系统管理员权限
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/system/metrics")
@RequiredArgsConstructor
public class PerformanceMetricsController {

    private final PerformanceMetricsService performanceMetricsService;

    @GetMapping("/report")
    @Operation(
        summary = "获取性能指标报告",
        description = """
            获取指定时间范围的性能指标报告。

            **查询参数:**
            - fondsNo: 全宗号（可选，不填则返回全局报告）

            **返回数据包括:**
            - reportTime: 报告生成时间
            - timeRange: 统计时间范围
            - metrics: 各项性能指标
            - summary: 性能摘要和评级

            **性能评级:**
            - EXCELLENT: 优秀（所有指标正常）
            - GOOD: 良好（部分指标接近阈值）
            - WARNING: 警告（有指标超过阈值）
            - CRITICAL: 严重（多个指标异常）

            **使用场景:**
            - 定期性能检查
            - 问题排查分析
            - 性能趋势跟踪
            """,
        operationId = "getPerformanceReport",
        tags = {"性能指标监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "报告生成成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PreAuthorize("hasAnyAuthority('system:monitor', 'system:admin')")
    public Result<PerformanceMetricsReport> getPerformanceReport(
            @Parameter(description = "全宗号（可选，不填则返回全局报告）", example = "F001")
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
    @Operation(
        summary = "获取当前性能指标快照",
        description = """
            获取当前时刻的系统性能指标快照。

            **返回数据包括:**
            - timestamp: 快照时间戳
            - responseTime: 当前平均响应时间 (ms)
            - throughput: 当前吞吐量 (req/s)
            - errorRate: 当前错误率 (%)
            - concurrentUsers: 当前在线用户数
            - dbConnections: 数据库连接数
            - cacheHitRate: 缓存命中率 (%)
            - memoryUsed: 已用内存 (MB)
            - memoryTotal: 总内存 (MB)
            - cpuUsage: CPU 使用率 (%)

            **使用场景:**
            - 实时监控面板
            - 健康检查接口
            - 告警触发判定
            """,
        operationId = "getCurrentMetrics",
        tags = {"性能指标监控"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
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
