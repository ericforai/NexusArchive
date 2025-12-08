package com.nexusarchive.integration.erp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngestResult {
    private String sourceVoucherId;
    private String archivalCode;
    private String status;
    private String message;
}
