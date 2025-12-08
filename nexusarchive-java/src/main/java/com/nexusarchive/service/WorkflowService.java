package com.nexusarchive.service;

import com.nexusarchive.dto.workflow.WorkflowTaskDto;

import java.util.List;

public interface WorkflowService {
    void startWorkflow(String processKey, String businessKey, String initiator);
    List<WorkflowTaskDto> getTasks(String assignee);
    void completeTask(String taskId, String comment, boolean approved);
}
