// Input: Apache Commons CSV, Spring Framework
// Output: CsvWriter 类
// Pos: 服务层 - CSV 写入服务

package com.nexusarchive.service.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * CSV 写入服务
 * <p>
 * 统一 CSV 文件写入逻辑，使用 UTF-8 BOM 编码
 * </p>
 */
@Slf4j
@Component
public class CsvWriter {

    /**
     * 将数据写入 CSV 文件
     *
     * @param data     数据列表
     * @param separator 分隔符
     * @return CSV 文件字节数组
     * @throws IOException 写入失败时抛出
     */
    public byte[] write(List<Map<String, Object>> data, char separator) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            // 添加 UTF-8 BOM，确保 Excel 正确显示中文
            out.write(0xEF);
            out.write(0xBB);
            out.write(0xBF);

            CSVFormat format = CSVFormat.DEFAULT
                .withDelimiter(separator)
                .withQuote('"');

            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                if (!data.isEmpty()) {
                    // 写入表头
                    String[] headers = data.get(0).keySet().toArray(new String[0]);
                    printer.printRecord(headers);

                    // 写入数据行
                    for (Map<String, Object> row : data) {
                        String[] values = row.values().stream()
                            .map(v -> v != null ? v.toString() : "")
                            .toArray(String[]::new);
                        printer.printRecord(values);
                    }
                }

                printer.flush();
                log.info("成功生成 CSV 文件，行数: {}", data.size());
                return out.toByteArray();
            }
        }
    }

    /**
     * 写入 CSV 文件（默认逗号分隔）
     */
    public byte[] write(List<Map<String, Object>> data) throws IOException {
        return write(data, ',');
    }
}
