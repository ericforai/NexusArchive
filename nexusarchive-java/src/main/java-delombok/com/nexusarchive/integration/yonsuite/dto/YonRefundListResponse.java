// com/nexusarchive/integration/yonsuite/dto/YonRefundListResponse.java
// 输入: YonSuite API 响应
// 输出: 退款单列表数据
// 位置: YonSuite 集成 - DTO
// 更新时请同步更新本文件注释及所属目录的 md

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;

/**
 * YonSuite 付款退款单列表查询响应
 *
 * API: /yonbip/EFI/apRefund/list
 */
@Data
public class YonRefundListResponse {

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
    private PageData data;

    // Manual Getters
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public PageData getData() { return data; }

    // Manual Setters
    public void setCode(String code) { this.code = code; }
    public void setMessage(String message) { this.message = message; }
    public void setData(PageData data) { this.data = data; }

    /**
     * 分页数据
     */
    @Data
    public static class PageData {
        /**
         * 当前页码
         */
        private Integer pageIndex;

        /**
         * 每页查询数据大小
         */
        private Integer pageSize;

        /**
         * 总记录数
         */
        private Integer recordCount;

        /**
         * 查询结果集
         */
        private List<RefundRecord> recordList;

        /**
         * 总页数
         */
        private Integer pageCount;

        /**
         * 起始页码
         */
        private Integer beginPageIndex;

        /**
         * 最终页码
         */
        private Integer endIndex;
    }

    /**
     * 退款单记录
     */
    @Data
    public static class RefundRecord {
        /**
         * 退款单 ID
         */
        private String id;

        /**
         * 单据编号
         */
        private String code;

        /**
         * 单据日期
         */
        private String billDate;

        /**
         * 单据状态
         * 0:开立、1:审批中、2:已审批、3:终止、4:已驳回
         */
        private Integer verifyState;

        /**
         * 单据类型编码
         */
        private String billTypeCode;

        /**
         * 单据类型名称
         */
        private String billTypeName;

        /**
         * 付款组织 ID
         */
        private String financeOrg;

        /**
         * 付款组织名称
         */
        private String financeOrgName;

        /**
         * 付款组织编码
         */
        private String financeOrgCode;

        /**
         * 交易类型编码
         */
        private String bustypeCode;

        /**
         * 交易类型名称
         */
        private String bustypeName;

        /**
         * 快速类型编码
         */
        private String quickTypeCode;

        /**
         * 快速类型名称
         */
        private String quickTypeName;

        /**
         * 币种
         */
        private String bodyItemOrgCurrencyName;

        /**
         * 修改时间
         */
        private String modifyTime;

        /**
         * 供应商 ID
         */
        private String supplier;

        /**
         * 供应商名称
         */
        private String supplierName;

        /**
         * 员工 ID
         */
        private String employee;

        /**
         * 员工名称
         */
        private String employeeName;

        /**
         * 原币含税金额
         */
        private Double bodyItemOriTaxIncludedAmount;

        /**
         * 原币未税金额
         */
        private Double bodyItemOriTaxExcludedAmount;

        /**
         * 表体项 ID
         */
        private String bodyItem_id;

        /**
         * 外部系统编码
         */
        private String extSystemCode;

        /**
         * 外部系统单号
         */
        private String extVouchCode;
    }
}
