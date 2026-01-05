// com/nexusarchive/integration/yonsuite/dto/YonRefundFileResponse.java
// 输入: YonSuite API 响应
// 输出: 退款文件下载信息
// 位置: YonSuite 集成 - DTO
// 更新时请同步更新本文件注释及所属目录的 md

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;

/**
 * YonSuite 付款退款单文件下载响应
 *
 * API: /yonbip/EFI/apRefund/file/url
 */
@Data
public class YonRefundFileResponse {

    /**
     * 状态码
     */
    private String code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 返回数据
     */
    private List<RefundFileInfo> data;

    /**
     * 退款文件信息
     */
    @Data
    public static class RefundFileInfo {

        /**
         * 下载地址
         */
        private String downLoadUrl;

        /**
         * 文件名称
         */
        private String fileName;

        /**
         * 文件 ID
         */
        private String id;
    }
}
