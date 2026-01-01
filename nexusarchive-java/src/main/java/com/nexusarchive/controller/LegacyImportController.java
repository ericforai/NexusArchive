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
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
@Slf4j
@RestController
@RequestMapping("/api/legacy-import")
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
    public Result<ImportPreviewResult> previewImport(
            @RequestParam("file") MultipartFile file,
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
    public Result<ImportResult> importData(
            @RequestParam("file") MultipartFile file,
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
    public Result<Page<LegacyImportTask>> getImportTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
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
    public Result<LegacyImportTask> getImportTaskDetail(@PathVariable String importId) {
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
    public org.springframework.http.ResponseEntity<byte[]> downloadErrorReport(@PathVariable String importId) {
        try {
            byte[] report = legacyImportService.downloadErrorReport(importId);
            return org.springframework.http.ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=error_report_" + importId + ".xlsx")
                .body(report);
        } catch (Exception e) {
            log.error("下载错误报告失败: {}", e.getMessage(), e);
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 下载CSV导入模板
     */
    @GetMapping("/template/csv")
    @PreAuthorize("hasAnyAuthority('admin:import', 'archive:import') or hasRole('SYSTEM_ADMIN')")
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

