// Input: Spring Framework
// Output: ModuleGovernanceController 类
// Pos: 控制层 - 模块治理端点

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.service.governance.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模块治理控制器
 *
 * PRD 来源: 架构防御模块
 * 提供模块发现、验证和导出功能
 *
 * <p>实现 Architecture Defense 的模块治理能力</p>
 */
@Tag(name = "模块治理", description = """
    模块发现、验证和导出接口。

    **功能说明:**
    - 获取所有模块信息
    - 自动发现新模块
    - 验证模块清单一致性
    - 导出模块目录
    - 分析模块依赖关系
    - 计算模块度量指标

    **模块信息包括:**
    - moduleName: 模块名称
    - moduleType: 模块类型 (backend/frontend)
    - layer: 所属层级
    - path: 源码路径
    - dependencies: 依赖列表

    **发现类型:**
    - 后端模块: 扫描 Java package 结构
    - 前端模块: 通过 Node.js 脚本扫描

    **验证内容:**
    - module-manifest.md 与实际代码一致性
    - 依赖关系完整性
    - 导出类型准确性

    **度量指标:**
    - 模块数量统计
    - 依赖复杂度
    - 层级分布

    **使用场景:**
    - 模块清单维护
    - 架构可视化
    - 重构规划参考
    - 技术债量化

    **权限要求:**
    - SYSTEM_ADMIN 角色
    - SECURITY_ADMIN 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/governance")
@RequiredArgsConstructor
public class ModuleGovernanceController {

    private final ModuleGovernanceService moduleGovernanceService;

    /**
     * 获取所有模块信息
     */
    @GetMapping("/modules")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SECURITY_ADMIN')")
    @Operation(
        summary = "获取所有模块信息",
        description = """
            返回系统中所有已注册的模块信息。

            **返回数据包括:**
            - moduleName: 模块名称
            - moduleType: 模块类型 (backend/frontend)
            - layer: 所属层级 (app/domain/infra)
            - path: 源码路径
            - dependencies: 依赖模块列表
            - exportedTypes: 导出类型列表

            **业务规则:**
            - 从预定义模块清单中读取
            - 包含后端 Java 模块
            - 包含前端 TypeScript 模块

            **使用场景:**
            - 模块目录展示
            - 依赖关系可视化
            """,
        operationId = "getAllModules",
        tags = {"模块治理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<ModuleInfo>> getAllModules() {
        return Result.success(moduleGovernanceService.getAllModules());
    }

    /**
     * 发现新模块
     */
    @GetMapping("/modules/discover")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SECURITY_ADMIN')")
    @Operation(
        summary = "自动发现新模块",
        description = """
            扫描代码库，识别未在预定义列表中的模块。

            **返回数据包括:**
            - moduleName: 新发现的模块名
            - path: 模块路径
            - confidence: 置信度
            - suggestion: 建议

            **发现规则:**
            - 扫描后端 Java package 结构
            - 识别符合 DDD 分层的目录
            - 过滤测试和构建目录

            **使用场景:**
            - 模块清单更新
            - 代码审查辅助
            - 架构漂移检测
            """,
        operationId = "discoverNewModules",
        tags = {"模块治理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "发现完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<ModuleDiscoveryResult>> discoverNewModules() {
        return Result.success(moduleGovernanceService.discoverNewModules());
    }

    /**
     * 发现前端模块
     */
    @GetMapping("/modules/frontend")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SECURITY_ADMIN')")
    @Operation(
        summary = "发现前端模块",
        description = """
            通过 Node.js 脚本扫描前端代码，返回前端模块信息。

            **返回数据包括:**
            - moduleName: 模块名称
            - path: 模块路径
            - exports: 导出内容
            - dependencies: 依赖模块

            **扫描范围:**
            - src/features/* 功能模块
            - src/components/* 公共组件
            - src/pages/* 页面组件

            **使用场景:**
            - 前端模块清单生成
            - 前后端模块对比
            """,
        operationId = "discoverFrontendModules",
        tags = {"模块治理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "扫描完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "Node.js 脚本执行失败")
    })
    public Result<List<FrontendModuleInfo>> discoverFrontendModules() {
        return Result.success(moduleGovernanceService.discoverFrontendModules());
    }

    /**
     * 验证模块清单
     */
    @GetMapping("/modules/validate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SECURITY_ADMIN')")
    @Operation(
        summary = "验证模块清单",
        description = """
            检查 module-manifest.md 与实际代码的一致性。

            **验证内容:**
            - 模块是否存在
            - 导出类型是否准确
            - 依赖关系是否完整
            - 层级声明是否正确

            **返回数据包括:**
            - valid: 是否通过验证
            - errorCount: 错误数量
            - warningCount: 警告数量
            - errors: 错误详情列表
            - warnings: 警告详情列表

            **使用场景:**
            - CI/CD 架构检查
            - 模块清单维护
            """,
        operationId = "validateManifest",
        tags = {"模块治理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<ModuleValidationResult> validateManifest() {
        return Result.success(moduleGovernanceService.validateManifest());
    }

    /**
     * 导出模块清单
     */
    @GetMapping("/modules/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SECURITY_ADMIN')")
    @Operation(
        summary = "导出模块清单",
        description = """
            导出 JSON 格式的模块目录。

            **返回数据包括:**
            - JSON 格式的模块目录
            - 包含所有模块的完整信息
            - 可用于模块清单更新

            **使用场景:**
            - 模块清单备份
            - 架构文档生成
            - 模块目录分享
            """,
        operationId = "exportModuleCatalog",
        tags = {"模块治理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "导出成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "导出失败")
    })
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
    @GetMapping("/modules/dependencies")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SECURITY_ADMIN')")
    @Operation(
        summary = "获取模块依赖关系",
        description = """
            返回模块之间的依赖关系图。

            **返回数据包括:**
            - source: 源模块
            - target: 目标模块
            - dependencyType: 依赖类型
            - strength: 依赖强度

            **依赖类型:**
            - DIRECT: 直接依赖
            - INDIRECT: 间接依赖
            - CIRCULAR: 循环依赖

            **使用场景:**
            - 依赖关系可视化
            - 循环依赖检测
            - 重构规划参考
            """,
        operationId = "getModuleDependencies",
        tags = {"模块治理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<ModuleDependency>> getDependencies() {
        return Result.success(moduleGovernanceService.getDependencies());
    }

    /**
     * 获取模块度量指标
     */
    @GetMapping("/modules/metrics")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'SECURITY_ADMIN')")
    @Operation(
        summary = "获取模块度量指标",
        description = """
            返回模块的度量指标和统计数据。

            **返回数据包括:**
            - totalModules: 总模块数
            - backendModules: 后端模块数
            - frontendModules: 前端模块数
            - avgDependencies: 平均依赖数
            - maxDepth: 最大依赖深度
            - circularDeps: 循环依赖数量

            **使用场景:**
            - 架构健康评估
            - 技术债量化
            - 重构优先级排序
            """,
        operationId = "getModuleMetrics",
        tags = {"模块治理"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<ModuleMetrics> getMetrics() {
        return Result.success(moduleGovernanceService.getMetrics());
    }
}
