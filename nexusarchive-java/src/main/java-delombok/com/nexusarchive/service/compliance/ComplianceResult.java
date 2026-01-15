// Input: Lombok
// Output: ComplianceResult 类
// Pos: 服务层 - 合规验证结果

package com.nexusarchive.service.compliance;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 合规验证结果
 */
@Data
public class ComplianceResult {

    /** 违规项列表（必须修复） */
    private final List<String> violations = new ArrayList<>();

    /** 警告项列表（建议修复） */
    private final List<String> warnings = new ArrayList<>();

    /** 添加违规项 */
    public ComplianceResult addViolation(String violation) {
        this.violations.add(violation);
        return this;
    }

    /** 添加警告项 */
    public ComplianceResult addWarning(String warning) {
        this.warnings.add(warning);
        return this;
    }

    /** 是否合规（无违规项） */
    public boolean isCompliant() {
        return this.violations.isEmpty();
    }

    /** 合并另一个结果 */
    public ComplianceResult merge(ComplianceResult other) {
        this.violations.addAll(other.getViolations());
        this.warnings.addAll(other.getWarnings());
        return this;
    }

    /** 获取违规项数量 */
    public int getViolationCount() {
        return violations.size();
    }

    /** 获取警告项数量 */
    public int getWarningCount() {
        return warnings.size();
    }

    /** 获取违规项列表 */
    public List<String> getViolations() {
        return new ArrayList<>(violations);
    }

    /** 获取警告项列表 */
    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    /** 获取合规级别 */
    public ComplianceLevel getComplianceLevel() {
        if (violations.isEmpty() && warnings.isEmpty()) {
            return ComplianceLevel.FULLY_COMPLIANT;
        } else if (violations.isEmpty()) {
            return ComplianceLevel.COMPLIANT_WITH_WARNINGS;
        } else {
            return ComplianceLevel.NON_COMPLIANT;
        }
    }

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
}
