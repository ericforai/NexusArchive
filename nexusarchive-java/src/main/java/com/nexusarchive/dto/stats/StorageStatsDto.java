package com.nexusarchive.dto.stats;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StorageStatsDto {
    private String total;
    private String used;
    private double usagePercent;
}
