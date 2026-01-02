// Input: Spring Framework
// Output: ModuleGovernanceController 类
// Pos: 控制层 - 模块治理端点

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.governance.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模块治理控制器
 * <p>
 * 提供模块发现、验证和导出功能
 * </p>
 */
@Tag(name = "模块治理", description = "模块发现、验证和导出")
@RestController
@RequestMapping("/api/governance")
@RequiredArgsConstructor
public class ModuleGovernanceController {

    private final ModuleGovernanceService moduleGovernanceService;

    /**
     * 获取所有模块信息
     */
    @Operation(summary = "获取所有模块信息")
    @GetMapping("/modules")
    public Result<List<ModuleInfo>> getAllModules() {
        return Result.success(moduleGovernanceService.getAllModules());
    }

    /**
     * 发现新模块
     */
    @Operation(summary = "自动发现新模块", description = "扫描代码库，识别未在预定义列表中的模块")
    @GetMapping("/modules/discover")
    public Result<List<ModuleDiscoveryResult>> discoverNewModules() {
        return Result.success(moduleGovernanceService.discoverNewModules());
    }

    /**
     * 验证模块清单
     */
    @Operation(summary = "验证模块清单", description = "检查 module-manifest.md 与实际代码的一致性")
    @GetMapping("/modules/validate")
    public Result<ModuleValidationResult> validateManifest() {
        return Result.success(moduleGovernanceService.validateManifest());
    }

    /**
     * 导出模块清单
     */
    @Operation(summary = "导出模块清单", description = "导出 JSON 格式的模块目录")
    @GetMapping("/modules/export")
    public Result<String> exportModuleCatalog() {
        try {
            return Result.success(moduleGovernanceService.exportModuleCatalog());
        } catch (Exception e) {
            return Result.error("导出失败: " + e.getMessage());
        }
    }

    /**
     * 获取模块依赖关系
     */
    @Operation(summary = "获取模块依赖关系")
    @GetMapping("/modules/dependencies")
    public Result<List<ModuleDependency>> getDependencies() {
        return Result.success(moduleGovernanceService.getDependencies());
    }

    /**
     * 获取模块度量指标
     */
    @Operation(summary = "获取模块度量指标")
    @GetMapping("/modules/metrics")
    public Result<ModuleMetrics> getMetrics() {
        return Result.success(moduleGovernanceService.getMetrics());
    }
}
