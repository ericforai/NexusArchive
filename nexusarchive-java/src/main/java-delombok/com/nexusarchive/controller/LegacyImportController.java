// Input: LegacyImportService、Result、MultipartFile、Spring Security、Spring Framework
// Output: LegacyImportController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.FieldMappingConfig;
import com.nexusarchive.dto.request.ImportPreviewResult;
import com.nexusarchive.dto.request.ImportResult;
import com.nexusarchive.entity.LegacyImportTask;
import com.nexusarchive.service.LegacyImportService;
import com.nexusarchive.service.LegacyImportTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 历史数据导入控制器
 *
 * PRD 来源: openspec-legacy-data-import.md
 * 提供历史档案数据导入功能
 *
 * <p>支持从旧系统迁移数据到新系统</p>
 */
@Tag(name = "历史数据导入", description = """
    历史档案数据导入接口。

    **功能说明:**
    - 预览导入数据
    - 执行数据导入
    - 查询导入历史
    - 下载错误报告
    - 下载导入模板

    **支持的文件格式:**
    - CSV: 逗号分隔值文件（UTF-8 编码）
    - Excel: .xlsx 格式（.xls 不支持）

    **导入流程:**
    1. 下载模板或自定义字段映射
    2. 填充数据文件
    3. 预览导入数据
    4. 执行导入
    5. 查看结果和错误报告

    **字段映射:**
    - 支持自定义源字段到目标字段的映射
    - 映射配置以 JSON 格式传递
    - 预览时可验证映射正确性

    **导入结果:**
    - totalCount: 总记录数
    - successCount: 成功导入数
    - failedCount: 失败数
    - errors: 错误详情列表

    **任务状态:**
    - PENDING: 待处理
    - PROCESSING: 处理中
    - COMPLETED: 已完成
    - FAILED: 失败
    - PARTIAL: 部分成功

    **使用场景:**
    - 旧系统数据迁移
    - 批量档案导入
    - 历史数据补录

    **权限要求:**
    - admin:import 权限
    - archive:import 权限
    - SYSTEM_ADMIN 角色
    """)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/legacy-import")
@RequiredArgsConstructor
public class LegacyImportController {

    private final LegacyImportService legacyImportService;
    private final LegacyImportTemplateService legacyImportTemplateService;
    private final ObjectMapper objectMapper;

