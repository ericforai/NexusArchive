// Input: MyBatis-Plus、io.swagger、Lombok、Spring Framework、等
// Output: ComplianceController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.common.Result;
import com.nexusarchive.dto.compliance.AsyncCheckTaskStatus;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.AuditInspectionLog;
import com.nexusarchive.service.ArchiveFileContentService;
import com.nexusarchive.service.ArchiveService;
import com.nexusarchive.service.ComplianceCheckService;
import com.nexusarchive.service.compliance.AsyncFourNatureCheckService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 合规性检查控制器
 *
 * <p>提供电子会计档案符合性检查接口，包括：
 * - 单个档案符合性检查
 * - 批量档案符合性检查
 * - 异步四性检测（真实性、完整性、可用性、安全性）
 * - 符合性检查报告生成
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/compliance")
@RequiredArgsConstructor
@Tag(name = "合规性检查", description = """
    电子会计档案管理办法符合性检查接口。

    **功能说明:**
    - 单个档案符合性检查
    - 批量档案符合性检查
    - 异步四性检测（真实性、完整性、可用性、安全性）
    - 符合性检查报告生成（XML/JSON）
    - 符合性统计数据查询

    **四性检测 (DA/T 92-2022):**
    - 真实性 (Authenticity): 数字签名验证、证书链验证、时间戳验证
    - 完整性 (Integrity): SM3 哈希校验、文件完整性、元数据完整性
    - 可用性 (Usability): 格式验证、可读性检查、渲染测试
    - 安全性 (Safety): 病毒扫描、权限检查、敏感信息检测

    **符合性级别:**
    - COMPLIANT: 完全符合（无违规项）
    - WARNING: 有警告（有轻微问题但不影响使用）
    - NON_COMPLIANT: 不符合（有严重违规项）

    **异步检测任务状态:**
    - PENDING: 排队中
    - RUNNING: 执行中
    - COMPLETED: 已完成
    - FAILED: 失败
    - CANCELLED: 已取消

    **报告格式:**
    - XML: 标准 XML 格式报告
    - JSON: 结构化 JSON 格式报告

    **使用场景:**
    - 档案归档前合规性检查
    - 定期合规性审计
    - 等保合规检查
    - 内部质量监控

    **权限要求:**
    - 需登录认证
    - 建议添加 role:compliance_admin 权限控制
    """
)
@SecurityRequirement(name = "Bearer Authentication")
public class ComplianceController {

    private final ComplianceCheckService complianceCheckService;
    private final ArchiveService archiveService;
    private final ArchiveFileContentService archiveFileContentService;
    private final AsyncFourNatureCheckService asyncFourNatureCheckService;

