// Input: XML + OFD/PDF 文件
// Output: 完整性校验实现
// Pos: NexusCore compliance/integrity
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.compliance;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * 完整性检测实现
 * 
 * 校验 XML 元数据与 OFD/PDF 版式文件内容一致性
 */
@Slf4j
@Service
public class DefaultIntegrityChecker implements IntegrityChecker {
    private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");

    @Override
    public IntegrityCheckResult verify(Path xmlPath, Path formatPath) {
        Objects.requireNonNull(xmlPath, "xmlPath must not be null");
        Objects.requireNonNull(formatPath, "formatPath must not be null");

        try {
            InvoiceMetadata xmlMetadata = parseXmlMetadata(xmlPath);
            InvoiceMetadata formatMetadata = parseFormatMetadata(formatPath);
            List<IntegrityDiff> diffs = compareMetadata(xmlMetadata, formatMetadata);
            
            if (diffs.isEmpty()) {
                return IntegrityCheckResult.success();
            }
            return IntegrityCheckResult.failure(diffs);
        } catch (Exception ex) {
            List<IntegrityDiff> diffs = List.of(
                new IntegrityDiff("parse_error", "", "", "解析失败: " + ex.getMessage())
            );
            return IntegrityCheckResult.failure(diffs);
        }
    }

    private InvoiceMetadata parseXmlMetadata(Path xmlPath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(Files.newInputStream(xmlPath));
        
        return InvoiceMetadata.builder()
                .invoiceNo(extractText(doc, "InvoiceNo", "fphm"))
                .invoiceCode(extractText(doc, "InvoiceCode", "fpdm"))
                .amount(extractAmount(doc, "Amount", "je"))
                .taxAmount(extractAmount(doc, "TaxAmount", "se"))
                .invoiceDate(extractText(doc, "InvoiceDate", "kprq"))
                .sellerName(extractText(doc, "SellerName", "xfmc"))
                .buyerName(extractText(doc, "BuyerName", "gfmc"))
                .build();
    }

