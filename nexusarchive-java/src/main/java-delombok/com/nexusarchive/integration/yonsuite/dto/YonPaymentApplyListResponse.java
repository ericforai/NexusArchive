// Input: Lombok、Jackson、Java 标准库
// Output: YonPaymentApplyListResponse 类
// Pos: YonSuite 集成 - DTO 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * YonSuite 付款申请单列表响应
 * <p>
 * 对应 YonSuite API: /yonbip/EFI/paymentApply/list
 * </p>
 */
@Data
public class YonPaymentApplyListResponse {

    /**
     * 响应码，200 表示成功
     */
    @JsonProperty("code")
    private String code;

    /**
     * 响应消息
     */
    @JsonProperty("message")
    private String message;

    /**
     * 响应数据
     */
    @JsonProperty("data")
    private PaymentApplyData data;

    /**
     * 判断响应是否成功
     */
    public boolean isSuccess() {
        return "200".equals(code);
    }

    /**
     * 付款申请单数据
     */
    @Data
    public static class PaymentApplyData {

        /**
         * 记录列表
         */
        @JsonProperty("recordList")
        private List<PaymentApplyRecord> recordList;

        /**
         * 总页数
         */
        @JsonProperty("pageCount")
        private Integer pageCount;

        /**
         * 总记录数
         */
        @JsonProperty("totalCount")
        private Integer totalCount;
    }

    /**
     * 付款申请单记录
     */
    @Data
    public static class PaymentApplyRecord {

        /**
         * 单据 ID
         */
        @JsonProperty("id")
        private String id;

        /**
         * 单据编码
         */
        @JsonProperty("code")
        private String code;

        /**
         * 申请日期
         */
        @JsonProperty("billDate")
        private String billDate;

        /**
         * 申请金额
         */
        @JsonProperty("applyAmount")
        private String applyAmount;

        /**
         * 币种
         */
        @JsonProperty("pkCurrency")
        private String pkCurrency;

        /**
         * 币种名称
         */
        @JsonProperty("currencyName")
        private String currencyName;

        /**
         * 申请人 ID
         */
        @JsonProperty("creatorId")
        private String creatorId;

        /**
         * 申请人名称
         */
        @JsonProperty("creatorName")
        private String creatorName;

        /**
         * 审核状态
         */
        @JsonProperty("verifyState")
        private String verifyState;

        /**
         * 审核状态名称
         */
        @JsonProperty("verifyStateName")
        private String verifyStateName;

        /**
         * 付款状态
         */
        @JsonProperty("payStatus")
        private String payStatus;

        /**
         * 付款状态名称
         */
        @JsonProperty("payStatusName")
        private String payStatusName;

        /**
         * 收款单位名称
         */
        @JsonProperty("receiveUnitName")
        private String receiveUnitName;

        /**
         * 收款账号
         */
        @JsonProperty("receiveAccount")
        private String receiveAccount;

        /**
         * 收款开户行
         */
        @JsonProperty("receiveBankName")
        private String receiveBankName;

        /**
         * 用途/摘要
         */
        @JsonProperty("purpose")
        private String purpose;

        /**
         * 来源系统
         */
        @JsonProperty("srcSystem")
        private String srcSystem;

        /**
         * 创建时间
         */
        @JsonProperty("createTime")
        private String createTime;

        /**
         * 修改时间
         */
        @JsonProperty("modifyTime")
        private String modifyTime;

        /**
         * 附件数量
         */
        @JsonProperty("attachmentQty")
        private Integer attachmentQty;
    }
}
