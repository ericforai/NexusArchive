// Input: Lombok、Jackson
// Output: SalesOrderListRequest DTO
// Pos: YonSuite 集成 - DTO

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SalesOrderListRequest {

    @JsonProperty("pageIndex")
    private Integer pageIndex = 1;

    @JsonProperty("pageSize")
    private Integer pageSize = 100;

    @JsonProperty("code")
    private String code;

    @JsonProperty("nextStatusName")
    private String nextStatusName;

    @JsonProperty("open_vouchdate_begin")
    private String vouchdateBegin;

    @JsonProperty("open_vouchdate_end")
    private String vouchdateEnd;

    @JsonProperty("simpleVOs")
    private List<SimpleVO> simpleVOs;

    @JsonProperty("queryOrders")
    private List<QueryOrder> queryOrders;

    @Data
    public static class SimpleVO {
        @JsonProperty("field")
        private String field;

        @JsonProperty("op")
        private String op;

        @JsonProperty("value1")
        private String value1;

        @JsonProperty("logicOp")
        private String logicOp;
    }

    @Data
    public static class QueryOrder {
        @JsonProperty("field")
        private String field;

        @JsonProperty("order")
        private String order;
    }
}
