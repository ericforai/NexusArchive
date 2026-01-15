// Input: Lombok
// Output: BackendModule 类
// Pos: 服务层 - 后端模块定义

package com.nexusarchive.service.governance;

import lombok.Builder;
import lombok.Data;

/**
 * 后端模块定义
 */
@Data
@Builder
public class BackendModule {
    /**
     * 模块 ID
     */
    private String id;

    /**
     * 模块名称
     */
    private String name;

    /**
     * 包名
     */
    private String packageName;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态
     */
    private String status;

    /**
     * 引入版本
     */
    private String sinceVersion;
}
