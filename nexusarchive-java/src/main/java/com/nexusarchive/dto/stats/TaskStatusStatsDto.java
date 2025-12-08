package com.nexusarchive.dto.stats;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TaskStatusStatsDto {
    private long total;
    private long completed;
    private long failed;
    private long running;
    private long pending;
    private Map<String, Long> byStatus;
}
