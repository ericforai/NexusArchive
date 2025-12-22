// Input: Jackson、Lombok、Java 标准库
// Output: YonVoucherDetailResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 凭证详情查询响应 DTO
 * Reference: YS凭证详情查询API
 */
@Data
public class YonVoucherDetailResponse {
    
    private String code;
    private String message;
    private VoucherDetail data;
    
    @Data
    public static class VoucherDetail {
        private String id;
        private String accBook;
        private RefObject accBookObj;
        private String periodUnion;
        private String voucherType;
        private RefObject voucherTypeObj;
        private Integer billCode;
        private String makeTime;
        private String displayName;
        private String voucherStatus;
        private String description;
        private String srcSystem;
        private List<RefObject> srcSystemObj;
        private Integer signStatus;
        private String attachmentQuantity;
        private BigDecimal totalDebitOrg;
        private BigDecimal totalCreditOrg;
        private String maker;
        private RefObject makerObj;
        private String auditor;
        private RefObject auditorObj;
        private String tallyMan;
        private RefObject tallyManObj;
        private String createTime;
        private String modifyTime;
        private String pubts;
        private String ts;
        private String auditTime;
        private String tallyTime;
        private List<VoucherBodyDetail> bodies;
    }
    
    @Data
    public static class VoucherBodyDetail {
        private String id;
        private String accSubject;
        private String accSubjectVid;
        private String description;
        private Integer recordNumber;
        private String currency;
        private String businessDate;
        private BigDecimal debitQuantity;
        private BigDecimal debitOriginal;
        private BigDecimal debitOrg;
        private BigDecimal creditQuantity;
        private BigDecimal creditOriginal;
        private BigDecimal creditOrg;
        private BigDecimal price;
        private BigDecimal quantity;
        private BigDecimal rateOrg;
        private String verifyNo;
        private Boolean checkFlag;
        private Map<String, String> auxiliary;
        private List<ClientAuxiliary> clientAuxiliary;
        private List<CashFlowItem> cashFlowItem;
    }
    
    @Data
    public static class RefObject {
        private String id;
        private String code;
        private String name;
    }
    
    @Data
    public static class ClientAuxiliary {
        private String dataType;
        private String docType;
        private String code;
        private String name;
        private String value;
    }
    
    @Data
    public static class CashFlowItem {
        private String itemId;
        private String itemCode;
        private String itemName;
        private Boolean negative;
        private BigDecimal amountOriginal;
        private BigDecimal amountOrg;
        private String innerOrg;
    }
}
