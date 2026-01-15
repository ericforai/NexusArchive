// Input: Lombok、Java 标准库
// Output: YonPaymentApplyFileRequest 类
// Pos: YonSuite 集成 - DTO 层

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;

/**
 * YonSuite 付款申请单文件查询请求
 * <p>
 * 通过文件 ID 获取付款申请单文件下载地址
 * </p>
 * <p>接口文档: /yonbip/EFI/paymentApply/file/url</p>
 */
@Data
public class YonPaymentApplyFileRequest {

    /**
     * 文件 ID 列表
     * <p>最多支持 20 个文件 ID</p>
     */
    private List<String> fileId;

    /**
     * 创建请求对象
     *
     * @param fileIds 文件 ID 列表
     * @return 请求对象
     */
    public static YonPaymentApplyFileRequest of(List<String> fileIds) {
        YonPaymentApplyFileRequest request = new YonPaymentApplyFileRequest();
        request.setFileId(fileIds);
        return request;
    }

    /**
     * 创建请求对象（单个文件）
     *
     * @param fileId 单个文件 ID
     * @return 请求对象
     */
    public static YonPaymentApplyFileRequest of(String fileId) {
        return of(List.of(fileId));
    }
}
