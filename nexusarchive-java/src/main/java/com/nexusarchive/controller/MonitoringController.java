// Input: Lombok、Spring Framework、Java 标准库、本地模块、Swagger OpenAPI
// Output: MonitoringController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.monitoring.IntegrationMonitoringDTO;
import com.nexusarchive.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nexusarchive.common.constants.HttpConstants;

/**
 * 系统监控控制器
 * <p>
 * 提供系统运行状态监控接口，包括 ERP 集成状态、系统健康检查等。
 * 需要系统管理员或审计员权限才能访问。
 * </p>
 */
@Tag(name = "系统监控", description = "系统运行状态监控接口，包括 ERP 集成状态、健康检查等")
@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    /**
     * 获取 ERP 集成监控指标
     * <p>
     * 返回各 ERP 适配器的运行状态，包括：
     * - YonSuite 适配器状态
     * - 最后同步时间
     * - 同步任务统计
     * - 错误率统计
     * </p>
     *
     * @return ERP 集成监控指标
     */
    @GetMapping("/integration")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDIT_ADMIN', 'AUDITOR')")
    @Operation(
            summary = "获取 ERP 集成监控指标",
            description = "返回各 ERP 适配器的运行状态和统计数据，包括连接状态、最后同步时间、同步任务统计、错误率统计、平均响应时间等。"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "获取成功",
                    content = @Content(
                            mediaType = HttpConstants.APPLICATION_JSON,
                            schema = @Schema(implementation = IntegrationMonitoringDTO.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "未授权 - 未登录或 token 过期"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "无权限 - 需要系统管理员或审计员权限"
            )
    })
    public Result<IntegrationMonitoringDTO> getIntegrationMetrics() {
        return Result.success(monitoringService.getIntegrationMetrics());
    }
}
