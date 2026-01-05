// com/nexusarchive/integration/yonsuite/dto/YonRefundFileRequest.java
// 输入: fileId 数组
// 输出: YonSuite 退款文件下载请求
// 位置: YonSuite 集成 - DTO
// 更新时请同步更新本文件注释及所属目录的 md

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;

/**
 * YonSuite 付款退款单文件下载请求
 *
 * API: /yonbip/EFI/apRefund/file/url
 */
@Data
public class YonRefundFileRequest {

    /**
     * 文件 ID 列表
     * 最大请求量: 20
     */
    private List<String> fileId;
}
