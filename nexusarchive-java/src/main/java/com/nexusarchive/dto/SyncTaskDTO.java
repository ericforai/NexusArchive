package com.nexusarchive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncTaskDTO {
    private String taskId;
    private String status;  // SUBMITTED, RUNNING, SUCCESS, FAIL
    private String message;
}
