// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: AccessReviewController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.dto.request.AccessReviewRequest;
import com.nexusarchive.entity.AccessReview;
import com.nexusarchive.service.AccessReviewService;
import com.nexusarchive.annotation.ArchivalAudit;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 访问权限复核控制器
 * <p>
 * 路径: /access-review
 * <p>
 * 功能：
 * 1. 查询待复核任务列表
 * 2. 执行复核（批准/拒绝）
 * 3. 创建复核任务
 */
@RestController
@RequestMapping("/access-review")
@RequiredArgsConstructor
public class AccessReviewController {

    private final AccessReviewService accessReviewService;

    /**
     * 获取待复核任务列表
     * <p>
     * GET /access-review/tasks
     *
     * @param reviewerId 复核人ID（可选，不传则查询所有待复核任务）
     * @return 待复核任务列表
     */
    @GetMapping("/tasks")
    @PreAuthorize("hasAnyAuthority('nav:access_review:query', 'nav:all')")
    public Result<List<AccessReview>> getPendingReviews(
            @RequestParam(required = false) String reviewerId) {
        List<AccessReview> reviews = accessReviewService.getPendingReviews(reviewerId);
        return Result.success(reviews);
    }

    /**
     * 执行复核
     * <p>
     * POST /access-review/execute
     *
     * @param request 执行复核请求
     */
    @PostMapping("/execute")
    @PreAuthorize("hasAnyAuthority('nav:access_review:execute', 'nav:all')")
    @ArchivalAudit(operationType = "ACCESS_REVIEW_EXECUTE", resourceType = "ACCESS_REVIEW",
            description = "执行访问权限复核")
    public Result<Void> executeReview(@Validated @RequestBody ExecuteReviewRequest request) {
        accessReviewService.executeReview(
                request.getReviewId(),
                request.getReviewerId(),
                request.isApproved(),
                request.getReviewResult(),
                request.getActionTaken()
        );
        return Result.success("复核执行完成", null);
    }

    /**
     * 创建复核任务
     * <p>
     * POST /access-review/create
     *
     * @param request 复核请求
     * @return 复核记录ID
     */
    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('nav:access_review:create', 'nav:all')")
    @ArchivalAudit(operationType = "ACCESS_REVIEW_CREATE", resourceType = "ACCESS_REVIEW",
            description = "创建访问权限复核任务")
    public Result<String> createReview(@Validated @RequestBody AccessReviewRequest request) {
        String reviewId = accessReviewService.createReview(request);
        return Result.success("复核任务创建成功", reviewId);
    }

    /**
     * 执行复核请求 DTO
     */
    @Data
    public static class ExecuteReviewRequest {
        /**
         * 复核记录ID
         */
        private String reviewId;

        /**
         * 复核人ID
         */
        private String reviewerId;

        /**
         * 是否批准
         */
        private boolean approved;

        /**
         * 复核结果说明
         */
        private String reviewResult;

        /**
         * 采取的行动
         */
        private String actionTaken;
    }
}
