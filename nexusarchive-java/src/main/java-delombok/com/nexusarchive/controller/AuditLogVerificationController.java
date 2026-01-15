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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

import jakarta.validation.Valid;

/**
 * 审计日志验真控制器
 *
 * PRD 来源: Section 6.2 - 审计日志防篡改要求
 * 提供审计日志哈希链验真、证据包导出、抽检验真功能
 *
 * <p>合规要求：DA/T 94-2022 电子会计档案管理规范</p>
 */
@Slf4j
@Tag(name = "审计日志验真", description = """
    审计日志哈希链验真、证据包导出、抽检验真接口。

    **功能说明:**
    - 验证单条审计日志完整性
    - 验证哈希链连续性
    - 导出审计证据包
    - 抽检验真功能

    **哈希链机制 (SM3):**
    ```
    日志1 → SM3(内容+NULL) → Hash1
    日志2 → SM3(内容+Hash1) → Hash2
    日志3 → SM3(内容+Hash2) → Hash3
    ```

    **验证内容包括:**
    - log_hash: 当前日志 SM3 哈希值
    - prev_log_hash: 前一条日志哈希值
    - hash 持续性验证
    - 内容完整性验证

    **证据包格式:**
    - ZIP 压缩包
    - 包含原始日志 JSON
    - 包含验证报告
    - 包含哈希链证明

    **抽检验真:**
    - 随机抽取指定数量日志
    - 支持按条件筛选
    - 支持自定义抽样规则

    **使用场景:**
    - 审计合规检查
    - 司法取证支持
    - 内部审计验证
    - 数据完整性证明

    **权限要求:**
    - AUDIT_ADMIN 角色（所有操作）
    - audit:view 权限（查看）
    - audit:verify 权限（验证）
    - audit:export 权限（导出）
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
public class AuditLogVerificationController {

    private final AuditLogVerificationService verificationService;
    private final AuditEvidencePackageService evidencePackageService;
    private final AuditLogSamplingService samplingService;

    /**
     * 验证单条审计日志
     */
    @PostMapping("/verify")
    @PreAuthorize("hasAnyAuthority('audit:view', 'audit:verify') or hasRole('AUDIT_ADMIN')")
    @Operation(
        summary = "验证单条审计日志",
        description = """
            验证单条审计日志的哈希完整性和内容一致性。

            **验证内容:**
            - log_hash 是否与内容匹配
            - prev_log_hash 是否正确
            - 与前一条日志的哈希链连接

            **返回数据包括:**
            - valid: 是否验证通过
            - logId: 日志ID
            - expectedHash: 期望的哈希值
            - actualHash: 实际哈希值
            - errorMessage: 错误信息（验证失败时）

            **业务规则:**
            - 使用 SM3 算法重新计算哈希
            - 对比存储的 log_hash
            - 检查哈希链连续性

            **使用场景:**
            - 单条日志完整性验证
            - 日志篡改检测
            """,
        operationId = "verifySingleAuditLog",
        tags = {"审计日志验真"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<VerificationResult> verifySingleLog(
            @Parameter(description = "日志ID", required = true, example = "log-001")
            @RequestParam String logId) {
        try {
            VerificationResult result = verificationService.verifySingleLog(logId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证单条审计日志失败: logId={}", logId, e);
            return Result.fail("验证失败: " + e.getMessage());
        }
    }

    /**
     * 验证审计日志哈希链
     */
    @PostMapping("/verify-chain")
    @PreAuthorize("hasAnyAuthority('audit:view', 'audit:verify') or hasRole('AUDIT_ADMIN')")
    @Operation(
        summary = "验证审计日志哈希链",
        description = """
            验证指定时间范围内审计日志哈希链的完整性。

            **验证内容:**
            - 每条日志的 log_hash 正确性
            - 哈希链的连续性
            - 时间范围内无断链

            **请求参数:**
            - startDate: 开始日期（ISO 8601 格式）
            - endDate: 结束日期（ISO 8601 格式）
            - fondsNo: 全宗号（可选，不传则验证所有全宗）

            **返回数据包括:**
            - valid: 整体验证是否通过
            - totalCount: 总日志数
            - validCount: 验证通过数
            - invalidCount: 验证失败数
            - breakPoints: 断链位置列表
            - errors: 错误详情列表

            **业务规则:**
            - 按时间顺序验证哈希链
            - 检测断链和篡改
            - 支持全宗过滤

            **使用场景:**
            - 定期审计验证
            - 合规检查
            - 数据恢复验证
            """,
        operationId = "verifyAuditLogChain",
        tags = {"审计日志验真"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<ChainVerificationResult> verifyChain(
            @Parameter(description = "开始日期", required = true, example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", required = true, example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "全宗号", example = "F001")
            @RequestParam(required = false) String fondsNo) {
        try {
            ChainVerificationResult result = verificationService.verifyChain(startDate, endDate, fondsNo);
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证审计日志哈希链失败: startDate={}, endDate={}", startDate, endDate, e);
            return Result.fail("验证失败: " + e.getMessage());
        }
    }

    /**
     * 验证指定日志ID列表的哈希链
     */
    @PostMapping("/verify-chain-by-ids")
    @PreAuthorize("hasAnyAuthority('audit:view', 'audit:verify') or hasRole('AUDIT_ADMIN')")
    @Operation(
        summary = "验证指定日志ID列表的哈希链",
        description = """
            验证指定日志ID列表的哈希链完整性。

            **请求参数:**
            - logIds: 日志ID列表（请求体 JSON 数组）

            **返回数据包括:**
            - valid: 整体验证是否通过
            - totalCount: 总日志数
            - validCount: 验证通过数
            - invalidCount: 验证失败数
            - errors: 错误详情列表

            **业务规则:**
            - 按 ID 列表顺序验证
            - 检查相邻日志的哈希链
            - 返回每条日志的验证结果

            **使用场景:**
            - 特定范围验证
            - 问题日志定位
            """,
        operationId = "verifyChainByLogIds",
        tags = {"审计日志验真"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "验证完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<ChainVerificationResult> verifyChainByLogIds(
            @Parameter(description = "日志ID列表", required = true)
            @RequestBody List<String> logIds) {
        try {
            ChainVerificationResult result = verificationService.verifyChainByLogIds(logIds);
            return Result.success(result);
        } catch (Exception e) {
            log.error("验证审计日志哈希链失败: logIds={}", logIds, e);
            return Result.fail("验证失败: " + e.getMessage());
        }
    }

    /**
     * 导出审计证据包
     */
    @PostMapping("/export-evidence")
    @PreAuthorize("hasAnyAuthority('audit:export') or hasRole('AUDIT_ADMIN')")
    @Operation(
        summary = "导出审计证据包",
        description = """
            导出指定时间范围的审计证据包（ZIP 格式）。

            **请求参数:**
            - startDate: 开始日期（ISO 8601 格式）
            - endDate: 结束日期（ISO 8601 格式）
            - fondsNo: 全宗号（可选）
            - includeVerificationReport: 是否包含验证报告（默认 true）

            **返回数据:**
            - ZIP 文件下载
            - 包含原始日志 JSON
            - 包含验证报告
            - 包含哈希链证明

            **证据包结构:**
            ```
            evidence-package-YYYY-MM-DD-YYYY-MM-DD.zip
            ├── logs/
            │   ├── log-001.json
            │   └── log-002.json
            ├── chain-proof.json
            └── verification-report.html
            ```

            **业务规则:**
            - 时间范围不超过 90 天
            - 压缩文件大小限制 500MB
            - 支持断点续传

            **使用场景:**
            - 司法取证导出
            - 审计证据提交
            - 合规报告附件
            """,
        operationId = "exportAuditEvidencePackage",
        tags = {"审计日志验真"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "导出成功，返回 ZIP 文件"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "导出失败")
    })
    public ResponseEntity<byte[]> exportEvidencePackage(
            @Parameter(description = "开始日期", required = true, example = "2024-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", required = true, example = "2024-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "全宗号", example = "F001")
            @RequestParam(required = false) String fondsNo,
            @Parameter(description = "是否包含验证报告", example = "true")
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

    /**
     * 抽检验真
     */
    @PostMapping("/sample-verify")
    @PreAuthorize("hasAnyAuthority('audit:view', 'audit:verify') or hasRole('AUDIT_ADMIN')")
    @Operation(
        summary = "抽检验真",
        description = """
            随机抽取指定数量的审计日志进行验证。

            **请求参数:**
            - sampleSize: 抽样数量
            - startDate: 开始日期（可选，用于随机抽样范围）
            - endDate: 结束日期（可选，用于随机抽样范围）
            - criteria: 抽样条件（请求体 JSON，可选）

            **抽样条件包括:**
            - operationType: 操作类型过滤
            - userId: 用户过滤
            - fondsNo: 全宗过滤
            - minSeverity: 最低严重级别

            **返回数据包括:**
            - totalCount: 总日志数
            - sampleSize: 抽样数量
            - validCount: 验证通过数
            - invalidCount: 验证失败数
            - samples: 抽样日志详情

            **业务规则:**
            - 随机抽样保证均匀分布
            - 支持条件筛选抽样
            - 默认从全部日志中随机抽样

            **使用场景:**
            - 审计抽检
            - 合规验证
            - 数据质量检查
            """,
        operationId = "sampleAuditLogs",
        tags = {"审计日志验真"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "抽检完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<SamplingResult> sampleVerify(
            @Parameter(description = "抽样数量", required = true, example = "100")
            @RequestParam int sampleSize,
            @Parameter(description = "开始日期", example = "2024-01-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期", example = "2024-01-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "抽样条件")
            @Valid @RequestBody(required = false) SamplingCriteria criteria) {
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
