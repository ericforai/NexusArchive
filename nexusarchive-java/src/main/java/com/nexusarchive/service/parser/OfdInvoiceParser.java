package com.nexusarchive.service.parser;

import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.keyword.KeywordExtractor;
import org.ofdrw.reader.keyword.KeywordPosition;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 OFDRW 的国标版式解析器
 * 
 * 注意: OFDRW 的 KeywordExtractor 用于关键词定位
 */
@Service
public class OfdInvoiceParser implements InvoiceParserService {

    @Override
    public boolean supports(String fileType) {
        return "ofd".equalsIgnoreCase(fileType);
    }

    @Override
    public Map<String, Object> parse(File file) {
         Map<String, Object> result = new HashMap<>();
         Map<String, Object> regions = new HashMap<>();
         result.put("regions", regions);

         try (OFDReader reader = new OFDReader(file.toPath())) {
             // OFD 页面尺寸 - A4 默认
             double pageWidth = 210.0 * 2.83; // mm to pt approx
             double pageHeight = 297.0 * 2.83;
             result.put("width", pageWidth);
             result.put("height", pageHeight);

             // 利用 KeywordExtractor 提取关键词位置
             // OFDRW 2.x API: getKeyWordPositionList(reader, String...)
             String[] keywords = {"价税合计", "税额", "小写"};

             List<KeywordPosition> positions;
             try {
                 positions = KeywordExtractor.getKeyWordPositionList(reader, keywords);
             } catch (org.dom4j.DocumentException e) {
                 throw new RuntimeException("Failed to parse OFD document", e);
             }

             for (KeywordPosition pos : positions) {
                 String kw = pos.getKeyword(); // 正确的方法名
                 
                 // 获取边界框 - ST_Box 类型
                 org.ofdrw.core.basicType.ST_Box box = pos.getBox();
                 if (box == null) continue;
                 
                 Map<String, Double> rect = new HashMap<>();
                 rect.put("x", box.getTopLeftX());
                 rect.put("y", box.getTopLeftY());
                 rect.put("w", box.getWidth());
                 rect.put("h", box.getHeight());

                 if ("价税合计".equals(kw) || "小写".equals(kw)) {
                     // 偏移一下去抓金额（模拟）
                     Map<String, Double> amountRect = new HashMap<>(rect);
                     amountRect.put("x", box.getTopLeftX() + 50);
                     amountRect.put("w", 100.0);
                     regions.put("total_amount", amountRect);
                 }
                 
                 if ("税额".equals(kw)) {
                     regions.put("tax_entry_mock", rect);
                 }
             }

         } catch (IOException e) {
             throw new RuntimeException("Failed to parse OFD", e);
         }
         
         return result;
    }
}
