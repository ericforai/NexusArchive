package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * 凭证列表查询请求 DTO
 * Reference: YS凭证列表查询API
 */
@Data
public class YonVoucherListRequest {
    
    private Pager pager;
    private String accbookCode;
    private List<String> accsubjectCodeList;
    private String periodStart;
    private String periodEnd;
    private String makeTimeStart;
    private String makeTimeEnd;
    private List<String> voucherStatusList;
    private List<String> voucherTypeCodeList;
    private String description;
    private List<String> makerNameList;
    private List<String> auditorNameList;
    private List<String> tallymanNameList;
    private Integer billcodeMin;
    private Integer billcodeMax;
    private BigDecimal moneyRangeMin = BigDecimal.ZERO;
    private BigDecimal moneyRangeMax = new BigDecimal("999999999999");
    private String tsStart;
    private String tsEnd;
    
    @Data
    public static class Pager {
        private Integer pageIndex = 1;
        private Integer pageSize = 20;
    }
}