    private InvoiceMetadata parseFormatMetadata(Path formatPath) throws IOException {
        String fileName = formatPath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".pdf")) {
            return parsePdfMetadata(formatPath);
        } else if (fileName.endsWith(".ofd")) {
            return parseOfdMetadata(formatPath);
        }
        throw new IllegalArgumentException("Unsupported format: " + fileName);
    }

    private InvoiceMetadata parsePdfMetadata(Path pdfPath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            return extractMetadataFromText(text);
        }
    }

    private InvoiceMetadata parseOfdMetadata(Path ofdPath) throws IOException {
        // [P1-FIX] 使用 ofdrw-reader 正确解析 OFD 文件
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(ofdPath)) {
            // 使用 ContentExtractor.extractAll() 提取所有页面文本
            org.ofdrw.reader.ContentExtractor extractor = new org.ofdrw.reader.ContentExtractor(reader);
            List<String> pageTexts = extractor.extractAll();
            String combinedText = String.join("\n", pageTexts);
            return extractMetadataFromText(combinedText);
        } catch (Exception e) {
            // OFD 解析失败时回退到简化实现
            log.warn("OFD 解析失败，回退到简化模式: {}", ofdPath, e);
            String text = Files.readString(ofdPath);
            return extractMetadataFromText(text);
        }
    }

    private InvoiceMetadata extractMetadataFromText(String text) {
        // 简化实现：通过正则提取关键字段
        return InvoiceMetadata.builder()
                .invoiceNo(extractPattern(text, "发票号码[：:]?\\s*(\\d+)"))
                .invoiceCode(extractPattern(text, "发票代码[：:]?\\s*(\\d+)"))
                .amount(extractAmountPattern(text, "金额[（(]?不含税[）)]?[：:]?\\s*[¥￥]?([\\d,.]+)"))
                .taxAmount(extractAmountPattern(text, "税额[：:]?\\s*[¥￥]?([\\d,.]+)"))
                .invoiceDate(extractPattern(text, "开票日期[：:]?\\s*(\\d{4}年\\d{1,2}月\\d{1,2}日|\\d{4}-\\d{2}-\\d{2})"))
                .sellerName(extractPattern(text, "销售方[名称称]*[：:]?\\s*(.+?)(?=\\n|$)"))
                .buyerName(extractPattern(text, "购买方[名称称]*[：:]?\\s*(.+?)(?=\\n|$)"))
                .build();
    }

    private List<IntegrityDiff> compareMetadata(InvoiceMetadata xml, InvoiceMetadata format) {
        List<IntegrityDiff> diffs = new ArrayList<>();
        
        compareField(diffs, "invoice_no", xml.invoiceNo(), format.invoiceNo());
        compareField(diffs, "invoice_code", xml.invoiceCode(), format.invoiceCode());
        compareAmount(diffs, "amount", xml.amount(), format.amount());
        compareAmount(diffs, "tax_amount", xml.taxAmount(), format.taxAmount());
        compareField(diffs, "invoice_date", normalizeDate(xml.invoiceDate()), normalizeDate(format.invoiceDate()));
        
        return diffs;
    }

    private void compareField(List<IntegrityDiff> diffs, String fieldName, String xmlVal, String formatVal) {
        if (xmlVal == null && formatVal == null) {
            return;
        }
        if (xmlVal == null || !xmlVal.equals(formatVal)) {
            diffs.add(IntegrityDiff.of(fieldName, xmlVal, formatVal));
        }
    }

    private void compareAmount(List<IntegrityDiff> diffs, String fieldName, BigDecimal xmlVal, BigDecimal formatVal) {
        if (xmlVal == null && formatVal == null) {
            return;
        }
        if (xmlVal == null || formatVal == null) {
            diffs.add(IntegrityDiff.of(fieldName, String.valueOf(xmlVal), String.valueOf(formatVal)));
            return;
        }
        if (xmlVal.subtract(formatVal).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            diffs.add(IntegrityDiff.of(fieldName, xmlVal.toPlainString(), formatVal.toPlainString()));
        }
    }

    private String extractText(Document doc, String... tagNames) {
        for (String tagName : tagNames) {
            NodeList nodes = doc.getElementsByTagName(tagName);
            if (nodes.getLength() > 0) {
                return nodes.item(0).getTextContent().trim();
            }
        }
        return null;
    }

    private BigDecimal extractAmount(Document doc, String... tagNames) {
        String text = extractText(doc, tagNames);
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.replaceAll("[,，]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extractPattern(String text, String pattern) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(text);
        if (m.find() && m.groupCount() >= 1) {
            return m.group(1).trim();
        }
        return null;
    }

    private BigDecimal extractAmountPattern(String text, String pattern) {
        String value = extractPattern(text, pattern);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.replaceAll("[,，]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeDate(String date) {
        if (date == null) {
            return null;
        }
        return date.replaceAll("[年月]", "-").replaceAll("日", "").trim();
    }

    /**
     * 发票元数据
     */
    private record InvoiceMetadata(
        String invoiceNo,
        String invoiceCode,
        BigDecimal amount,
        BigDecimal taxAmount,
        String invoiceDate,
        String sellerName,
        String buyerName
    ) {
        static Builder builder() {
            return new Builder();
        }

        static class Builder {
            private String invoiceNo;
            private String invoiceCode;
            private BigDecimal amount;
            private BigDecimal taxAmount;
            private String invoiceDate;
            private String sellerName;
            private String buyerName;

            Builder invoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; return this; }
            Builder invoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; return this; }
            Builder amount(BigDecimal amount) { this.amount = amount; return this; }
            Builder taxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; return this; }
            Builder invoiceDate(String invoiceDate) { this.invoiceDate = invoiceDate; return this; }
            Builder sellerName(String sellerName) { this.sellerName = sellerName; return this; }
            Builder buyerName(String buyerName) { this.buyerName = buyerName; return this; }

            InvoiceMetadata build() {
                return new InvoiceMetadata(invoiceNo, invoiceCode, amount, taxAmount, 
                        invoiceDate, sellerName, buyerName);
            }
        }
    }
}
