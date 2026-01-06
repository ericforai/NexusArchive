// Input: NIO Files, ModuleInfo
// Output: ModuleAnalyzer
// Pos: Service Layer - Governance
// 负责分析模块健康状态

package com.nexusarchive.service.governance.analyzer;

import com.nexusarchive.service.governance.ModuleInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 模块分析器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>扫描模块目录</li>
 *   <li>统计文件数和代码行数</li>
 *   <li>评估模块健康状况</li>
 * </ul>
 */
@Component
@Slf4j
public class ModuleAnalyzer {

    private static final double HEALTH_THRESHOLD_WARNING = 500;
    private static final double HEALTH_THRESHOLD_ATTENTION = 300;
    private static final int FILE_COUNT_WARNING = 50;
    private static final int FILE_COUNT_ATTENTION = 30;

    /**
     * 分析单个模块
     *
     * @param moduleDir 模块目录（相对于 BASE_PATH）
     * @param basePath 基础路径
     * @param label 模块标签
     * @return 模块信息，如果目录不存在返回 null
     */
    public ModuleInfo analyze(String moduleDir, String basePath, String label) {
        Path path = Paths.get(basePath, moduleDir);
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
                        } catch (Exception e) {
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

        } catch (Exception e) {
            log.warn("Failed to analyze module: {}", moduleDir, e);
            return null;
        }
    }

    /**
     * 评估模块健康状况
     *
     * @param complexity 复杂度（平均行数/文件）
     * @param fileCount 文件数量
     * @return 健康状态：HEALTHY, ATTENTION, WARNING
     */
    public String assessHealth(double complexity, int fileCount) {
        if (complexity > HEALTH_THRESHOLD_WARNING || fileCount > FILE_COUNT_WARNING) {
            return "WARNING"; // 高复杂度或文件过多
        } else if (complexity > HEALTH_THRESHOLD_ATTENTION || fileCount > FILE_COUNT_ATTENTION) {
            return "ATTENTION"; // 需要关注
        } else {
            return "HEALTHY"; // 健康
        }
    }
}
