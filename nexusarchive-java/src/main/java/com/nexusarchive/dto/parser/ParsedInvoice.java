// Input: Java 标准库
// Output: ParsedInvoice 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ParsedInvoice {
    private String invoiceCode;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private String sellerName;
    private LocalDate issueDate;
    private boolean success;
    private String errorMessage;

    public ParsedInvoice() {}

    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public static ParsedInvoiceBuilder builder() {
        return new ParsedInvoiceBuilder();
    }

    public static class ParsedInvoiceBuilder {
        private ParsedInvoice dto = new ParsedInvoice();

        public ParsedInvoiceBuilder invoiceCode(String invoiceCode) { dto.setInvoiceCode(invoiceCode); return this; }
        public ParsedInvoiceBuilder invoiceNumber(String invoiceNumber) { dto.setInvoiceNumber(invoiceNumber); return this; }
        public ParsedInvoiceBuilder totalAmount(BigDecimal totalAmount) { dto.setTotalAmount(totalAmount); return this; }
        public ParsedInvoiceBuilder sellerName(String sellerName) { dto.setSellerName(sellerName); return this; }
        public ParsedInvoiceBuilder issueDate(LocalDate issueDate) { dto.setIssueDate(issueDate); return this; }
        public ParsedInvoiceBuilder success(boolean success) { dto.setSuccess(success); return this; }
        public ParsedInvoiceBuilder errorMessage(String errorMessage) { dto.setErrorMessage(errorMessage); return this; }

        public ParsedInvoice build() { return dto; }
    }
}
