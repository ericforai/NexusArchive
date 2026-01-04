// Input: 模块信息 DTO
// Output: 数据传输对象
// Pos: dto.response - 架构防御

package com.nexusarchive.dto.response;

import lombok.Data;

import java.util.List;

/**
 * 模块信息 DTO
 * <p>
 * 包含单个模块的清单信息
 * </p>
 */
@Data
public class ModuleInfoDto {

    /**
     * 包名
     */
    private String packageName;

    // Alias for packageName for easier access
    public String getPackage() {
        return packageName;
    }

    public void setPackage(String packageName) {
        this.packageName = packageName;
    }

    /**
     * 模块 ID
     */
    private String id;

    /**
     * 模块名称
     */
    private String name;

    /**
     * 模块所有者
     */
    private String owner;

    /**
     * 所属层级
     */
    private String layer;

    /**
     * 模块描述
     */
    private String description;

    /**
     * 声明的依赖
     */
    private List<String> declaredDependencies;

    /**
     * 模块标签
     */
    private List<String> tags;

    /**
     * 是否为遗留代码
     */
    private boolean legacy;

    /**
     * 合规目标日期
     */
    private String complianceTarget;

    /**
     * 例外原因
     */
    private String exceptionReason;

    /**
     * 复审日期
     */
    private String reviewDate;

    /**
     * 是否需要复审
     */
    public boolean needsReview() {
        return legacy || exceptionReason != null && !exceptionReason.isEmpty();
    }
}
