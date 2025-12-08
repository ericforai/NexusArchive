package com.nexusarchive.service.impl;

import com.nexusarchive.dto.workflow.WorkflowTaskDto;
import com.nexusarchive.service.BorrowingService;
import com.nexusarchive.service.DestructionService;
import com.nexusarchive.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A very simple mock workflow engine.
 * In a real system, this would integrate with Camunda, Flowable, or a custom engine table.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final BorrowingService borrowingService;
    private final DestructionService destructionService;

    // In-memory mock storage for tasks
    private final List<WorkflowTaskDto> mockTaskStore = new ArrayList<>();

    @Override
    public void startWorkflow(String processKey, String businessKey, String initiator) {
        log.info("Starting workflow: processKey={}, businessKey={}, initiator={}", processKey, businessKey, initiator);
        
        // Create a mock task for the admin
        WorkflowTaskDto task = WorkflowTaskDto.builder()
                .id(UUID.randomUUID().toString())
                .name("Approval Task for " + processKey)
                .assignee("admin") // Assign to admin by default
                .status("PENDING")
                .createdTime(LocalDateTime.now().toString())
                .businessKey(businessKey)
                .businessType(processKey)
                .build();
        
        mockTaskStore.add(task);
    }

    @Override
    public List<WorkflowTaskDto> getTasks(String assignee) {
        // Return all tasks for now for simplicity, or filter by assignee
        return mockTaskStore;
    }

    @Override
    public void completeTask(String taskId, String comment, boolean approved) {
        log.info("Completing task: taskId={}, approved={}", taskId, approved);
        
        WorkflowTaskDto task = mockTaskStore.stream()
                .filter(t -> t.getId().equals(taskId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Task not found"));

        mockTaskStore.remove(task);

        // Callback to business services based on businessType
        if ("BORROWING".equals(task.getBusinessType())) {
            borrowingService.approveBorrowing(task.getBusinessKey(), approved, comment);
            log.info("Borrowing request {} {}", task.getBusinessKey(), approved ? "APPROVED" : "REJECTED");
        } else if ("DESTRUCTION".equals(task.getBusinessType())) {
            if (approved) {
                destructionService.approveDestruction(task.getBusinessKey(), "admin", comment);
            } else {
                // destructionService.reject(...)
                log.info("Destruction request {} REJECTED", task.getBusinessKey());
            }
        }
    }
}
