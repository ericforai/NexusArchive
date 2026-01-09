// Input: Java 标准库
// Output: ComplianceLevel 枚举
// Pos: 业务服务层

package com.nexusarchive.service.compliance.value;

/**
 * 合规级别
 */
public enum ComplianceLevel {
    FULLY_COMPLIANT("完全合规"),
    COMPLIANT_WITH_WARNINGS("合规但有警告"),
    NON_COMPLIANT("不合规");

    private final String description;

    ComplianceLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
