// Input: Spring Framework、Lombok、Java 标准库、本地模块
// Output: AppraisalListController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.annotation.ArchivalAudit;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.AppraisalListDetail;
import com.nexusarchive.service.ArchiveAppraisalService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;

/**
 * 档案鉴定清单控制器
 *
 * 路径: /archive/appraisal
 *
 * 功能：
 * 1. 生成鉴定清单（选择待鉴定档案）
 * 2. 查看鉴定清单列表
 * 3. 查看鉴定清单详情
 * 4. 提交鉴定结论（同意销毁/不同意销毁/延期保管）
 * 5. 导出鉴定清单（Excel/PDF）
 */
@Slf4j
@RestController
@RequestMapping("/archive/appraisal")
@RequiredArgsConstructor
@Validated
public class AppraisalListController {

    private final ArchiveAppraisalService appraisalService;

    /**
     * 生成鉴定清单
     *
     * POST /archive/appraisal/generate
     *
     * @param request 生成鉴定清单请求
     * @return 鉴定清单ID
     */
    @PostMapping("/generate")
    @ArchivalAudit(operationType = "CREATE", resourceType = "APPRAISAL_LIST", description = "生成鉴定清单")
    @PreAuthorize("hasAnyAuthority('appraisal:create', 'nav:all')")
    public Result<String> createAppraisalList(@Valid @RequestBody CreateAppraisalListRequest request) {
        String fondsNo = getFondsNo();
        String appraiserId = getCurrentUserId();
        String appraisalListId = appraisalService.createAppraisalList(
                request.getArchiveIds(),
                fondsNo,
                appraiserId,
                request.getAppraisalDate() != null ? request.getAppraisalDate() : LocalDate.now()
        );
        return Result.success("鉴定清单生成成功", appraisalListId);
    }

    /**
     * 获取鉴定清单列表
     *
     * GET /archive/appraisal/list?status=PENDING&page=1&limit=20
     *
     * @param status 状态筛选（可选）
     * @param page 页码
     * @param limit 每页数量
     * @return 鉴定清单列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('appraisal:view', 'nav:all')")
    public Result<?> getAppraisalList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        // TODO: 实现列表查询逻辑
        // 需要在 ArchiveAppraisalService 中添加列表查询方法
        return Result.success("功能待实现", null);
    }

    /**
     * 获取鉴定清单详情
     *
     * GET /archive/appraisal/{id}
     *
     * @param id 鉴定清单ID
     * @return 鉴定清单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('appraisal:view', 'nav:all')")
    public Result<AppraisalListDetail> getAppraisalListDetail(@PathVariable String id) {
        AppraisalListDetail detail = appraisalService.getAppraisalListDetail(id);
        return Result.success(detail);
    }

    /**
     * 提交鉴定结论
     *
     * POST /archive/appraisal/{id}/conclusion
     *
     * @param id 鉴定清单ID
     * @param request 鉴定结论请求
     * @return 操作结果
     */
    @PostMapping("/{id}/conclusion")
    @ArchivalAudit(operationType = "UPDATE", resourceType = "APPRAISAL_LIST", description = "提交鉴定结论")
    @PreAuthorize("hasAnyAuthority('appraisal:submit', 'nav:all')")
    public Result<Void> submitAppraisalConclusion(
            @PathVariable String id,
            @Valid @RequestBody SubmitConclusionRequest request) {
        appraisalService.submitAppraisalConclusion(
                id,
                request.getConclusion(),
                request.getComment()
        );
        return Result.success("鉴定结论提交成功", null);
    }

    /**
     * 导出鉴定清单
     *
     * GET /archive/appraisal/{id}/export?format=EXCEL
     *
     * @param id 鉴定清单ID
     * @param format 导出格式（EXCEL 或 PDF）
     * @param response HTTP响应
     */
    @GetMapping("/{id}/export")
    @ArchivalAudit(operationType = "EXPORT", resourceType = "APPRAISAL_LIST", description = "导出鉴定清单")
    @PreAuthorize("hasAnyAuthority('appraisal:export', 'nav:all')")
    public void exportAppraisalList(
            @PathVariable String id,
            @RequestParam(defaultValue = "EXCEL") String format,
            HttpServletResponse response) throws IOException {
        ArchiveAppraisalService.ExportFormat exportFormat = parseExportFormat(format);
        byte[] data = appraisalService.exportAppraisalList(id, exportFormat);

        String filename = "appraisal_list_" + id;
        String contentType = exportFormat == ArchiveAppraisalService.ExportFormat.EXCEL
                ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                : "application/pdf";
        String extension = exportFormat == ArchiveAppraisalService.ExportFormat.EXCEL
                ? ".xlsx"
                : ".pdf";

        response.setContentType(contentType);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + extension + "\"");
        response.setContentLength(data.length);

        try (OutputStream os = response.getOutputStream()) {
            os.write(data);
            os.flush();
        }
    }

    /**
     * 解析导出格式
     */
    private ArchiveAppraisalService.ExportFormat parseExportFormat(String format) {
        try {
            return ArchiveAppraisalService.ExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("无效的导出格式: {}, 使用默认格式 EXCEL", format);
            return ArchiveAppraisalService.ExportFormat.EXCEL;
        }
    }

    /**
     * 获取当前全宗号
     */
    private String getFondsNo() {
        // TODO: 从 FondsContext 或 SecurityContext 获取
        // 暂时从请求属性获取，需配合 FondsContextFilter
        return (String) org.springframework.web.context.request.RequestContextHolder
                .getRequestAttributes()
                .getAttribute("fondsNo", 0);
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("获取当前用户ID失败: {}", e.getMessage());
        }
        return "SYSTEM";
    }

    /**
     * 生成鉴定清单请求
     */
    @lombok.Data
    public static class CreateAppraisalListRequest {

        /**
         * 待鉴定档案ID列表
         */
        @NotEmpty(message = "待鉴定档案ID列表不能为空")
        private java.util.List<String> archiveIds;

        /**
         * 鉴定日期（可选，默认为当天）
         */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate appraisalDate;
    }

    /**
     * 提交鉴定结论请求
     */
    @lombok.Data
    public static class SubmitConclusionRequest {

        /**
         * 鉴定结论：APPROVED（同意销毁）/ REJECTED（不同意销毁）/ DEFERRED（延期保管）
         */
        @NotEmpty(message = "鉴定结论不能为空")
        private String conclusion;

        /**
         * 鉴定意见
         */
        private String comment;
    }
}
