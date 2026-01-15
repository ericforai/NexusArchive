// Input: Lombok
// Output: ModuleCatalog 类
// Pos: 服务层 - 模块清单

package com.nexusarchive.service.governance;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 模块清单
 * <p>
 * 包含前后端所有模块的完整目录
 * </p>
 */
@Data
@Builder
public class ModuleCatalog {
    /**
     * 版本号
     */
    private String version;

    /**
     * 生成时间
     */
    private Date generatedAt;

    /**
     * 后端模块列表
     */
    private List<BackendModule> backendModules;

    /**
     * 前端模块列表
     */
    private List<FrontendModule> frontendModules;
}
