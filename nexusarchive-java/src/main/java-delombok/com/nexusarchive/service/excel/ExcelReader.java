// Input: Apache POI, Spring Framework
// Output: ExcelReader 类
// Pos: 服务层 - Excel 读取服务

package com.nexusarchive.service.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Excel 读取服务
 * <p>
 * 统一 Excel 文件读取逻辑，支持 .xlsx 和 .xls 格式
 * </p>
 */
@Slf4j
@Component
public class ExcelReader {

    /**
     * 读取 Excel 文件指定 Sheet
     *
     * @param inputStream Excel 文件输入流
     * @param sheetIndex  Sheet 索引（从 0 开始）
     * @return 数据列表，每个 Map 代表一行，key 为列名，value 为单元格值
     * @throws IOException 读取失败时抛出
     */
    public List<Map<String, Object>> read(InputStream inputStream, int sheetIndex) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);

            // 读取表头（第一行）
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                log.warn("Excel 文件没有表头行");
                return result;
            }

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell));
            }

            // 读取数据行
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);
                if (dataRow == null) continue;

                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    Cell cell = dataRow.getCell(j);
                    rowData.put(header, getCellValue(cell));
                }
                result.add(rowData);
            }

            log.info("成功读取 Excel 文件，Sheet: {}, 行数: {}", sheet.getSheetName(), result.size());
        }

        return result;
    }

    /**
     * 读取 Excel 文件第一个 Sheet
     */
    public List<Map<String, Object>> read(InputStream inputStream) throws IOException {
        return read(inputStream, 0);
    }

    /**
     * 获取单元格值（带类型转换）
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue();
                } else {
                    yield cell.getNumericCellValue();
                }
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            case BLANK -> null;
            default -> null;
        };
    }

    /**
     * 获取单元格值作为字符串
     */
    private String getCellValueAsString(Cell cell) {
        Object value = getCellValue(cell);
        return value != null ? value.toString() : "";
    }
}
