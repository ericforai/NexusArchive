// Input: Lombok, Java 标准库
// Output: ModuleInfo 类
// Pos: 服务层 - 模块信息

package com.nexusarchive.service.governance;

import lombok.Builder;
import lombok.Data;

/**
 * 模块信息
 */
@Data
@Builder
public class ModuleInfo {
    /**
     * 模块名称
     */
    private String name;

    /**
     * 模块标签
     */
    private String label;

    /**
     * 模块路径
     */
    private String path;

    /**
     * 文件数量
     */
    private int fileCount;

    /**
     * 代码行数
     */
    private int linesOfCode;

    /**
     * 复杂度
     */
    private double complexity;

    /**
     * 健康状态
     */
    private String healthStatus;

    /**
     * 最后更新时间
     */
    private long lastUpdated;
}
