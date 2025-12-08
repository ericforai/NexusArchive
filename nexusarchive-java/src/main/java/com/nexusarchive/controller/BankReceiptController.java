package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 银企直连回单抓取接口 (Stub)
 * 对接银行前置机
 */
@Slf4j
@RestController
@RequestMapping("/v1/sync/bank")
@RequiredArgsConstructor
public class BankReceiptController {

    /**
     * 接收银行回单
     */
    @PostMapping("/receipt")
    public Result<String> syncBankReceipt(@RequestBody Map<String, Object> receiptData) {
        log.info("Received bank receipt sync request: {}", receiptData);
        // TODO: Implement actual sync logic
        // 1. Parse XML/PDF
        // 2. Match with Bank Journal
        // 3. Create SIP
        // 4. Call IngestService
        return Result.success("Receipt sync request received");
    }
}
