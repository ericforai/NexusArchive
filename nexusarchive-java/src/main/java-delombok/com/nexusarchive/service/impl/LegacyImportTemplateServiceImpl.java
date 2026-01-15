// Input: Apache POI、文件IO、Spring Resource
// Output: LegacyImportTemplateServiceImpl 模板生成服务实现
// Pos: 历史数据导入模板服务实现
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.service.LegacyImportTemplateService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 历史数据导入模板生成服务实现
 */
@Service
public class LegacyImportTemplateServiceImpl implements LegacyImportTemplateService {

    // 标准字段名（表头）
    private static final List<String> STANDARD_FIELDS = Arrays.asList(
        "fonds_no",              // 全宗号（必填）
        "fonds_name",            // 全宗名称（必填）
        "entity_name",           // 法人实体名称（可选）
        "entity_tax_code",       // 统一社会信用代码（可选）
        "archive_year",          // 归档年度（必填）
        "doc_type",              // 档案类型（必填）
        "title",                 // 档案标题（必填）
        "doc_date",              // 形成日期（可选）
        "amount",                // 金额（可选）
        "counterparty",          // 对方单位（可选）
        "voucher_no",            // 凭证号（可选）
        "invoice_no",            // 发票号（可选）
        "retention_policy_name"  // 保管期限名称（必填）
    );


    // 示例数据
    private static final List<List<String>> SAMPLE_DATA = Arrays.asList(
        Arrays.asList("JD-001", "京东集团", "北京京东世纪贸易有限公司", "91110000MA01234567", "2024", "凭证", "2024年1月记账凭证", "2024-01-15", "1000000.00", "供应商A", "V-202401-001", "INV-202401-001", "永久"),
        Arrays.asList("JD-001", "京东集团", "北京京东世纪贸易有限公司", "91110000MA01234567", "2024", "报表", "2024年度财务报表", "2024-12-31", "50000000.00", "", "", "", "永久"),
        Arrays.asList("JD-002", "京东物流", "北京京东物流有限公司", "91110000MA01234568", "2024", "凭证", "2024年2月记账凭证", "2024-02-15", "2000000.00", "供应商B", "V-202402-001", "INV-202402-001", "30年")
    );

    @Override
    public Resource generateCsvTemplate() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // 写入BOM（UTF-8 BOM）以便Excel正确识别编码
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                 CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
                
                // 写入表头
                printer.printRecord(STANDARD_FIELDS);
                
                // 写入示例数据
                for (List<String> row : SAMPLE_DATA) {
                    printer.printRecord(row);
                }
                
                printer.flush();
            }
            
            byte[] csvBytes = outputStream.toByteArray();
            return new ByteArrayResource(csvBytes) {
                @Override
                public String getFilename() {
                    return "legacy-import-template.csv";
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("生成CSV模板失败", e);
        }
    }

    @Override
    public Resource generateExcelTemplate() {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("导入模板");
            
            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            
            // 创建数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            
            // 写入表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < STANDARD_FIELDS.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(STANDARD_FIELDS.get(i));
                cell.setCellStyle(headerStyle);
                
                // 设置列宽
                sheet.setColumnWidth(i, 4000);
            }
            
            // 写入示例数据
            for (int rowIndex = 0; rowIndex < SAMPLE_DATA.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                List<String> rowData = SAMPLE_DATA.get(rowIndex);
                
                for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    String value = rowData.get(colIndex);
                    cell.setCellValue(value);
                    cell.setCellStyle(dataStyle);
                }
            }
            
            // 添加说明行（在示例数据后）
            int noteRowIndex = SAMPLE_DATA.size() + 2;
            Row noteRow1 = sheet.createRow(noteRowIndex++);
            Cell noteCell1 = noteRow1.createCell(0);
            noteCell1.setCellValue("说明：");
            noteCell1.setCellStyle(headerStyle);
            
            Row noteRow2 = sheet.createRow(noteRowIndex++);
            Cell noteCell2 = noteRow2.createCell(0);
            noteCell2.setCellValue("1. 第一行为表头（字段名），请勿删除或修改");
            
            Row noteRow3 = sheet.createRow(noteRowIndex++);
            Cell noteCell3 = noteRow3.createCell(0);
            noteCell3.setCellValue("2. 带（必填）标记的字段必须填写，带（可选）标记的字段可以为空");
            
            Row noteRow4 = sheet.createRow(noteRowIndex++);
            Cell noteCell4 = noteRow4.createCell(0);
            noteCell4.setCellValue("3. 文件大小限制：最大100MB，建议单次导入不超过10,000行");
            
            Row noteRow5 = sheet.createRow(noteRowIndex++);
            Cell noteCell5 = noteRow5.createCell(0);
            noteCell5.setCellValue("4. 日期格式：YYYY-MM-DD（如：2024-01-15）");
            
            Row noteRow6 = sheet.createRow(noteRowIndex++);
            Cell noteCell6 = noteRow6.createCell(0);
            noteCell6.setCellValue("5. 删除示例数据后，填写您的实际数据");
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            
            byte[] excelBytes = outputStream.toByteArray();
            return new ByteArrayResource(excelBytes) {
                @Override
                public String getFilename() {
                    return "legacy-import-template.xlsx";
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("生成Excel模板失败", e);
        }
    }
}

