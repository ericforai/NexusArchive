// Input: Spring Framework、Java 标准库
// Output: OpsController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维自检控制器
 *
 * PRD 来源: 运维监控模块
 * 提供系统运行状态自检接口
 */
@Tag(name = "运维自检", description = """
    系统运维自检接口。

    **功能说明:**
    - 快速检查应用运行状态
    - 负载均衡器健康检查
    - 容器编排就绪探针

    **返回值:**
    - OK: 应用正常运行

    **使用场景:**
    - K8s liveness/readiness probe
    - 负载均衡器健康检查
    - 监控告警心跳检测

    **注意事项:**
    - 无需认证
    - 极简响应，减少开销
    """)
@RestController
@RequestMapping("/ops")
public class OpsController {

    /**
     * 应用自检接口
     */
    @GetMapping("/self-check")
    @Operation(
        summary = "应用自检",
        description = """
            快速检查应用是否正常运行。

            **业务规则:**
            - 不执行任何复杂逻辑
            - 直接返回成功响应
            - 用于负载均衡器健康检查

            **返回值:**
            - OK: 应用正常运行

            **使用场景:**
            - Kubernetes liveness probe
            - 负载均衡器健康检查
            - 监控系统心跳检测
            """,
        operationId = "selfCheck",
        tags = {"运维自检"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "应用正常运行"),
        @ApiResponse(responseCode = "503", description = "服务不可用")
    })
    public ResponseEntity<String> selfCheck() {
        return ResponseEntity.ok("OK");
    }
}
