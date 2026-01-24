// Input: Lombok、Jackson
// Output: SalesOrderDetailResponse DTO
// Pos: YonSuite 集成 - DTO

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SalesOrderDetailResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private SalesOrderData data;

    @Data
    public static class SalesOrderData {
        @JsonProperty("id")
        private String id;

        @JsonProperty("code")
        private String code;

        @JsonProperty("vouchdate")
        private String vouchdate;

        @JsonProperty("orderDate")
        private String orderDate;

        @JsonProperty("agentId")
        private String agentId;

        @JsonProperty("agentId_name")
        private String agentName;

        @JsonProperty("salesOrgId")
        private String salesOrgId;

        @JsonProperty("totalMoney")
        private Double totalMoney;

        @JsonProperty("promotionMoney")
        private Double promotionMoney;

        @JsonProperty("rebateMoney")
        private Double rebateMoney;

        @JsonProperty("payMoney")
        private Double payMoney;

        @JsonProperty("realMoney")
        private Double realMoney;

        @JsonProperty("orderPayMoney")
        private Double orderPayMoney;

        @JsonProperty("orderRealMoney")
        private Double orderRealMoney;

        @JsonProperty("nextStatusName")
        private String nextStatusName;

        @JsonProperty("orderPrices")
        private OrderPrices orderPrices;

        @JsonProperty("orderDetails")
        private List<OrderDetail> orderDetails;

        @JsonProperty("pubts")
        private String pubts;
    }

    @Data
    public static class OrderPrices {
        @JsonProperty("totalMoneyOrigTaxfree")
        private Double totalMoneyOrigTaxfree;
    }

    @Data
    public static class OrderDetail {
        @JsonProperty("id")
        private String id;

        @JsonProperty("productId")
        private String productId;

        @JsonProperty("productCode")
        private String productCode;

        @JsonProperty("productName")
        private String productName;

        @JsonProperty("skuId")
        private String skuId;

        @JsonProperty("skuCode")
        private String skuCode;

        @JsonProperty("skuName")
        private String skuName;

        @JsonProperty("qty")
        private Double qty;

        @JsonProperty("oriUnitPrice")
        private Double oriUnitPrice;

        @JsonProperty("oriMoney")
        private Double oriMoney;

        @JsonProperty("oriTax")
        private Double oriTax;

        @JsonProperty("natMoney")
        private Double natMoney;

        @JsonProperty("natTax")
        private Double natTax;

        @JsonProperty("taxRate")
        private String taxRate;

        @JsonProperty("taxId")
        private String taxId;

        @JsonProperty("stockId")
        private String stockId;

        @JsonProperty("stockName")
        private String stockName;

        @JsonProperty("hopeReceiveDate")
        private String hopeReceiveDate;
    }
}
