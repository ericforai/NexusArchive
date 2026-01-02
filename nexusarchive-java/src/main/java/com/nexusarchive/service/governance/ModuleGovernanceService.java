// Input: Java 标准库, Spring Framework
// Output: ModuleGovernanceService 类
// Pos: 服务层 - 模块治理服务

package com.nexusarchive.service.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模块治理服务
 * <p>
 * 负责监控和管理代码模块的健康状况
 * </p>
 */
@Slf4j
@Service
public class ModuleGovernanceService {

    private static final String BASE_PATH = "src/main/java/com/nexusarchive";
    private static final String DOCS_PATH = "docs/architecture";
    private static final String FRONTEND_DISCOVERY_SCRIPT = "node scripts/discover-frontend-modules.js --discover";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有模块信息
     */
    public List<ModuleInfo> getAllModules() {
        List<ModuleInfo> modules = new ArrayList<>();

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
            ModuleInfo info = analyzeModule(dir, label);
            if (info != null) {
                modules.add(info);
            }
        }

        return modules;
    }

    /**
     * 自动发现新模块
     * <p>
     * 扫描 BASE_PATH 下的所有一级目录，识别出未在预定义列表中的模块
     * </p>
     */
    public List<ModuleDiscoveryResult> discoverNewModules() {
        List<ModuleDiscoveryResult> results = new ArrayList<>();

        // 预定义的已知模块
        Set<String> knownModules = Set.of(
            "controller", "service", "mapper", "entity", "dto",
            "config", "security", "integration", "util", "common"
        );

        Path basePath = Paths.get(BASE_PATH);
        if (!Files.exists(basePath)) {
            log.warn("Base path does not exist: {}", BASE_PATH);
            return results;
        }

        try {
            // 扫描一级目录
            List<Path> directories = Files.list(basePath)
                .filter(Files::isDirectory)
                .toList();

            for (Path dir : directories) {
                String moduleName = dir.getFileName().toString();
                if (!knownModules.contains(moduleName)) {
                    // 发现新模块
                    ModuleInfo info = analyzeModule(moduleName, "新发现模块");
                    if (info != null) {
                        results.add(ModuleDiscoveryResult.builder()
                            .moduleName(moduleName)
                            .modulePath(info.getPath())
                            .fileCount(info.getFileCount())
                            .isNewModule(true)
                            .recommendation(getRecommendation(moduleName))
                            .build());
                    }
                }
            }

            // 检查 modules 子目录下的模块
            Path modulesPath = Paths.get(BASE_PATH, "modules");
            if (Files.exists(modulesPath)) {
                List<Path> subModules = Files.list(modulesPath)
                    .filter(Files::isDirectory)
                    .toList();

                for (Path subModule : subModules) {
                    String subModuleName = "modules." + subModule.getFileName().toString();
                    ModuleInfo info = analyzeModule("modules/" + subModule.getFileName().toString(), "子模块");
                    if (info != null) {
                        results.add(ModuleDiscoveryResult.builder()
                            .moduleName(subModuleName)
                            .modulePath(info.getPath())
                            .fileCount(info.getFileCount())
                            .isNewModule(false) // 模块化子模块是预期的
                            .recommendation("已在模块化架构中，可补充到模块清单")
                            .build());
                    }
                }
            }

        } catch (IOException e) {
            log.error("Failed to discover new modules", e);
        }

        return results;
    }

    /**
     * 获取模块建议
     */
    private String getRecommendation(String moduleName) {
        if (moduleName.startsWith("test")) {
            return "测试模块，不需要添加到清单";
        }
        if (moduleName.equals("modules")) {
            return "模块化目录，请扫描子模块";
        }
        return "建议：1. 评估是否应独立为模块 2. 添加到 module-manifest.md 3. 补充架构测试规则";
    }

    /**
     * 发现前端模块
     * <p>
     * 通过调用 Node.js 脚本扫描前端代码，返回前端模块信息
     * </p>
     */
    public List<FrontendModuleInfo> discoverFrontendModules() {
        List<FrontendModuleInfo> modules = new ArrayList<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(FRONTEND_DISCOVERY_SCRIPT.split(" "));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Frontend module discovery script exited with code {}: {}", exitCode, output);
                return modules;
            }

            // 解析输出（简单的文本解析）
            modules = parseFrontendModuleOutput(output.toString());

        } catch (IOException | InterruptedException e) {
            log.error("Failed to discover frontend modules", e);
            Thread.currentThread().interrupt();
        }

        return modules;
    }

    /**
     * 解析前端模块发现脚本的输出
     */
    private List<FrontendModuleInfo> parseFrontendModuleOutput(String output) {
        List<FrontendModuleInfo> modules = new ArrayList<>();
        String[] lines = output.split("\n");

        String currentModuleId = null;
        String currentModuleName = null;
        String currentPath = null;
        int currentFileCount = 0;
        List<String> currentSubModules = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            if (line.contains(":") && !line.startsWith(" ")) {
                // 新模块开始
                if (currentModuleId != null) {
                    modules.add(FrontendModuleInfo.builder()
                        .moduleId(currentModuleId)
                        .name(currentModuleName)
                        .path(currentPath)
                        .fileCount(currentFileCount)
                        .subModules(new ArrayList<>(currentSubModules))
                        .build());
                }
                String[] parts = line.split(":");
                currentModuleId = parts[0].trim();
                currentModuleName = parts.length > 1 ? parts[1].trim() : currentModuleId;
                currentPath = null;
                currentFileCount = 0;
                currentSubModules.clear();
            } else if (line.contains("范围:") || line.contains("Scope:")) {
                currentPath = line.split(":")[1].trim();
            } else if (line.contains("文件:") || line.contains("Files:")) {
                try {
                    currentFileCount = Integer.parseInt(line.split(":")[1].trim());
                } catch (NumberFormatException e) {
                    currentFileCount = 0;
                }
            } else if (line.trim().startsWith("-")) {
                currentSubModules.add(line.trim().substring(1).trim());
            }
        }

        // 添加最后一个模块
        if (currentModuleId != null) {
            modules.add(FrontendModuleInfo.builder()
                .moduleId(currentModuleId)
                .name(currentModuleName)
                .path(currentPath)
                .fileCount(currentFileCount)
                .subModules(new ArrayList<>(currentSubModules))
                .build());
        }

        return modules;
    }

    /**
     * 验证模块清单与代码的一致性
     * <p>
     * 读取 module-manifest.md，验证其中列出的模块是否真实存在
     * </p>
     */
    public ModuleValidationResult validateManifest() {
        ModuleValidationResult result = new ModuleValidationResult();
        result.setValidationTime(System.currentTimeMillis());
        List<String> issues = new ArrayList<>();

        // 检查前端模块清单
        Path manifestPath = Paths.get(DOCS_PATH, "module-manifest.md");
        if (!Files.exists(manifestPath)) {
            issues.add("模块清单文件不存在: " + manifestPath);
        } else {
            try {
                String content = Files.readString(manifestPath);
                // 简单验证：检查是否包含 FE.SYS 和 BE.BORROWING
                if (!content.contains("FE.SYS")) {
                    issues.add("前端模块清单缺少 FE.SYS");
                }
                if (!content.contains("BE.BORROWING")) {
                    issues.add("后端模块清单缺少 BE.BORROWING");
                }
            } catch (IOException e) {
                issues.add("无法读取模块清单: " + e.getMessage());
            }
        }

        // 检查实际模块
        List<ModuleDiscoveryResult> discoveredModules = discoverNewModules();
        List<ModuleDiscoveryResult> actualNewModules = discoveredModules.stream()
            .filter(m -> Boolean.TRUE.equals(m.getIsNewModule()))
            .toList();

        if (!actualNewModules.isEmpty()) {
            issues.add("发现未在清单中记录的模块: " +
                actualNewModules.stream()
                    .map(ModuleDiscoveryResult::getModuleName)
                    .collect(Collectors.joining(", ")));
        }

        result.setValid(issues.isEmpty());
        result.setIssues(issues);
        result.setDiscoveredModules(discoveredModules);

        return result;
    }

    /**
     * 导出模块清单为 JSON
     */
    public String exportModuleCatalog() throws IOException {
        ModuleCatalog catalog = ModuleCatalog.builder()
            .version("2.1.0")
            .generatedAt(new Date())
            .backendModules(exportBackendModules())
            .frontendModules(exportFrontendModules())
            .build();

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(catalog);
    }

    /**
     * 导出后端模块
     */
    private List<BackendModule> exportBackendModules() {
        List<BackendModule> modules = new ArrayList<>();

        // Core layers
        modules.add(BackendModule.builder()
            .id("BE.CONTROLLER")
            .name("Controller Layer")
            .packageName("com.nexusarchive.controller")
            .description("REST API 端点")
            .status("ACTIVE")
            .build());

        modules.add(BackendModule.builder()
            .id("BE.SERVICE")
            .name("Service Layer")
            .packageName("com.nexusarchive.service")
            .description("业务逻辑实现")
            .status("ACTIVE")
            .build());

        modules.add(BackendModule.builder()
            .id("BE.MAPPER")
            .name("Mapper Layer")
            .packageName("com.nexusarchive.mapper")
            .description("数据访问层")
            .status("ACTIVE")
            .build());

        modules.add(BackendModule.builder()
            .id("BE.ENTITY")
            .name("Entity Layer")
            .packageName("com.nexusarchive.entity")
            .description("数据模型定义")
            .status("ACTIVE")
            .build());

        // Modularized components
        modules.add(BackendModule.builder()
            .id("BE.BORROWING")
            .name("Borrowing Module")
            .packageName("com.nexusarchive.modules.borrowing")
            .description("借阅全生命周期管理")
            .status("ACTIVE")
            .sinceVersion("2.0.0")
            .build());

        // Integration layer
        modules.add(BackendModule.builder()
            .id("BE.INTEGRATION")
            .name("Integration Layer")
            .packageName("com.nexusarchive.integration")
            .description("外部系统集成 (ERP)")
            .status("ACTIVE")
            .build());

        return modules;
    }

    /**
     * 导出前端模块
     */
    private List<FrontendModule> exportFrontendModules() {
        List<FrontendModule> modules = new ArrayList<>();

        modules.add(FrontendModule.builder()
            .id("FE.SYS")
            .name("Settings Module")
            .scope("src/features/settings + src/pages/settings + src/components/settings")
            .description("系统基础配置/字典/日志")
            .allowedDependencies("src/api, src/store, src/utils, src/hooks, src/types.ts")
            .status("LOCKED")
            .build());

        modules.add(FrontendModule.builder()
            .id("FE.ADMIN")
            .name("Admin Module")
            .scope("src/pages/admin + 相关组件")
            .description("用户/角色/全宗管理")
            .allowedDependencies("src/api, src/store, src/utils, src/types.ts")
            .status("IN_PROGRESS")
            .build());

        modules.add(FrontendModule.builder()
            .id("FE.SHARED")
            .name("Shared Infrastructure")
            .scope("src/api, src/store, src/utils, src/hooks")
            .description("跨模块通用能力与基础设施")
            .allowedDependencies("无跨模块依赖")
            .status("BASE")
            .build());

        return modules;
    }

    /**
     * 分析单个模块
     */
    private ModuleInfo analyzeModule(String moduleDir, String label) {
        Path path = Paths.get(BASE_PATH, moduleDir);
        if (!Files.exists(path)) {
            return null;
        }

        try {
            // 统计文件数
            long fileCount = Files.walk(path)
                    .filter(p -> p.toString().endsWith(".java"))
                    .count();

            // 统计代码行数
            long lines = Files.walk(path)
                    .filter(p -> p.toString().endsWith(".java"))
                    .mapToLong(p -> {
                        try {
                            return Files.lines(p).count();
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();

            // 计算复杂度（简化版：行数/文件数）
            double complexity = fileCount > 0 ? (double) lines / fileCount : 0;

            // 健康状态评估
            String health = assessHealth(complexity, (int) fileCount);

            return ModuleInfo.builder()
                    .name(moduleDir)
                    .label(label)
                    .path(path.toString())
                    .fileCount((int) fileCount)
                    .linesOfCode((int) lines)
                    .complexity(complexity)
                    .healthStatus(health)
                    .lastUpdated(System.currentTimeMillis())
                    .build();

        } catch (IOException e) {
            log.warn("Failed to analyze module: {}", moduleDir, e);
            return null;
        }
    }

    /**
     * 评估模块健康状况
     */
    private String assessHealth(double complexity, int fileCount) {
        if (complexity > 500 || fileCount > 50) {
            return "WARNING"; // 高复杂度或文件过多
        } else if (complexity > 300 || fileCount > 30) {
            return "ATTENTION"; // 需要关注
        } else {
            return "HEALTHY"; // 健康
        }
    }

    /**
     * 获取模块依赖关系
     */
    public List<ModuleDependency> getDependencies() {
        List<ModuleDependency> dependencies = new ArrayList<>();

        // 定义已知依赖关系
        dependencies.add(ModuleDependency.builder()
                .from("controller")
                .to("service")
                .type("uses")
                .strength("strong")
                .build());

        dependencies.add(ModuleDependency.builder()
                .from("service")
                .to("mapper")
                .type("uses")
                .strength("strong")
                .build());

        dependencies.add(ModuleDependency.builder()
                .from("service")
                .to("dto")
                .type("uses")
                .strength("moderate")
                .build());

        dependencies.add(ModuleDependency.builder()
                .from("controller")
                .to("dto")
                .type("uses")
                .strength("moderate")
                .build());

        return dependencies;
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
