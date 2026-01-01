// Input: Java 标准库, Spring Framework
// Output: ModuleGovernanceService 类
// Pos: 服务层 - 模块治理服务

package com.nexusarchive.service.governance;

import com.nexusarchive.service.governance.ModuleGovernanceService;
import com.nexusarchive.service.governance.ModuleInfo;
import com.nexusarchive.service.governance.ModuleDependency;
import com.nexusarchive.service.governance.ModuleMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