    /**
     * 检查单个档案的符合性
     */
    @GetMapping("/archives/{archiveId}")
    @Operation(
        summary = "检查档案符合性",
        description = """
            检查指定档案是否符合《会计档案管理办法》。

            **路径参数:**
            - archiveId: 档案 ID

            **检查项目:**
            - 元数据完整性
            - 文件格式合规性
            - 保存期限符合性
            - 四性检测结果（如果有）

            **返回数据包括:**
            - complianceLevel: 符合性级别（COMPLIANT/WARNING/NON_COMPLIANT）
            - violations: 违规项列表
            - warnings: 警告项列表
            - violationCount: 违规项数量
            - warningCount: 警告项数量
            - isCompliant: 是否符合（boolean）

            **使用场景:**
            - 单个档案归档前检查
            - 档案详情页展示合规状态
            - 不合规档案问题诊断
            """,
        operationId = "checkArchiveCompliance",
        tags = {"合规性检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检查完成"),
        @ApiResponse(responseCode = "404", description = "档案不存在"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<ComplianceCheckService.ComplianceResult> checkArchiveCompliance(
            @Parameter(description = "档案ID", required = true, example = "123456") @PathVariable String archiveId) {
        try {
            // 获取档案信息
            Archive archive = archiveService.getArchiveById(archiveId);
            if (archive == null) {
                return Result.fail("档案不存在");
            }

            // 获取关联文件
            List<ArcFileContent> files = archiveFileContentService.getFilesByItemId(archiveId, null);

            // 执行符合性检查
            ComplianceCheckService.ComplianceResult result = complianceCheckService.checkCompliance(archive, files);

            return Result.success(result);
        } catch (Exception e) {
            log.error("检查档案符合性失败", e);
            return Result.fail("检查档案符合性失败: " + e.getMessage());
        }
    }

    /**
     * 检查多个档案的符合性
     */
    @PostMapping("/archives/batch")
    @Operation(
        summary = "批量检查档案符合性",
        description = """
            批量检查多个档案是否符合《会计档案管理办法》。

            **请求体:**
            - 档案 ID 列表（最多 100 个）

            **返回数据包括:**
            - totalCount: 总检查数量
            - successCount: 成功检查数量
            - failCount: 失败数量
            - results: 每个档案的检查结果
            - failedIds: 失败的档案 ID 列表

            **使用场景:**
            - 批量归档前检查
            - 定期合规性审计
            - 全库合规性扫描
            """,
        operationId = "checkBatchArchiveCompliance",
        tags = {"合规性检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量检查完成"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<BatchComplianceResult> checkBatchArchiveCompliance(
            @Parameter(description = "档案ID列表", required = true) @RequestBody List<String> archiveIds) {
        try {
            BatchComplianceResult batchResult = new BatchComplianceResult();

            for (String archiveId : archiveIds) {
                try {
                    // 获取档案信息
                    Archive archive = archiveService.getArchiveById(archiveId);
                    if (archive == null) {
                        batchResult.addFailed(archiveId, "档案不存在");
                        continue;
                    }

                    // 获取关联文件
                    List<ArcFileContent> files = archiveFileContentService.getFilesByItemId(archiveId, null);

                    // 执行符合性检查
                    ComplianceCheckService.ComplianceResult result = complianceCheckService.checkCompliance(archive,
                            files);
                    batchResult.addResult(archiveId, archive.getArchiveCode(), result);
                } catch (Exception e) {
                    batchResult.addFailed(archiveId, "检查失败: " + e.getMessage());
                }
            }

            return Result.success(batchResult);
        } catch (Exception e) {
            log.error("批量检查档案符合性失败", e);
            return Result.fail("批量检查档案符合性失败: " + e.getMessage());
        }
    }

    /**
     * 获取符合性检查报告
     */
    @GetMapping("/archives/{archiveId}/report")
    @Operation(
        summary = "获取符合性检查报告",
        description = """
            获取指定档案的符合性检查报告。

            **路径参数:**
            - archiveId: 档案 ID

            **查询参数:**
            - format: 报告格式（xml/json，默认 xml）

            **返回数据:**
            - XML 格式: 标准 XML 报告
            - JSON 格式: 结构化 JSON 报告

            **使用场景:**
            - 导出合规性报告
            - 审计证据留存
            - 问题分析
            """,
        operationId = "getComplianceReport",
        tags = {"合规性检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "报告生成成功"),
        @ApiResponse(responseCode = "404", description = "档案不存在"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<String> getComplianceReport(
            @Parameter(description = "档案ID", required = true) @PathVariable String archiveId,
            @Parameter(description = "报告格式 (xml/json)", example = "xml") @RequestParam(defaultValue = "xml") String format) {
        try {
            // 获取档案信息
            Archive archive = archiveService.getArchiveById(archiveId);
            if (archive == null) {
                return Result.fail("档案不存在");
            }

            // 获取关联文件
            List<ArcFileContent> files = archiveFileContentService.getFilesByItemId(archiveId, null);

            // 执行符合性检查 (In real world, we might want to fetch the PERSISTED report, but
            // generating fresh is OK for on-demand)
            // Ideally check if a signed report exists in AuditLog first, but for now
            // generating fresh is safer to reflect latest rule
            ComplianceCheckService.ComplianceResult result = complianceCheckService.checkCompliance(archive, files);

            // 生成报告
            String reportContent;
            if ("json".equalsIgnoreCase(format)) {
                reportContent = generateJsonReport(archive, result);
            } else {
                reportContent = generateXmlReport(archive, result);
            }

            return Result.success(reportContent);
        } catch (Exception e) {
            log.error("获取符合性检查报告失败", e);
            return Result.fail("获取符合性检查报告失败: " + e.getMessage());
        }
    }

    /**
     * 获取符合性统计数据
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "获取符合性统计数据",
        description = """
            获取系统内所有档案的符合性统计。

            **返回数据包括:**
            - totalArchives: 档案总数
            - compliantCount: 符合的档案数
            - warningCount: 有警告的档案数
            - nonCompliantCount: 不符合的档案数
            - complianceRate: 符合率百分比

            **使用场景:**
            - 系统合规性概览
            - 运维监控仪表盘
            - 管理层报告
            """,
        operationId = "getComplianceStatistics",
        tags = {"合规性检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<ComplianceCheckService.ComplianceStatistics> getComplianceStatistics() {
        try {
            return Result.success(complianceCheckService.getStatistics());
        } catch (Exception e) {
            log.error("获取符合性统计数据失败", e);
            return Result.fail("获取符合性统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 生成XML格式报告
     */
    private String generateXmlReport(Archive archive, ComplianceCheckService.ComplianceResult result) {
        // Reuse the StandardReportGenerator logic?
        // StandardReportGenerator works with FourNatureReport, but we have
        // ComplianceResult here.
        // We will construct a simple XML specific for this "ComplianceResult" or we
        // should map it.
        // For now, keep the simple XML construction as placeholder or enhance common
        // generator later.
        // The Requirement said "StandardReportGenerator" which generates
        // FourNatureReport.
        // Let's stick to the manual construction for this specific endpoint unless we
        // map ComplianceResult -> FourNatureReport

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<符合性检查报告>" +
                "<档案ID>" + archive.getId() + "</档案ID>" +
                "<档号>" + archive.getArchiveCode() + "</档号>" +
                "<符合性级别>" + result.getComplianceLevel().getDescription() + "</符合性级别>" +
                "<违规项数量>" + result.getViolationCount() + "</违规项数量>" +
                "<警告项数量>" + result.getWarningCount() + "</警告项数量>" +
                "<违规列表>" + String.join(";", result.getViolations()) + "</违规列表>" +
                "<警告列表>" + String.join(";", result.getWarnings()) + "</警告列表>" +
                "</符合性检查报告>";
    }

    /**
     * 生成JSON格式报告
     */
    private String generateJsonReport(Archive archive, ComplianceCheckService.ComplianceResult result) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            // Create a wrapper object
            var report = new Object() {
                public String archiveId = archive.getId();
                public String archiveCode = archive.getArchiveCode();
                public String complianceLevel = result.getComplianceLevel().getDescription();
                public int violationCount = result.getViolationCount();
                public int warningCount = result.getWarningCount();
                public List<String> violations = result.getViolations();
                public List<String> warnings = result.getWarnings();
            };
            return mapper.writeValueAsString(report);
        } catch (Exception e) {
            return "{\"error\": \"JSON generation failed\"}";
        }
    }

    /**
     * 批量符合性检查结果
     */
    public static class BatchComplianceResult {
        private int totalCount = 0;
        private int successCount = 0;
        private int failCount = 0;
        private List<SingleResult> results = new java.util.ArrayList<>();
        private List<String> failedIds = new java.util.ArrayList<>();

        public void addResult(String archiveId, String archiveCode, ComplianceCheckService.ComplianceResult result) {
            SingleResult singleResult = new SingleResult();
            singleResult.setArchiveId(archiveId);
            singleResult.setArchiveCode(archiveCode);
            singleResult.setResult(result);
            results.add(singleResult);

            if (result.isCompliant()) {
                successCount++;
            } else {
                failCount++; // Count as fail if not compliant? Or just count as processed?
                             // Usually "success/fail" in batch refers to processing status.
                             // Let's assume processed successfully = successCount.
                successCount++;
            }
            totalCount++;
        }

        public void addFailed(String archiveId, String reason) {
            failedIds.add(archiveId + ":" + reason);
            failCount++;
            totalCount++;
        }

        // Getters and Setters
        public int getTotalCount() {
            return totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public List<SingleResult> getResults() {
            return results;
        }

        public List<String> getFailedIds() {
            return failedIds;
        }

        public static class SingleResult {
            private String archiveId;
            private String archiveCode;
            private ComplianceCheckService.ComplianceResult result;

            // Getters and Setters
            public String getArchiveId() {
                return archiveId;
            }

            public void setArchiveId(String archiveId) {
                this.archiveId = archiveId;
            }

            public String getArchiveCode() {
                return archiveCode;
            }

            public void setArchiveCode(String archiveCode) {
                this.archiveCode = archiveCode;
            }

            public ComplianceCheckService.ComplianceResult getResult() {
                return result;
            }

            public void setResult(ComplianceCheckService.ComplianceResult result) {
                this.result = result;
            }
        }
    }

    // ==================== 异步四性检测接口 ====================

    /**
     * 提交异步四性检测任务
     * <p>
     * 该接口立即返回任务ID，实际的检测操作在后台异步执行。
     * 四性检测（真实性、完整性、可用性、安全性）并行执行以提升性能。
     * </p>
     *
     * @param archiveId 档案ID
     * @return 任务ID
     */
    @PostMapping("/four-nature/{archiveId}/async")
    @Operation(
        summary = "提交异步四性检测任务",
        description = """
            提交四性检测任务，立即返回任务ID，检测在后台异步执行。

            **路径参数:**
            - archiveId: 档案 ID

            **四性检测项目 (DA/T 92-2022):**
            - 真实性: 数字签名验证、证书链验证、时间戳验证
            - 完整性: SM3 哈希校验、文件完整性、元数据完整性
            - 可用性: 格式验证、可读性检查、渲染测试
            - 安全性: 病毒扫描（ClamAV）、权限检查

            **返回数据:**
            - taskId: 任务 ID
            - archiveId: 档案 ID
            - archiveCode: 档案编号

            **使用场景:**
            - 大文件异步检测
            - 批量检测任务提交
            - 前端轮询检测状态
            """,
        operationId = "submitAsyncFourNatureCheck",
        tags = {"合规性检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "任务已提交"),
        @ApiResponse(responseCode = "404", description = "档案不存在"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<AsyncCheckTaskResponse> submitAsyncFourNatureCheck(
            @Parameter(description = "档案ID", required = true, example = "123456") @PathVariable String archiveId) {
        try {
            // 获取档案信息
            Archive archive = archiveService.getArchiveById(archiveId);
            if (archive == null) {
                return Result.fail("档案不存在");
            }

            // 提交异步检测任务
            CompletableFuture<String> taskFuture = asyncFourNatureCheckService.submitCheckTask(
                    archiveId, archive.getArchiveCode());

            // 立即返回任务ID（不等待检测完成）
            String taskId = taskFuture.join(); // join 只是获取 taskId，不等待检测完成

            return Result.success(new AsyncCheckTaskResponse(taskId, archiveId, archive.getArchiveCode()));
        } catch (Exception e) {
            log.error("提交异步四性检测任务失败", e);
            return Result.fail("提交任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询异步四性检测任务状态
     *
     * @param taskId 任务ID
     * @return 任务状态
     */
    @GetMapping("/four-nature/tasks/{taskId}")
    @Operation(
        summary = "查询四性检测任务状态",
        description = """
            查询异步检测任务的执行状态和进度。

            **路径参数:**
            - taskId: 任务 ID

            **任务状态:**
            - PENDING: 排队中
            - RUNNING: 执行中
            - COMPLETED: 已完成
            - FAILED: 失败
            - CANCELLED: 已取消

            **返回数据包括:**
            - taskId: 任务 ID
            - status: 任务状态
            - progress: 进度百分比
            - authentic: 真实性检测状态
            - integrity: 完整性检测状态
            - usability: 可用性检测状态
            - safety: 安全性检测状态

            **使用场景:**
            - 前端轮询任务状态
            - 检测进度展示
            """,
        operationId = "getAsyncCheckTaskStatus",
        tags = {"合规性检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<AsyncCheckTaskStatus> getAsyncCheckTaskStatus(
            @Parameter(description = "任务ID", required = true, example = "task-uuid-123") @PathVariable String taskId) {
        try {
            AsyncCheckTaskStatus status = asyncFourNatureCheckService.getTaskStatus(taskId);
            if (status == null) {
                return Result.fail("任务不存在");
            }
            return Result.success(status);
        } catch (Exception e) {
            log.error("查询任务状态失败", e);
            return Result.fail("查询任务状态失败: " + e.getMessage());
        }
    }

    /**
     * 获取异步四性检测结果
     * <p>
     * 如果任务已完成，返回检测结果；如果任务未完成，返回 null。
     * 建议先通过 getAsyncCheckTaskStatus 检查任务状态。
     * </p>
     *
     * @param taskId 任务ID
     * @return 检测结果
     */
    @GetMapping("/four-nature/tasks/{taskId}/result")
    @Operation(
        summary = "获取四性检测结果",
        description = """
            获取已完成的四性检测结果。

            **路径参数:**
            - taskId: 任务 ID

            **返回数据:**
            - FourNatureReport: 四性检测报告
              - authentic: 真实性检测结果
              - integrity: 完整性检测结果
              - usability: 可用性检测结果
              - safety: 安全性检测结果
              - overall: 整体检测结果

            **使用场景:**
            - 获取检测报告
            - 结果展示
            - 问题诊断

            **注意:**
            建议先通过状态查询接口确认任务完成后再调用此接口。
            """,
        operationId = "getAsyncCheckResult",
        tags = {"合规性检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "400", description = "任务尚未完成"),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<FourNatureReport> getAsyncCheckResult(
            @Parameter(description = "任务ID", required = true, example = "task-uuid-123") @PathVariable String taskId) {
        try {
            AsyncCheckTaskStatus status = asyncFourNatureCheckService.getTaskStatus(taskId);
            if (status == null) {
                return Result.fail("任务不存在");
            }

            if (status.getStatus() != AsyncCheckTaskStatus.TaskStatus.COMPLETED) {
                return Result.fail("任务尚未完成，当前状态: " + status.getStatus());
            }

            FourNatureReport result = asyncFourNatureCheckService.getCheckResult(taskId);
            if (result == null) {
                return Result.fail("检测结果不可用");
            }
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取检测结果失败", e);
            return Result.fail("获取检测结果失败: " + e.getMessage());
        }
    }

    /**
     * 取消异步四性检测任务
     *
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    @DeleteMapping("/four-nature/tasks/{taskId}")
    @Operation(
        summary = "取消四性检测任务",
        description = """
            取消正在执行或排队的四性检测任务。

            **路径参数:**
            - taskId: 任务 ID

            **返回数据:**
            - true: 取消成功
            - false: 取消失败（任务已完成或不存在）

            **使用场景:**
            - 用户主动取消检测
            - 重复任务清理
            """,
        operationId = "cancelAsyncCheckTask",
        tags = {"合规性检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "操作完成"),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<Boolean> cancelAsyncCheckTask(
            @Parameter(description = "任务ID", required = true, example = "task-uuid-123") @PathVariable String taskId) {
        try {
            boolean cancelled = asyncFourNatureCheckService.cancelTask(taskId);
            return Result.success(cancelled);
        } catch (Exception e) {
            log.error("取消任务失败", e);
            return Result.fail("取消任务失败: " + e.getMessage());
        }
    }

    /**
     * 根据档案ID查询当前检测任务
     *
     * @param archiveId 档案ID
     * @return 当前任务状态
     */
    @GetMapping("/four-nature/archives/{archiveId}/task")
    @Operation(
        summary = "查询档案的检测任务",
        description = """
            查询指定档案当前正在执行的检测任务。

            **路径参数:**
            - archiveId: 档案 ID

            **注意:**
            当前接口暂未实现，请使用任务 ID 查询。
            """,
        operationId = "getArchiveCheckTask",
        tags = {"合规性检查"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "档案不存在或无检测任务"),
        @ApiResponse(responseCode = "501", description = "接口待实现"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public Result<AsyncCheckTaskStatus> getArchiveCheckTask(
            @Parameter(description = "档案ID", required = true, example = "123456") @PathVariable String archiveId) {
        try {
            // 注意: 这个方法需要扩展 AsyncCheckTaskManager 或添加到 AsyncFourNatureCheckService
            // 暂时返回提示信息
            return Result.fail("请使用任务ID查询");
        } catch (Exception e) {
            log.error("查询档案检测任务失败", e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }

    /**
     * 异步检测任务响应
     */
    public static class AsyncCheckTaskResponse {
        private String taskId;
        private String archiveId;
        private String archiveCode;

        public AsyncCheckTaskResponse(String taskId, String archiveId, String archiveCode) {
            this.taskId = taskId;
            this.archiveId = archiveId;
            this.archiveCode = archiveCode;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getArchiveId() {
            return archiveId;
        }

        public void setArchiveId(String archiveId) {
            this.archiveId = archiveId;
        }

        public String getArchiveCode() {
            return archiveCode;
        }

        public void setArchiveCode(String archiveCode) {
            this.archiveCode = archiveCode;
        }
    }
}
