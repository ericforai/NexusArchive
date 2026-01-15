package com.nexusarchive.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncTaskStatus {
    private String taskId;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double progress;  // 0.0 to 1.0
}
