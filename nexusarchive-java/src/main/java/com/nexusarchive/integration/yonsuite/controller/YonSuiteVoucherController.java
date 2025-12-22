// Input: Lombok、Spring Framework、Java 标准库、本地模块
// Output: YonSuiteVoucherController 类
// Pos: 接口层 Controller
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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

    private final com.nexusarchive.integration.service.UniversalSyncEngine universalSyncEngine;

    /**
     * 按期间同步凭证
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncVouchers(@RequestBody SyncByPeriodRequest request) {
        log.info("Request to sync vouchers via Universal Engine: {}", request);

        // Build Context
        com.nexusarchive.integration.core.context.SyncContext context = com.nexusarchive.integration.core.context.SyncContext
                .builder()
                .accountBookCode(request.getAccbookCode())
                .startDate(java.time.LocalDate.parse(request.getPeriodStart() + "-01")) // Assuming YYYY-MM
                .endDate(java.time.LocalDate.parse(request.getPeriodEnd() + "-01")) // Simplified
                .accessToken(request.getAccessToken())
                .build();

        String result = universalSyncEngine.sync(context, "YONSUITE");

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", result));
    }

    /**
     * 按凭证ID同步单个凭证
     */
    @PostMapping("/sync-by-id")
    public ResponseEntity<Map<String, Object>> syncVoucherById(@RequestBody SyncByIdRequest request) {
        log.info("Request to sync voucher by id via Universal Engine: {}", request.getVoucherId());

        com.nexusarchive.integration.core.context.SyncContext context = com.nexusarchive.integration.core.context.SyncContext
                .builder()
                .accessToken(request.getAccessToken())
                .build();

        String archiveId = universalSyncEngine.syncById(context, "YONSUITE", request.getVoucherId());

        if (archiveId == null) {
            return ResponseEntity.ok(Map.of(
                    "status", "NOT_FOUND",
                    "message", "Voucher not found in YonSuite"));
        }

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "archive_id", archiveId));
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
