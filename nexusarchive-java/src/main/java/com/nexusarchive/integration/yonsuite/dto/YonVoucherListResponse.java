package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 凭证列表查询响应 DTO
 * Reference: YS凭证列表查询API
 */
@Data
public class YonVoucherListResponse {
    
    private String code;
    private String message;
    private PageData data;
    
    @Data
    public static class PageData {
        private Integer pageIndex;
        private Integer pageSize;
        private Integer recordCount;
        private List<VoucherRecord> recordList;
    }
    
    @Data
    public static class VoucherRecord {
        private VoucherHeader header;
        private List<VoucherBody> body;
    }
    
    @Data
    public static class VoucherHeader {
        private String id;
        private Integer billcode;
        private String displaybillcode;
        private String description;
        private String period;
        private String displayname;
        private String srcsystem;
        private String voucherstatus;
        @JsonProperty("totaldebit_org")
        private BigDecimal totalDebitOrg;
        @JsonProperty("totalcredit_org")
        private BigDecimal totalCreditOrg;
        private String maketime;
        private String ts;
        private RefObject maker;
        private RefObject auditor;
        private RefObject tallyman;
        private AccBook accbook;
        private VoucherType vouchertype;
    }
    
    @Data
    public static class VoucherBody {
        private String id;
        private String voucherid;
        private Integer recordnumber;
        private String description;
        @JsonProperty("debit_original")
        private BigDecimal debitOriginal;
        @JsonProperty("credit_original")
        private BigDecimal creditOriginal;
        @JsonProperty("debit_org")
        private BigDecimal debitOrg;
        @JsonProperty("credit_org")
        private BigDecimal creditOrg;
        private AccSubject accsubject;
        private Currency currency;
    }
    
    @Data
    public static class RefObject {
        private String id;
        private String code;
        private String name;
    }
    
    @Data
    public static class AccBook {
        private String id;
        private String code;
        private String name;
        @JsonProperty("pk_org")
        private RefObject pkOrg;
    }
    
    @Data
    public static class VoucherType {
        private String id;
        private String code;
        private String name;
        private String voucherstr;
    }
    
    @Data
    public static class AccSubject {
        private String id;
        private String code;
        private String name;
        private String cashCategory;
    }
    
    @Data
    public static class Currency {
        private String id;
        private String code;
        private String name;
    }
}
