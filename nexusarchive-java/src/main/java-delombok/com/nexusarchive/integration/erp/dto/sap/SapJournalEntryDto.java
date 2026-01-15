// Input: Jackson、Lombok、Java 标准库
// Output: SapJournalEntryDto 类
// Pos: 数据传输对象 - SAP Journal Entry OData 响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto.sap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * SAP Journal Entry OData 响应 DTO
 * 代表 SAP S/4HANA 会计凭证
 *
 * <p>参考 SAP S/4HANA API Journal Entry – Post
 * OData 服务: API_JOURNAL_ENTRY_SRV
 *
 * @author Agent D (基础设施工程师)
 * @see <a href="https://help.sap.com/doc/">SAP S/4HANA API Journal Entry</a>
 */
@Data
public class SapJournalEntryDto {

    /**
     * 凭证号
     */
    @JsonProperty("JournalEntry")
    private String journalEntry;

    /**
     * 公司代码
     */
    @JsonProperty("CompanyCode")
    private String companyCode;

    /**
     * 会计年度
     */
    @JsonProperty("FiscalYear")
    private String fiscalYear;

    /**
     * 过账日期
     */
    @JsonProperty("PostingDate")
    private String postingDate;

    /**
     * 凭证日期
     */
    @JsonProperty("DocumentDate")
    private String documentDate;

    /**
     * 凭证抬头文本
     */
    @JsonProperty("DocumentHeaderText")
    private String documentHeaderText;

    /**
     * 创建日期
     */
    @JsonProperty("CreationDate")
    private String creationDate;

    /**
     * 创建时间
     */
    @JsonProperty("CreationTime")
    private String creationTime;

    /**
     * 创建人
     */
    @JsonProperty("CreatedByUser")
    private String createdByUser;

    /**
     * 参考凭证
     */
    @JsonProperty("ReferenceDocument")
    private String referenceDocument;

    /**
     * 凭证类型
     */
    @JsonProperty("JournalEntryType")
    private String journalEntryType;

    /**
     * 分录项列表 (通过导航属性 to_JournalEntryItem)
     */
    @JsonProperty("to_JournalEntryItem")
    private List<SapJournalEntryItemDto> items;

    /**
     * 附件关联 (通过导航属性 to_Attachment)
     */
    @JsonProperty("to_Attachment")
    private List<SapAttachmentDto> attachments;

    /**
     * 获取借方金额合计
     */
    public double getTotalDebitAmount() {
        if (items == null) {
            return 0.0;
        }
        return items.stream()
            .filter(item -> "S".equals(item.getDebitCreditCode()))
            .mapToDouble(item -> {
                try {
                    return Double.parseDouble(item.getAmountInTransactionCurrency() != null
                        ? item.getAmountInTransactionCurrency() : "0");
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            })
            .sum();
    }

    /**
     * 获取贷方金额合计
     */
    public double getTotalCreditAmount() {
        if (items == null) {
            return 0.0;
        }
        return items.stream()
            .filter(item -> "H".equals(item.getDebitCreditCode()))
            .mapToDouble(item -> {
                try {
                    return Double.parseDouble(item.getAmountInTransactionCurrency() != null
                        ? item.getAmountInTransactionCurrency() : "0");
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            })
            .sum();
    }

    /**
     * 获取交易货币 (从第一条分录获取)
     */
    public String getTransactionCurrency() {
        if (items != null && !items.isEmpty()) {
            String currency = items.get(0).getTransactionCurrency();
            return currency != null ? currency : "CNY";
        }
        return "CNY";
    }
}
