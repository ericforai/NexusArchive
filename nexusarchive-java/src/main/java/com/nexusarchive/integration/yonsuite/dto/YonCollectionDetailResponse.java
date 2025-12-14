package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * 收款单详情查询响应
 * 对应 API: GET /yonbip/EFI/collection/detail
 * 官方文档: docs/api/收款单详情查询.md
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YonCollectionDetailResponse {

    private String code;
    private String message;
    private CollectionDetail data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionDetail {
        private String id; // 单据id
        private String code; // 单据编号 (如 RECar220802000502)
        private String billDate; // 单据日期
        private String billTypeName; // 单据类型名称 (收款单)
        private String billTypeCode; // 单据类型编码
        private Long verifyState; // 审批流状态 (0:开立; 1:审批中; 2:已审批; 3:终止; 4:已驳回)
        private String effectState; // 生效状态 (0:未生效; 1:已生效)
        private Double oriTaxIncludedAmount; // 收款金额
        private Double localTaxIncludedAmount; // 本币含税金额
        private String customerName; // 客户名称
        private String customerCode; // 客户编码
        private String financeOrgName; // 收款组织名称
        private String financeOrgCode; // 收款组织编码
        private String orgName; // 业务组织名称
        private String orgCode; // 业务组织编码
        private String deptName; // 部门名称
        private String deptCode; // 部门编码
        private String staffName; // 员工姓名
        private String staffCode; // 员工编码
        private String bustypeName; // 交易类型名称
        private String bustypeCode; // 交易类型编码
        private String settleModeName; // 结算方式名称
        private String creatorUserName; // 创建人
        private String createTime; // 创建时间
        private String modifyTime; // 修改时间
        private String pubts; // 时间戳
        private String oriCurrencyName; // 原币币种名称
        private String oriCurrencyCode; // 原币币种编码
        private String remarks; // 备注
        private String orderNo; // 订单编号
        private String projectName; // 项目名称
        private String projectCode; // 项目编码
        private String enterpriseBankAccountName; // 企业银行账户名称
        private String enterpriseBankAccountNo; // 企业银行账户编码

        private List<BodyItem> bodyItem; // 表体对象
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BodyItem {
        private String id;
        private String headerId;
        private Integer rowNo;
        private String customerName;
        private String customerCode;
        private Double oriTaxIncludedAmount;
        private Double localTaxIncludedAmount;
        private String deptName;
        private String deptCode;
        private String staffName;
        private String staffCode;
        private String projectName;
        private String projectCode;
        private String orderNo;
        private String remarks;
        private String expenseItemName;
        private String expenseItemCode;
    }
}
