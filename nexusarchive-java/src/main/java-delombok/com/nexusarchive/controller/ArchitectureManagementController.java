// Input: 架构管理控制器
// Output: REST API 端点
// Pos: controller 包 - 架构防御

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.response.ArchitectureReportDto;
import com.nexusarchive.dto.response.ViolationReportDto;
import com.nexusarchive.service.ArchitectureIntrospectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 架构管理控制器
 *
 * PRD 来源: 架构防御模块
 * 提供 Architecture Defense 的运行时可见性 API
 *
 * <p>实现 J4 (Reflex) 原则：运行时架构自省</p>
 */
@Tag(name = "架构管理", description = """
    Architecture Defense - 架构自省和验证接口。

    **功能说明:**
    - 获取所有模块清单
    - 验证模块依赖规则
    - 运行所有架构测试
    - 获取架构健康状态

    **模块清单:**
    - moduleName: 模块名称
    - layer: 所属层级
    - exportedTypes: 导出类型
    - dependencies: 声明依赖
    - isLegacy: 是否遗留代码

    **依赖验证:**
    - 检查实际依赖与声明是否一致
    - 识别违规依赖
    - 检测循环依赖

    **架构测试:**
    - totalTests: 总测试数
    - passed: 通过数
    - failed: 失败数
    - violations: 违规详情

    **健康评分:**
    - excellent (90-100): 架构优秀
    - good (75-89): 架构良好
    - warning (60-74): 需要关注
    - critical (<60): 急需重构

    **使用场景:**
    - 架构监控仪表盘
    - 重构规划参考
    - 技术债量化

    **权限要求:**
    - SYSTEM_ADMIN 角色
    - SECURITY_ADMIN 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/architecture")
public class ArchitectureManagementController {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureManagementController.class);

    private final ArchitectureIntrospectionService introspectionService;

    public ArchitectureManagementController(ArchitectureIntrospectionService introspectionService) {
        this.introspectionService = introspectionService;
    }

    @PostConstruct
    public void init() {
        log.info("[ArchitectureManagementController] Bean created successfully!");
    }

    /**
     * 获取所有模块清单
     */
    @GetMapping("/modules")
    @Operation(
        summary = "获取模块清单",
        description = """
            返回所有带有 @ModuleManifest 注解的模块信息。

            **返回数据包括:**
            - totalModules: 总模块数
            - legacyModuleCount: 遗留模块数
            - modules: 模块列表
              - moduleName: 模块名称
              - layer: 所属层级（app/domain/infra）
              - exportedTypes: 导出类型列表
              - dependencies: 依赖模块列表
              - isLegacy: 是否遗留代码

            **业务规则:**
            - 扫描 classpath 中的 @ModuleManifest
            - 运行时反射收集信息

            **使用场景:**
            - 模块清单展示
            - 依赖可视化
            """,
        operationId = "getModuleManifests",
        tags = {"架构管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<ArchitectureReportDto> getModuleManifests() {
        ArchitectureReportDto report = introspectionService.getModuleManifests();
        return Result.success(report);
    }

    /**
     * 验证模块依赖规则
     */
    @GetMapping("/validate")
    @Operation(
        summary = "验证依赖规则",
        description = """
            验证实际依赖与模块清单声明是否一致。

            **返回数据包括:**
            - totalViolations: 违规总数
            - violations: 违规详情列表
              - sourceModule: 源模块
              - targetModule: 目标模块
              - violationType: 违规类型
              - description: 违规描述

            **违规类型:**
            - UNDECLARED_DEPENDENCY: 未声明的依赖
            - LAYER_VIOLATION: 层级违规
            - CIRCULAR_DEPENDENCY: 循环依赖

            **使用场景:**
            - 依赖验证
            - 违规检测
            """,
        operationId = "validateModuleDependencies",
        tags = {"架构管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<ViolationReportDto> validateDependencies() {
        ViolationReportDto report = introspectionService.validateModuleDependencies();
        return Result.success(report);
    }

    /**
     * 运行所有架构测试
     */
    @GetMapping("/tests")
    @Operation(
        summary = "运行架构测试",
        description = """
            运行所有 ArchUnit 架构测试并返回结果。

            **返回数据包括:**
            - totalTests: 总测试数
            - passed: 通过数
            - failed: 失败数
            - duration: 执行耗时（毫秒）
            - results: 详细结果

            **测试覆盖:**
            - 层级依赖规则
            - 模块边界规则
            - 命名约定规则
            - 循环依赖检测

            **使用场景:**
            - CI/CD 集成
            - 架构回归测试
            """,
        operationId = "runArchitectureTests",
        tags = {"架构管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "测试完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<Map<String, Object>> runArchitectureTests() {
        Map<String, Object> results = introspectionService.runArchitectureTests();
        return Result.success(results);
    }

    /**
     * 获取架构健康状态
     */
    @GetMapping("/health")
    @Operation(
        summary = "获取架构健康状态",
        description = """
            返回整体架构健康评分和改进建议。

            **返回数据包括:**
            - score: 健康评分 (0-100)
            - status: 健康状态
            - totalModules: 总模块数
            - legacyModules: 遗留模块数
            - testResults: 测试结果
            - recommendations: 改进建议列表

            **健康等级:**
            - excellent: 90-100 分
            - good: 75-89 分
            - warning: 60-74 分
            - critical: <60 分

            **评分规则:**
            - 遗留模块扣 5 分/个
            - 测试失败扣 10 分/个

            **使用场景:**
            - 健康仪表盘
            - 管理层报告
            """,
        operationId = "getArchitectureHealth",
        tags = {"架构管理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<Map<String, Object>> getArchitectureHealth() {
        ArchitectureReportDto manifests = introspectionService.getModuleManifests();
        Map<String, Object> testResults = introspectionService.runArchitectureTests();

        Map<String, Object> health = new java.util.HashMap<>();

        // 健康评分计算
        int score = calculateHealthScore(manifests, testResults);
        health.put("score", score);
        health.put("status", getHealthStatus(score));
        health.put("totalModules", manifests.getTotalModules());
        health.put("legacyModules", manifests.getLegacyModuleCount());
        health.put("testResults", testResults);

        // 生成建议
        health.put("recommendations", generateRecommendations(manifests, testResults));

        return Result.success(health);
    }

    /**
     * 计算健康评分
     */
    private int calculateHealthScore(ArchitectureReportDto manifests, Map<String, Object> testResults) {
        int score = 100;

        // 遗留模块扣分
        score -= manifests.getLegacyModuleCount() * 5;

        // 测试失败扣分
        Object totalTestsObj = testResults.get("totalTests");
        Object passedObj = testResults.get("passed");
        int totalTests = totalTestsObj instanceof Number ? ((Number) totalTestsObj).intValue() : 0;
        int passed = passedObj instanceof Number ? ((Number) passedObj).intValue() : 0;
        int failedTests = totalTests - passed;
        score -= failedTests * 10;

        return Math.max(0, score);
    }

    /**
     * 获取健康状态
     */
    private String getHealthStatus(int score) {
        if (score >= 90) {
            return "excellent";
        } else if (score >= 75) {
            return "good";
        } else if (score >= 60) {
            return "warning";
        } else {
            return "critical";
        }
    }

    /**
     * 生成改进建议
     */
    private java.util.List<String> generateRecommendations(ArchitectureReportDto manifests, Map<String, Object> testResults) {
        java.util.List<String> recommendations = new java.util.ArrayList<>();

        // 遗留模块建议
        if (manifests.getLegacyModuleCount() > 0) {
            recommendations.add("发现 " + manifests.getLegacyModuleCount() + " 个遗留模块，建议制定重构计划");
        }

        // 测试失败建议
        Object totalTestsObj = testResults.get("totalTests");
        Object passedObj = testResults.get("passed");
        int totalTests = totalTestsObj instanceof Number ? ((Number) totalTestsObj).intValue() : 0;
        int passed = passedObj instanceof Number ? ((Number) passedObj).intValue() : 0;
        int failedTests = totalTests - passed;
        if (failedTests > 0) {
            recommendations.add("有 " + failedTests + " 个架构测试失败，需要修复违规代码");
        }

        // 通用建议
        if (recommendations.isEmpty()) {
            recommendations.add("架构健康状况良好，继续保持");
        }

        return recommendations;
    }
}
