// Input: 架构报告 DTO
// Output: 数据传输对象
// Pos: dto.response - 架构防御

package com.nexusarchive.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 架构报告 DTO
 * <p>
 * 包含所有模块的清单信息
 * </p>
 */
@Data
public class ArchitectureReportDto {

    /**
     * 报告生成时间
     */
    private Date timestamp;

    /**
     * 所有模块信息
     */
    private List<ModuleInfoDto> modules = new ArrayList<>();

    /**
     * 错误信息（如果有）
     */
    private String error;

    /**
     * 模块总数
     */
    public int getTotalModules() {
        return modules != null ? modules.size() : 0;
    }

    /**
     * 遗留模块数量
     */
    public long getLegacyModuleCount() {
        return modules != null ? modules.stream().filter(ModuleInfoDto::isLegacy).count() : 0;
    }
}
