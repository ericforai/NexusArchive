// Input: Lombok、Jackson
// Output: SalesOutListRequest 类
// Pos: YonSuite 集成 - DTO
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 销售出库单列表请求
 */
@Data
public class SalesOutListRequest {

    @JsonProperty("pageIndex")
    private Integer pageIndex = 1;

    @JsonProperty("pageSize")
    private Integer pageSize = 10;

    @JsonProperty("vouchdate")
    private String vouchdate;

    @JsonProperty("code")
    private String code;

    @JsonProperty("isSum")
    private Boolean isSum = false;

    @JsonProperty("simpleVOs")
    private List<SimpleVO> simpleVOs;

    @Data
    public static class SimpleVO {
        @JsonProperty("field")
        private String field;

        @JsonProperty("op")
        private String op;

        @JsonProperty("value1")
        private String value1;

        @JsonProperty("value2")
        private String value2;
    }
}
