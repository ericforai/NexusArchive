// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: WorkflowServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.dto.workflow.WorkflowTaskDto;
import com.nexusarchive.modules.borrowing.api.dto.BorrowingApprovalRequest;
import com.nexusarchive.modules.borrowing.app.BorrowingFacade;
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

    private final BorrowingFacade borrowingFacade;
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
            BorrowingApprovalRequest approvalRequest = new BorrowingApprovalRequest();
            approvalRequest.setApproved(approved);
            approvalRequest.setComment(comment);
            borrowingFacade.approveBorrowing(task.getBusinessKey(), approvalRequest);
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
