package com.nexusarchive.integration.erp.dto;

import com.nexusarchive.dto.sip.AccountingSipDto;
import lombok.Data;
import java.util.List;

@Data
public class BatchIngestRequest {
    private String sourceSystem;
    private String batchId;
    private List<AccountingSipDto> vouchers;
}
