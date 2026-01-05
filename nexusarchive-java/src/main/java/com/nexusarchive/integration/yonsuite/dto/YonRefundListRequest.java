// com/nexusarchive/integration/yonsuite/dto/YonRefundListRequest.java
// 输入: 查询条件
// 输出: YonSuite 退款单列表查询请求
// 位置: YonSuite 集成 - DTO
// 更新时请同步更新本文件注释及所属目录的 md

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * YonSuite 付款退款单列表查询请求
 *
 * API: /yonbip/EFI/apRefund/list
 */
@Data
public class YonRefundListRequest {

    /**
     * 当前页码
     */
    private String pageIndex = "1";

    /**
     * 每页查询数据大小，限制500
     */
    private String pageSize = "100";

    /**
     * 单据状态
     * 0:开立、1:审批中、2:已审批、3:终止、4:已驳回
     * 不输入时为查询全部
     */
    private List<String> verifyState;

    /**
     * 单据日期开始时间
     * 格式: yyyy-MM-dd HH:mm:ss
     */
    private String open_billDate_begin;

    /**
     * 单据日期结束时间
     * 格式: yyyy-MM-dd HH:mm:ss
     */
    private String open_billDate_end;

    /**
     * 修改时间开始时间
     * 格式: yyyy-MM-dd HH:mm:ss
     */
    private String open_modifyTime_begin;

    /**
     * 修改时间结束时间
     * 格式: yyyy-MM-dd HH:mm:ss
     */
    private String open_modifyTime_end;

    /**
     * 付款组织id
     */
    private String financeOrg;

    /**
     * 扩展查询条件
     */
    private SimpleCondition simple;

    /**
     * 是否仅查询表头
     * true: 仅表头, false: 表头+表体
     */
    private Boolean isSum = false;

    /**
     * 扩展查询条件
     */
    @Data
    public static class SimpleCondition {
        /**
         * 付款组织编码
         */
        private String financeOrg_code;
    }
}
