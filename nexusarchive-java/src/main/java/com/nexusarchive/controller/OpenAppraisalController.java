// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: OpenAppraisalController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.OpenAppraisal;
import com.nexusarchive.service.OpenAppraisalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 开放鉴定控制器
 */
@RestController
@RequestMapping("/open-appraisal")
@RequiredArgsConstructor
public class OpenAppraisalController {

    private final OpenAppraisalService appraisalService;

    /**
     * 获取鉴定任务列表
     */
    @GetMapping("/list")
    public Result<Page<OpenAppraisal>> getAppraisalList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status) {
        Page<OpenAppraisal> result = appraisalService.getAppraisalList(page, limit, status);
        return Result.success(result);
    }

    /**
     * 获取鉴定详情
     */
    @GetMapping("/{id}")
    public Result<OpenAppraisal> getAppraisalById(@PathVariable String id) {
        OpenAppraisal appraisal = appraisalService.getAppraisalById(id);
        if (appraisal == null) {
            return Result.error(404, "Appraisal record not found");
        }
        return Result.success(appraisal);
    }

    /**
     * 创建鉴定任务
     */
    @PostMapping("/create")
    public Result<OpenAppraisal> createAppraisal(@Valid @RequestBody OpenAppraisal appraisal) {
        try {
            OpenAppraisal created = appraisalService.createAppraisal(appraisal);
            return Result.success(created);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 提交鉴定结果
     */
    @PostMapping("/submit")
    public Result<Void> submitAppraisal(@Valid @RequestBody AppraisalSubmitRequest request) {
        try {
            appraisalService.submitAppraisal(
                request.getId(),
                request.getAppraiserId(),
                request.getAppraiserName(),
                request.getAppraisalResult(),
                request.getOpenLevel(),
                request.getReason()
            );
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    /**
     * 鉴定提交请求DTO
     */
    @lombok.Data
    public static class AppraisalSubmitRequest {
        private String id;
        private String appraiserId;
        private String appraiserName;
        private String appraisalResult;
        private String openLevel;
        private String reason;
    }
}
