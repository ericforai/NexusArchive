package com.nexusarchive.controller;

import com.nexusarchive.entity.ReconciliationRecord;
import com.nexusarchive.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.nexusarchive.dto.reconciliation.ReconciliationRequest;
import com.nexusarchive.entity.ReconciliationRecord;
import com.nexusarchive.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 账、凭、证三位一体核对控制器
 */
@RestController
@RequestMapping("/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    /**
     * 触发对账
     */
    @PostMapping("/trigger")
    public ReconciliationRecord triggerReconciliation(@RequestBody @Valid ReconciliationRequest request) {
        // TODO: 集成Spring Security后，应从SecurityContextHolder获取operatorId
        // String operatorId =
        // SecurityContextHolder.getContext().getAuthentication().getName();
        String operatorId = "user_admin"; // 暂使用默认安全上下文，杜绝前端伪造

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
