package com.nexusarchive.service.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 PDFBox 的 PDF 矢量指令解析器
 * 不使用 OCR，直接提取文字指令坐标
 */
@Service
public class PdfInvoiceParser implements InvoiceParserService {

    @Override
    public boolean supports(String fileType) {
        return "pdf".equalsIgnoreCase(fileType);
    }

    @Override
    public Map<String, Object> parse(File file) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> regions = new HashMap<>();
        result.put("regions", regions);

        try (PDDocument document = PDDocument.load(file)) {
            // 获取页面尺寸 (假设发票只有一页或处理第一页)
            float width = document.getPage(0).getMediaBox().getWidth();
            float height = document.getPage(0).getMediaBox().getHeight();
            result.put("width", width);
            result.put("height", height);

            // 自定义 Stripper 提取所有文字坐标
            InvoiceTextStripper stripper = new InvoiceTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            stripper.getText(document);

            List<TextToken> tokens = stripper.tokens;

            // 锚点定位逻辑
            // 1. 合计金额
            TextToken totalAnchor = findToken(tokens, "小写", "价税合计");
            if (totalAnchor != null) {
                // 寻找锚点右下方的数字
                TextToken amountToken = findNearestNumber(tokens, totalAnchor);
                if (amountToken != null) {
                    regions.put("total_amount", amountToken.toRect());
                    // 增加：提取数值文本
                    result.put("total_amount_value", amountToken.text.replaceAll("[￥¥,]", "").trim());
                }
            }
            
            // 2. 开票日期
            TextToken dateAnchor = findToken(tokens, "开票日期");
            if (dateAnchor != null) {
                TextToken dateToken = findNearestDate(tokens, dateAnchor);
                if (dateToken != null) {
                    regions.put("invoice_date", dateToken.toRect());
                    // 格式化日期
                    try {
                        String dateStr = dateToken.text.replaceAll("[年月日]", "-");
                        // 移除除了数字和横杠以外的字符
                        dateStr = dateStr.replaceAll("[^0-9-]", "");
                        
                        // 使用正则提取标准的 YYYY-MM-DD 格式
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2})");
                        java.util.regex.Matcher matcher = pattern.matcher(dateStr);
                        
                        if (matcher.find()) {
                             String cleanDate = matcher.group(1);
                             // 尝试解析验证
                             java.time.LocalDate.parse(cleanDate); // will throw if invalid
                             result.put("invoice_date_value", cleanDate);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }

            // 3. 税额 (通常在表格中，定位 "税额" 关键字)
            // 简化逻辑：找到含有 "税额" 的表头，取其下方或附近的数字
            // 演示目的：寻找金额较小的几个数字作为候选
             List<TextToken> taxAnchors = findTokens(tokens, "税额");
             int index = 0;
             for (TextToken anchor : taxAnchors) {
                 TextToken taxVal = findNearestNumber(tokens, anchor);
                 if (taxVal != null) {
                     // 避免和合计金额重复
                     if (!regions.containsValue(taxVal.toRect())) {
                         regions.put("tax_entry_" + (index++), taxVal.toRect());
                     }
                 }
             }

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse PDF", e);
        }

        return result;
    }

    // 也就是 TextBlock
    private static class TextToken {
        String text;
        float x, y, width, height;

        public TextToken(String text, float x, float y, float width, float height) {
            this.text = text;
            this.x = x;
            this.y = y; // PDFBox y is from bottom (sometimes), need verification. Wait, Stripper handles transformation usually.
            this.width = width;
            this.height = height;
        }

        public Map<String, Float> toRect() {
            Map<String, Float> rect = new HashMap<>();
            rect.put("x", x);
            rect.put("y", y); // Check coordinate system match with Frontend
            rect.put("w", width);
            rect.put("h", height);
            return rect;
        }
    }

    private TextToken findToken(List<TextToken> tokens, String... keywords) {
        for (TextToken token : tokens) {
            for (String kw : keywords) {
                if (token.text.contains(kw)) {
                    return token;
                }
            }
        }
        return null;
    }
    
    private List<TextToken> findTokens(List<TextToken> tokens, String keyword) {
        return tokens.stream().filter(t -> t.text.contains(keyword)).collect(Collectors.toList());
    }

    private TextToken findNearestNumber(List<TextToken> tokens, TextToken anchor) {
        TextToken bestMatch = null;
        double minDist = Double.MAX_VALUE;

        for (TextToken token : tokens) {
            // 过滤非数字
            String cleanText = token.text.replaceAll("[￥¥,]", "").trim();
            if (!cleanText.matches("-?\\d+\\.?\\d*")) {
                continue;
            }

            // 必须在锚点右侧或下方 (允许一定的垂直偏差以捕获同一行)
            if (token.x < anchor.x && Math.abs(token.y - anchor.y) > 20 && token.y < anchor.y) continue;
            
            // 简单的欧氏距离
            double dist = Math.sqrt(Math.pow(token.x - anchor.x, 2) + Math.pow(token.y - anchor.y, 2));
            if (dist < minDist && dist < 500) { // 放宽搜索范围到 500
                minDist = dist;
                bestMatch = token;
            }
        }
        return bestMatch;
    }

    private TextToken findNearestDate(List<TextToken> tokens, TextToken anchor) {
        TextToken bestMatch = null;
        double minDist = Double.MAX_VALUE;

        for (TextToken token : tokens) {
            // 匹配日期格式：2023年11月02日 或 2023-11-02
            String cleanText = token.text.trim();
            if (!cleanText.matches(".*\\d{4}[年-]\\d{1,2}[月-]\\d{1,2}.*")) {
                continue;
            }

            // 必须在锚点右侧或附近
            // 日期通常在"开票日期"标签的右侧
            
            double dist = Math.sqrt(Math.pow(token.x - anchor.x, 2) + Math.pow(token.y - anchor.y, 2));
            if (dist < minDist && dist < 300) {
                minDist = dist;
                bestMatch = token;
            }
        }
        return bestMatch;
    }

    /**
     * 扩展 PDFTextStripper 以捕获坐标
     */
    private static class InvoiceTextStripper extends PDFTextStripper {
        List<TextToken> tokens = new ArrayList<>();

        public InvoiceTextStripper() throws IOException {
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            if (textPositions.isEmpty()) return;

            TextPosition first = textPositions.get(0);
            TextPosition last = textPositions.get(textPositions.size() - 1);

            float x = first.getX();
            float y = first.getY();
            float w = last.getX() + last.getWidth() - x;
            float h = first.getHeight(); // 简化处理，取第一个字符高度

            tokens.add(new TextToken(string, x, y, w, h));
            super.writeString(string, textPositions);
        }
    }
}
