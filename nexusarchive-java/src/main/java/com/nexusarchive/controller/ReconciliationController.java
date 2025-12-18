package com.nexusarchive.controller;

import com.nexusarchive.entity.ReconciliationRecord;
import com.nexusarchive.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
    public ReconciliationRecord triggerReconciliation(@RequestBody Map<String, Object> params) {
        Long configId = Long.valueOf(params.get("configId").toString());
        String subjectCode = (String) params.get("subjectCode");
        String startDateStr = (String) params.get("startDate");
        String endDateStr = (String) params.get("endDate");
        String operatorId = (String) params.get("operatorId");

        LocalDate startDate = LocalDate.parse(startDateStr);
        LocalDate endDate = LocalDate.parse(endDateStr);

        return reconciliationService.performReconciliation(configId, subjectCode, startDate, endDate, 
                operatorId != null ? operatorId : "user_admin");
    }

    /**
     * 获取历史记录
     */
    @GetMapping("/history")
    public List<ReconciliationRecord> getHistory(@RequestParam Long configId) {
        return reconciliationService.getHistory(configId);
    }
}
