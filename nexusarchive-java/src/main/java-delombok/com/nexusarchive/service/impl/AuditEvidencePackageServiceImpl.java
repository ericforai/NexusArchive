// Input: AuditEvidencePackageService, AuditLogService, AuditLogVerificationService, ObjectMapper
// Output: AuditEvidencePackageServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.ChainVerificationResult;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.service.AuditEvidencePackageService;
import com.nexusarchive.service.AuditLogVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 审计证据包导出服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEvidencePackageServiceImpl implements AuditEvidencePackageService {
    
    private final SysAuditLogMapper auditLogMapper;
    private final AuditLogVerificationService verificationService;
    private final ObjectMapper objectMapper;
    
    @Override
    public byte[] exportEvidencePackage(LocalDate startDate, LocalDate endDate, 
                                       String fondsNo, boolean includeVerificationReport) {
        try {
            // 1. 查询审计日志
            List<SysAuditLog> logs = auditLogMapper.findByDateRange(startDate, endDate);
            
            // 2. 如果指定了全宗号，过滤日志
            // TODO: SysAuditLog 可能没有 fondsNo 字段，需要检查实体类
            // if (fondsNo != null && !fondsNo.isEmpty()) {
            //     logs = logs.stream()
            //         .filter(log -> fondsNo.equals(log.getFondsNo()))
            //         .toList();
            // }
            
            // 3. 执行验真（如果需要）
            ChainVerificationResult verificationResult = null;
            if (includeVerificationReport) {
                verificationResult = verificationService.verifyChain(startDate, endDate, fondsNo);
            }
            
            // 4. 生成证据包
            return generateEvidencePackage(logs, verificationResult, startDate, endDate, fondsNo);
            
        } catch (Exception e) {
            log.error("导出审计证据包失败", e);
            throw new RuntimeException("导出审计证据包失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String exportEvidencePackageAsync(LocalDate startDate, LocalDate endDate, 
                                            String fondsNo, boolean includeVerificationReport) {
        // TODO: 实现异步导出任务
        // 可以使用 Spring 的 @Async 或消息队列
        String taskId = UUID.randomUUID().toString();
        log.info("创建异步导出任务: taskId={}, startDate={}, endDate={}", taskId, startDate, endDate);
        return taskId;
    }
    
    /**
     * 生成证据包（ZIP 格式）
     */
    private byte[] generateEvidencePackage(List<SysAuditLog> logs, 
                                          ChainVerificationResult verificationResult,
                                          LocalDate startDate, LocalDate endDate, 
                                          String fondsNo) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 1. 添加审计日志文件
            int index = 1;
            for (SysAuditLog log : logs) {
                String filename = String.format("audit-logs/audit-log-%03d.json", index++);
                ZipEntry entry = new ZipEntry(filename);
                zos.putNextEntry(entry);
                
                String logJson = objectMapper.writeValueAsString(log);
                zos.write(logJson.getBytes("UTF-8"));
                zos.closeEntry();
            }
            
            // 2. 添加验真报告（如果存在）
            if (verificationResult != null) {
                ZipEntry entry = new ZipEntry("verification-report.json");
                zos.putNextEntry(entry);
                String reportJson = objectMapper.writeValueAsString(verificationResult);
                zos.write(reportJson.getBytes("UTF-8"));
                zos.closeEntry();
            }
            
            // 3. 添加 manifest.json
            ZipEntry manifestEntry = new ZipEntry("manifest.json");
            zos.putNextEntry(manifestEntry);
            Map<String, Object> manifest = new HashMap<>();
            manifest.put("packageId", UUID.randomUUID().toString());
            manifest.put("exportDate", LocalDateTime.now().toString());
            manifest.put("dateRange", Map.of("startDate", startDate.toString(), "endDate", endDate.toString()));
            if (fondsNo != null) {
                manifest.put("fondsNo", fondsNo);
            }
            manifest.put("totalLogs", logs.size());
            manifest.put("verificationStatus", verificationResult != null && verificationResult.isChainIntact() ? "INTACT" : "UNKNOWN");
            manifest.put("hashAlgorithm", "SM3");
            String manifestJson = objectMapper.writeValueAsString(manifest);
            zos.write(manifestJson.getBytes("UTF-8"));
            zos.closeEntry();
            
            // 4. 添加 README.txt
            ZipEntry readmeEntry = new ZipEntry("README.txt");
            zos.putNextEntry(readmeEntry);
            String readme = String.format(
                "审计证据包\n" +
                "导出日期: %s\n" +
                "日期范围: %s 至 %s\n" +
                "全宗号: %s\n" +
                "日志数量: %d\n" +
                "哈希算法: SM3\n" +
                "\n" +
                "文件说明:\n" +
                "- audit-logs/: 审计日志文件（JSON格式）\n" +
                "- verification-report.json: 验真报告（如果包含）\n" +
                "- manifest.json: 证据包清单\n",
                LocalDateTime.now(), startDate, endDate, fondsNo != null ? fondsNo : "全部", logs.size()
            );
            zos.write(readme.getBytes("UTF-8"));
            zos.closeEntry();
        }
        
        return baos.toByteArray();
    }
}

