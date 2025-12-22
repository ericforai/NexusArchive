// Input: Lombok、Spring Framework、Jakarta EE、Java 标准库、等
// Output: ReconciliationController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.dto.reconciliation.ReconciliationRequest;
import com.nexusarchive.entity.ReconciliationRecord;
import com.nexusarchive.service.ReconciliationService;
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
    public ReconciliationRecord triggerReconciliation(
            @RequestBody @Valid ReconciliationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // ✅ P1 修复: 速率限制检查
        if (!rateLimiter.tryAcquire(1, java.time.Duration.ofSeconds(3))) {
            throw new com.nexusarchive.common.exception.BusinessException("对账请求过于频繁,请稍后再试");
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
    public List<ReconciliationRecord> getHistory(@RequestParam Long configId) {
        return reconciliationService.getHistory(configId);
    }
}
