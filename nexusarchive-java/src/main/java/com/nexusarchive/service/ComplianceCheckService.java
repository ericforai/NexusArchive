// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: ComplianceCheckService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.AuditInspectionLog;
import com.nexusarchive.mapper.AuditInspectionLogMapper;
import com.nexusarchive.service.compliance.ComplianceCheckFacade;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 会计档案管理办法符合性检查服务
 * <p>
 * 向后兼容层，委托给 ComplianceCheckFacade
 * </p>
 */
@Deprecated
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceCheckService {

    private final AuditInspectionLogMapper auditInspectionLogMapper;
    private final ComplianceCheckFacade facade;
    private final com.nexusarchive.service.helper.ComplianceCheckHelper helper;

    @Deprecated
    public ComplianceResult checkCompliance(Archive archive, List<ArcFileContent> files) {
        com.nexusarchive.service.compliance.ComplianceResult newResult = facade.checkCompliance(archive, files);
        ComplianceResult legacyResult = new ComplianceResult();
        newResult.getViolations().forEach(legacyResult::addViolation);
        newResult.getWarnings().forEach(legacyResult::addWarning);
        return legacyResult;
    }

    private ComplianceResult checkComplianceLegacy(Archive archive, List<ArcFileContent> files) {
        ComplianceResult result = new ComplianceResult();
        helper.checkRetention(archive, result);
        helper.checkCompleteness(archive, files, result);
        helper.checkSignature(files, result);
        helper.checkTiming(archive, result);
        return result;
    }

    public ComplianceStatistics getStatistics() {
        ComplianceStatistics stats = new ComplianceStatistics();
        Long total = auditInspectionLogMapper.selectCount(new LambdaQueryWrapper<AuditInspectionLog>().isNotNull(AuditInspectionLog::getIsCompliant));
        if (total == 0) return stats;
        stats.setTotalArchives(total.intValue());
        
        Long strict = auditInspectionLogMapper.selectCount(new LambdaQueryWrapper<AuditInspectionLog>().eq(AuditInspectionLog::getIsCompliant, true)
                .and(w -> w.isNull(AuditInspectionLog::getComplianceWarnings).or().eq(AuditInspectionLog::getComplianceWarnings, "[]")));
        Long warn = auditInspectionLogMapper.selectCount(new LambdaQueryWrapper<AuditInspectionLog>().eq(AuditInspectionLog::getIsCompliant, true)
                .isNotNull(AuditInspectionLog::getComplianceWarnings).ne(AuditInspectionLog::getComplianceWarnings, "[]"));
        Long non = auditInspectionLogMapper.selectCount(new LambdaQueryWrapper<AuditInspectionLog>().eq(AuditInspectionLog::getIsCompliant, false));

        stats.setFullyCompliant(strict.intValue());
        stats.setCompliantWithWarnings(warn.intValue());
        stats.setNonCompliant(non.intValue());
        stats.setComplianceRate(Math.round((strict + warn) * 100.0 / total * 100.0) / 100.0);
        return stats;
    }

    /**
     * 符合性统计数据
     */
    public static class ComplianceStatistics {
        private int totalArchives;
        private int fullyCompliant;
        private int compliantWithWarnings;
        private int nonCompliant;
        private double complianceRate;

        // Getters and Setters
        public int getTotalArchives() {
            return totalArchives;
        }

        public void setTotalArchives(int totalArchives) {
            this.totalArchives = totalArchives;
        }

        public int getFullyCompliant() {
            return fullyCompliant;
        }

        public void setFullyCompliant(int fullyCompliant) {
            this.fullyCompliant = fullyCompliant;
        }

        public int getCompliantWithWarnings() {
            return compliantWithWarnings;
        }

        public void setCompliantWithWarnings(int compliantWithWarnings) {
            this.compliantWithWarnings = compliantWithWarnings;
        }

        public int getNonCompliant() {
            return nonCompliant;
        }

        public void setNonCompliant(int nonCompliant) {
            this.nonCompliant = nonCompliant;
        }

        public double getComplianceRate() {
            return complianceRate;
        }

        public void setComplianceRate(double complianceRate) {
            this.complianceRate = complianceRate;
        }
    }

    /**
     * 合规性检查结果
     */
    public static class ComplianceResult {
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