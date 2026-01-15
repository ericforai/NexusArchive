package com.nexusarchive.integration.erp.adapter.yonsuite.dto;

import lombok.Data;

@Data
public class ApiRequest {
    private String startDate;
    private String endDate;
    private Integer pageSize = 100;
    private Integer pageNo = 1;
}
