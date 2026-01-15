// Input: Lombok、Java 标准库
// Output: YonPaymentApplyListRequest 类
// Pos: YonSuite 集成 - DTO 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

/**
 * YonSuite 付款申请单列表请求
 * <p>
 * 对应 YonSuite API: /yonbip/EFI/paymentApply/list
 * </p>
 */
@Data
public class YonPaymentApplyListRequest {

    /**
     * 页码，默认 1
     */
    private Integer pageIndex = 1;

    /**
     * 每页数量，默认 100
     */
    private Integer pageSize = 100;

    /**
     * 来源系统
     */
    private String srcApp;

    /**
     * 审核状态：END-已审核，NOTSTART-未审核，PROGRESS-审核中
     */
    private String verifyState;

    /**
     * 申请单开始日期，格式 yyyy-MM-dd HH:mm:ss
     */
    private String beginDate;

    /**
     * 申请单结束日期，格式 yyyy-MM-dd HH:mm:ss
     */
    private String endDate;

    /**
     * 是否包含子单据
     */
    private Boolean isIncludeSub;

    /**
     * 申请人 ID
     */
    private String creatorId;

    /**
     * 申请人名称（模糊查询）
     */
    private String creatorName;

    /**
     * 是否查询挂起单据
     */
    private Boolean isHangUp;
}
