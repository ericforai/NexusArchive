// Input: Lombok、Jackson
// Output: SalesOutDetailResponse 类
// Pos: YonSuite 集成 - DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 销售出库单详情响应
 */
@Data
public class SalesOutDetailResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private SalesOutDetail data;

    @Data
    public static class SalesOutDetail {
        @JsonProperty("id")
        private String id;

        @JsonProperty("code")
        private String code;

        @JsonProperty("vouchdate")
        private String vouchdate;

        @JsonProperty("cust_name")
        private String custName;

        @JsonProperty("warehouse_name")
        private String warehouseName;

        @JsonProperty("operator_name")
        private String operatorName;

        @JsonProperty("diliverStatus")
        private String diliverStatus;

        @JsonProperty("status")
        private String status;

        @JsonProperty("totalQuantity")
        private String totalQuantity;

        @JsonProperty("details")
        private List<SalesOutDetailItem> details;
    }

    @Data
    public static class SalesOutDetailItem {
        @JsonProperty("product_cCode")
        private String productCode;

        @JsonProperty("product_cName")
        private String productName;

        @JsonProperty("qty")
        private Double quantity;

        @JsonProperty("unit")
        private String unit;

        @JsonProperty("natMoney")
        private Double natMoney;

        @JsonProperty("taxRate")
        private Double taxRate;
    }
}
