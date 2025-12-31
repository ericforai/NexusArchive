// Input: LegacyImportService、FileParser、ImportValidationService、FondsAutoCreationService、ArchiveService、AuditLogService、Lombok、Spring Framework、Apache POI、Commons CSV、Jackson
// Output: LegacyImportServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.*;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.LegacyImportTask;
import com.nexusarchive.mapper.LegacyImportTaskMapper;
import com.nexusarchive.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 历史数据导入服务实现
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyImportServiceImpl implements LegacyImportService {
    
    private final LegacyImportTaskMapper importTaskMapper;
    private final ImportValidationService validationService;
    private final FondsAutoCreationService fondsAutoCreationService;
    private final ArchiveService archiveService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    
    // 默认字段映射（CSV/Excel 列名 -> 系统字段名）
    private static final Map<String, String> DEFAULT_FIELD_MAPPINGS = Map.ofEntries(
        Map.entry("全宗号", "fonds_no"),
        Map.entry("全宗名称", "fonds_name"),
        Map.entry("法人名称", "entity_name"),
        Map.entry("统一社会信用代码", "entity_tax_code"),
        Map.entry("归档年度", "archive_year"),
        Map.entry("档案类型", "doc_type"),
        Map.entry("档案标题", "title"),
        Map.entry("形成日期", "doc_date"),
        Map.entry("金额", "amount"),
        Map.entry("对方单位", "counterparty"),
        Map.entry("凭证号", "voucher_no"),
        Map.entry("发票号", "invoice_no"),
        Map.entry("保管期限", "retention_policy_name")
    );
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importLegacyData(MultipartFile file, 
                                         FieldMappingConfig mappingConfig,
                                         String operatorId, 
                                         String fondsNo) {
        LocalDateTime startTime = LocalDateTime.now();
        String importId = "import-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        
        // 创建导入任务记录
        LegacyImportTask task = new LegacyImportTask();
        task.setId(importId);
        task.setOperatorId(operatorId);
        task.setOperatorName(getCurrentUserName());
        task.setFondsNo(fondsNo);
        task.setFileName(file.getOriginalFilename());
        task.setFileSize(file.getSize());
        task.setStatus("PROCESSING");
        task.setStartedAt(startTime);
        task.setCreatedAt(LocalDateTime.now());
        importTaskMapper.insert(task);
        
        try {
            // 1. 解析文件
            List<ImportRow> rows = parseFile(file, mappingConfig);
            task.setTotalRows(rows.size());
            importTaskMapper.updateById(task);
            
            // 2. 验证数据
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
            
            // 3. 自动创建全宗和实体
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
                    
                    // 检查是否是新创建的全宗
                    if (fondsId != null) {
                        // 这里需要查询确认是否是新创建的，简化处理：记录全宗号
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
            
            // 4. 批量导入档案（分批执行，每批 1000 条）
            int successCount = 0;
            for (List<ImportRow> batch : Lists.partition(validRows, 1000)) {
                successCount += batchImportArchives(batch, operatorId);
            }
            
            // 5. 更新任务状态
            LocalDateTime endTime = LocalDateTime.now();
            task.setSuccessRows(successCount);
            task.setFailedRows(rows.size() - successCount);
            task.setStatus(successCount == rows.size() ? "SUCCESS" : 
                          (successCount > 0 ? "PARTIAL_SUCCESS" : "FAILED"));
            task.setCompletedAt(endTime);
            
            // 保存创建的全宗和实体列表
            if (!createdFondsNos.isEmpty()) {
                task.setCreatedFondsNos(objectMapper.writeValueAsString(new ArrayList<>(createdFondsNos)));
            }
            if (!createdEntityIds.isEmpty()) {
                task.setCreatedEntityIds(objectMapper.writeValueAsString(new ArrayList<>(createdEntityIds)));
            }
            
            // 如果有错误，生成错误报告
            if (!allErrors.isEmpty()) {
                String errorReportPath = generateErrorReport(importId, allErrors);
                task.setErrorReportPath(errorReportPath);
            }
            
            importTaskMapper.updateById(task);
            
            // 6. 记录审计日志
            // 6. 记录审计日志
            auditLogService.log(
                operatorId,
                getCurrentUserName(),
                "LEGACY_IMPORT",
                "IMPORT_TASK",
                importId,
                "SUCCESS",
                String.format("历史数据导入: 总数=%d, 成功=%d, 失败=%d", rows.size(), successCount, rows.size() - successCount),
                "UNKNOWN"
            );
            
            // 7. 构建返回结果
            ImportResult.ImportStatus status = successCount == rows.size() ? 
                ImportResult.ImportStatus.SUCCESS : 
                (successCount > 0 ? ImportResult.ImportStatus.PARTIAL_SUCCESS : ImportResult.ImportStatus.FAILED);
            
            return ImportResult.builder()
                .importId(importId)
                .totalRows(rows.size())
                .successRows(successCount)
                .failedRows(rows.size() - successCount)
                .errors(allErrors)
                .createdFondsNos(new ArrayList<>(createdFondsNos))
                .createdEntityIds(new ArrayList<>(createdEntityIds))
                .startTime(startTime)
                .endTime(endTime)
                .status(status)
                .errorReportUrl("/api/legacy-import/tasks/" + importId + "/error-report")
                .build();
                
        } catch (Exception e) {
            log.error("导入失败: importId={}, error={}", importId, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setCompletedAt(LocalDateTime.now());
            importTaskMapper.updateById(task);
            
            throw new BusinessException("导入失败: " + e.getMessage());
        }
    }
    
    @Override
    public ImportPreviewResult previewImport(MultipartFile file, FieldMappingConfig mappingConfig) {
        try {
            // 1. 解析文件（仅解析前100行用于预览）
            List<ImportRow> rows = parseFile(file, mappingConfig, 100);
            
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
    
    @Override
    public Page<LegacyImportTask> getImportTasks(int page, int size, String status) {
        Page<LegacyImportTask> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<LegacyImportTask> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(status)) {
            wrapper.eq(LegacyImportTask::getStatus, status);
        }
        
        wrapper.orderByDesc(LegacyImportTask::getCreatedAt);
        
        return importTaskMapper.selectPage(pageObj, wrapper);
    }
    
    @Override
    public LegacyImportTask getImportTaskDetail(String importId) {
        LegacyImportTask task = importTaskMapper.selectById(importId);
        if (task == null) {
            throw new BusinessException("导入任务不存在");
        }
        return task;
    }
    
    @Override
    public byte[] downloadErrorReport(String importId) {
        LegacyImportTask task = getImportTaskDetail(importId);
        if (!StringUtils.hasText(task.getErrorReportPath())) {
            throw new BusinessException("错误报告不存在");
        }
        
        // TODO: 从文件系统读取错误报告文件
        // 这里简化处理，返回空数组
        return new byte[0];
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 解析文件（CSV 或 Excel）
     */
    private List<ImportRow> parseFile(MultipartFile file, FieldMappingConfig mappingConfig) {
        return parseFile(file, mappingConfig, Integer.MAX_VALUE);
    }
    
    /**
     * 解析文件（支持限制行数，用于预览）
     */
    private List<ImportRow> parseFile(MultipartFile file, FieldMappingConfig mappingConfig, int maxRows) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        
        try {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return parseExcel(file, mappingConfig, maxRows);
            } else if (filename.endsWith(".csv")) {
                return parseCsv(file, mappingConfig, maxRows);
            } else {
                throw new BusinessException("不支持的文件格式，仅支持 CSV 和 Excel");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析文件失败: filename={}, error={}", filename, e.getMessage(), e);
            throw new BusinessException("解析文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析 Excel 文件
     */
    private List<ImportRow> parseExcel(MultipartFile file, FieldMappingConfig mappingConfig, int maxRows) 
            throws Exception {
        List<ImportRow> rows = new ArrayList<>();
        
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel 文件为空");
            }
            
            // 读取表头
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BusinessException("Excel 文件缺少表头");
            }
            
            Map<Integer, String> columnMapping = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String headerName = getCellString(cell);
                    String mappedField = mappingConfig != null ? 
                        mappingConfig.getMappedField(headerName) : 
                        DEFAULT_FIELD_MAPPINGS.getOrDefault(headerName, headerName);
                    columnMapping.put(i, mappedField);
                }
            }
            
            // 读取数据行
            int firstRow = 1;
            int lastRow = Math.min(sheet.getLastRowNum(), firstRow + maxRows - 1);
            
            for (int i = firstRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                ImportRow importRow = parseRowFromExcel(row, columnMapping, i + 1);
                if (importRow != null) {
                    rows.add(importRow);
                }
            }
        }
        
        return rows;
    }
    
    /**
     * 解析 CSV 文件
     */
    private List<ImportRow> parseCsv(MultipartFile file, FieldMappingConfig mappingConfig, int maxRows) 
            throws Exception {
        List<ImportRow> rows = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {
            
            Map<String, Integer> headerMap = parser.getHeaderMap();
            Map<String, String> fieldMapping = new HashMap<>();
            
            // 构建字段映射
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                String headerName = entry.getKey();
                String mappedField = mappingConfig != null ? 
                    mappingConfig.getMappedField(headerName) : 
                    DEFAULT_FIELD_MAPPINGS.getOrDefault(headerName, headerName);
                fieldMapping.put(headerName, mappedField);
            }
            
            int rowCount = 0;
            for (CSVRecord record : parser) {
                if (rowCount >= maxRows) break;
                
                ImportRow importRow = parseRowFromCsv(record, fieldMapping, (int) record.getRecordNumber() + 1);
                if (importRow != null) {
                    rows.add(importRow);
                }
                rowCount++;
            }
        }
        
        return rows;
    }
    
    /**
     * 从 Excel 行解析数据
     */
    private ImportRow parseRowFromExcel(Row row, Map<Integer, String> columnMapping, int rowNumber) {
        Map<String, String> rawData = new HashMap<>();
        
        for (Map.Entry<Integer, String> entry : columnMapping.entrySet()) {
            int colIndex = entry.getKey();
            String fieldName = entry.getValue();
            Cell cell = row.getCell(colIndex);
            String value = getCellString(cell);
            rawData.put(fieldName, value);
        }
        
        return buildImportRow(rawData, rowNumber);
    }
    
    /**
     * 从 CSV 记录解析数据
     */
    private ImportRow parseRowFromCsv(CSVRecord record, Map<String, String> fieldMapping, int rowNumber) {
        Map<String, String> rawData = new HashMap<>();
        
        for (Map.Entry<String, String> entry : fieldMapping.entrySet()) {
            String headerName = entry.getKey();
            String fieldName = entry.getValue();
            String value = record.get(headerName);
            rawData.put(fieldName, value != null ? value.trim() : null);
        }
        
        return buildImportRow(rawData, rowNumber);
    }
    
    /**
     * 构建 ImportRow 对象
     */
    private ImportRow buildImportRow(Map<String, String> rawData, int rowNumber) {
        ImportRow.ImportRowBuilder builder = ImportRow.builder()
            .rowNumber(rowNumber)
            .rawData(rawData);
        
        // 映射字段
        builder.fondsNo(getStringValue(rawData, "fonds_no"));
        builder.fondsName(getStringValue(rawData, "fonds_name"));
        builder.archiveYear(getIntegerValue(rawData, "archive_year"));
        builder.docType(getStringValue(rawData, "doc_type"));
        builder.title(getStringValue(rawData, "title"));
        builder.retentionPolicyName(getStringValue(rawData, "retention_policy_name"));
        builder.entityName(getStringValue(rawData, "entity_name"));
        builder.entityTaxCode(getStringValue(rawData, "entity_tax_code"));
        builder.docDate(getDateValue(rawData, "doc_date"));
        builder.amount(getBigDecimalValue(rawData, "amount"));
        builder.counterparty(getStringValue(rawData, "counterparty"));
        builder.voucherNo(getStringValue(rawData, "voucher_no"));
        builder.invoiceNo(getStringValue(rawData, "invoice_no"));
        builder.customMetadata(getStringValue(rawData, "custom_metadata"));
        builder.filePath(getStringValue(rawData, "file_path"));
        builder.fileHash(getStringValue(rawData, "file_hash"));
        
        return builder.build();
    }
    
    /**
     * 批量导入档案
     */
    private int batchImportArchives(List<ImportRow> rows, String operatorId) {
        int successCount = 0;
        
        for (ImportRow row : rows) {
            try {
                Archive archive = convertToArchive(row);
                archiveService.createArchive(archive, operatorId);
                successCount++;
            } catch (Exception e) {
                log.error("导入档案失败: rowNumber={}, error={}", row.getRowNumber(), e.getMessage());
                // 继续处理下一行，错误已在验证阶段收集
            }
        }
        
        return successCount;
    }
    
    /**
     * 将 ImportRow 转换为 Archive 实体
     */
    private Archive convertToArchive(ImportRow row) {
        Archive archive = new Archive();
        
        // 必需字段
        archive.setFondsNo(row.getFondsNo());
        archive.setFiscalYear(String.valueOf(row.getArchiveYear()));
        archive.setCategoryCode(row.getDocType()); // Use docType as categoryCode
        archive.setTitle(row.getTitle());
        
        // 解析保管期限
        String retentionPeriod = validationService.resolveRetentionPeriod(row.getRetentionPolicyName());
        if (retentionPeriod == null) {
            retentionPeriod = "PERMANENT"; // 默认值
        }
        archive.setRetentionPeriod(retentionPeriod);
        
        // 可选字段
        if (row.getDocDate() != null) {
            archive.setDocDate(row.getDocDate());
        }
        
        if (row.getAmount() != null) {
            archive.setAmount(row.getAmount());
        }
        
        // 设置扩展元数据
        if (StringUtils.hasText(row.getCustomMetadata())) {
            archive.setCustomMetadata(row.getCustomMetadata());
        } else {
            // 构建扩展元数据
            Map<String, Object> metadata = new HashMap<>();
            if (StringUtils.hasText(row.getCounterparty())) {
                metadata.put("counterparty", row.getCounterparty());
            }
            if (StringUtils.hasText(row.getVoucherNo())) {
                metadata.put("voucherNo", row.getVoucherNo());
            }
            if (StringUtils.hasText(row.getInvoiceNo())) {
                metadata.put("invoiceNo", row.getInvoiceNo());
            }
            if (!metadata.isEmpty()) {
                try {
                    archive.setCustomMetadata(objectMapper.writeValueAsString(metadata));
                } catch (Exception e) {
                    log.warn("构建扩展元数据失败: {}", e.getMessage());
                }
            }
        }
        
        // 设置状态
        archive.setStatus("archived");
        
        return archive;
    }
    
    /**
     * 生成错误报告
     */
    private String generateErrorReport(String importId, List<ImportError> errors) {
        try {
            // 使用 Apache POI 生成 Excel 错误报告
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("错误报告");
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("行号");
            headerRow.createCell(1).setCellValue("字段名");
            headerRow.createCell(2).setCellValue("错误代码");
            headerRow.createCell(3).setCellValue("错误消息");
            
            // 填充数据
            int rowNum = 1;
            for (ImportError error : errors) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(error.getRowNumber());
                row.createCell(1).setCellValue(error.getFieldName());
                row.createCell(2).setCellValue(error.getErrorCode());
                row.createCell(3).setCellValue(error.getErrorMessage());
            }
            
            // 保存到文件系统
            String reportPath = "data/import-reports/" + importId + "_error_report.xlsx";
            // TODO: 实现文件保存逻辑
            // Files.createDirectories(Paths.get("data/import-reports"));
            // try (FileOutputStream fos = new FileOutputStream(reportPath)) {
            //     workbook.write(fos);
            // }
            
            workbook.close();
            return reportPath;
        } catch (Exception e) {
            log.error("生成错误报告失败: importId={}, error={}", importId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取当前用户名
     */
    private String getCurrentUserName() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.warn("获取当前用户名失败: {}", e.getMessage());
        }
        return "SYSTEM";
    }
    
    // ========== 工具方法 ==========
    
    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return new org.apache.poi.ss.usermodel.DataFormatter().formatCellValue(cell).trim();
    }
    
    private String getStringValue(Map<String, String> data, String key) {
        String value = data.get(key);
        return StringUtils.hasText(value) ? value : null;
    }
    
    private Integer getIntegerValue(Map<String, String> data, String key) {
        String value = data.get(key);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private BigDecimal getBigDecimalValue(Map<String, String> data, String key) {
        String value = data.get(key);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private LocalDate getDateValue(Map<String, String> data, String key) {
        String value = data.get(key);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        // 尝试多种日期格式
        String[] dateFormats = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd"};
        for (String format : dateFormats) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDate.parse(value.trim(), formatter);
            } catch (DateTimeParseException e) {
                // 继续尝试下一个格式
            }
        }
        return null;
    }
}

