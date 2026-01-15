// Input: Lombok, Java 标准库
// Output: FrontendModuleInfo 类
// Pos: 服务层 - 前端模块信息

package com.nexusarchive.service.governance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 前端模块信息
 * <p>
 * 由前端模块发现脚本扫描得到的前端模块详细信息
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrontendModuleInfo {

    /**
     * 模块 ID（如 FE.PAGES, FE.COMPONENTS）
     */
    private String moduleId;

    /**
     * 模块名称
     */
    private String name;

    /**
     * 模块路径
     */
    private String path;

    /**
     * 包含的文件数量
     */
    private Integer fileCount;

    /**
     * 子模块列表
     */
    private List<String> subModules;
}
