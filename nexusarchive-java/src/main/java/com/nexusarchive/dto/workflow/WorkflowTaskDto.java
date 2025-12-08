package com.nexusarchive.dto.workflow;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowTaskDto {
    private String id;
    private String name;
    private String assignee;
    private String status;
    private String createdTime;
    private String businessKey;
    private String businessType; // e.g., BORROWING, DESTRUCTION
}
