// Input: Lombok, Java 标准库
// Output: ModuleMetrics 类
// Pos: 服务层 - 模块度量

package com.nexusarchive.service.governance;

import lombok.Builder;
import lombok.Data;

/**
 * 模块度量指标
 */
@Data
@Builder
public class ModuleMetrics {
    /**
     * 模块总数
     */
    private int totalModules;

    /**
     * 总文件数
     */
    private int totalFiles;

    /**
     * 总代码行数
     */
    private int totalLines;

    /**
     * 平均复杂度
     */
    private double averageComplexity;

    /**
     * 健康模块数
     */
    private int healthyModules;

    /**
     * 警告模块数
     */
    private int warningModules;

    /**
     * 关注模块数
     */
    private int attentionModules;

    /**
     * 健康率
     */
    public double getHealthRate() {
        return totalModules > 0 ? (double) healthyModules / totalModules * 100 : 0;
    }
}
