// Input: Lombok、Apache、Spring Framework、Java 标准库、等
// Output: PdfInvoiceParser 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.parser.impl;

import com.nexusarchive.dto.parser.ParsedInvoice;
import com.nexusarchive.service.parser.InvoiceParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class PdfInvoiceParser implements InvoiceParser {

    private static final Pattern INVOICE_CODE_REGEX = Pattern.compile("发票代码[:：]\\s*(\\d{10,12})");
    private static final Pattern INVOICE_NO_REGEX = Pattern.compile("发票号码[:：]\\s*(\\d{8,20})");
    private static final Pattern TOTAL_AMOUNT_REGEX = Pattern.compile("(小写|Total).*?[￥¥]?\\s*(\\d+\\.\\d{2})");
    private static final Pattern DATE_REGEX = Pattern.compile("开票日期[:：]\\s*(\\d{4})\\s*年\\s*(\\d{2})\\s*月\\s*(\\d{2})\\s*日");
    private static final Pattern SELLER_NAME_REGEX = Pattern.compile("称[:：]\\s*([\\u4e00-\\u9fa5()（）]+公司|[\\u4e00-\\u9fa5()（）]+店)");

    @Override
    public boolean supports(String fileType) {
        return "PDF".equalsIgnoreCase(fileType);
    }

    @Override
    public ParsedInvoice parse(File file) {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            
            ParsedInvoice.ParsedInvoiceBuilder builder = ParsedInvoice.builder();
            
            // Invoice Code
            Matcher mCode = INVOICE_CODE_REGEX.matcher(text);
            if (mCode.find()) builder.invoiceCode(mCode.group(1));
            
            // Invoice No
            Matcher mNo = INVOICE_NO_REGEX.matcher(text);
            if (mNo.find()) builder.invoiceNumber(mNo.group(1));
            
            // Total Amount
            Matcher mAmount = TOTAL_AMOUNT_REGEX.matcher(text);
            if (mAmount.find()) {
                try {
                    builder.totalAmount(new BigDecimal(mAmount.group(2)));
                } catch (Exception ignored) {}
            }
            
            // Date
            Matcher mDate = DATE_REGEX.matcher(text);
            if (mDate.find()) {
                try {
                    LocalDate date = LocalDate.of(
                            Integer.parseInt(mDate.group(1)),
                            Integer.parseInt(mDate.group(2)),
                            Integer.parseInt(mDate.group(3))
                    );
                    builder.issueDate(date);
                } catch (Exception ignored) {}
            }
            
            // Seller Name (Heuristic)
            // Usually "名称" appears twice, once for Buyer, once for Seller.
            // Seller usually appears later or in a specific block.
            // This is a weak regex, but better than nothing.
            Matcher mSeller = SELLER_NAME_REGEX.matcher(text);
            while (mSeller.find()) {
                String name = mSeller.group(1);
                // Simple heuristic: Seller name often contains "公司"
                if (name.length() > 4) {
                    builder.sellerName(name);
                    // We might pick the first one (Buyer) or second (Seller).
                    // For now, just pick the last one found, as Seller block is often at bottom or right?
                    // Actually in China VAT Invoice, Seller is at bottom.
                }
            }
            
            return builder.success(true).build();
            
        } catch (Exception e) {
            log.error("PDF Parsing failed for file: {}", file.getName(), e);
            return ParsedInvoice.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
