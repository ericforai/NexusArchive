package com.nexusarchive.integration.erp.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchIngestResponse {
    private String status;
    private String timestamp;
    private List<IngestResult> results;
    private String globalError;
}