    /**
     * 预览导入数据
     */
    @PostMapping("/preview")
    @PreAuthorize("hasAnyAuthority('admin:import', 'archive:import') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "预览导入数据",
        description = """
            上传文件并预览导入数据，不执行实际导入。

            **请求参数:**
            - file: 导入文件（multipart/form-data）
            - mappingConfig: 字段映射配置（JSON 字符串，可选）

            **字段映射配置示例:**
            ```json
            {
              "mappings": [
                {"source": "档案号", "target": "archiveCode"},
                {"source": "题名", "target": "title"},
                {"source": "日期", "target": "docDate", "format": "yyyy-MM-dd"}
              ]
            }
            ```

            **返回数据包括:**
            - totalCount: 解析的记录总数
            - previewRecords: 预览记录列表（前 10 条）
            - mappedFields: 映射后的字段列表
            - warnings: 警告信息（格式问题等）
            - errors: 错误信息

            **业务规则:**
            - 仅解析验证，不写入数据库
            - 最多返回 10 条预览记录
            - 自动检测文件编码

            **使用场景:**
            - 导入前验证数据格式
            - 检查字段映射配置
            """,
        operationId = "previewImport",
        tags = {"历史数据导入"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "预览成功"),
        @ApiResponse(responseCode = "400", description = "文件格式错误或解析失败"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<ImportPreviewResult> previewImport(
            @Parameter(description = "导入文件（CSV/Excel）", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "字段映射配置（JSON 字符串）")
            @RequestParam(value = "mappingConfig", required = false) String mappingConfigJson) {
        try {
            FieldMappingConfig mappingConfig = null;
            if (mappingConfigJson != null && !mappingConfigJson.isEmpty()) {
                mappingConfig = objectMapper.readValue(mappingConfigJson, FieldMappingConfig.class);
            }

            ImportPreviewResult result = legacyImportService.previewImport(file, mappingConfig);
            return Result.success("预览成功", result);
        } catch (Exception e) {
            log.error("预览失败: {}", e.getMessage(), e);
            return Result.error("预览失败: " + e.getMessage());
        }
    }

    /**
     * 执行数据导入
     */
    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('admin:import', 'archive:import') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "执行数据导入",
        description = """
            上传文件并执行数据导入到系统。

            **请求参数:**
            - file: 导入文件（multipart/form-data）
            - mappingConfig: 字段映射配置（JSON 字符串，可选）

            **字段映射配置示例:**
            ```json
            {
              "mappings": [
                {"source": "档案号", "target": "archiveCode"},
                {"source": "题名", "target": "title"}
              ]
            }
            ```

            **返回数据包括:**
            - taskId: 导入任务ID
            - totalCount: 总记录数
            - successCount: 成功导入数
            - failedCount: 失败数
            - errorCount: 错误记录数
            - status: 任务状态
            - message: 结果消息

            **业务规则:**
            - 创建导入任务记录
            - 异步处理大文件（>1000 条）
            - 自动记录当前操作人
            - 使用当前全宗上下文

            **使用场景:**
            - 执行历史数据迁移
            - 批量档案导入
            """,
        operationId = "importLegacyData",
        tags = {"历史数据导入"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "导入任务已创建"),
        @ApiResponse(responseCode = "400", description = "文件格式错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<ImportResult> importData(
            @Parameter(description = "导入文件（CSV/Excel）", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "字段映射配置（JSON 字符串）")
            @RequestParam(value = "mappingConfig", required = false) String mappingConfigJson) {
        try {
            String operatorId = getCurrentUserId();
            String fondsNo = getCurrentFondsNo();

            FieldMappingConfig mappingConfig = null;
            if (mappingConfigJson != null && !mappingConfigJson.isEmpty()) {
                mappingConfig = objectMapper.readValue(mappingConfigJson, FieldMappingConfig.class);
            }

            ImportResult result = legacyImportService.importLegacyData(file, mappingConfig, operatorId, fondsNo);
            return Result.success("导入完成", result);
        } catch (Exception e) {
            log.error("导入失败: {}", e.getMessage(), e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    /**
     * 查询导入历史
     */
    @GetMapping("/tasks")
    @PreAuthorize("hasAnyAuthority('admin:import', 'archive:import') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "查询导入历史",
        description = """
            分页查询导入任务历史记录。

            **请求参数:**
            - page: 页码（从 1 开始，默认 1）
            - size: 每页大小（默认 20）
            - status: 状态过滤（可选）

            **状态值:**
            - PENDING: 待处理
            - PROCESSING: 处理中
            - COMPLETED: 已完成
            - FAILED: 失败
            - PARTIAL: 部分成功

            **返回数据包括:**
            - records: 任务记录列表
            - total: 总记录数
            - size: 每页大小
            - current: 当前页码

            **任务记录包括:**
            - taskId: 任务ID
            - fileName: 文件名
            - status: 状态
            - totalCount: 总记录数
            - successCount: 成功数
            - failedCount: 失败数
            - operatorId: 操作人
            - createdAt: 创建时间

            **使用场景:**
            - 查看导入历史
            - 追踪导入任务状态
            """,
        operationId = "getImportTasks",
        tags = {"历史数据导入"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public Result<Page<LegacyImportTask>> getImportTasks(
            @Parameter(description = "页码", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "状态过滤", example = "COMPLETED")
            @RequestParam(required = false) String status) {
        try {
            Page<LegacyImportTask> tasks = legacyImportService.getImportTasks(page, size, status);
            return Result.success(tasks);
        } catch (Exception e) {
            log.error("查询导入历史失败: {}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取导入任务详情
     */
    @GetMapping("/tasks/{importId}")
    @PreAuthorize("hasAnyAuthority('admin:import', 'archive:import') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "获取导入任务详情",
        description = """
            查询指定导入任务的详细信息。

            **路径参数:**
            - importId: 导入任务ID

            **返回数据包括:**
            - taskId: 任务ID
            - fileName: 文件名
            - status: 状态
            - totalCount: 总记录数
            - successCount: 成功数
            - failedCount: 失败数
            - operatorId: 操作人
            - fondsNo: 全宗号
            - createdAt: 创建时间
            - updatedAt: 更新时间
            - errorMessage: 错误信息
            - errorRecords: 错误记录详情

            **使用场景:**
            - 查看任务详细结果
            - 下载错误报告前预览
            """,
        operationId = "getImportTaskDetail",
        tags = {"历史数据导入"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "任务不存在")
    })
    public Result<LegacyImportTask> getImportTaskDetail(
            @Parameter(description = "导入任务ID", required = true, example = "import-001")
            @PathVariable String importId) {
        try {
            LegacyImportTask task = legacyImportService.getImportTaskDetail(importId);
            return Result.success(task);
        } catch (Exception e) {
            log.error("获取导入任务详情失败: {}", e.getMessage(), e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 下载错误报告
     */
    @GetMapping("/tasks/{importId}/error-report")
    @PreAuthorize("hasAnyAuthority('admin:import', 'archive:import') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "下载错误报告",
        description = """
            下载指定任务的错误报告（Excel 格式）。

            **路径参数:**
            - importId: 导入任务ID

            **返回数据:**
            - Excel 文件下载（.xlsx）
            - 包含所有错误记录详情
            - 包含错误原因说明

            **报告内容包括:**
            - 行号
            - 原始数据
            - 错误字段
            - 错误原因
            - 修正建议

            **使用场景:**
            - 导入失败后分析错误
            - 数据修正参考
            """,
        operationId = "downloadErrorReport",
        tags = {"历史数据导入"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "报告文件下载"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "404", description = "任务不存在或无错误报告")
    })
    public ResponseEntity<byte[]> downloadErrorReport(
            @Parameter(description = "导入任务ID", required = true, example = "import-001")
            @PathVariable String importId) {
        try {
            byte[] report = legacyImportService.downloadErrorReport(importId);
            return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=error_report_" + importId + ".xlsx")
                .body(report);
        } catch (Exception e) {
            log.error("下载错误报告失败: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 下载CSV导入模板
     */
    @GetMapping("/template/csv")
    @PreAuthorize("hasAnyAuthority('admin:import', 'archive:import') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "下载CSV导入模板",
        description = """
            下载标准 CSV 格式的导入模板。

            **返回数据:**
            - CSV 文件下载（UTF-8 编码）
            - 包含标准字段列表
            - 包含示例数据

            **模板字段:**
            - 档案号: 必填
            - 题名: 必填
            - 文件编号
            - 责任者
            - 日期: yyyy-MM-dd 格式
            - 页数
            - 保管期限
            - 密级
            - 备注

            **使用场景:**
            - 准备导入数据
            - 参考标准格式
            """,
        operationId = "downloadCsvTemplate",
        tags = {"历史数据导入"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "模板文件下载"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<Resource> downloadCsvTemplate() {
        try {
            Resource resource = legacyImportTemplateService.generateCsvTemplate();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=legacy-import-template.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(resource);
        } catch (Exception e) {
            log.error("下载CSV模板失败: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 下载Excel导入模板
     */
    @GetMapping("/template/excel")
    @PreAuthorize("hasAnyAuthority('admin:import', 'archive:import') or hasRole('SYSTEM_ADMIN')")
    @Operation(
        summary = "下载Excel导入模板",
        description = """
            下载标准 Excel 格式的导入模板。

            **返回数据:**
            - Excel 文件下载（.xlsx）
            - 包含标准字段列表
            - 包含数据验证规则
            - 包含示例数据

            **模板字段:**
            - 档案号: 必填，文本
            - 题名: 必填，文本
            - 文件编号: 文本
            - 责任者: 文本
            - 日期: 日期格式
            - 页数: 数字
            - 保管期限: 下拉选择
            - 密级: 下拉选择
            - 备注: 文本

            **使用场景:**
            - 准备导入数据
            - 参考标准格式
            - 使用数据验证
            """,
        operationId = "downloadExcelTemplate",
        tags = {"历史数据导入"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "模板文件下载"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "403", description = "权限不足")
    })
    public ResponseEntity<Resource> downloadExcelTemplate() {
        try {
            Resource resource = legacyImportTemplateService.generateExcelTemplate();
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=legacy-import-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
        } catch (Exception e) {
            log.error("下载Excel模板失败: {}", e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                // 假设 Principal 是 CustomUserDetails 或类似的对象
                // 这里需要根据实际实现调整
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
        }
        return "SYSTEM";
    }

    /**
     * 获取当前全宗号
     */
    private String getCurrentFondsNo() {
        try {
            // 从请求头或 SecurityContext 中获取全宗号
            // 这里需要根据实际实现调整
            org.springframework.web.context.request.RequestAttributes requestAttributes =
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                jakarta.servlet.http.HttpServletRequest request =
                    ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
                String fondsNo = request.getHeader("X-Fonds-No");
                if (fondsNo != null && !fondsNo.isEmpty()) {
                    return fondsNo;
                }
            }
        } catch (Exception e) {
            log.warn("获取当前全宗号失败: {}", e.getMessage());
        }
        return null;
    }
}
