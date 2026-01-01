// Input: Lombok, Java 标准库
// Output: ModuleDependency 类
// Pos: 服务层 - 模块依赖

package com.nexusarchive.service.governance;

import lombok.Builder;
import lombok.Data;

/**
 * 模块依赖关系
 */
@Data
@Builder
public class ModuleDependency {
    /**
     * 源模块
     */
    private String from;

    /**
     * 目标模块
     */
    private String to;

    /**
     * 依赖类型
     */
    private String type;

    /**
     * 依赖强度
     */
    private String strength;
}
