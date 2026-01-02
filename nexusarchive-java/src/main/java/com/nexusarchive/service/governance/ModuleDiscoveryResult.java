// Input: Lombok
// Output: ModuleDiscoveryResult 类
// Pos: 服务层 - 模块发现结果

package com.nexusarchive.service.governance;

import lombok.Builder;
import lombok.Data;

/**
 * 模块发现结果
 * <p>
 * 用于记录自动发现的新模块信息
 * </p>
 */
@Data
@Builder
public class ModuleDiscoveryResult {
    /**
     * 模块名称
     */
    private String moduleName;

    /**
     * 模块路径
     */
    private String modulePath;

    /**
     * 文件数量
     */
    private Integer fileCount;

    /**
     * 是否为新发现的模块
     */
    private Boolean isNewModule;

    /**
     * 建议
     */
    private String recommendation;
}
