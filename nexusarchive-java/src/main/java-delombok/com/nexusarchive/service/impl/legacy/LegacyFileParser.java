// Input: Apache POI、Commons CSV、Lombok、Spring Framework
// Output: LegacyFileParser 类
// Pos: 历史数据导入服务 - 文件解析层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.legacy;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.FieldMappingConfig;
import com.nexusarchive.dto.request.ImportRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 历史数据文件解析器
 * <p>
 * 支持 CSV 和 Excel (xls/xlsx) 格式的历史数据文件解析
 * </p>
 */
@Slf4j
@Component
public class LegacyFileParser {

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

    /**
     * 解析文件（CSV 或 Excel）
     */
    public List<ImportRow> parseFile(MultipartFile file, FieldMappingConfig mappingConfig) {
        return parseFile(file, mappingConfig, Integer.MAX_VALUE);
    }

    /**
     * 解析文件（支持限制行数，用于预览）
     */
    public List<ImportRow> parseFile(MultipartFile file, FieldMappingConfig mappingConfig, int maxRows) {
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

        return LegacyImportUtils.buildImportRow(rawData, rowNumber);
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

        return LegacyImportUtils.buildImportRow(rawData, rowNumber);
    }

    /**
     * 获取单元格字符串值
     */
    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return new DataFormatter().formatCellValue(cell).trim();
    }
}
