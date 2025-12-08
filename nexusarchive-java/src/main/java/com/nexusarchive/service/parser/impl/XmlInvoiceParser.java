package com.nexusarchive.service.parser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.nexusarchive.dto.parser.ParsedInvoice;
import com.nexusarchive.service.parser.InvoiceParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;

@Slf4j
@Component
public class XmlInvoiceParser implements InvoiceParser {

    private final XmlMapper xmlMapper = new XmlMapper();
    private static final DateTimeFormatter DATE_FMT_1 = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DATE_FMT_2 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public boolean supports(String fileType) {
        return "XML".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedInvoice parse(File file) {
        try {
            JsonNode root = xmlMapper.readTree(file);
            ParsedInvoice.ParsedInvoiceBuilder builder = ParsedInvoice.builder();
            
            // 递归查找字段
            findAndSet(root, builder);
            
            return builder.success(true).build();
        } catch (Exception e) {
            log.error("XML Parsing failed for file: {}", file.getName(), e);
            return ParsedInvoice.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private void findAndSet(JsonNode node, ParsedInvoice.ParsedInvoiceBuilder builder) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String key = field.getKey();
                JsonNode value = field.getValue();
                
                if (value.isValueNode()) {
                    String text = value.asText();
                    if (text != null && !text.isEmpty()) {
                        matchField(key, text, builder);
                    }
                } else {
                    findAndSet(value, builder);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                findAndSet(item, builder);
            }
        }
    }

    private void matchField(String key, String value, ParsedInvoice.ParsedInvoiceBuilder builder) {
        String k = key.toLowerCase();
        
        // Invoice Code
        if (k.contains("invoicecode") || k.equals("fpdm")) {
            builder.invoiceCode(value);
        }
        // Invoice Number
        else if (k.contains("invoiceno") || k.contains("invoicenumber") || k.equals("fphm")) {
            builder.invoiceNumber(value);
        }
        // Total Amount
        else if (k.contains("totalamount") || k.equals("jshj") || k.equals("hjje")) { // jshj=价税合计, hjje=合计金额
            try {
                builder.totalAmount(new BigDecimal(value));
            } catch (Exception ignored) {}
        }
        // Seller Name
        else if (k.contains("sellername") || k.equals("xhfmc") || k.equals("nsrmc")) {
            builder.sellerName(value);
        }
        // Issue Date
        else if (k.contains("issuedate") || k.equals("kprq")) {
            try {
                if (value.length() == 8) {
                    builder.issueDate(LocalDate.parse(value, DATE_FMT_1));
                } else if (value.contains("-")) {
                    builder.issueDate(LocalDate.parse(value.substring(0, 10), DATE_FMT_2));
                }
            } catch (Exception ignored) {}
        }
    }
}
