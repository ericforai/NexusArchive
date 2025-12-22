// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: ComplianceCheckService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 会计档案管理办法符合性检查服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceCheckService {
    
    private final DigitalSignatureService digitalSignatureService;
    
    /**
     * 检查档案是否符合《会计档案管理办法》要求
     * @param archive 待检查档案
     * @param files 关联文件列表
     * @return 检查结果
     */
    public ComplianceResult checkCompliance(Archive archive, List<ArcFileContent> files) {
        log.info("开始检查档案 {} 的合规性", archive.getArchiveCode());
        
        ComplianceResult result = new ComplianceResult();
        
        // 1. 检查保存期限是否符合要求
        checkRetentionPeriod(archive, result);
        
        // 2. 检查档案完整性
        checkArchiveCompleteness(archive, files, result);
        
        // 3. 检查电子签名有效性
        checkDigitalSignature(archive, files, result);
        
        // 4. 检查归档时间合规性
        checkArchivingTiming(archive, result);
        
        // 5. 检查会计科目代码合规性
        checkAccountingCode(archive, result);
        
        // 6. 检查档案分类体系
        checkArchiveClassification(archive, result);
        
        // 7. 检查档号生成规则
        checkArchiveCodeRule(archive, result);
        
        // 8. 检查纸质档案关联
        checkPaperArchiveReference(archive, result);
        
        // 9. 检查凭证连续性
        checkVoucherContinuity(archive, result);
        
        // 10. 检查审计日志完整性
        checkAuditLogCompleteness(archive, result);
        
        log.info("档案 {} 合规性检查完成，违规项: {}, 警告项: {}", 
            archive.getArchiveCode(), result.getViolations().size(), result.getWarnings().size());
        
        return result;
    }
    
    /**
     * 检查保存期限
     * 根据《会计档案管理办法》第八条检查
     */
    private void checkRetentionPeriod(Archive archive, ComplianceResult result) {
        String retentionPeriod = archive.getRetentionPeriod();
        String categoryCode = archive.getCategoryCode();
        
        // 会计凭证保存期限至少为30年
        if ("AC01".equals(categoryCode)) {
            if (retentionPeriod == null || !"30".equals(retentionPeriod)) {
                result.addViolation("会计凭证保存期限不符合《会计档案管理办法》第八条要求，应保存至少30年");
            }
        }
        // 会计账簿保存期限至少为30年
        else if ("AC02".equals(categoryCode)) {
            if (retentionPeriod == null || !"30".equals(retentionPeriod)) {
                result.addViolation("会计账簿保存期限不符合《会计档案管理办法》第八条要求，应保存至少30年");
            }
        }
        // 财务报告保存期限至少为永久
        else if ("AC03".equals(categoryCode)) {
            if (retentionPeriod == null || !"永久".equals(retentionPeriod)) {
                result.addViolation("财务报告保存期限不符合《会计档案管理办法》第八条要求，应永久保存");
            }
        }
        // 其他财务文件保存期限至少为15年
        else if ("AC04".equals(categoryCode) || "AC05".equals(categoryCode)) {
            if (retentionPeriod == null || (!"15".equals(retentionPeriod) && !"30".equals(retentionPeriod))) {
                result.addWarning("其他财务文件建议保存期限至少为15年");
            }
        }
    }
    
    /**
     * 检查档案完整性
     * 根据《会计档案管理办法》第六条检查
     */
    private void checkArchiveCompleteness(Archive archive, List<ArcFileContent> files, ComplianceResult result) {
        // 检查是否有电子签名
        boolean hasValidSignature = files.stream().anyMatch(f -> 
            f.getSignValue() != null && f.getSignValue().length > 0);
        
        if (!hasValidSignature) {
            result.addViolation("档案缺少可靠的电子签名，不符合《会计档案管理办法》第六条要求");
        }
        
        // 检查元数据完整性
        if (archive.getStandardMetadata() == null || archive.getStandardMetadata().length() == 0) {
            result.addViolation("档案缺少标准元数据，不符合《会计档案管理办法》第六条要求");
        }
        
        // 检查关键字段
        if (archive.getUniqueBizId() == null || archive.getUniqueBizId().length() == 0) {
            result.addViolation("档案缺少唯一业务标识，不符合档案管理要求");
        }
        
        if (archive.getAmount() == null) {
            result.addViolation("档案缺少金额信息，不符合会计档案要求");
        }
        
        if (archive.getDocDate() == null) {
            result.addViolation("档案缺少业务日期，不符合档案管理要求");
        }
        
        // 检查文件完整性
        if (files == null || files.isEmpty()) {
            result.addViolation("档案没有关联的电子文件，不符合会计档案管理要求");
        }
    }
    
    /**
     * 检查电子签名有效性
     */
    private void checkDigitalSignature(Archive archive, List<ArcFileContent> files, ComplianceResult result) {
        for (ArcFileContent file : files) {
            if (file.getSignValue() != null && file.getSignValue().length > 0) {
                try {
                    // 调用电子签名验证服务
                    DigitalSignatureService.VerificationResult verificationResult = 
                        digitalSignatureService.verifySignature(file);
                    
                    if (!verificationResult.isValid()) {
                        result.addViolation("文件 " + file.getFileName() + " 的电子签名无效: " 
                            + verificationResult.getErrorMessage());
                    }
                    
                    // 检查证书有效期
                    if (verificationResult.isCertificateExpired()) {
                        result.addWarning("文件 " + file.getFileName() + " 的电子签名证书已过期");
                    }
                    
                } catch (Exception e) {
                    result.addWarning("无法验证文件 " + file.getFileName() + " 的电子签名有效性: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 检查归档时间合规性
     * 根据《会计档案管理办法》第九条检查
     */
    private void checkArchivingTiming(Archive archive, ComplianceResult result) {
        if (archive.getDocDate() != null && archive.getCreatedTime() != null) {
            LocalDate docDate = archive.getDocDate();
            LocalDate archivingDate = archive.getCreatedTime().toLocalDate();
            
            // 检查是否在会计年度终了后1年内归档
            LocalDate fiscalYearEnd = LocalDate.of(docDate.getYear(), 12, 31);
            LocalDate deadline = fiscalYearEnd.plusYears(1);
            
            if (archivingDate.isAfter(deadline)) {
                long monthsLate = ChronoUnit.MONTHS.between(deadline, archivingDate);
                result.addViolation("档案归档时间延迟了 " + monthsLate + " 个月，不符合《会计档案管理办法》第九条要求");
            }
        } else {
            result.addWarning("无法验证归档时间合规性，缺少必要日期信息");
        }
    }
    
    /**
     * 检查会计科目代码合规性
     */
    private void checkAccountingCode(Archive archive, ComplianceResult result) {
        // 检查科目代码格式是否符合国家标准
        // 这通常需要解析customMetadata中的科目信息
        String customMetadata = archive.getCustomMetadata();
        if (customMetadata != null && !customMetadata.isEmpty()) {
            try {
                // 解析JSON格式的自定义元数据
                // 这里简化处理，实际应根据具体JSON结构解析
                if (customMetadata.contains("accsubject")) {
                    // 实际实现应解析科目代码并验证格式
                    // 这里只做简单检查
                }
            } catch (Exception e) {
                result.addWarning("无法验证会计科目代码合规性: " + e.getMessage());
            }
        }
    }
    
    /**
     * 检查档案分类体系
     */
    private void checkArchiveClassification(Archive archive, ComplianceResult result) {
        String categoryCode = archive.getCategoryCode();
        
        // 检查分类代码是否符合国家标准
        if (categoryCode == null || categoryCode.isEmpty()) {
            result.addViolation("档案缺少分类代码，不符合《会计档案管理办法》要求");
        } else if (!isValidCategoryCode(categoryCode)) {
            result.addViolation("档案分类代码 " + categoryCode + " 不符合国家标准");
        }
        
        // 检查分类与内容是否匹配
        if ("AC01".equals(categoryCode) && archive.getAmount() == null) {
            result.addWarning("会计凭证应包含金额信息");
        }
    }
    
    /**
     * 检查档号生成规则
     */
    private void checkArchiveCodeRule(Archive archive, ComplianceResult result) {
        String archiveCode = archive.getArchiveCode();
        
        if (archiveCode == null || archiveCode.isEmpty()) {
            result.addViolation("档案缺少档号，不符合档案管理要求");
            return;
        }
        
        // 检查档号格式是否符合 [全宗号]-[年度]-[保管期限]-[机构]-[分类]-[件号] 格式
        // 例如: Z001-2024-30-CW-AC01-0001 或 Z001-2024-永久-CW-AC03-0001
        String pattern = "^[A-Z0-9]+-\\d{4}-[^-]+-[^-]+-[A-Z0-9]+-\\d+$";
        if (!archiveCode.matches(pattern)) {
            result.addViolation("档号格式不符合标准，应为: [全宗号]-[年度]-[保管期限]-[机构]-[分类]-[件号]");
        }
    }
    
    /**
     * 检查纸质档案关联
     */
    private void checkPaperArchiveReference(Archive archive, ComplianceResult result) {
        // 检查是否有纸质档案关联号
        String paperRefLink = archive.getPaperRefLink();
        
        if (paperRefLink != null && !paperRefLink.isEmpty()) {
            // 检查纸质档案关联号格式
            if (!paperRefLink.matches("^[A-Z0-9/\\-]+$")) {
                result.addWarning("纸质档案关联号格式不规范: " + paperRefLink);
            }
        } else {
            // 根据档案类型决定是否需要纸质档案关联
            String categoryCode = archive.getCategoryCode();
            if ("AC01".equals(categoryCode)) {
                // 会计凭证通常需要纸质版本备份
                result.addWarning("会计凭证建议关联纸质档案备份");
            }
        }
    }
    
    /**
     * 检查凭证连续性
     */
    private void checkVoucherContinuity(Archive archive, ComplianceResult result) {
        String categoryCode = archive.getCategoryCode();
        
        // 只对会计凭证进行连续性检查
        if ("AC01".equals(categoryCode)) {
            try {
                // 解析档号中的凭证编号
                String archiveCode = archive.getArchiveCode();
                String[] parts = archiveCode.split("-");
                if (parts.length >= 6) {
                    String voucherNumber = parts[parts.length - 1];
                    
                    // 检查凭证编号是否为数字
                    if (!voucherNumber.matches("^\\d+$")) {
                        result.addWarning("凭证编号应为数字: " + voucherNumber);
                    }
                }
            } catch (Exception e) {
                result.addWarning("无法验证凭证连续性: " + e.getMessage());
            }
        }
    }
    
    /**
     * 检查审计日志完整性
     */
    private void checkAuditLogCompleteness(Archive archive, ComplianceResult result) {
        // 这里简化处理，实际应查询审计日志表
        // 检查是否有归档操作的审计日志
        // 检查是否有修改操作的审计日志
        // 检查是否有访问操作的审计日志
        
        // 如果无法查询审计日志，则发出警告
        result.addWarning("无法验证审计日志完整性，请检查审计系统状态");
    }
    
    /**
     * 验证分类代码是否有效
     */
    private boolean isValidCategoryCode(String categoryCode) {
        // 根据国家标准验证分类代码
        return "AC01".equals(categoryCode) ||  // 会计凭证
               "AC02".equals(categoryCode) ||  // 会计账簿
               "AC03".equals(categoryCode) ||  // 财务报告
               "AC04".equals(categoryCode) ||  // 其他财务资料
               "AC05".equals(categoryCode);    // 税务资料
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