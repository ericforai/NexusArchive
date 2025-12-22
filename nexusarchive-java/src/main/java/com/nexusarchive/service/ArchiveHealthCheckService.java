// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: ArchiveHealthCheckService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.AuditInspectionLog;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.AuditInspectionLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案健康巡检服务
 * 定时执行四性检测，确保档案长期保存的安全性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveHealthCheckService {

    private final ArchiveMapper archiveMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final AuditInspectionLogMapper auditInspectionLogMapper;
    private final FourNatureCheckService fourNatureCheckService;
    private final ComplianceCheckService complianceCheckService;
    private final StandardReportGenerator standardReportGenerator;

    /**
     * 每日凌晨 3:00 执行全量健康巡检
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void runNightlyHealthCheck() {
        log.info("Starting Nightly Archive Health Check...");
        long startTime = System.currentTimeMillis();
        
        // 1. 获取所有已归档的档案
        // 注意：如果数据量大，应该分页处理。这里演示简单逻辑。
        List<Archive> archives = archiveMapper.selectList(new LambdaQueryWrapper<Archive>()
                .eq(Archive::getStatus, "ARCHIVED")); // 假设状态为 ARCHIVED
        
        int successCount = 0;
        int failCount = 0;
        int complianceViolationCount = 0;
        
        for (Archive archive : archives) {
            try {
                // 2. 获取关联文件
                List<ArcFileContent> files = arcFileContentMapper.selectList(new LambdaQueryWrapper<ArcFileContent>()
                        .eq(ArcFileContent::getItemId, archive.getId()));
                
                // 3. 执行四性检测
                FourNatureReport report = fourNatureCheckService.performHealthCheck(archive, files);
                
                // 4. 执行符合性检查（新增）
                ComplianceCheckService.ComplianceResult complianceResult = 
                    complianceCheckService.checkCompliance(archive, files);
                
                if (!complianceResult.isCompliant()) {
                    complianceViolationCount++;
                    log.warn("Compliance Violations for Archive {}: {}", 
                        archive.getArchiveCode(), complianceResult.getViolations());
                }
                
                // 5. 记录日志（新增符合性检查结果）
                saveInspectionLog(archive, report, complianceResult);
                
                if (report.getStatus() == OverallStatus.PASS) {
                    successCount++;
                } else {
                    failCount++;
                    log.warn("Health Check Failed/Warning for Archive: {} - Status: {}", archive.getArchiveCode(), report.getStatus());
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

    @org.springframework.beans.factory.annotation.Value("${nexus.storage.path:/data/nexusarchive/storage}")
    private String storageBasePath;

    private void saveInspectionLog(Archive archive, FourNatureReport report, 
                                  ComplianceCheckService.ComplianceResult complianceResult) {
        AuditInspectionLog logEntry = new AuditInspectionLog();
        logEntry.setArchiveId(archive.getId());
        logEntry.setInspectionStage("patrol"); // 巡检
        logEntry.setInspectionTime(LocalDateTime.now());
        logEntry.setInspectorId("SYSTEM");
        
        logEntry.setIsAuthentic(report.getAuthenticity().getStatus() == OverallStatus.PASS);
        logEntry.setIsComplete(report.getIntegrity().getStatus() == OverallStatus.PASS);
        logEntry.setIsAvailable(report.getUsability().getStatus() == OverallStatus.PASS);
        logEntry.setIsSecure(report.getSafety().getStatus() == OverallStatus.PASS);
        
        logEntry.setCheckResult(report.getStatus().name());
        
        // 保存详细报告 (JSON)
        logEntry.setAuthenticityCheck(report.getAuthenticity());
        logEntry.setIntegrityCheck(report.getIntegrity());
        logEntry.setAvailabilityCheck(report.getUsability());
        logEntry.setSecurityCheck(report.getSafety());
        
        // 添加符合性检查结果（新增）
        logEntry.setIsCompliant(complianceResult.isCompliant());
        if (!complianceResult.isCompliant()) {
            // 将违规信息记录到详细报告
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                logEntry.setComplianceViolations(mapper.writeValueAsString(complianceResult.getViolations()));
                logEntry.setComplianceWarnings(mapper.writeValueAsString(complianceResult.getWarnings()));
            } catch (Exception e) {
                log.error("Error serializing compliance violations", e);
            }
        }
        
        // ---------------------------------------------------------
        // 固化检测报告文件 (Persistence) - DA/T 94 Requirement
        // ---------------------------------------------------------
        try {
            // 1. Ensure directory exists
            String reportDir = storageBasePath + "/reports/" + java.time.LocalDate.now().toString();
            java.io.File dir = new java.io.File(reportDir);
            if (!dir.exists()) dir.mkdirs();

            // 2. Generate XML using StandardReportGenerator
            String fileName = "report_" + archive.getArchiveCode() + "_" + report.getCheckId() + ".xml";
            java.io.File reportFile = new java.io.File(dir, fileName);
            
            // Use injected StandardReportGenerator for XML generation
            String xmlContent = standardReportGenerator.generateComplianceReport(report);
            
            // Write XML to file
            java.nio.file.Files.write(reportFile.toPath(), xmlContent.getBytes());
            
            // 3. Calculate Hash (SM3 or SHA256)
            String fileHash = cn.hutool.crypto.digest.DigestUtil.sha256Hex(java.nio.file.Files.readAllBytes(reportFile.toPath()));
            
            logEntry.setReportFilePath(reportFile.getAbsolutePath());
            logEntry.setReportFileHash(fileHash);
            
        } catch (Exception e) {
            log.error("Failed to persist FourNatureReport to XML", e);
            // Don't block the main log saving, but mark as error?
        }
        
        auditInspectionLogMapper.insert(logEntry);
    }
}
