# 第一阶段：会计合规性优化

## 1. 金额精度增强校验

### 创建金额校验增强工具类

```java
// 新建文件：nexusarchive-java/src/main/java/com/nexusarchive/util/AmountValidator.java
package com.nexusarchive.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

/**
 * 会计金额精度校验工具
 * 确保符合中国会计准则精度要求
 */
@Component
public class AmountValidator {
    
    // 默认保留两位小数
    private static final int DEFAULT_SCALE = 2;
    
    // 最大金额限制（可根据实际业务调整）
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999.99");
    
    /**
     * 校验会计金额精度和有效性
     * @param amount 待校验金额
     * @return 校验结果
     */
    public ValidationResult validateAmount(BigDecimal amount) {
        if (amount == null) {
            return ValidationResult.fail("金额不能为空");
        }
        
        // 检查精度
        if (amount.scale() > DEFAULT_SCALE) {
            return ValidationResult.fail("金额精度超过两位小数");
        }
        
        // 检查范围
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            return ValidationResult.fail("金额超过最大允许值");
        }
        
        // 检查负数（根据业务规则，收入类凭证金额应为正）
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.fail("金额不能为负数");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 标准化金额格式
     * 确保金额格式符合会计准则
     * @param amount 原始金额
     * @return 标准化后的金额
     */
    public BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * 校验结果
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;
        
        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}
```

### 增强四性检测服务中的金额校验

```java
// 修改文件：nexusarchive-java/src/main/java/com/nexusarchive/service/impl/FourNatureCheckServiceImpl.java
// 在 checkIntegrity 方法中添加金额精度校验

private CheckItem checkIntegrity(AccountingSipDto sip, Map<String, byte[]> fileStreams) {
    CheckItem item = CheckItem.pass("Integrity Check", "Metadata and structure valid");
    VoucherHeadDto header = sip.getHeader();
    
    // ... 现有校验代码 ...
    
    // 金额精度校验（新增）
    if (header.getAmount() != null) {
        AmountValidator.ValidationResult result = amountValidator.validateAmount(header.getAmount());
        if (!result.isValid()) {
            item.addError("金额格式不符合会计准则: " + result.getMessage());
        }
    }
    
    return item;
}
```

## 2. 四性检测XML报告优化

### 创建符合GB/T 39674标准的报告生成器

```java
// 新建文件：nexusarchive-java/src/main/java/com/nexusarchive/service/StandardReportGenerator.java
package com.nexusarchive.service;

import com.nexusarchive.dto.sip.report.FourNatureReport;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.time.format.DateTimeFormatter;

/**
 * 标准报告生成器
 * 符合GB/T 39674和DA/T 94-2022标准
 */
@Service
public class StandardReportGenerator {
    
    /**
     * 生成符合国家标准的四性检测报告
     * @param report 四性检测结果
     * @return XML格式的报告字符串
     */
    public String generateComplianceReport(FourNatureReport report) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            
            // 创建文档根元素
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("档案四性检测报告");
            
            // 添加命名空间和版本信息
            rootElement.setAttribute("xmlns", "http://www.saac.gov.cn/national/standard");
            rootElement.setAttribute("版本", "DA/T 94-2022");
            rootElement.setAttribute("生成时间", report.getCheckTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            doc.appendChild(rootElement);
            
            // 添加基本信息
            addBasicInfo(doc, rootElement, report);
            
            // 添加四性检测结果
            addFourNatureResults(doc, rootElement, report);
            
            // 转换为XML字符串
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            
            return writer.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("生成合规报告失败", e);
        }
    }
    
    private void addBasicInfo(Document doc, Element parent, FourNatureReport report) {
        Element basicInfo = doc.createElement("基本信息");
        parent.appendChild(basicInfo);
        
        addElement(doc, basicInfo, "检测ID", report.getCheckId());
        addElement(doc, basicInfo, "档号", report.getArchivalCode());
        addElement(doc, basicInfo, "检测时间", report.getCheckTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        addElement(doc, basicInfo, "总体结果", report.getStatus().name());
    }
    
    private void addFourNatureResults(Document doc, Element parent, FourNatureReport report) {
        Element fourNature = doc.createElement("四性检测结果");
        parent.appendChild(fourNature);
        
        // 真实性
        addNatureResult(doc, fourNature, "真实性", report.getAuthenticity());
        
        // 完整性
        addNatureResult(doc, fourNature, "完整性", report.getIntegrity());
        
        // 可用性
        addNatureResult(doc, fourNature, "可用性", report.getUsability());
        
        // 安全性
        addNatureResult(doc, fourNature, "安全性", report.getSafety());
    }
    
    private void addNatureResult(Document doc, Element parent, String natureName, CheckItem checkItem) {
        Element natureElement = doc.createElement(natureName);
        parent.appendChild(natureElement);
        
        addElement(doc, natureElement, "结果", checkItem.getStatus().name());
        addElement(doc, natureElement, "描述", checkItem.getMessage());
        
        if (!checkItem.getErrors().isEmpty()) {
            Element errorsElement = doc.createElement("错误");
            natureElement.appendChild(errorsElement);
            
            for (String error : checkItem.getErrors()) {
                addElement(doc, errorsElement, "错误项", error);
            }
        }
    }
    
    private void addElement(Document doc, Element parent, String name, String value) {
        Element element = doc.createElement(name);
        element.appendChild(doc.createTextNode(value != null ? value : ""));
        parent.appendChild(element);
    }
}
```

