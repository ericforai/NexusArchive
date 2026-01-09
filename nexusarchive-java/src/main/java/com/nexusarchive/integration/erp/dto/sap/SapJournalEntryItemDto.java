// Input: Jackson、Lombok、Java 标准库
// Output: SapJournalEntryItemDto 类
// Pos: 数据传输对象 - SAP Journal Entry Item OData 响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto.sap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * SAP Journal Entry Item OData 响应 DTO
 * 代表 SAP S/4HANA 凭证分录项
 *
 * @author Agent D (基础设施工程师)
 * @see <a href="https://help.sap.com/doc/">SAP S/4HANA API Journal Entry Item</a>
 */
@Data
public class SapJournalEntryItemDto {

    /**
     * 凭证行项目号
     */
    @JsonProperty("JournalEntryItem")
    private String journalEntryItem;

    /**
     * 总账科目
     */
    @JsonProperty("GLAccount")
    private String glAccount;

    /**
     * 借贷标识
     * S = Soll (借方)
     * H = Haben (贷方)
     */
    @JsonProperty("DebitCreditCode")
    private String debitCreditCode;

    /**
     * 交易货币金额
     */
    @JsonProperty("AmountInTransactionCurrency")
    private String amountInTransactionCurrency;

    /**
     * 交易货币
     */
    @JsonProperty("TransactionCurrency")
    private String transactionCurrency;

    /**
     * 行项目文本
     */
    @JsonProperty("DocumentItemText")
    private String documentItemText;

    /**
     * 成本中心
     */
    @JsonProperty("CostCenter")
    private String costCenter;

    /**
     * 利润中心
     */
    @JsonProperty("ProfitCenter")
    private String profitCenter;

    /**
     * 税码
     */
    @JsonProperty("TaxCode")
    private String taxCode;

    /**
     * 参考键 (业务事务)
     */
    @JsonProperty("ReferenceKeyByBusinessTransaction")
    private String referenceKey;
}
