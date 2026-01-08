// Input: Jackson、Lombok、Java 标准库
// Output: KingdeeVoucherDetailResponse 类
// Pos: 数据传输对象 - 金蝶 API 响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto.kingdee;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 金蝶云星空凭证详情查询响应 DTO
 * Reference: Kingdee K3Cloud View Bill API / ExecuteBillQuery with details
 *
 * @author Agent D (基础设施工程师)
 */
@Data
public class KingdeeVoucherDetailResponse {

    /**
     * 响应状态码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private KingdeeVoucherDetail data;

    @Data
    public static class KingdeeVoucherDetail {
        /**
         * 凭证ID
         */
        @JsonProperty("FVoucherID")
        private String voucherId;

        /**
         * 凭证编号
         */
        @JsonProperty("FNumber")
        private String voucherNumber;

        /**
         * 凭证日期
         */
        @JsonProperty("FDate")
        private String voucherDate;

        /**
         * 会计年度
         */
        @JsonProperty("FYear")
        private Integer fiscalYear;

        /**
         * 会计期间
         */
        @JsonProperty("FPeriod")
        private Integer fiscalPeriod;

        /**
         * 凭证字
         */
        @JsonProperty("FVoucherGroup")
        private String voucherGroup;

        /**
         * 制单人ID
         */
        @JsonProperty("FCreator")
        private String creatorId;

        /**
         * 制单人名称
         */
        @JsonProperty("FCreatorName")
        private String creatorName;

        /**
         * 审核人ID
         */
        @JsonProperty("FAuditor")
        private String auditorId;

        /**
         * 审核人名称
         */
        @JsonProperty("FAuditorName")
        private String auditorName;

        /**
         * 记账人ID
         */
        @JsonProperty("FPoster")
        private String posterId;

        /**
         * 记账人名称
         */
        @JsonProperty("FPosterName")
        private String posterName;

        /**
         * 凭证状态
         */
        @JsonProperty("FDocumentStatus")
        private String documentStatus;

        /**
         * 借方金额合计
         */
        @JsonProperty("FDebitTotal")
        private Double debitTotal;

        /**
         * 贷方金额合计
         */
        @JsonProperty("FCreditTotal")
        private Double creditTotal;

        /**
         * 附件数量
         */
        @JsonProperty("FAttachmentCount")
        private Integer attachmentCount;

        /**
         * 创建时间
         */
        @JsonProperty("FCreateDate")
        private String createDate;

        /**
         * 创建时间 (时间戳)
         */
        @JsonProperty("FCreateTime")
        private String createTime;

        /**
         * 修改时间
         */
        @JsonProperty("FModifyDate")
        private String modifyDate;

        /**
         * 修改时间 (时间戳)
         */
        @JsonProperty("FModifyTime")
        private String modifyTime;

        /**
         * 摘要 (凭证级)
         */
        @JsonProperty("FExplanation")
        private String explanation;

        /**
         * 账套ID
         */
        @JsonProperty("FAcctID")
        private String acctId;

        /**
         * 账套名称
         */
        @JsonProperty("FAcctName")
        private String acctName;

        /**
         * 组织ID
         */
        @JsonProperty("FOrgId")
        private String orgId;

        /**
         * 分录明细
         */
        @JsonProperty("FEntity")
        private List<VoucherEntry> entries;

        /**
         * 附件列表
         */
        @JsonProperty("FAttachments")
        private List<VoucherAttachment> attachments;
    }

    /**
     * 凭证分录
     */
    @Data
    public static class VoucherEntry {
        /**
         * 分录ID
         */
        @JsonProperty("FEntryID")
        private String entryId;

        /**
         * 分录序号
         */
        @JsonProperty("FSeq")
        private Integer sequence;

        /**
         * 摘要
         */
        @JsonProperty("FExplanation")
        private String explanation;

        /**
         * 科目代码
         */
        @JsonProperty("FAccountCode")
        private String accountCode;

        /**
         * 科目名称
         */
        @JsonProperty("FAccountName")
        private String accountName;

        /**
         * 科目ID
         */
        @JsonProperty("FAccountID")
        private String accountId;

        /**
         * 币别代码
         */
        @JsonProperty("FCurrencyCode")
        private String currencyCode;

        /**
         * 币别名称
         */
        @JsonProperty("FCurrencyName")
        private String currencyName;

        /**
         * 原币金额
         */
        @JsonProperty("FAmountFor")
        private Double amountFor;

        /**
         * 借方金额 (本位币)
         */
        @JsonProperty("FDebit")
        private Double debit;

        /**
         * 贷方金额 (本位币)
         */
        @JsonProperty("FCredit")
        private Double credit;

        /**
         * 借方原币金额
         */
        @JsonProperty("FDebitFor")
        private Double debitFor;

        /**
         * 贷方原币金额
         */
        @JsonProperty("FCreditFor")
        private Double creditFor;

        /**
         * 汇率
         */
        @JsonProperty("FExchangeRate")
        private Double exchangeRate;

        /**
         * 数量
         */
        @JsonProperty("FQty")
        private Double quantity;

        /**
         * 单价
         */
        @JsonProperty("FPrice")
        private Double price;

        /**
         * 核算项目维度 (JSON 格式)
         * 例如: {"FDeptID":"xxx","FPersonID":"yyy"}
         */
        @JsonProperty("FDimension")
        private String dimension;
    }

    /**
     * 凭证附件
     */
    @Data
    public static class VoucherAttachment {
        /**
         * 附件ID
         */
        @JsonProperty("FAttachmentID")
        private String attachmentId;

        /**
         * 文件名
         */
        @JsonProperty("FFileName")
        private String fileName;

        /**
         * 文件扩展名
         */
        @JsonProperty("FFileExt")
        private String fileExt;

        /**
         * 文件大小 (字节)
         */
        @JsonProperty("FFileSize")
        private Long fileSize;

        /**
         * 上传时间
         */
        @JsonProperty("FUploadDate")
        private String uploadDate;

        /**
         * 上传人
         */
        @JsonProperty("FUploader")
        private String uploader;

        /**
         * 下载URL
         */
        @JsonProperty("FDownloadUrl")
        private String downloadUrl;

        /**
         * 文件ID (用于下载接口)
         */
        @JsonProperty("FFileId")
        private String fileId;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return "200".equals(code) || "0".equals(code);
    }
}
