// Input: MyBatis-Plus、Java 标准库
// Output: ArcFileMetadataIndex 类
// Pos: 领域实体/模型
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 智能解析元数据索引
 * 对应表: arc_file_metadata_index
 */
@TableName("arc_file_metadata_index")
public class ArcFileMetadataIndex {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 关联的文件ID
     */
    private String fileId;

    /**
     * 发票代码
     */
    private String invoiceCode;

    /**
     * 发票号码
     */
    private String invoiceNumber;

    /**
     * 价税合计 (Total Amount)
     */
    private BigDecimal totalAmount;

    /**
     * 销售方名称
     */
    private String sellerName;

    /**
     * 开票日期
     */
    private LocalDate issueDate;

    /**
     * 解析时间
     */
    private LocalDateTime parsedTime;

    /**
     * 解析器类型 (e.g., XML_V1, PDF_REGEX)
     */
    private String parserType;

    public ArcFileMetadataIndex() {}

    public ArcFileMetadataIndex(String id, String fileId, String invoiceCode, String invoiceNumber, BigDecimal totalAmount, String sellerName, LocalDate issueDate, LocalDateTime parsedTime, String parserType) {
        this.id = id;
        this.fileId = fileId;
        this.invoiceCode = invoiceCode;
        this.invoiceNumber = invoiceNumber;
        this.totalAmount = totalAmount;
        this.sellerName = sellerName;
        this.issueDate = issueDate;
        this.parsedTime = parsedTime;
        this.parserType = parserType;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

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

    public LocalDateTime getParsedTime() { return parsedTime; }
    public void setParsedTime(LocalDateTime parsedTime) { this.parsedTime = parsedTime; }

    public String getParserType() { return parserType; }
    public void setParserType(String parserType) { this.parserType = parserType; }

    public static ArcFileMetadataIndexBuilder builder() {
        return new ArcFileMetadataIndexBuilder();
    }

    public static class ArcFileMetadataIndexBuilder {
        private ArcFileMetadataIndex entity = new ArcFileMetadataIndex();

        public ArcFileMetadataIndexBuilder id(String id) { entity.setId(id); return this; }
        public ArcFileMetadataIndexBuilder fileId(String fileId) { entity.setFileId(fileId); return this; }
        public ArcFileMetadataIndexBuilder invoiceCode(String invoiceCode) { entity.setInvoiceCode(invoiceCode); return this; }
        public ArcFileMetadataIndexBuilder invoiceNumber(String invoiceNumber) { entity.setInvoiceNumber(invoiceNumber); return this; }
        public ArcFileMetadataIndexBuilder totalAmount(BigDecimal totalAmount) { entity.setTotalAmount(totalAmount); return this; }
        public ArcFileMetadataIndexBuilder sellerName(String sellerName) { entity.setSellerName(sellerName); return this; }
        public ArcFileMetadataIndexBuilder issueDate(LocalDate issueDate) { entity.setIssueDate(issueDate); return this; }
        public ArcFileMetadataIndexBuilder parsedTime(LocalDateTime parsedTime) { entity.setParsedTime(parsedTime); return this; }
        public ArcFileMetadataIndexBuilder parserType(String parserType) { entity.setParserType(parserType); return this; }

        public ArcFileMetadataIndex build() { return entity; }
    }
}
