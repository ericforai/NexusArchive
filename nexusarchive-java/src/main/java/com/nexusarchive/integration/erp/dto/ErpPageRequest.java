package com.nexusarchive.integration.erp.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ErpPageRequest {
    private int pageIndex = 1;
    private int pageSize = 20;
    private LocalDate startDate;
    private LocalDate endDate;
    private String orgCode; // 会计主体/组织编码
}
