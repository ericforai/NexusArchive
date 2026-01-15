// Input: Lombok、Java 标准库
// Output: YonPaymentApplyFileResponse 类
// Pos: YonSuite 集成 - DTO 层

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;

/**
 * YonSuite 付款申请单文件查询响应
 * <p>
 * 返回文件下载地址和元信息
 * </p>
 */
@Data
public class YonPaymentApplyFileResponse {

    /**
     * 状态码 (200 表示成功)
     */
    private String code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 文件数据列表
     */
    private List<FileData> data;

    /**
     * 文件数据
     */
    @Data
    public static class FileData {

        /**
         * 文件 ID
         */
        private String id;

        /**
         * 文件名称
         */
        private String fileName;

        /**
         * 下载地址 (带签名的 OSS URL)
         * <p>注意: 此 URL 有时效性，需及时使用</p>
         */
        private String downLoadUrl;

        /**
         * 判断是否有效
         */
        public boolean isValid() {
            return id != null && !id.isEmpty() &&
                   downLoadUrl != null && !downLoadUrl.isEmpty();
        }
    }

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return "200".equals(code);
    }

    /**
     * 获取有效的文件数据列表
     */
    public List<FileData> getValidFiles() {
        if (data == null) {
            return List.of();
        }
        return data.stream()
                .filter(FileData::isValid)
                .toList();
    }
}
