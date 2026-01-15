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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 凭证匹配 API
 */
@Tag(name = "凭证匹配", description = """
    凭证智能匹配引擎接口。

    **功能说明:**
    - 根据规则模板智能匹配关联凭证
    - 支持单凭证和批量凭证匹配
    - 异步任务处理
    - 匹配规则模板管理

    **匹配规则:**
    - 按金额精确匹配
    - 按日期范围匹配
    - 按发票号匹配
    - 按摘要关键字匹配
    - 组合条件匹配

    **任务状态:**
    - PROCESSING: 处理中
    - COMPLETED: 已完成
    - ERROR: 处理失败

    **使用场景:**
    - 凭证关联自动匹配
    - 跨凭证关系建立
    - 财务对账辅助
    """
)
@SecurityRequirement(name = "Bearer Authentication")
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
    @Operation(
        summary = "异步执行单凭证匹配",
        description = """
            异步执行指定凭证的关联匹配任务。

            **路径参数:**
            - voucherId: 凭证ID

            **返回数据包括:**
            - taskId: 任务ID
            - voucherId: 凭证ID
            - status: 任务状态（PROCESSING/COMPLETED/ERROR）

            **业务规则:**
            - 任务在后台异步执行
            - 任务结果缓存 10 分钟
            - 使用 taskId 查询最终结果

            **使用场景:**
            - 单凭证关联匹配
            - 按需触发匹配
            """,
        operationId = "executeVoucherMatch",
        tags = {"凭证匹配"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "匹配任务已提交"),
        @ApiResponse(responseCode = "404", description = "凭证不存在")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<MatchTask> executeMatch(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String voucherId) {
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
    @Operation(
        summary = "批量凭证匹配",
        description = """
            批量执行多个凭证的匹配任务。

            **请求体:**
            - 凭证ID列表

            **返回数据包括:**
            - batchTaskId: 批次任务ID
            - total: 总凭证数
            - completed: 已完成数
            - status: 任务状态

            **业务规则:**
            - 同步执行（便于前端立即获取结果）
            - 每个凭证的匹配结果会关联到批次ID

            **使用场景:**
            - 批量凭证关联匹配
            - 月度批量处理
            """,
        operationId = "executeBatchVoucherMatch",
        tags = {"凭证匹配"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "批量匹配任务已提交")
    })
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Result<BatchMatchTask> executeBatchMatch(
            @Parameter(description = "凭证ID列表", required = true)
            @RequestBody List<String> voucherIds) {
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
    @Operation(
        summary = "获取凭证匹配结果",
        description = """
            获取指定凭证的匹配结果。

            **路径参数:**
            - voucherId: 凭证ID

            **返回数据包括:**
            - voucherId: 凭证ID
            - matches: 匹配到的关联凭证列表
            - matchCount: 匹配数量
            - matchedAt: 匹配时间

            **使用场景:**
            - 查看凭证关联关系
            - 验证匹配结果
            """,
        operationId = "getVoucherMatchResult",
        tags = {"凭证匹配"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "凭证不存在或未匹配")
    })
    public Result<MatchResult> getMatchResult(
            @Parameter(description = "凭证ID", required = true, example = "voucher-001")
            @PathVariable String voucherId) {
        MatchResult result = matchingEngine.getMatchResult(voucherId);
        return Result.success(result);
    }

    /**
     * 查询任务结果
     */
    @GetMapping("/task/{taskId}")
    @Operation(
        summary = "查询匹配任务状态",
        description = """
            查询异步匹配任务的执行状态。

            **路径参数:**
            - taskId: 任务ID

            **返回数据包括:**
            - taskId: 任务ID
            - voucherId: 凭证ID
            - status: 任务状态
            - result: 匹配结果（完成后）
            - message: 错误消息（失败时）

            **业务规则:**
            - 任务结果缓存 10 分钟
            - 过期任务返回不存在

            **使用场景:**
            - 轮询任务状态
            - 获取异步匹配结果
            """,
        operationId = "getMatchTaskStatus",
        tags = {"凭证匹配"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "任务不存在或已过期")
    })
    public Result<MatchTask> getTaskResult(
            @Parameter(description = "任务ID", required = true, example = "task-123")
            @PathVariable String taskId) {
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
    @Operation(
        summary = "获取匹配规则模板列表",
        description = """
            获取所有可用的匹配规则模板。

            **返回数据包括:**
            - templateId: 模板ID
            - templateName: 模板名称
            - description: 模板描述
            - rules: 匹配规则列表
            - isEnabled: 是否启用

            **内置模板:**
            - amount_exact: 金额精确匹配
            - date_range: 日期范围匹配
            - invoice_no: 发票号匹配
            - summary_keyword: 摘要关键字匹配

            **使用场景:**
            - 规则模板选择
            - 匹配规则配置
            """,
        operationId = "getMatchTemplates",
        tags = {"凭证匹配"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public Result<List<RuleTemplate>> getTemplates() {
        return Result.success(templateManager.getAllTemplates());
    }

    /**
     * 刷新模板
     */
    @PostMapping("/templates/reload")
    @Operation(
        summary = "重新加载匹配规则模板",
        description = """
            从数据库或配置文件重新加载匹配规则模板。

            **业务规则:**
            - 清空内存缓存
            - 重新加载所有模板
            - 更新后立即生效

            **使用场景:**
            - 规则配置更新后重载
            - 模板变更生效
            """,
        operationId = "reloadMatchTemplates",
        tags = {"凭证匹配"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "刷新成功")
    })
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
