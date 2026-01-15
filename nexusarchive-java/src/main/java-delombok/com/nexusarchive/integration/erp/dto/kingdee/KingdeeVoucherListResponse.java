// Input: Jackson、Lombok、Java 标准库
// Output: KingdeeVoucherListResponse 类
// Pos: 数据传输对象 - 金蝶 API 响应
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.integration.erp.dto.kingdee;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 金蝶云星空凭证列表查询响应 DTO
 * Reference: Kingdee K3Cloud ExecuteBillQuery API
 *
 * @author Agent D (基础设施工程师)
 */
@Data
public class KingdeeVoucherListResponse {

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
    private ResponseData data;

    /**
     * 会话ID (用于后续请求)
     */
    private String sessionId;

    @Data
    public static class ResponseData {
        /**
         * 凭证记录列表
         */
        private List<KingdeeVoucher> vouchers;

        /**
         * 总记录数
         */
        @JsonProperty("total_count")
        private Integer totalCount;

        /**
         * 当前页码
         */
        @JsonProperty("page_index")
        private Integer pageIndex;

        /**
         * 每页大小
         */
        @JsonProperty("page_size")
        private Integer pageSize;
    }

    /**
     * 金蝶凭证数据结构
     */
    @Data
    public static class KingdeeVoucher {
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
         * 制单人
         */
        @JsonProperty("FCreator")
        private String creator;

        /**
         * 制单人名称
         */
        @JsonProperty("FCreatorName")
        private String creatorName;

        /**
         * 审核人
         */
        @JsonProperty("FAuditor")
        private String auditor;

        /**
         * 审核人名称
         */
        @JsonProperty("FAuditorName")
        private String auditorName;

        /**
         * 记账人
         */
        @JsonProperty("FPoster")
        private String poster;

        /**
         * 凭证状态
         * 0-暂存, 1-创建, 2-审核, 3-记账
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
         * 修改时间
         */
        @JsonProperty("FModifyDate")
        private String modifyDate;

        /**
         * 摘要
         */
        @JsonProperty("FExplanation")
        private String explanation;

        /**
         * 账套ID
         */
        @JsonProperty("FAcctID")
        private String acctId;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return "200".equals(code) || "0".equals(code);
    }
}
