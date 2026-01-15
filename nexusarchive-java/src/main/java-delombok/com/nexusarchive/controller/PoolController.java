// Input: MyBatis-Plus、Spring Framework、Lombok、Spring Security、等
// Output: PoolController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.BatchOperationResult;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.PoolItemDto;
import com.nexusarchive.dto.MetadataUpdateDTO;
import com.nexusarchive.dto.search.CandidateSearchRequest;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.entity.ArchiveApproval;
import com.nexusarchive.service.PreArchiveCheckService;
import com.nexusarchive.service.PreArchiveSubmitService;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.PoolService;
import com.nexusarchive.service.AttachmentService;
import com.nexusarchive.service.VoucherPdfGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 电子凭证池控制器
 *
 * PRD 来源: 预归档模块
 * 提供电子凭证池的完整管理功能
 */
@Tag(name = "电子凭证池", description = """
    电子凭证池（预归档）管理接口。

    **功能说明:**
    - 元数据补录
    - 文件列表查询
    - 四性检测
    - 归档申请提交
    - 文件预览

    **预归档状态:**
    - PENDING_CHECK: 待检测
    - CHECK_FAILED: 检测失败
    - PENDING_METADATA: 待补录
    - PENDING_ARCHIVE: 待归档
    - ARCHIVED: 已归档

    **四性检测 (DA/T 92-2022):**
    - 真实性: 数字签名验证
    - 完整性: SM3 哈希校验
    - 可用性: 格式验证
    - 安全性: 病毒扫描

    **元数据字段:**
    - fiscalYear: 会计年度
    - voucherType: 凭证类型
    - creator: 制单人
    - fondsCode: 全宗号

    **文件来源:**
    - Web上传: 手动上传
    - 用友: YonSuite 同步
    - 金蝶: K/3 Cloud 同步
    - 泛微OA: OA 系统同步
    - 易快报: 费用报销
    - 汇联易: 费用管理
    - SAP: ERP 系统

    **使用场景:**
    - 凭证关联匹配
    - 元数据补录
    - 归档前检测
    - 批量归档申请

    **权限要求:**
    - archive:view 查看权限
    - archive:manage 管理权限
    """)
@SecurityRequirement(name = "Bearer Authentication")
@RestController
@RequestMapping("/pool")
@RequiredArgsConstructor
@Slf4j
public class PoolController {

