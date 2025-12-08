package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.service.DestructionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/destruction")
@RequiredArgsConstructor
public class DestructionController {

    private final DestructionService destructionService;

    @PostMapping
    public Result<Destruction> createDestruction(@RequestBody Destruction destruction) {
        // Mock user info
        if (destruction.getApplicantId() == null) {
            destruction.setApplicantId("current-user-id");
            destruction.setApplicantName("Current User");
        }
        return Result.success(destructionService.createDestruction(destruction));
    }

    @GetMapping
    public Result<Page<Destruction>> getDestructions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status) {
        return Result.success(destructionService.getDestructions(page, limit, status));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approveDestruction(@PathVariable String id, @RequestBody(required = false) String comment) {
        // Mock approver
        String approverId = "admin-user-id";
        destructionService.approveDestruction(id, approverId, comment);
        return Result.success();
    }

    @PostMapping("/{id}/execute")
    public Result<Void> executeDestruction(@PathVariable String id) {
        destructionService.executeDestruction(id);
        return Result.success();
    }
}
