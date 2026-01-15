// Input: Lombok、Spring Framework、Jakarta EE、Java 标准库、等
// Output: ReconciliationController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.common.exception.ErrorCode;
import com.nexusarchive.dto.reconciliation.ReconciliationRequest;
import com.nexusarchive.entity.ReconciliationRecord;
import com.nexusarchive.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 账、凭、证三位一体核对控制器
 */
@Tag(name = "账凭证核对", description = """
    账、凭、证三位一体核对接口。

    **功能说明:**
    - 执行账簿、凭证、档案的三位一体核对
    - 检查数据一致性和完整性
    - 生成核对报告和差异清单

    **核对内容:**
    - 账簿核对: 总账与明细账核对
    - 凭证核对: 凭证与原始单据核对
    - 档案核对: 归档数据与源数据核对
    - 金额核对: 各环节金额一致性
    - 数量核对: 记录数量一致性

    **核对结果:**
    - BALANCED: 平衡一致
    - DIFFERENCE: 存在差异
    - ERROR: 核对异常

    **限流策略:**
    - 使用 Guava RateLimiter
    - 每秒最多 2 次请求
    - 超出限制返回 429 错误

    **使用场景:**
    - 月度账务核对
    - 年度审计检查
    - 数据质量验证
    """
)
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
@RestController
@RequestMapping("/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    // ✅ P1 修复: 使用 Guava RateLimiter 限流,每秒最多 2 次请求
    private final com.google.common.util.concurrent.RateLimiter rateLimiter =
        com.google.common.util.concurrent.RateLimiter.create(2.0);

    /**
     * 触发对账
     * ✅ P0 修复: 使用 Spring Security 获取真实用户,添加审计日志
     * ✅ P1 修复: 添加速率限制
     */
    @PostMapping("/trigger")
    @Operation(
        summary = "触发三位一体核对",
        description = """
            执行账、凭、证三位一体核对任务。

            **请求体:**
            - configId: 配置ID（必填）
            - subjectCode: 科目编码（必填）
            - startDate: 开始日期（必填，格式：YYYY-MM-DD）
            - endDate: 结束日期（必填，格式：YYYY-MM-DD）

            **返回数据包括:**
            - id: 核对记录ID
            - configId: 配置ID
            - subjectCode: 科目编码
            - startDate: 开始日期
            - endDate: 结束日期
            - status: 核对状态
            - totalAmount: 总金额
            - diffAmount: 差异金额
            - matchedCount: 匹配数量
            - diffCount: 差异数量
            - operatorId: 操作人
            - createdAt: 创建时间

            **业务规则:**
            - 限流: 每秒最多 2 次请求
            - 核对操作会记录审计日志
            - 核对结果会保存到数据库

            **使用场景:**
            - 月度账务核对
            - 年度审计检查
            - 数据质量验证
            """,
        operationId = "triggerReconciliation",
        tags = {"账凭证核对"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "核对完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "429", description = "请求过于频繁")
    })
    public ReconciliationRecord triggerReconciliation(
            @Parameter(description = "核对请求", required = true)
            @RequestBody @Valid ReconciliationRequest request,
            @Parameter(description = "当前用户信息", hidden = true)
            @AuthenticationPrincipal UserDetails userDetails) {

        // ✅ P1 修复: 速率限制检查
        if (!rateLimiter.tryAcquire(1, java.time.Duration.ofSeconds(3))) {
            throw new BusinessException(ErrorCode.RECONCILIATION_TOO_FREQUENT);
        }

        // ✅ 从 Spring Security 上下文获取真实用户
        String operatorId = userDetails != null ? userDetails.getUsername() : "anonymous";

        // ✅ 记录审计日志
        log.info("用户 {} 触发对账: configId={}, subject={}, range={} to {}",
            operatorId, request.getConfigId(), request.getSubjectCode(),
            request.getStartDate(), request.getEndDate());

        return reconciliationService.performReconciliation(
                request.getConfigId(),
                request.getSubjectCode(),
                request.getStartDate(),
                request.getEndDate(),
                operatorId);
    }

    /**
     * 获取历史记录
     */
    @GetMapping("/history")
    @Operation(
        summary = "获取核对历史记录",
        description = """
            获取指定配置的核对历史记录。

            **查询参数:**
            - configId: 配置ID（必填）

            **返回数据包括:**
            - id: 核对记录ID
            - subjectCode: 科目编码
            - startDate: 开始日期
            - endDate: 结束日期
            - status: 核对状态
            - totalAmount: 总金额
            - diffAmount: 差异金额
            - operatorId: 操作人
            - createdAt: 创建时间

            **使用场景:**
            - 查看核对历史
            - 核对结果追溯
            """,
        operationId = "getReconciliationHistory",
        tags = {"账凭证核对"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未认证")
    })
    public List<ReconciliationRecord> getHistory(
            @Parameter(description = "配置ID", required = true, example = "1")
            @RequestParam Long configId) {
        return reconciliationService.getHistory(configId);
    }
}