    private final PreArchiveCheckService preArchiveCheckService;
    private final PreArchiveSubmitService preArchiveSubmitService;
    private final AuditLogService auditLogService;
    private final AttachmentService attachmentService;
    private final PoolService poolService;
    private final JdbcTemplate jdbcTemplate;
    private final VoucherPdfGeneratorService pdfGeneratorService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] SOURCE_SYSTEMS = {
            "Web上传", "用友", "金蝶", "泛微OA", "易快报", "汇联易", "SAP"
    };

    // ===== 候选凭证搜索 =====

    /**
     * 搜索可关联的候选凭证
     */
    @PostMapping("/candidates/search")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "搜索候选凭证",
        description = """
            按条件搜索可用于关联的候选凭证。

            **请求参数:**
            - amount: 金额（匹配范围 ±0.01）
            - docDate: 单据日期（格式: yyyy-MM-dd）
            - invoiceNumber: 发票号（模糊匹配）

            **返回数据包括:**
            - id: 文件ID
            - fileName: 文件名
            - amount: 金额
            - docDate: 单据日期
            - status: 预归档状态

            **匹配规则:**
            - 金额容差 ±0.01 元
            - 日期精确匹配
            - 发票号模糊匹配

            **使用场景:**
            - 凭证手动关联
            - 发票匹配
            """,
        operationId = "searchPoolCandidates",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "搜索成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<PoolItemDto>> searchCandidates(@Valid @RequestBody CandidateSearchRequest request) {
        log.info("API 搜索候选凭证: {}", request);
        try {
            List<PoolItemDto> results = poolService.searchCandidates(request);
            log.info("API 搜索成功, 结果条数: {}", results.size());
            return Result.success(results);
        } catch (Exception e) {
            log.error("API 搜索候选凭证失败: {}", e.getMessage(), e);
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件详情
     */
    @GetMapping("/detail/{id}")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取文件详情",
        description = """
            获取指定文件的详细信息（包含元数据）。

            **路径参数:**
            - id: 文件ID

            **返回数据包括:**
            - id: 文件ID
            - fileName: 文件名
            - fileType: 文件类型
            - fileSize: 文件大小
            - status: 预归档状态
            - createdTime: 创建时间
            - fiscalYear: 会计年度
            - voucherType: 凭证类型
            - creator: 制单人
            - fondsCode: 全宗号
            - sourceSystem: 来源系统

            **使用场景:**
            - 文件详情查看
            - 元数据展示
            """,
        operationId = "getPoolFileDetail",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    public Result<PoolItemDetailDto> getFileDetail(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String id) {
        log.info("获取文件详情: {}", id);

        ArcFileContent file = poolService.getFileById(id);
        if (file == null) {
            return Result.error("文件不存在");
        }

        PoolItemDetailDto dto = new PoolItemDetailDto();
        dto.setId(file.getId());
        dto.setFileName(file.getFileName());
        dto.setFileType(file.getFileType());
        dto.setFileSize(file.getFileSize());
        dto.setStatus(file.getPreArchiveStatus());
        dto.setCreatedTime(file.getCreatedTime());
        dto.setFiscalYear(file.getFiscalYear());
        dto.setVoucherType(file.getVoucherType());
        dto.setCreator(file.getCreator());
        dto.setFondsCode(file.getFondsCode());
        dto.setSourceSystem(file.getSourceSystem());

        return Result.success(dto);
    }

    /**
     * 获取关联附件列表
     */
    @GetMapping("/related/{id}")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取关联附件列表",
        description = """
            获取指定文件的关联附件列表。

            **路径参数:**
            - id: 主文件ID

            **业务规则:**
            - 优先通过关联表（archive_attachment）查询
            - 兼容旧的命名约定（business_doc_no + _ATT_）
            - 合并去重返回

            **返回数据包括:**
            - id: 附件文件ID
            - fileName: 文件名
            - fileSize: 文件大小
            - relationType: 关联类型

            **使用场景:**
            - 附件列表展示
            - 关联文件查看
            """,
        operationId = "getRelatedAttachments",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "主文件不存在")
    })
    public Result<List<PoolItemDto>> getRelatedFiles(
            @Parameter(description = "主文件ID", required = true)
            @PathVariable String id) {
        log.info("查询关联附件: id={}", id);
        ArcFileContent mainFile = poolService.getFileById(id);
        if (mainFile == null) {
            return Result.error("文件不存在");
        }

        // 1. 通过关联表查询 (新逻辑)
        List<ArcFileContent> linkedAttachments = attachmentService.getAttachmentsByArchive(id);

        // 2. 通过命名约定查询 (旧逻辑兼容)
        String businessDocNo = mainFile.getBusinessDocNo();
        if (businessDocNo != null && !businessDocNo.isEmpty()) {
            List<ArcFileContent> legacyAttachments = poolService.getLegacyAttachments(businessDocNo);

            // 合并并去重
            for (ArcFileContent legacy : legacyAttachments) {
                boolean exists = linkedAttachments.stream().anyMatch(a -> a.getId().equals(legacy.getId()));
                if (!exists) {
                    linkedAttachments.add(legacy);
                }
            }
        }

        List<PoolItemDto> dtos = linkedAttachments.stream()
                .map(this::convertToPoolItemDto)
                .collect(Collectors.toList());

        return Result.success(dtos);
    }

    /**
     * 更新文件元数据
     */
    @PostMapping("/metadata/update")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "METADATA_UPDATE", resourceType = "ARC_FILE_CONTENT", description = "更新预归档元数据")
    @Operation(
        summary = "更新文件元数据",
        description = """
            更新预归档文件的元数据（元数据补录）。

            **请求参数:**
            - id: 文件ID（必填）
            - fiscalYear: 会计年度
            - voucherType: 凭证类型
            - creator: 制单人
            - fondsCode: 全宗号
            - modifyReason: 修改原因（必填）

            **业务规则:**
            - 记录修改前后的值（审计日志）
            - 自动触发四性检测
            - 更新后状态保持不变

            **合规要求:**
            - GB/T 39784-2021: 审计日志记录

            **使用场景:**
            - 元数据补录
            - 信息修正
            """,
        operationId = "updatePoolMetadata",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    public Result<String> updateMetadata(
            @RequestBody @Validated MetadataUpdateDTO dto,
            HttpServletRequest request) {
        log.info("更新文件元数据: fileId={}, reason={}", dto.getId(), dto.getModifyReason());

        ArcFileContent file = poolService.getFileById(dto.getId());
        if (file == null) {
            return Result.error("文件不存在");
        }

        // 1. 记录修改前的值 (用于审计)
        String beforeValue = String.format(
                "fiscalYear=%s, voucherType=%s, creator=%s, fondsCode=%s",
                file.getFiscalYear(), file.getVoucherType(), file.getCreator(), file.getFondsCode());

        // 2. 执行更新
        jdbcTemplate.update(
            "UPDATE arc_file_content SET fiscal_year = ?, voucher_type = ?, creator = ?, fonds_code = ? WHERE id = ?",
            dto.getFiscalYear(), dto.getVoucherType(), dto.getCreator(), dto.getFondsCode(), dto.getId()
        );

        // 3. 记录审计日志 (合规要求)
        String afterValue = String.format(
                "fiscalYear=%s, voucherType=%s, creator=%s, fondsCode=%s",
                dto.getFiscalYear(), dto.getVoucherType(), dto.getCreator(), dto.getFondsCode());

        String userId = (String) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        String clientIp = request.getRemoteAddr();
        try {
            auditLogService.log(
                    userId != null ? userId : "anonymous",
                    username != null ? username : "未知用户",
                    "METADATA_UPDATE",
                    "ARC_FILE_CONTENT",
                    dto.getId(),
                    "SUCCESS",
                    "元数据补录: " + dto.getModifyReason() + " | 修改前:" + beforeValue + " | 修改后:" + afterValue,
                    clientIp);
        } catch (Exception e) {
            log.warn("审计日志记录失败: {}", e.getMessage());
        }

        // 4. 自动重新触发四性检测
        log.info("自动触发四性检测: {}", dto.getId());
        FourNatureReport report = preArchiveCheckService.checkSingleFile(dto.getId());

        return Result.success("元数据更新成功，检测结果: " + report.getStatus());
    }

    /**
     * 查询电子凭证池列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取凭证池列表",
        description = """
            获取电子凭证池的文件列表。

            **返回数据包括:**
            - id: 文件ID
            - fileName: 文件名
            - fileSize: 文件大小
            - status: 预归档状态
            - createdTime: 创建时间
            - sourceSystem: 来源系统

            **业务规则:**
            - 按创建时间倒序排列
            - 按当前用户全宗过滤

            **使用场景:**
            - 凭证池列表展示
            - 文件浏览
            """,
        operationId = "listPoolItems",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<PoolItemDto>> listPoolItems() {
        log.info("查询电子凭证池列表");
        List<PoolItemDto> poolItems = poolService.listPoolItems();
        log.info("查询到 {} 条电子凭证池记录", poolItems.size());
        return Result.success(poolItems);
    }

    /**
     * 按状态查询预归档文件
     */
    @GetMapping("/list/status/{status}")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "按状态查询文件",
        description = """
            按预归档状态查询文件列表。

            **路径参数:**
            - status: 预归档状态

            **状态枚举:**
            - PENDING_CHECK: 待检测
            - CHECK_FAILED: 检测失败
            - PENDING_METADATA: 待补录
            - PENDING_ARCHIVE: 待归档
            - ARCHIVED: 已归档

            **返回数据:**
            该状态的文件列表

            **使用场景:**
            - 状态筛选
            - 待办事项查看
            """,
        operationId = "listPoolByStatus",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<PoolItemDto>> listByStatus(
            @Parameter(description = "预归档状态", required = true,
                    schema = @Schema(allowableValues = {"PENDING_CHECK", "CHECK_FAILED", "PENDING_METADATA", "PENDING_ARCHIVE", "ARCHIVED"}))
            @PathVariable String status) {
        log.info("按状态查询预归档文件: {}", status);
        List<PoolItemDto> poolItems = poolService.listByStatus(status);
        log.info("状态 {} 共有 {} 条记录", status, poolItems.size());
        return Result.success(poolItems);
    }

    /**
     * 统计各状态数量
     */
    @GetMapping("/stats/status")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取状态统计",
        description = """
            统计各预归档状态的文件数量。

            **返回数据包括:**
            - PENDING_CHECK: 待检测数量
            - CHECK_FAILED: 检测失败数量
            - PENDING_METADATA: 待补录数量
            - PENDING_ARCHIVE: 待归档数量
            - ARCHIVED: 已归档数量

            **使用场景:**
            - 仪表盘统计
            - 状态分布查看
            """,
        operationId = "getPoolStatusStats",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<Map<String, Long>> getStatusStats() {
        log.info("统计预归档各状态数量");
        return Result.success(poolService.getStatusStats());
    }

    /**
     * 更新预归档状态
     */
    @GetMapping("/status/{id}/{status}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "STATUS_UPDATE", resourceType = "PRE_ARCHIVE", description = "更新预归档状态")
    @Operation(
        summary = "更新预归档状态",
        description = """
            更新指定文件的预归档状态。

            **路径参数:**
            - id: 文件ID
            - status: 新状态

            **业务规则:**
            - 状态变更记录审计日志
            - 不进行状态转换校验

            **使用场景:**
            - 状态手动调整
            - 批量状态更新
            """,
        operationId = "updatePoolStatus",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    public Result<String> updateStatus(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String id,
            @Parameter(description = "新状态", required = true)
            @PathVariable String status) {
        log.info("更新文件状态: {} -> {}", id, status);
        poolService.updateStatus(id, status);
        log.info("文件 {} 状态已更新为 {}", id, status);
        return Result.success("状态更新成功");
    }

    /**
     * 执行单个文件四性检测
     */
    @GetMapping("/check/{id}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "执行四性检测",
        description = """
            对指定文件执行四性检测。

            **路径参数:**
            - id: 文件ID

            **四性检测包括:**
            1. **真实性**: 数字签名验证
            2. **完整性**: SM3 哈希校验
            3. **可用性**: 格式验证
            4. **安全性**: 病毒扫描

            **返回数据包括:**
            - fileId: 文件ID
            - status: 检测状态（PASSED/FAILED）
            - authenticity: 真实性结果
            - integrity: 完整性结果
            - usability: 可用性结果
            - safety: 安全性结果
            - checkedAt: 检测时间

            **使用场景:**
            - 归档前检测
            - 单文件验证
            """,
        operationId = "checkFourNature",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检测完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    public Result<FourNatureReport> checkSingleFile(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String id) {
        log.info("执行四性检测: {}", id);
        FourNatureReport report = preArchiveCheckService.checkSingleFile(id);
        return Result.success(report);
    }

    /**
     * 批量执行四性检测
     */
    @PostMapping("/check/batch")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "批量四性检测",
        description = """
            批量执行多个文件的四性检测。

            **请求参数:**
            - fileIds: 文件ID列表

            **返回数据:**
            检测报告列表

            **业务规则:**
            - 逐个执行检测
            - 失败不影响其他文件

            **使用场景:**
            - 批量检测
            - 归档前批量验证
            """,
        operationId = "checkBatchFourNature",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检测完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<FourNatureReport>> checkBatchFiles(
            @Parameter(description = "文件ID列表", required = true)
            @RequestBody List<String> fileIds) {
        log.info("批量执行四性检测: {} 个文件", fileIds.size());
        List<FourNatureReport> reports = preArchiveCheckService.checkMultipleFiles(fileIds);
        return Result.success(reports);
    }

    /**
     * 检测所有待检测文件
     */
    @GetMapping("/check/all-pending")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "检测所有待检测文件",
        description = """
            对所有待检测状态的文件执行四性检测。

            **业务规则:**
            - 自动查询 PENDING_CHECK 状态的文件
            - 批量执行检测
            - 可能耗时较长

            **返回数据:**
            检测报告列表

            **使用场景:**
            - 定时批量检测
            - 系统维护
            """,
        operationId = "checkAllPendingFiles",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "检测完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<List<FourNatureReport>> checkAllPendingFiles() {
        log.info("检测所有待检测文件");
        List<ArcFileContent> pendingFiles = poolService.listPendingCheckFiles();
        List<String> fileIds = pendingFiles.stream()
                .map(ArcFileContent::getId)
                .collect(Collectors.toList());

        log.info("找到 {} 个待检测文件", fileIds.size());
        List<FourNatureReport> reports = preArchiveCheckService.checkMultipleFiles(fileIds);
        return Result.success(reports);
    }

    /**
     * 提交单个文件归档申请
     */
    @PostMapping("/submit/{id}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SUBMIT_ARCHIVE", resourceType = "PRE_ARCHIVE", description = "提交归档申请")
    @Operation(
        summary = "提交归档申请",
        description = """
            提交单个文件的归档审批申请。

            **路径参数:**
            - id: 文件ID

            **请求参数:**
            - applicantId: 申请人ID
            - applicantName: 申请人姓名
            - reason: 申请原因

            **业务规则:**
            - 只能提交已通过四性检测的文件
            - 创建审批记录（状态为 PENDING）
            - 记录审计日志

            **返回数据:**
            审批记录对象

            **使用场景:**
            - 单文件归档申请
            """,
        operationId = "submitForArchival",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "申请已提交"),
        @ApiResponse(responseCode = "400", description = "参数错误或状态不允许"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    public Result<ArchiveApproval> submitForArchival(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String id,
            @Valid @RequestBody SubmitRequest request) {
        log.info("提交归档申请: fileId={}", id);
        try {
            ArchiveApproval approval = preArchiveSubmitService.submitForArchival(
                    id, request.getApplicantId(), request.getApplicantName(), request.getReason());
            return Result.success(approval);
        } catch (Exception e) {
            log.error("提交归档申请失败: {}", e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 批量提交归档申请
     */
    @PostMapping("/submit/batch")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "SUBMIT_ARCHIVE_BATCH", resourceType = "PRE_ARCHIVE", description = "批量提交归档申请")
    @Operation(
        summary = "批量提交归档申请",
        description = """
            批量提交多个文件的归档审批申请。

            **请求参数:**
            - fileIds: 文件ID列表
            - applicantId: 申请人ID
            - applicantName: 申请人姓名
            - reason: 申请原因

            **返回数据包括:**
            - success: 成功数量
            - failed: 失败数量
            - results: 详细结果列表

            **使用场景:**
            - 批量归档申请
            """,
        operationId = "submitBatchForArchival",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "申请已提交"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<BatchOperationResult<ArchiveApproval>> submitBatchForArchival(
            @Valid @RequestBody BatchSubmitRequest request) {
        log.info("批量提交归档申请: {} 个文件", request.getFileIds().size());
        try {
            BatchOperationResult<ArchiveApproval> result = preArchiveSubmitService.submitBatchForArchival(
                    request.getFileIds(), request.getApplicantId(),
                    request.getApplicantName(), request.getReason());
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量提交归档申请失败: {}", e.getMessage());
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 完成归档
     */
    @PostMapping("/complete/{archiveId}")
    @PreAuthorize("hasAnyAuthority('archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @ArchivalAudit(operationType = "COMPLETE_ARCHIVE", resourceType = "ARCHIVE", description = "完成归档")
    @Operation(
        summary = "完成归档",
        description = """
            完成归档流程（审批通过后调用）。

            **路径参数:**
            - archiveId: 档案ID

            **业务规则:**
            - 只能处理已批准的申请
            - 创建正式档案记录
            - 更新预归档状态为 ARCHIVED

            **使用场景:**
            - 审批通过后完成归档
            """,
        operationId = "completeArchival",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "归档完成"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "档案不存在")
    })
    public Result<String> completeArchival(
            @Parameter(description = "档案ID", required = true)
            @PathVariable String archiveId) {
        log.info("完成归档: archiveId={}", archiveId);
        preArchiveSubmitService.completeArchival(archiveId);
        return Result.success("归档完成");
    }

    /**
     * 预览文件
     */
    @GetMapping("/preview/{id}")
    @PreAuthorize("hasAnyAuthority('archive:view','archive:manage','nav:all') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "预览文件",
        description = """
            在线预览文件内容。

            **路径参数:**
            - id: 文件ID

            **支持格式:**
            - PDF: application/pdf
            - OFD: application/ofd
            - 图片: image/jpeg, image/png
            - XML: text/xml

            **业务规则:**
            - 先查 arc_file_content
            - 找不到则查 arc_original_voucher_file
            - PDF 不存在时尝试实时生成

            **返回数据:**
            文件流（inline 展示）

            **使用场景:**
            - 在线预览
            - 文件查看
            """,
        operationId = "previewPoolFile",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "文件内容"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    public ResponseEntity<Resource> previewFile(
            @Parameter(description = "文件ID", required = true)
            @PathVariable String id) {
        log.info("请求预览文件: {}", id);

        String storagePath = null;
        String fileName = null;

        // 1. 先查 arc_file_content
        ArcFileContent fileContent = poolService.getFileById(id);
        if (fileContent != null) {
            storagePath = fileContent.getStoragePath();
            fileName = fileContent.getFileName();
        } else {
            // 2. 如果找不到，查 arc_original_voucher_file (智能匹配关联的文件)
            log.debug("arc_file_content 未找到 {}, 尝试查询 arc_original_voucher_file", id);
            try {
                String sql = "SELECT storage_path, file_name FROM arc_original_voucher_file WHERE id = ? AND deleted = 0";
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);

                if (!rows.isEmpty()) {
                    storagePath = (String) rows.get(0).get("storage_path");
                    fileName = (String) rows.get(0).get("file_name");
                    log.info("从 arc_original_voucher_file 找到文件: {} -> {}", id, storagePath);
                }
            } catch (Exception e) {
                log.debug("查询 arc_original_voucher_file 失败: {}", e.getMessage());
            }

            if (storagePath == null) {
                log.error("文件不存在: {}", id);
                return ResponseEntity.notFound().build();
            }
        }

        try {
            Path filePath = Paths.get(storagePath);
            Resource resource = new UrlResource(filePath.toUri());

            // 如果文件不存在，但在 arc_file_content 表记录存在，且是 PDF，则尝试实时生成
            if (!resource.exists() && fileContent != null) {
                if (fileName.toLowerCase().endsWith(".pdf")) {
                    log.info("PDF 文件未找到，尝试实时生成: {}", filePath);
                    try {
                        // 优先使用数据库中保存的原始JSON数据
                        String sourceData = fileContent.getSourceData();
                        String voucherJson = (sourceData != null && !sourceData.isEmpty()) ? sourceData : "{}";

                        pdfGeneratorService.generatePdfForPreArchive(id, voucherJson);
                        log.debug("PDF 实时生成完成");
                        resource = new UrlResource(filePath.toUri()); // 重新加载
                    } catch (Exception e) {
                        log.error("实时生成 PDF 失败", e);
                    }
                }
            }

            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";
                String fileNameLower = fileName.toLowerCase();
                if (fileNameLower.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileNameLower.endsWith(".ofd")) {
                    contentType = "application/ofd";
                } else if (fileNameLower.endsWith(".jpg") || fileNameLower.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileNameLower.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileNameLower.endsWith(".xml")) {
                    contentType = "text/xml";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                log.error("文件无法读取: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("文件路径错误", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 生成演示数据
     */
    @GetMapping("/generate-demo")
    @Operation(
        summary = "生成演示数据",
        description = """
            生成演示用的预归档数据。

            **业务规则:**
            - 清理旧的演示数据
            - 生成 10 条新记录
            - 使用模板文件复制

            **生成的数据包括:**
            - PDF 文件（从模板复制）
            - 元数据索引
            - 随机来源系统

            **注意事项:**
            - 仅用于演示/测试环境
            - 生产环境应禁用

            **使用场景:**
            - 演示环境初始化
            - 功能测试
            """,
        operationId = "generateDemoData",
        tags = {"电子凭证池"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "生成成功"),
        @ApiResponse(responseCode = "500", description = "生成失败")
    })
    public Result<String> generateDemoData() {
        log.info("开始生成演示数据...");

        try {
            ClassPathResource templateResource = new ClassPathResource("templates/default_voucher.pdf");
            if (!templateResource.exists()) {
                return Result.error("模板文件不存在(classpath): templates/default_voucher.pdf");
            }
            Random random = new Random();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateStr = LocalDateTime.now().format(dateFormatter);

            // 1. 清理旧的演示数据
            int deletedCount = poolService.cleanupDemoData();
            log.info("已清理 {} 条旧演示数据", deletedCount);

            // 2. 生成新数据
            for (int i = 0; i < 10; i++) {
                String fileId = UUID.randomUUID().toString();
                String targetFileName = fileId + ".pdf";
                Path targetPath = Paths.get("/tmp/nexusarchive/uploads", targetFileName);

                // 确保目录存在
                Files.createDirectories(targetPath.getParent());

                // 复制模板文件 (从Classpath读取)
                try (java.io.InputStream is = templateResource.getInputStream()) {
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }

                // 使用固定金额 (与模板 default_voucher.pdf 一致)
                BigDecimal amount = new BigDecimal("43758.00");

                // 随机来源系统 (0-6)
                int sourceIndex = random.nextInt(SOURCE_SYSTEMS.length);

                // 创建记录
                ArcFileContent content = ArcFileContent.builder()
                        .id(fileId)
                        .archivalCode("TEMP-POOL-" + dateStr + "-" + fileId.substring(0, 8).toUpperCase())
                        .fileName("凭证_" + dateStr + "_" + (1000 + i) + ".pdf")
                        .fileType("PDF")
                        .fileSize(Files.size(targetPath))
                        .fileHash("DEMO_HASH_" + fileId.substring(0, 8) + "_" + sourceIndex) // 演示数据用伪哈希 + 来源索引
                        .hashAlgorithm("SHA-256")
                        .storagePath(targetPath.toString())
                        .createdTime(LocalDateTime.now().minusMinutes(random.nextInt(60)))
                        .build();

                poolService.insertDemoFile(content);

                // 创建元数据索引 (包含金额)
                ArcFileMetadataIndex metadata = ArcFileMetadataIndex.builder()
                        .fileId(fileId)
                        .totalAmount(amount)
                        .invoiceNumber("INV-" + dateStr + "-" + (1000 + i))
                        .issueDate(java.time.LocalDate.now())
                        .sellerName("演示供应商 " + (char) ('A' + random.nextInt(26)))
                        .parsedTime(LocalDateTime.now())
                        .parserType("DEMO_GENERATOR")
                        .build();
                poolService.insertDemoMetadata(metadata);
            }

            return Result.success("成功生成10条演示数据");
        } catch (Exception e) {
            log.error("生成演示数据失败", e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }

    /**
     * 转换实体为DTO
     */
    private PoolItemDto convertToPoolItemDto(ArcFileContent fileContent) {
        return poolService.convertToPoolItemDto(fileContent);
    }

    // ===== DTO 类 =====

    /**
     * 文件详情 DTO
     */
    @lombok.Data
    public static class PoolItemDetailDto {
        private String id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String status;
        private LocalDateTime createdTime;
        private String fiscalYear;
        private String voucherType;
        private String creator;
        private String fondsCode;
        private String sourceSystem;
    }

    /**
     * 提交申请请求 DTO
     */
    @lombok.Data
    public static class SubmitRequest {
        private String applicantId;
        private String applicantName;
        private String reason;
    }

    /**
     * 批量提交申请请求 DTO
     */
    @lombok.Data
    public static class BatchSubmitRequest {
        private List<String> fileIds;
        private String applicantId;
        private String applicantName;
        private String reason;
    }
}
