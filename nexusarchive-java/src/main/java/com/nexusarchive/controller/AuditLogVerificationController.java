// Input: Spring Web、AuditLogVerificationService、AuditEvidencePackageService、AuditLogSamplingService
// Output: AuditLogVerificationController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.ChainVerificationResult;
import com.nexusarchive.dto.SamplingCriteria;
import com.nexusarchive.dto.SamplingResult;
import com.nexusarchive.dto.VerificationResult;
import com.nexusarchive.service.AuditEvidencePackageService;
import com.nexusarchive.service.AuditLogSamplingService;
import com.nexusarchive.service.AuditLogVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 审计日志验真控制器
 * 
 * PRD 来源: Section 6.2 - 审计日志防篡改要求
 */
@Slf4j
@RestController
@RequestMapping("/api/audit-log")
@RequiredArgsConstructor
@Tag(name = "审计日志验真", description = "审计日志哈希链验真、证据包导出、抽检验真接口")
public class AuditLogVerificationController {
    
    private final AuditLogVerificationService verificationService;
    private final AuditEvidencePackageService evidencePackageService;
    private final AuditLogSamplingService samplingService;
    
    @PostMapping("/verify")
    @Operation(summary = "验证单条审计日志")
    @PreAuthorize("hasAnyAuthority('audit:view', 'audit:verify') or hasRole('AUDIT_ADMIN')")
    public Result<VerificationResult> verifySingleLog(@RequestParam String logId) {
        try {
            VerificationResult result = verificationService.verifySingleLog(logId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证单条审计日志失败: logId={}", logId, e);
            return Result.fail("验证失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/verify-chain")
    @Operation(summary = "验证审计日志哈希链")
    @PreAuthorize("hasAnyAuthority('audit:view', 'audit:verify') or hasRole('AUDIT_ADMIN')")
    public Result<ChainVerificationResult> verifyChain(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String fondsNo) {
        try {
            ChainVerificationResult result = verificationService.verifyChain(startDate, endDate, fondsNo);
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证审计日志哈希链失败: startDate={}, endDate={}", startDate, endDate, e);
            return Result.fail("验证失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/verify-chain-by-ids")
    @Operation(summary = "验证指定日志ID列表的哈希链")
    @PreAuthorize("hasAnyAuthority('audit:view', 'audit:verify') or hasRole('AUDIT_ADMIN')")
    public Result<ChainVerificationResult> verifyChainByLogIds(@RequestBody List<String> logIds) {
        try {
            ChainVerificationResult result = verificationService.verifyChainByLogIds(logIds);
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证审计日志哈希链失败: logIds={}", logIds, e);
            return Result.fail("验证失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/export-evidence")
    @Operation(summary = "导出审计证据包")
    @PreAuthorize("hasAnyAuthority('audit:export') or hasRole('AUDIT_ADMIN')")
    public ResponseEntity<byte[]> exportEvidencePackage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String fondsNo,
            @RequestParam(defaultValue = "true") boolean includeVerificationReport) {
        try {
            byte[] evidencePackage = evidencePackageService.exportEvidencePackage(
                startDate, endDate, fondsNo, includeVerificationReport);
            
            String filename = String.format("evidence-package-%s-%s.zip", 
                startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(evidencePackage.length);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(evidencePackage);
        } catch (Exception e) {
            log.error("导出审计证据包失败: startDate={}, endDate={}", startDate, endDate, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/sample-verify")
    @Operation(summary = "抽检验真")
    @PreAuthorize("hasAnyAuthority('audit:view', 'audit:verify') or hasRole('AUDIT_ADMIN')")
    public Result<SamplingResult> sampleVerify(
            @RequestParam int sampleSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestBody(required = false) SamplingCriteria criteria) {
        try {
            SamplingResult result;
            if (criteria != null) {
                result = samplingService.sampleByCriteria(criteria, sampleSize);
            } else {
                if (startDate == null || endDate == null) {
                    return Result.fail("随机抽检需要提供开始日期和结束日期");
                }
                result = samplingService.randomSample(sampleSize, startDate, endDate);
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("抽检验真失败", e);
            return Result.fail("抽检验真失败: " + e.getMessage());
        }
    }
}

