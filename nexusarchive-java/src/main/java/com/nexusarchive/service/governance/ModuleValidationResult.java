// Input: Lombok
// Output: ModuleValidationResult 类
// Pos: 服务层 - 模块验证结果

package com.nexusarchive.service.governance;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 模块验证结果
 * <p>
 * 用于记录模块清单与实际代码的一致性验证结果
 * </p>
 */
@Data
public class ModuleValidationResult {
    /**
     * 验证时间
     */
    private Long validationTime;

    /**
     * 是否有效
     */
    private Boolean valid;

    /**
     * 发现的问题列表
     */
    private List<String> issues = new ArrayList<>();

    /**
     * 发现的模块列表
     */
    private List<ModuleDiscoveryResult> discoveredModules;
}
