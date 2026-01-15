// Input: Jackson、Lombok、Java 标准库
// Output: YonCloseInfoResponse 类
// Pos: 数据传输对象
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 关账状态查询响应 DTO
 * Reference: YonSuite /yonbip/EFI/closeInfo API
 */
@Data
public class YonCloseInfoResponse {

    private String code;
    private String message;
    private CloseInfoData data;

    @Data
    public static class CloseInfoData {
        /**
         * 关账状态，true=已关账
         */
        @JsonProperty("closeStatus")
        private boolean closed;

        /**
         * 关账时间，格式 "yyyy-MM-dd HH:mm:ss"
         */
        @JsonProperty("closeTime")
        private String closeTime;
    }
}
