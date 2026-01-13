package com.nexusarchive.controller.borrow;

// Input: HTTP Requests
// Output: JSON Results
// Pos: controller/borrow
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import com.nexusarchive.common.result.Result;
import com.nexusarchive.domain.borrow.ApproveBorrowRequestCommand;
import com.nexusarchive.domain.borrow.SubmitBorrowRequestCommand;
import com.nexusarchive.domain.borrow.BorrowRequestVO;
import com.nexusarchive.entity.BorrowRequest;
import com.nexusarchive.service.borrow.BorrowRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 借阅申请控制器
 * 
 * 遵循深奥的简洁：只做映射，逻辑下沉到 Service。
 */
@RestController
@RequestMapping("/borrow/requests")
@RequiredArgsConstructor
public class BorrowRequestController {

    private final BorrowRequestService borrowRequestService;

    @GetMapping
    public Result<com.baomidou.mybatisplus.core.metadata.IPage<BorrowRequestVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        
        // 分页参数适配
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<BorrowRequest> pageParam = 
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, limit);
            
        return Result.success(borrowRequestService.list(pageParam, status, keyword));
    }

    @PostMapping
    public Result<BorrowRequest> submit(@RequestBody SubmitBorrowRequestCommand command) {
        return Result.success(borrowRequestService.submit(command));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable String id, @RequestBody ApproveBorrowRequestCommand command) {
        // 确保 ID 一致
        ApproveBorrowRequestCommand finalCommand = new ApproveBorrowRequestCommand(
            id, command.approverId(), command.approverName(), command.approved(), command.comment()
        );
        borrowRequestService.approve(finalCommand);
        return Result.success();
    }

    @PostMapping("/{id}/confirm-out")
    public Result<Void> confirmOut(@PathVariable String id) {
        borrowRequestService.confirmOut(id);
        return Result.success();
    }

    @PostMapping("/{id}/return")
    public Result<Void> returnArchives(@PathVariable String id, @RequestParam String operatorId) {
        borrowRequestService.returnArchives(id, operatorId);
        return Result.success();
    }
}
