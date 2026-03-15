// Input: NIO Files, ModuleAnalyzer
// Output: ModuleDiscoveryService
// Pos: Service Layer - Governance
// 负责发现新模块

package com.nexusarchive.service.governance.discovery;

import com.nexusarchive.service.governance.ModuleDiscoveryResult;
import com.nexusarchive.service.governance.ModuleInfo;
import com.nexusarchive.service.governance.analyzer.ModuleAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import com.nexusarchive.service.governance.FrontendModuleInfo;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模块发现服务
 *
 * <p>职责：</p>
 * <ul>
 *   <li>扫描代码库发现新模块</li>
 *   <li>执行前端模块发现脚本</li>
 *   <li>提供模块建议</li>
 * </ul>
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ModuleDiscoveryService {

    private static final String FRONTEND_DISCOVERY_SCRIPT = "node scripts/discover-frontend-modules.js --discover";
    private static final Set<String> KNOWN_BACKEND_MODULES = Set.of(
        "controller", "service", "mapper", "entity", "dto",
        "config", "security", "integration", "util", "common"
    );

    private final ModuleAnalyzer moduleAnalyzer;

    /**
     * 发现后端新模块
     *
     * @param basePath 基础路径
     * @return 发现的新模块列表
     */
    public List<ModuleDiscoveryResult> discoverBackendModules(String basePath) {
        List<ModuleDiscoveryResult> results = new ArrayList<>();

        Path modulesPath = Paths.get(basePath);
        if (!Files.exists(modulesPath)) {
            log.warn("Base path does not exist: {}", basePath);
            return results;
        }

        try {
            // 扫描一级目录
            List<Path> directories = Files.list(modulesPath)
                .filter(Files::isDirectory)
                .toList();

            for (Path dir : directories) {
                String moduleName = dir.getFileName().toString();
                if (!KNOWN_BACKEND_MODULES.contains(moduleName)) {
                    // 发现新模块
                    ModuleInfo info = moduleAnalyzer.analyze(moduleName, basePath, "新发现模块");
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
            Path subModulesPath = Paths.get(basePath, "modules");
            if (Files.exists(subModulesPath)) {
                List<Path> subModules = Files.list(subModulesPath)
                    .filter(Files::isDirectory)
                    .toList();

                for (Path subModule : subModules) {
                    String subModuleName = "modules." + subModule.getFileName().toString();
                    ModuleInfo info = moduleAnalyzer.analyze("modules/" + subModule.getFileName().toString(), basePath, "子模块");
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
     * 发现前端模块
     *
     * @return 前端模块信息列表
     */
    public List<FrontendModuleInfo> discoverFrontendModules() {
        List<FrontendModuleInfo> modules = new ArrayList<>();

        try {
            // 使用 bash -c 包装脚本命令，避免空格分割导致的命令注入风险
            ProcessBuilder pb = new ProcessBuilder(
                "/bin/bash",
                "-c",
                FRONTEND_DISCOVERY_SCRIPT
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
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

            // 解析输出
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
     * 获取模块建议
     *
     * @param moduleName 模块名称
     * @return 建议文本
     */
    public String getRecommendation(String moduleName) {
        if (moduleName.startsWith("test")) {
            return "测试模块，不需要添加到清单";
        }
        if (moduleName.equals("modules")) {
            return "模块化目录，请扫描子模块";
        }
        return "建议：1. 评估是否应独立为模块 2. 添加到 module-manifest.md 3. 补充架构测试规则";
    }
}
