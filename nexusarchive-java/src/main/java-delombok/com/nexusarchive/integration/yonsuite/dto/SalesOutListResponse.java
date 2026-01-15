// Input: Lombok、Jackson
// Output: SalesOutListResponse 类
// Pos: YonSuite 集成 - DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 销售出库单列表响应
 */
@Data
public class SalesOutListResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private SalesOutData data;

    @Data
    public static class SalesOutData {
        @JsonProperty("pageIndex")
        private Integer pageIndex;

        @JsonProperty("pageSize")
        private Integer pageSize;

        @JsonProperty("recordCount")
        private Integer recordCount;

        @JsonProperty("pageCount")
        private Integer pageCount;

        @JsonProperty("recordList")
        private List<SalesOutRecord> recordList;
    }

    @Data
    public static class SalesOutRecord {
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

        @JsonProperty("totalQuantity")
        private String totalQuantity;
    }
}
