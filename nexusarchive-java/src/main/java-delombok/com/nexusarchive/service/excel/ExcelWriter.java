// Input: Apache POI, Spring Framework
// Output: ExcelWriter 类
// Pos: 服务层 - Excel 写入服务

package com.nexusarchive.service.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Excel 写入服务
 * <p>
 * 统一 Excel 文件写入逻辑，生成 .xlsx 格式
 * </p>
 */
@Slf4j
@Component
public class ExcelWriter {

    /**
     * 将数据写入 Excel 文件
     *
     * @param data     数据列表，每个 Map 代表一行
     * @param sheetName Sheet 名称
     * @return Excel 文件字节数组
     * @throws IOException 写入失败时抛出
     */
    public byte[] write(List<Map<String, Object>> data, String sheetName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);

            if (data.isEmpty()) {
                workbook.write(out);
                return out.toByteArray();
            }

            // 创建表头样式
            CellStyle headerStyle = createHeaderStyle(workbook);

            // 写入表头
            Map<String, Object> firstRow = data.get(0);
            Row headerRow = sheet.createRow(0);
            int colIndex = 0;
            for (String header : firstRow.keySet()) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(header);
                cell.setCellStyle(headerStyle);
            }

            // 写入数据行
            int rowIndex = 1;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowIndex++);
                colIndex = 0;
                for (Object value : rowData.values()) {
                    Cell cell = row.createCell(colIndex++);
                    setCellValue(cell, value);
                }
            }

            // 自动调整列宽
            for (int i = 0; i < firstRow.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("成功生成 Excel 文件，Sheet: {}, 行数: {}", sheetName, data.size());
            return out.toByteArray();
        }
    }

    /**
     * 创建表头样式
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    /**
     * 设置单元格值
     */
    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }
}
