// Input: Spring Framework、匹配引擎组件
// Output: VoucherMatchingController API
// Pos: Controller 层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.nexusarchive.common.Result;
import com.nexusarchive.engine.matching.VoucherMatchingEngine;
import com.nexusarchive.engine.matching.RuleTemplateManager;
import com.nexusarchive.engine.matching.dto.*;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 凭证匹配 API
 */
@Slf4j
@RestController
@RequestMapping("/matching")
public class VoucherMatchingController {
    
    private final VoucherMatchingEngine matchingEngine;
    private final RuleTemplateManager templateManager;
    private final ThreadPoolTaskExecutor matchingExecutor;
    
    // 构造函数注入，使用 @Qualifier 明确指定 bean
    public VoucherMatchingController(
            VoucherMatchingEngine matchingEngine,
            RuleTemplateManager templateManager,
            @Qualifier("matchingExecutor") ThreadPoolTaskExecutor matchingExecutor) {
        this.matchingEngine = matchingEngine;
        this.templateManager = templateManager;
        this.matchingExecutor = matchingExecutor;
        log.info("[VoucherMatchingController] Bean created successfully!");
    }
    
    // 任务状态缓存（Caffeine，10分钟过期，防止内存泄漏）
    private static final Cache<String, MatchTask> taskCache = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build();
    
    /**
     * 异步执行单凭证匹配
     */
    @PostMapping("/execute/{voucherId}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<MatchTask> executeMatch(@PathVariable String voucherId) {
        String taskId = java.util.UUID.randomUUID().toString().replace("-", "");
        
        MatchTask task = new MatchTask(taskId, voucherId, "PROCESSING", null);
        taskCache.put(taskId, task);
        
        matchingExecutor.submit(() -> {
            try {
                MatchResult result = matchingEngine.match(voucherId);
                result.setTaskId(taskId);
                task.setStatus("COMPLETED");
                task.setResult(result);
            } catch (Exception e) {
                log.error("Match failed for voucher: {}", voucherId, e);
                task.setStatus("ERROR");
                task.setMessage(e.getMessage());
            }
        });
        
        return Result.success(task);
    }
    
    /**
     * 批量匹配
     */
    @PostMapping("/execute/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<BatchMatchTask> executeBatchMatch(@RequestBody List<String> voucherIds) {
        String batchTaskId = java.util.UUID.randomUUID().toString().replace("-", "");
        
        BatchMatchTask batchTask = new BatchMatchTask(batchTaskId, voucherIds.size(), 0, "PROCESSING");
        
        // 同步执行 (为了前端能立即获取结果数量，暂不使用 matchingExecutor)
        for (String voucherId : voucherIds) {
            try {
                MatchResult result = matchingEngine.match(voucherId);
                result.setBatchTaskId(batchTaskId);
                batchTask.setCompleted(batchTask.getCompleted() + 1);
            } catch (Exception e) {
                log.error("Batch match failed for voucher: {}", voucherId, e);
            }
        }
        batchTask.setStatus("COMPLETED");
        
        return Result.success(batchTask);
    }

    /**
     * 获取单条凭证匹配结果
     */
    @GetMapping("/result/{voucherId}")
    public Result<MatchResult> getMatchResult(@PathVariable String voucherId) {
        MatchResult result = matchingEngine.getMatchResult(voucherId);
        return Result.success(result);
    }
    
    /**
     * 查询任务结果
     */
    @GetMapping("/task/{taskId}")
    public Result<MatchTask> getTaskResult(@PathVariable String taskId) {
        MatchTask task = taskCache.getIfPresent(taskId);
        if (task == null) {
            return Result.fail("任务不存在或已过期");
        }
        return Result.success(task);
    }
    
    /**
     * 获取模板列表
     */
    @GetMapping("/templates")
    public Result<List<RuleTemplate>> getTemplates() {
        return Result.success(templateManager.getAllTemplates());
    }
    
    /**
     * 刷新模板
     */
    @PostMapping("/templates/reload")
    public Result<Void> reloadTemplates() {
        templateManager.reloadTemplates();
        return Result.success();
    }
    
    // ========== DTO ==========
    
    @Data
    public static class MatchTask {
        private final String taskId;
        private final String voucherId;
        private String status;
        private MatchResult result;
        private String message;
        
        public MatchTask(String taskId, String voucherId, String status, MatchResult result) {
            this.taskId = taskId;
            this.voucherId = voucherId;
            this.status = status;
            this.result = result;
        }
    }
    
    @Data
    public static class BatchMatchTask {
        private final String batchTaskId;
        private final int total;
        private int completed;
        private String status;
        
        public BatchMatchTask(String batchTaskId, int total, int completed, String status) {
            this.batchTaskId = batchTaskId;
            this.total = total;
            this.completed = completed;
            this.status = status;
        }
        
        public synchronized void incrementCompleted() {
            this.completed++;
            if (this.completed >= this.total) {
                this.status = "COMPLETED";
            }
        }
    }
}
