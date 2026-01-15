// Input: Lombok
// Output: FrontendModule 类
// Pos: 服务层 - 前端模块定义

package com.nexusarchive.service.governance;

import lombok.Builder;
import lombok.Data;

/**
 * 前端模块定义
 */
@Data
@Builder
public class FrontendModule {
    /**
     * 模块 ID
     */
    private String id;

    /**
     * 模块名称
     */
    private String name;

    /**
     * 范围
     */
    private String scope;

    /**
     * 描述
     */
    private String description;

    /**
     * 允许的依赖
     */
    private String allowedDependencies;

    /**
     * 状态
     */
    private String status;
}