## 3. 会计档案管理办法符合性检查

### 创建合规性检查服务

```java
// 新建文件：nexusarchive-java/src/main/java/com/nexusarchive/service/ComplianceCheckService.java
package com.nexusarchive.service;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 会计档案管理办法符合性检查服务
 */
@Service
public class ComplianceCheckService {
    
    /**
     * 检查档案是否符合《会计档案管理办法》要求
     * @param archive 待检查档案
     * @param files 关联文件列表
     * @return 检查结果
     */
    public ComplianceResult checkCompliance(Archive archive, List<ArcFileContent> files) {
        ComplianceResult result = new ComplianceResult();
        
        // 1. 检查保存期限是否符合要求
        checkRetentionPeriod(archive, result);
        
        // 2. 检查档案完整性
        checkArchiveCompleteness(archive, files, result);
        
        // 3. 检查电子签名有效性
        checkDigitalSignature(archive, files, result);
        
        // 4. 检查归档时间合规性
        checkArchivingTiming(archive, result);
        
        return result;
    }
    
    /**
     * 检查保存期限
     * 根据《会计档案管理办法》第八条检查
     */
    private void checkRetentionPeriod(Archive archive, ComplianceResult result) {
        String retentionPeriod = archive.getRetentionPeriod();
        
        // 会计凭证保存期限至少为30年
        if ("凭证".equals(archive.getCategoryCode()) && 
            (retentionPeriod == null || !"30".equals(retentionPeriod))) {
            result.addViolation("会计凭证保存期限不符合《会计档案管理办法》第八条要求，应保存至少30年");
        }
        
        // 会计账簿保存期限至少为30年
        if ("账簿".equals(archive.getCategoryCode()) && 
            (retentionPeriod == null || !"30".equals(retentionPeriod))) {
            result.addViolation("会计账簿保存期限不符合《会计档案管理办法》第八条要求，应保存至少30年");
        }
        
        // 财务报告保存期限至少为永久
        if ("报表".equals(archive.getCategoryCode()) && 
            (retentionPeriod == null || !"永久".equals(retentionPeriod))) {
            result.addViolation("财务报告保存期限不符合《会计档案管理办法》第八条要求，应永久保存");
        }
    }
    
    /**
     * 检查档案完整性
     * 根据《会计档案管理办法》第六条检查
     */
    private void checkArchiveCompleteness(Archive archive, List<ArcFileContent> files, ComplianceResult result) {
        // 检查是否有电子签名
        if (files.stream().noneMatch(f -> f.getSignValue() != null && !f.getSignValue().isEmpty())) {
            result.addWarning("档案缺少可靠的电子签名，不符合《会计档案管理办法》第六条要求");
        }
        
        // 检查元数据完整性
        if (archive.getStandardMetadata() == null || archive.getStandardMetadata().isEmpty()) {
            result.addViolation("档案缺少标准元数据，不符合《会计档案管理办法》第六条要求");
        }
    }
    
    /**
     * 检查电子签名有效性
     */
    private void checkDigitalSignature(Archive archive, List<ArcFileContent> files, ComplianceResult result) {
        for (ArcFileContent file : files) {
            if (file.getSignValue() != null && !file.getSignValue().isEmpty()) {
                try {
                    // 这里应调用电子签名验证服务
                    // 验证签名有效性、证书有效性等
                    boolean isValid = verifyDigitalSignature(file);
                    
                    if (!isValid) {
                        result.addViolation("文件 " + file.getFileName() + " 的电子签名无效");
                    }
                } catch (Exception e) {
                    result.addWarning("无法验证文件 " + file.getFileName() + " 的电子签名有效性");
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
            LocalDate deadline = docDate.plusYears(1).plusMonths(3); // 允许3个月宽限期
            
            if (archivingDate.isAfter(deadline)) {
                long monthsLate = ChronoUnit.MONTHS.between(deadline, archivingDate);
                result.addWarning("档案归档时间延迟了 " + monthsLate + " 个月，不符合《会计档案管理办法》第九条要求");
            }
        }
    }
    
    private boolean verifyDigitalSignature(ArcFileContent file) {
        // 实际实现应调用电子签名验证服务
        // 这里只是一个占位符
        return true;
    }
    
    /**
     * 合规性检查结果
     */
    public static class ComplianceResult {
        private List<String> violations = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
        
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
            return violations;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
    }
}
```

