// Input: Lombok、Jackson
// Output: SalesOrderListResponse DTO
// Pos: YonSuite 集成 - DTO

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SalesOrderListResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private SalesOrderData data;

    @Data
    public static class SalesOrderData {
        @JsonProperty("pageIndex")
        private Integer pageIndex;

        @JsonProperty("pageSize")
        private Integer pageSize;

        @JsonProperty("recordCount")
        private Integer recordCount;

        @JsonProperty("pageCount")
        private Integer pageCount;

        @JsonProperty("recordList")
        private List<SalesOrderRecord> recordList;
    }

    @Data
    public static class SalesOrderRecord {
        @JsonProperty("id")
        private String id;

        @JsonProperty("code")
        private String code;

        @JsonProperty("vouchdate")
        private String vouchdate;

        @JsonProperty("agentId")
        private String agentId;

        @JsonProperty("agentId_name")
        private String agentName;

        @JsonProperty("salesOrgId")
        private String salesOrgId;

        @JsonProperty("totalMoney")
        private Double totalMoney;

        @JsonProperty("payMoney")
        private Double payMoney;

        @JsonProperty("realMoney")
        private Double realMoney;

        @JsonProperty("nextStatusName")
        private String nextStatusName;

        @JsonProperty("pubts")
        private String pubts;
    }
}
