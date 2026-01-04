// Input: 架构管理控制器
// Output: REST API 端点
// Pos: controller 包 - 架构防御

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.response.ArchitectureReportDto;
import com.nexusarchive.dto.response.ViolationReportDto;
import com.nexusarchive.service.ArchitectureIntrospectionService;
import io.swagger.v3.oas.annotations.Operation;
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
 * <p>
 * 提供 Architecture Defense 的运行时可见性 API。
 * 实现 J4 (Reflex) 原则：运行时架构自省。
 * </p>
 *
 * <h3>API 端点：</h3>
 * <ul>
 *   <li>GET /api/architecture/modules - 获取所有模块清单</li>
 *   <li>GET /api/architecture/validate - 验证模块依赖规则</li>
 *   <li>GET /api/architecture/tests - 运行所有架构测试</li>
 *   <li>GET /api/architecture/health - 获取架构健康状态</li>
 * </ul>
 */
@Tag(name = "架构管理", description = "Architecture Defense - 架构自省和验证")
@RestController
@RequestMapping("/api/architecture")
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
     * <p>
     * 返回所有带有 @ModuleManifest 注解的模块信息
     * </p>
     */
    @GetMapping("/modules")
    @Operation(summary = "获取模块清单", description = "返回所有模块的 @ModuleManifest 信息")
    public Result<ArchitectureReportDto> getModuleManifests() {
        ArchitectureReportDto report = introspectionService.getModuleManifests();
        return Result.success(report);
    }

    /**
     * 验证模块依赖规则
     * <p>
     * 检查实际依赖是否与模块清单中声明的一致
     * </p>
     */
    @GetMapping("/validate")
    @Operation(summary = "验证依赖规则", description = "验证实际依赖与模块清单声明是否一致")
    public Result<ViolationReportDto> validateDependencies() {
        ViolationReportDto report = introspectionService.validateModuleDependencies();
        return Result.success(report);
    }

    /**
     * 运行所有架构测试
     * <p>
     * 运行 ArchUnit 测试并返回结果
     * </p>
     */
    @GetMapping("/tests")
    @Operation(summary = "运行架构测试", description = "运行所有 ArchUnit 架构测试")
    public Result<Map<String, Object>> runArchitectureTests() {
        Map<String, Object> results = introspectionService.runArchitectureTests();
        return Result.success(results);
    }

    /**
     * 获取架构健康状态
     * <p>
     * 返回整体架构健康评分和建议
     * </p>
     */
    @GetMapping("/health")
    @Operation(summary = "架构健康状态", description = "返回架构健康评分和改进建议")
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
