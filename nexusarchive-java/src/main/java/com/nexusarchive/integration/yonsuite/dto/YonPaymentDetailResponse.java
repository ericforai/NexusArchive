// Input: Jackson、Lombok、Java 标准库
// Output: YonPaymentDetailResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YonPaymentDetailResponse {

    private String code;
    private String message;
    private PaymentDetail data;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentDetail {
        private String id;
        private String code; // 单据编号
        private String billDate; // 单据日期
        private String financeOrgName; // 付款组织名称
        private String financeOrg; // 付款组织ID
        private String supplierName; // 供应商名称
        private String supplier; // 供应商ID
        private BigDecimal oriTaxIncludedAmount; // 付款金额 (原币含税)
        private String oriCurrencyName; // 原币名称
        private String settleState; // 结算状态
        private String verifyState; // 审批流状态
        private String createTime; // 创建时间
        private String creatorUserName; // 创建人

        // Capture all other fields
        private java.util.Map<String, Object> otherProps = new java.util.HashMap<>();

        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void setOther(String key, Object value) {
            this.otherProps.put(key, value);
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        public java.util.Map<String, Object> getOther() {
            return otherProps;
        }

        // 表体列表
        private List<PaymentBodyItem> bodyItem;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentBodyItem {
        private String id;
        private String srcBillNo; // 来源单据号
        private BigDecimal oriTaxIncludedAmount; // 行金额
        private String quickTypeName; // 款项类型

        // Capture all other fields (material, orderNo etc)
        private java.util.Map<String, Object> otherProps = new java.util.HashMap<>();

        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void setOther(String key, Object value) {
            this.otherProps.put(key, value);
        }

        @com.fasterxml.jackson.annotation.JsonAnyGetter
        public java.util.Map<String, Object> getOther() {
            return otherProps;
        }
    }
}
