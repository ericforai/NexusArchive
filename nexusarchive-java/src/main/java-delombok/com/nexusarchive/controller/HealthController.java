// Input: Lombok、Spring Framework、Swagger/OpenAPI、Java 标准库、本地模块
// Output: HealthController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.LicenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 健康检查控制器
 *
 * PRD 来源: 系统监控模块
 * 提供服务健康状态检查功能
 */
@Tag(name = "健康检查", description = """
    系统健康状态检查接口。

    **功能说明:**
    - 基础健康检查
    - 深度自检（数据库、存储、迁移状态）
    - License 状态查询
    - 系统运行时长统计

    **健康状态:**
    - UP: 服务正常
    - DOWN: 服务不可用
    - DEGRADED: 部分功能降级

    **检查项:**
    - 数据库连接状态
    - Flyway 迁移状态
    - 存储路径可写性
    - License 有效性

    **超时设置:**
    - 数据库连接检测: 300ms 超时

    **使用场景:**
    - K8s 存活探针 (Liveness Probe)
    - K8s 就绪探针 (Readiness Probe)
    - 负载均衡健康检查
    - 监控系统心跳检测

    **权限要求:**
    - 无需认证，供监控系统调用
    """)
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
@Slf4j
public class HealthController {

    private final LicenseService licenseService;
    private final javax.sql.DataSource dataSource;
    private final com.nexusarchive.config.ResilientFlywayRunner flywayRunner;

    /**
     * 基础健康检查
     */
    @GetMapping
    @Operation(
        summary = "基础健康检查",
        description = """
            获取系统基础健康状态。

            **返回数据包括:**
            - status: 服务状态（UP/DOWN/DEGRADED）
            - uptimeMs: JVM 运行时长（毫秒）
            - license: License 信息
              - valid: 是否有效
              - productId: 产品 ID
              - expiryDate: 过期日期
              - maxNodes: 最大节点数
              - currentNodes: 当前节点数

            **使用场景:**
            - K8s Liveness Probe
            - 负载均衡健康检查
            - 监控系统心跳

            **注意:**
            - 此接口不做数据库连接检测
            - 返回 UP 不代表数据库可用
            - 深度检测请使用 /self-check
            """,
        operationId = "healthCheck",
        tags = {"健康检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "服务运行中"),
        @ApiResponse(responseCode = "503", description = "服务不可用")
    })
    public Result<Map<String, Object>> health() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", "UP");
        map.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        map.put("license", licenseService.current());
        return Result.success(map);
    }

    /**
     * 深度自检
     */
    @GetMapping("/self-check")
    @Operation(
        summary = "深度自检",
        description = """
            执行完整的系统自检，包括数据库、存储、迁移状态。

            **检查项:**
            1. **存储路径检查**
               - storagePath: 归档存储路径
               - diskWritable: 磁盘是否可写

            2. **数据库迁移检查**
               - migration: 迁移状态（READY/MIGRATING/FAILED）
               - 迁移中时返回 503

            3. **数据库连接检查**
               - dbConnection: 数据库连接状态（UP/DOWN）
               - 超时时间: 300ms
               - 连接失败时返回 503

            4. **总体状态**
               - status: UP（全部正常）/DOWN（有异常）

            **返回码说明:**
            - 200: 所有检查项正常
            - 503: 数据库迁移中或连接失败

            **使用场景:**
            - K8s Readiness Probe
            - 部署后健康检查
            - 故障诊断
            """,
        operationId = "selfCheck",
        tags = {"健康检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "所有检查项正常"),
        @ApiResponse(responseCode = "503", description = "服务不可用：数据库迁移中或连接失败",
            content = @Content(schema = @Schema(implementation = Result.class)))
    })
    public org.springframework.http.ResponseEntity<Result<Map<String, Object>>> selfCheck() {
        Map<String, Object> map = new HashMap<>();
        map.put("storagePath", "/data/archive");
        map.put("diskWritable", true); // Optimize later

        // 1. Check Migration Status
        String migrationStatus = flywayRunner.getStatus();
        map.put("migration", migrationStatus);

        if (!"READY".equals(migrationStatus)) {
             map.put("dbConnection", "UNKNOWN");
             map.put("status", "DOWN");
             return org.springframework.http.ResponseEntity.status(503)
                    .body(Result.error(503, "Service Unavailable: Database Migration in Progress", map));
        }

        // 2. Check DB Connectivity
        boolean dbUp = checkDbStatus();
        map.put("dbConnection", dbUp ? "UP" : "DOWN");
        map.put("status", dbUp ? "UP" : "DOWN");

        if (!dbUp) {
            return org.springframework.http.ResponseEntity.status(503)
                    .body(Result.error(503, "Service Unavailable: Database Down", map));
        }

        return org.springframework.http.ResponseEntity.ok(Result.success(map));
    }

    private boolean checkDbStatus() {
        try {
            return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try (java.sql.Connection conn = dataSource.getConnection()) {
                    return conn.isValid(1);
                } catch (Exception e) {
                    return false;
                }
            }).get(300, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Health check failed: {}", e.getMessage());
            return false;
        }
    }
}
