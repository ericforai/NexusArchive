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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统监控控制器
 *
 * PRD 来源: 运维监控模块
 * 提供系统运行状态监控接口
 *
 * <p>包括 ERP 集成状态、系统健康检查等功能</p>
 */
@Tag(name = "系统监控", description = """
    系统运行状态监控接口。

    **功能说明:**
    - 获取 ERP 集成监控指标
    - 监控适配器连接状态
    - 统计同步任务数据
    - 计算错误率和响应时间

    **监控指标包括:**
    - YonSuite 适配器状态
    - 最后同步时间
    - 同步任务统计
    - 错误率统计
    - 平均响应时间

    **适配器类型:**
    - YonSuite: 用友 YonSuite ERP
    - SAP: SAP ERP
    - 自定义: 其他第三方系统

    **使用场景:**
    - 集成状态监控
    - 运维仪表盘
    - 告警触发依据

    **权限要求:**
    - SYSTEM_ADMIN 角色
    - AUDIT_ADMIN 角色
    - AUDITOR 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringService monitoringService;

    /**
     * 获取 ERP 集成监控指标
     */
    @GetMapping("/integration")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDIT_ADMIN', 'AUDITOR')")
    @Operation(
        summary = "获取 ERP 集成监控指标",
        description = """
            返回各 ERP 适配器的运行状态和统计数据。

            **返回数据包括:**
            - adapterType: 适配器类型 (YonSuite/SAP/自定义)
            - connectionStatus: 连接状态 (CONNECTED/DISCONNECTED/ERROR)
            - lastSyncTime: 最后同步时间
            - syncTaskCount: 同步任务总数
            - successCount: 成功任务数
            - failedCount: 失败任务数
            - errorRate: 错误率（百分比）
            - avgResponseTime: 平均响应时间（毫秒）
            - lastError: 最后一次错误信息

            **连接状态:**
            - CONNECTED: 正常连接
            - DISCONNECTED: 未连接
            - ERROR: 连接错误

            **业务规则:**
            - 实时统计最近 24 小时数据
            - 按适配器类型分组
            - 错误率 = 失败数 / 总数 × 100%

            **使用场景:**
            - 集成健康检查
            - 运维监控仪表盘
            - 告警规则配置
            """,
        operationId = "getIntegrationMetrics",
        tags = {"系统监控"}
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "获取成功",
            content = @Content(
                mediaType = "application/json",
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
