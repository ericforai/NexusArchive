package com.nexusarchive.integration.yonsuite.controller;

import com.nexusarchive.integration.yonsuite.service.YonSuiteVoucherSyncService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * YonSuite 凭证同步控制器
 */
@RestController
@RequestMapping("/integration/yonsuite/vouchers")
@RequiredArgsConstructor
@Slf4j
public class YonSuiteVoucherController {

    private final YonSuiteVoucherSyncService syncService;

    /**
     * 按期间同步凭证
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncVouchers(@RequestBody SyncByPeriodRequest request) {
        log.info("Request to sync vouchers: {}", request);
        
        List<String> syncedIds = syncService.syncVouchersByPeriod(
                request.getAccessToken(),
                request.getAccbookCode(),
                request.getPeriodStart(),
                request.getPeriodEnd()
        );
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "synced_count", syncedIds.size(),
                "synced_ids", syncedIds
        ));
    }

    /**
     * 按凭证ID同步单个凭证
     */
    @PostMapping("/sync-by-id")
    public ResponseEntity<Map<String, Object>> syncVoucherById(@RequestBody SyncByIdRequest request) {
        log.info("Request to sync voucher by id: {}", request.getVoucherId());
        
        String archiveId = syncService.syncVoucherById(
                request.getAccessToken(),
                request.getVoucherId()
        );
        
        if (archiveId == null) {
            return ResponseEntity.ok(Map.of(
                    "status", "NOT_FOUND",
                    "message", "Voucher not found in YonSuite"
            ));
        }
        
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "archive_id", archiveId
        ));
    }

    @Data
    public static class SyncByPeriodRequest {
        private String accessToken;
        private String accbookCode;
        private String periodStart;
        private String periodEnd;
    }

    @Data
    public static class SyncByIdRequest {
        private String accessToken;
        private String voucherId;
    }
}
