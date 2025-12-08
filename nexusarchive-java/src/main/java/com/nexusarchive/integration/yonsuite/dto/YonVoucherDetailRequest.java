package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

/**
 * 凭证详情查询请求 DTO
 * Reference: YS凭证详情查询API
 */
@Data
public class YonVoucherDetailRequest {
    private String voucherId;
}
