package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.workflow.WorkflowTaskDto;
import com.nexusarchive.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping("/start")
    public Result<Void> startWorkflow(@RequestBody Map<String, String> payload) {
        String workflowCode = payload.get("workflowCode");
        String businessId = payload.get("businessId");
        // Mock initiator
        String initiator = "current-user";
        workflowService.startWorkflow(workflowCode, businessId, initiator);
        return Result.success();
    }

    @GetMapping("/tasks")
    public Result<List<WorkflowTaskDto>> getTasks() {
        // Mock current user
        String currentUser = "admin";
        return Result.success(workflowService.getTasks(currentUser));
    }

    @PostMapping("/instances/{id}/approve")
    public Result<Void> approveTask(@PathVariable String id, @RequestBody(required = false) Map<String, String> payload) {
        String comment = payload != null ? payload.get("comment") : "";
        workflowService.completeTask(id, comment, true);
        return Result.success();
    }

    @PostMapping("/instances/{id}/reject")
    public Result<Void> rejectTask(@PathVariable String id, @RequestBody(required = false) Map<String, String> payload) {
        String comment = payload != null ? payload.get("comment") : "";
        workflowService.completeTask(id, comment, false);
        return Result.success();
    }
}