## 4. 增强四性检测定时任务

### 修改定时任务，增加符合性检查

```java
// 修改文件：nexusarchive-java/src/main/java/com/nexusarchive/service/ArchiveHealthCheckService.java
// 在定时任务中增加符合性检查

@Scheduled(cron = "0 0 3 * * ?")
public void runNightlyHealthCheck() {
    log.info("Starting Nightly Archive Health Check...");
    long startTime = System.currentTimeMillis();
    
    // 获取所有已归档的档案
    List<Archive> archives = archiveMapper.selectList(new LambdaQueryWrapper<Archive>()
            .eq(Archive::getStatus, "ARCHIVED"));
    
    int successCount = 0;
    int failCount = 0;
    int complianceViolationCount = 0;
    
    for (Archive archive : archives) {
        try {
            // 获取关联文件
            List<ArcFileContent> files = arcFileContentMapper.selectList(new LambdaQueryWrapper<ArcFileContent>()
                    .eq(ArcFileContent::getItemId, archive.getId()));
            
            // 执行四性检测
            FourNatureReport report = fourNatureCheckService.performHealthCheck(archive, files);
            
            // 执行符合性检查（新增）
            ComplianceCheckService.ComplianceResult complianceResult = 
                complianceCheckService.checkCompliance(archive, files);
            
            if (!complianceResult.isCompliant()) {
                complianceViolationCount++;
                log.warn("Compliance Violations for Archive {}: {}", 
                    archive.getArchiveCode(), complianceResult.getViolations());
            }
            
            // 记录日志
            saveInspectionLog(archive, report, complianceResult);
            
            if (report.getStatus() == OverallStatus.PASS) {
                successCount++;
            } else {
                failCount++;
                log.warn("Health Check Failed/Warning for Archive: {} - Status: {}", 
                    archive.getArchiveCode(), report.getStatus());
            }
            
        } catch (Exception e) {
            log.error("Error checking archive: " + archive.getArchiveCode(), e);
            failCount++;
        }
    }
    
    long duration = System.currentTimeMillis() - startTime;
    log.info("Nightly Health Check Completed in {} ms. Checked: {}, Success: {}, Issues: {}, Compliance Violations: {}", 
            duration, archives.size(), successCount, failCount, complianceViolationCount);
}

// 修改 saveInspectionLog 方法，增加符合性检查结果
private void saveInspectionLog(Archive archive, FourNatureReport report, 
                              ComplianceCheckService.ComplianceResult complianceResult) {
    AuditInspectionLog logEntry = new AuditInspectionLog();
    // ... 现有代码 ...
    
    // 添加符合性检查结果（新增）
    logEntry.setIsCompliant(complianceResult.isCompliant());
    if (!complianceResult.isCompliant()) {
        // 将违规信息记录到详细报告
        try {
            ObjectMapper mapper = new ObjectMapper();
            logEntry.setComplianceViolations(mapper.writeValueAsString(complianceResult.getViolations()));
            logEntry.setComplianceWarnings(mapper.writeValueAsString(complianceResult.getWarnings()));
        } catch (Exception e) {
            log.error("Error serializing compliance violations", e);
        }
    }
    
    // ... 其余代码 ...
}
```

## 实施计划

1. **第1-3天**：实施金额精度校验增强
2. **第4-6天**：实现标准化XML报告生成器
3. **第7-9天**：开发《会计档案管理办法》符合性检查服务
4. **第10-14天**：集成符合性检查到定时任务，完成测试

## 预期效果

1. 金额精度校验更加严格，完全符合会计准则
2. 四性检测报告格式符合国家标准，便于审计
3. 系统自动检查档案是否符合《会计档案管理办法》要求
4. 降低合规风险，提高档案管理质量

## 风险控制措施

1. 所有更改不影响现有功能，只是增强校验
2. 在测试环境充分验证后再部署到生产环境
3. 提供配置开关，可暂时关闭新增校验以兼容老数据
4. 提供数据清理脚本，处理不符合新规的历史数据