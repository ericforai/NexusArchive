// Input: Java 标准库, Spring Framework
// Output: ModuleGovernanceService 类
// Pos: 服务层 - 模块治理服务

package com.nexusarchive.service.governance;

import com.nexusarchive.service.governance.analyzer.ModuleAnalyzer;
// DTOs are in com.nexusarchive.service.governance.*;
import com.nexusarchive.service.governance.FrontendModuleInfo;
import com.nexusarchive.service.governance.discovery.ModuleDiscoveryService;
import com.nexusarchive.service.governance.export.ModuleCatalogExporter;
import com.nexusarchive.service.governance.validation.ModuleManifestValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 模块治理服务
 * <p>
 * 负责监控和管理代码模块的健康状况
 * </p>
 *
 * <p>具体实现已委托给：</p>
 * <ul>
 *   <li>{@link ModuleAnalyzer} - 模块分析</li>
 *   <li>{@link ModuleDiscoveryService} - 模块发现</li>
 *   <li>{@link ModuleCatalogExporter} - 目录导出</li>
 *   <li>{@link ModuleManifestValidator} - 清单验证</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModuleGovernanceService {

    private static final String BASE_PATH = "src/main/java/com/nexusarchive";

    // 提取的服务
    private final ModuleAnalyzer moduleAnalyzer;
    private final ModuleDiscoveryService discoveryService;
    private final ModuleCatalogExporter catalogExporter;
    private final ModuleManifestValidator manifestValidator;

    /**
     * 获取所有模块信息
     */
    public List<ModuleInfo> getAllModules() {
        List<ModuleInfo> modules = new java.util.ArrayList<>();

        // 定义模块目录映射
        Map<String, String> moduleDirs = Map.of(
            "controller", "控制器层",
            "service", "服务层",
            "mapper", "数据访问层",
            "entity", "实体层",
            "dto", "数据传输对象",
            "config", "配置",
            "security", "安全",
            "integration", "集成",
            "util", "工具类"
        );

        for (Map.Entry<String, String> entry : moduleDirs.entrySet()) {
            String dir = entry.getKey();
            String label = entry.getValue();
            ModuleInfo info = moduleAnalyzer.analyze(dir, BASE_PATH, label);
            if (info != null) {
                modules.add(info);
            }
        }

        return modules;
    }

    /**
     * 自动发现新模块
     */
    public List<ModuleDiscoveryResult> discoverNewModules() {
        return discoveryService.discoverBackendModules(BASE_PATH);
    }

    /**
     * 发现前端模块
     */
    public List<FrontendModuleInfo> discoverFrontendModules() {
        return discoveryService.discoverFrontendModules();
    }

    /**
     * 验证模块清单与代码的一致性
     */
    public ModuleValidationResult validateManifest() {
        return manifestValidator.validate(BASE_PATH);
    }

    /**
     * 导出模块清单为 JSON
     */
    public String exportModuleCatalog() throws java.io.IOException {
        return catalogExporter.exportCatalog();
    }

    /**
     * 获取模块依赖关系
     */
    public List<ModuleDependency> getDependencies() {
        return java.util.List.of(
            ModuleDependency.builder()
                .from("controller")
                .to("service")
                .type("uses")
                .strength("strong")
                .build(),
            ModuleDependency.builder()
                .from("service")
                .to("mapper")
                .type("uses")
                .strength("strong")
                .build(),
            ModuleDependency.builder()
                .from("service")
                .to("dto")
                .type("uses")
                .strength("moderate")
                .build(),
            ModuleDependency.builder()
                .from("controller")
                .to("dto")
                .type("uses")
                .strength("moderate")
                .build()
        );
    }

    /**
     * 获取模块度量指标
     */
    public ModuleMetrics getMetrics() {
        List<ModuleInfo> modules = getAllModules();

        int totalFiles = modules.stream().mapToInt(ModuleInfo::getFileCount).sum();
        int totalLines = modules.stream().mapToInt(ModuleInfo::getLinesOfCode).sum();
        double avgComplexity = modules.stream().mapToDouble(ModuleInfo::getComplexity).average().orElse(0);

        long warningCount = modules.stream().filter(m -> "WARNING".equals(m.getHealthStatus())).count();
        long attentionCount = modules.stream().filter(m -> "ATTENTION".equals(m.getHealthStatus())).count();
        long healthyCount = modules.stream().filter(m -> "HEALTHY".equals(m.getHealthStatus())).count();

        return ModuleMetrics.builder()
                .totalModules(modules.size())
                .totalFiles(totalFiles)
                .totalLines(totalLines)
                .averageComplexity(avgComplexity)
                .healthyModules((int) healthyCount)
                .warningModules((int) warningCount)
                .attentionModules((int) attentionCount)
                .build();
    }
}
