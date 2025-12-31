// Input: Apache Commons CSV, Spring Framework
// Output: CsvReader 类
// Pos: 服务层 - CSV 读取服务

package com.nexusarchive.service.excel;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CSV 读取服务
 * <p>
 * 统一 CSV 文件读取逻辑，支持自定义分隔符和字符编码
 * </p>
 */
@Slf4j
@Component
public class CsvReader {

    /**
     * 读取 CSV 文件
     *
     * @param inputStream CSV 文件输入流
     * @param separator   分隔符（默认逗号）
     * @return 数据列表，每个 Map 代表一行
     * @throws IOException 读取失败时抛出
     */
    public List<Map<String, Object>> read(InputStream inputStream, char separator) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT
                 .withDelimiter(separator)
                 .withFirstRecordAsHeader()
                 .parse(reader)) {

            Map<String, Integer> headerMap = parser.getHeaderMap();
            List<String> headers = new ArrayList<>(headerMap.keySet());

            // 读取数据行
            for (CSVRecord record : parser) {
                Map<String, Object> rowData = new LinkedHashMap<>();

                for (String header : headers) {
                    String value = record.get(header);
                    rowData.put(header, value != null ? value.trim() : "");
                }

                result.add(rowData);
            }

            log.info("成功读取 CSV 文件，行数: {}", result.size());
        }

        return result;
    }

    /**
     * 读取 CSV 文件（默认逗号分隔）
     */
    public List<Map<String, Object>> read(InputStream inputStream) throws IOException {
        return read(inputStream, ',');
    }
}
