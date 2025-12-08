package com.nexusarchive.dto.stats;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArchivalTrendDto {
    private String date;
    private long count;
}
