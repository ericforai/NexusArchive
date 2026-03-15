// Input: Jackson、Lombok、Spring Framework、Guava
// Output: LegacyImportOrchestrator 类
// Pos: 历史数据导入服务 - 导入流程编排层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.nexusarchive.common.constants.OperationResult;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.FieldMappingConfig;
import com.nexusarchive.dto.request.ImportError;
import com.nexusarchive.dto.request.ImportPreviewResult;
import com.nexusarchive.dto.request.ImportResult;
import com.nexusarchive.dto.request.ImportRow;
import com.nexusarchive.entity.LegacyImportTask;
import com.nexusarchive.mapper.LegacyImportTaskMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.FondsAutoCreationService;
import com.nexusarchive.service.ImportValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 历史数据导入流程编排器
 * <p>
 * 负责编排历史数据导入的完整流程：解析 → 验证 → 创建全宗/实体 → 导入档案 → 记录日志
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacyImportOrchestrator {

    private final LegacyImportTaskMapper importTaskMapper;
    private final ImportValidationService validationService;
    private final FondsAutoCreationService fondsAutoCreationService;
    private final LegacyDataConverter dataConverter;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /**
     * 执行导入流程
     */
    @Transactional(rollbackFor = Exception.class)
    public ImportResult executeImport(MultipartFile file,
                                      FieldMappingConfig mappingConfig,
                                      String operatorId,
                                      String fondsNo) {
        LocalDateTime startTime = LocalDateTime.now();
        String importId = generateImportId();

        // 创建导入任务记录
        LegacyImportTask task = createImportTask(importId, operatorId, fondsNo, file, startTime);
        importTaskMapper.insert(task);

        try {
            // 1. 解析文件
            List<ImportRow> rows = parseAndUpdateTaskCount(file, mappingConfig, task);

            // 2. 验证数据并分离有效/无效行
            ValidationResult validationResult = validateRows(rows);
            List<ImportRow> validRows = validationResult.validRows;
            List<ImportError> allErrors = validationResult.allErrors;

            // 3. 自动创建全宗和实体
            CreationResult creationResult = ensureFondsAndEntities(validRows, operatorId, allErrors);

            // 4. 批量导入档案
            int successCount = batchImportArchives(validRows, operatorId);

            // 5. 更新任务状态并完成
            return finalizeImport(task, rows, successCount, allErrors, creationResult,
                    startTime, importId, operatorId);

        } catch (Exception e) {
            handleImportFailure(task, e, importId);
            throw e;
        }
    }

    /**
     * 生成导入ID
     */
    private String generateImportId() {
        return "import-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 创建导入任务记录
     */
    private LegacyImportTask createImportTask(String importId, String operatorId,
                                               String fondsNo, MultipartFile file,
                                               LocalDateTime startTime) {
        LegacyImportTask task = new LegacyImportTask();
        task.setId(importId);
        task.setOperatorId(operatorId);
        task.setOperatorName(LegacyImportUtils.getCurrentUserName());
        task.setFondsNo(fondsNo);
        task.setFileName(file.getOriginalFilename());
        task.setFileSize(file.getSize());
        task.setStatus("PROCESSING");
        task.setStartedAt(startTime);
        task.setCreatedAt(LocalDateTime.now());
        return task;
    }

    /**
     * 解析文件并更新任务计数
     */
    private List<ImportRow> parseAndUpdateTaskCount(MultipartFile file,
                                                     FieldMappingConfig mappingConfig,
                                                     LegacyImportTask task) {
        List<ImportRow> rows = new LegacyFileParser().parseFile(file, mappingConfig);
        task.setTotalRows(rows.size());
        importTaskMapper.updateById(task);
        return rows;
    }

    /**
     * 验证数据行，分离有效和无效行
     */
    private ValidationResult validateRows(List<ImportRow> rows) {
        ImportValidationService.ValidationContext context = new ImportValidationService.ValidationContext();
        if (!rows.isEmpty()) {
            context.setCurrentFondsNo(rows.get(0).getFondsNo());
        }

        List<ImportRow> validRows = new ArrayList<>();
        List<ImportError> allErrors = new ArrayList<>();

        for (ImportRow row : rows) {
            ImportValidationService.ValidationResult result =
                    validationService.validateRow(row, row.getRowNumber(), context);
            if (result.isValid()) {
                validRows.add(row);
            } else {
                allErrors.addAll(result.getErrors());
            }
        }

        return new ValidationResult(validRows, allErrors);
    }

    /**
     * 自动创建全宗和实体
     */
    private CreationResult ensureFondsAndEntities(List<ImportRow> validRows,
                                                   String operatorId,
                                                   List<ImportError> allErrors) {
        Set<String> createdFondsNos = new HashSet<>();
        Set<String> createdEntityIds = new HashSet<>();

        for (ImportRow row : validRows) {
            try {
                String fondsId = fondsAutoCreationService.ensureFondsExists(
                        row.getFondsNo(),
                        row.getFondsName(),
                        row.getEntityName(),
                        row.getEntityTaxCode(),
                        operatorId
                );

                if (fondsId != null) {
                    createdFondsNos.add(row.getFondsNo());
                }

                if (StringUtils.hasText(row.getEntityName()) || StringUtils.hasText(row.getEntityTaxCode())) {
                    String entityId = fondsAutoCreationService.ensureEntityExists(
                            row.getEntityName(),
                            row.getEntityTaxCode()
                    );
                    if (entityId != null) {
                        createdEntityIds.add(entityId);
                    }
                }
            } catch (Exception e) {
                log.error("自动创建全宗/实体失败: fondsNo={}, error={}", row.getFondsNo(), e.getMessage());
                allErrors.add(ImportError.builder()
                        .rowNumber(row.getRowNumber())
                        .fieldName("fonds_no")
                        .errorCode("FONDS_CREATION_FAILED")
                        .errorMessage("自动创建全宗失败: " + e.getMessage())
                        .build());
            }
        }

        return new CreationResult(createdFondsNos, createdEntityIds);
    }

    /**
     * 批量导入档案
     */
    private int batchImportArchives(List<ImportRow> validRows, String operatorId) {
        int successCount = 0;
        for (List<ImportRow> batch : Lists.partition(validRows, 1000)) {
            successCount += dataConverter.batchImportArchives(batch, operatorId);
        }
        return successCount;
    }

    /**
     * 完成导入：更新任务状态、记录日志、构建返回结果
     */
    private ImportResult finalizeImport(LegacyImportTask task, List<ImportRow> rows,
                                       int successCount, List<ImportError> allErrors,
                                       CreationResult creationResult,
                                       LocalDateTime startTime, String importId,
                                       String operatorId) {
        LocalDateTime endTime = LocalDateTime.now();

        // 更新任务状态
        task.setSuccessRows(successCount);
        task.setFailedRows(rows.size() - successCount);
        task.setStatus(successCount == rows.size() ? OperationResult.SUCCESS :
                (successCount > 0 ? "PARTIAL_SUCCESS" : "FAILED"));
        task.setCompletedAt(endTime);

        // 保存创建的全宗和实体列表
        if (!creationResult.createdFondsNos.isEmpty()) {
            try {
                task.setCreatedFondsNos(objectMapper.writeValueAsString(
                        new ArrayList<>(creationResult.createdFondsNos)));
            } catch (Exception e) {
                log.warn("序列化全宗列表失败", e);
            }
        }
        if (!creationResult.createdEntityIds.isEmpty()) {
            try {
                task.setCreatedEntityIds(objectMapper.writeValueAsString(
                        new ArrayList<>(creationResult.createdEntityIds)));
            } catch (Exception e) {
                log.warn("序列化实体列表失败", e);
            }
        }

        // 如果有错误，生成错误报告
        if (!allErrors.isEmpty()) {
            String errorReportPath = generateErrorReport(importId, allErrors);
            task.setErrorReportPath(errorReportPath);
        }

        importTaskMapper.updateById(task);

        // 记录审计日志
        auditLogService.log(
                operatorId,
                LegacyImportUtils.getCurrentUserName(),
                "LEGACY_IMPORT",
                "IMPORT_TASK",
                importId,
                OperationResult.SUCCESS,
                String.format("历史数据导入: 总数=%d, 成功=%d, 失败=%d",
                        rows.size(), successCount, rows.size() - successCount),
                OperationResult.UNKNOWN
        );

        // 构建返回结果
        ImportResult.ImportStatus status = successCount == rows.size() ?
                ImportResult.ImportStatus.SUCCESS :
                (successCount > 0 ? ImportResult.ImportStatus.PARTIAL_SUCCESS : ImportResult.ImportStatus.FAILED);

        return ImportResult.builder()
                .importId(importId)
                .totalRows(rows.size())
                .successRows(successCount)
                .failedRows(rows.size() - successCount)
                .errors(allErrors)
                .createdFondsNos(new ArrayList<>(creationResult.createdFondsNos))
                .createdEntityIds(new ArrayList<>(creationResult.createdEntityIds))
                .startTime(startTime)
                .endTime(endTime)
                .status(status)
                .errorReportUrl("/api/legacy-import/tasks/" + importId + "/error-report")
                .build();
    }

    /**
     * 处理导入失败
     */
    private void handleImportFailure(LegacyImportTask task, Exception e, String importId) {
        log.error("导入失败: importId={}, error={}", importId, e.getMessage(), e);
        task.setStatus("FAILED");
        task.setCompletedAt(LocalDateTime.now());
        importTaskMapper.updateById(task);
        throw new BusinessException("导入失败: " + e.getMessage());
    }

    /**
     * 验证结果内部类
     */
    private static class ValidationResult {
        final List<ImportRow> validRows;
        final List<ImportError> allErrors;

        ValidationResult(List<ImportRow> validRows, List<ImportError> allErrors) {
            this.validRows = validRows;
            this.allErrors = allErrors;
        }
    }

    /**
     * 创建结果内部类
     */
    private static class CreationResult {
        final Set<String> createdFondsNos;
        final Set<String> createdEntityIds;

        CreationResult(Set<String> createdFondsNos, Set<String> createdEntityIds) {
            this.createdFondsNos = createdFondsNos;
            this.createdEntityIds = createdEntityIds;
        }
    }

    /**
     * 预览导入数据
     */
    public ImportPreviewResult previewImport(MultipartFile file, FieldMappingConfig mappingConfig) {
        try {
            // 1. 解析文件（仅解析前100行用于预览）
            List<ImportRow> rows = new LegacyFileParser().parseFile(file, mappingConfig, 100);

            // 2. 验证数据
            ImportValidationService.ValidationContext context = new ImportValidationService.ValidationContext();
            if (!rows.isEmpty()) {
                context.setCurrentFondsNo(rows.get(0).getFondsNo());
            }

            List<ImportPreviewResult.ImportRowPreview> previewData = new ArrayList<>();
            List<ImportError> allErrors = new ArrayList<>();
            Set<String> fondsNos = new HashSet<>();
            Set<String> entityNames = new HashSet<>();

            for (ImportRow row : rows) {
                ImportValidationService.ValidationResult result =
                    validationService.validateRow(row, row.getRowNumber(), context);

                Map<String, Object> data = new HashMap<>();
                data.put("fonds_no", row.getFondsNo());
                data.put("fonds_name", row.getFondsName());
                data.put("archive_year", row.getArchiveYear());
                data.put("doc_type", row.getDocType());
                data.put("title", row.getTitle());

                previewData.add(ImportPreviewResult.ImportRowPreview.builder()
                    .rowNumber(row.getRowNumber())
                    .data(data)
                    .validationErrors(result.getErrors())
                    .build());

                allErrors.addAll(result.getErrors());

                if (StringUtils.hasText(row.getFondsNo())) {
                    fondsNos.add(row.getFondsNo());
                }
                if (StringUtils.hasText(row.getEntityName())) {
                    entityNames.add(row.getEntityName());
                }
            }

            // 3. 统计信息
            ImportPreviewResult.PreviewStatistics statistics = ImportPreviewResult.PreviewStatistics.builder()
                .fondsCount(fondsNos.size())
                .entityCount(entityNames.size())
                .willCreateFonds(new ArrayList<>(fondsNos))
                .willCreateEntities(new ArrayList<>(entityNames))
                .build();

            return ImportPreviewResult.builder()
                .totalRows(rows.size())
                .validRows((int) rows.stream().filter(r -> {
                    ImportValidationService.ValidationResult result =
                        validationService.validateRow(r, r.getRowNumber(), context);
                    return result.isValid();
                }).count())
                .invalidRows(rows.size() - (int) rows.stream().filter(r -> {
                    ImportValidationService.ValidationResult result =
                        validationService.validateRow(r, r.getRowNumber(), context);
                    return result.isValid();
                }).count())
                .previewData(previewData)
                .errors(allErrors)
                .statistics(statistics)
                .build();

        } catch (Exception e) {
            log.error("预览失败: error={}", e.getMessage(), e);
            throw new BusinessException("预览失败: " + e.getMessage());
        }
    }

    /**
     * 生成错误报告
     */
    private String generateErrorReport(String importId, List<ImportError> errors) {
        try {
            // 使用 Apache POI 生成 Excel 错误报告
            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("错误报告");

            // 创建表头
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("行号");
            headerRow.createCell(1).setCellValue("字段名");
            headerRow.createCell(2).setCellValue("错误代码");
            headerRow.createCell(3).setCellValue("错误消息");

            // 填充数据
            int rowNum = 1;
            for (ImportError error : errors) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(error.getRowNumber());
                row.createCell(1).setCellValue(error.getFieldName());
                row.createCell(2).setCellValue(error.getErrorCode());
                row.createCell(3).setCellValue(error.getErrorMessage());
            }

            // 保存到文件系统
            String reportPath = saveErrorReportToFile(workbook, importId);
            return reportPath;
        } catch (Exception e) {
            log.error("生成错误报告失败: importId={}, error={}", importId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 保存错误报告到文件系统
     */
    private String saveErrorReportToFile(org.apache.poi.ss.usermodel.Workbook workbook, String importId) {
        java.nio.file.Path reportDir = java.nio.file.Paths.get("data/import-reports");
        try {
            // 确保目录存在
            if (!java.nio.file.Files.exists(reportDir)) {
                java.nio.file.Files.createDirectories(reportDir);
            }

            // 写入文件
            java.nio.file.Path reportFile = reportDir.resolve(importId + "_error_report.xlsx");
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(reportFile.toFile())) {
                workbook.write(fos);
            }
            log.info("错误报告已生成: {}", reportFile);
            return reportFile.toString();
        } catch (Exception e) {
            log.error("保存错误报告失败: importId={}, error={}", importId, e.getMessage(), e);
            return "data/import-reports/" + importId + "_error_report.xlsx";
        } finally {
            try {
                workbook.close();
            } catch (Exception e) {
                log.warn("关闭 workbook 失败", e);
            }
        }
    }
}
