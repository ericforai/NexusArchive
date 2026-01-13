package com.nexusarchive.controller.borrow;

// Input: HTTP Requests
// Output: JSON Results
// Pos: controller/borrow
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import com.nexusarchive.common.result.Result;
import com.nexusarchive.domain.borrow.ApproveBorrowRequestCommand;
import com.nexusarchive.domain.borrow.SubmitBorrowRequestCommand;
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
