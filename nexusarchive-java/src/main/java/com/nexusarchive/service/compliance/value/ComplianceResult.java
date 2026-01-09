// Input: Lombok、Java 标准库
// Output: ComplianceResult 类
// Pos: 业务服务层

package com.nexusarchive.service.compliance.value;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 合规性检查结果
 */
@Data
public class ComplianceResult {
    private final List<String> violations = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void addViolation(String violation) {
        violations.add(violation);
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public boolean isCompliant() {
        return violations.isEmpty();
    }

    public List<String> getViolations() {
        return new ArrayList<>(violations);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public int getViolationCount() {
        return violations.size();
    }

    public int getWarningCount() {
        return warnings.size();
    }

    public ComplianceLevel getComplianceLevel() {
        if (violations.isEmpty() && warnings.isEmpty()) {
            return ComplianceLevel.FULLY_COMPLIANT;
        } else if (violations.isEmpty()) {
            return ComplianceLevel.COMPLIANT_WITH_WARNINGS;
        } else {
            return ComplianceLevel.NON_COMPLIANT;
        }
    }
}
