// Input: 违规报告 DTO
// Output: 数据传输对象
// Pos: dto.response - 架构防御

package com.nexusarchive.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 架构违规报告 DTO
 * <p>
 * 包含所有架构违规信息
 * </p>
 */
@Data
public class ViolationReportDto {

    /**
     * 报告生成时间
     */
    private Date timestamp;

    /**
     * 违规列表
     */
    private List<ViolationDto> violations = new ArrayList<>();

    /**
     * 是否有严重违规
     */
    public boolean hasCriticalViolations() {
        return violations.stream().anyMatch(v -> "error".equals(v.getSeverity()));
    }

    /**
     * 违规 DTO
     */
    @Data
    public static class ViolationDto {

        /**
         * 违规类型
         */
        private String type;

        /**
         * 违规严重程度
         */
        private String severity;

        /**
         * 违规描述
         */
        private String description;

        /**
         * 涉及的类
         */
        private String className;

        /**
         * 涉及的包
         */
        private String packageName;

        /**
         * 修复建议
         */
        private String suggestion;
    }
}
