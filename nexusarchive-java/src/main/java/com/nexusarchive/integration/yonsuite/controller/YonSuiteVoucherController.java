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

        // Robust date parsing - handle multiple formats
        java.time.LocalDate startDate = parsePeriodToDate(request.getPeriodStart());
        java.time.LocalDate endDate = parsePeriodToDate(request.getPeriodEnd());
        
        if (startDate == null || endDate == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "无效的期间格式。请使用 YYYY-MM 格式（如 2024-01）"));
        }

        // Build Context
        com.nexusarchive.integration.core.context.SyncContext context = com.nexusarchive.integration.core.context.SyncContext
                .builder()
                .accountBookCode(request.getAccbookCode())
                .startDate(startDate)
                .endDate(endDate)
                .accessToken(request.getAccessToken())
                .build();

        String result = universalSyncEngine.sync(context, "YONSUITE");

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", result));
    }
    
    /**
     * 解析期间字符串为 LocalDate（支持多种格式）
     * 支持: YYYY-MM, YYYY-MM-DD, YYYYMM
     */
    private java.time.LocalDate parsePeriodToDate(String period) {
        if (period == null || period.isEmpty()) {
            return null;
        }
        try {
            // 尝试 YYYY-MM-DD 格式
            if (period.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return java.time.LocalDate.parse(period);
            }
            // 尝试 YYYY-MM 格式
            if (period.matches("\\d{4}-\\d{2}")) {
                return java.time.LocalDate.parse(period + "-01");
            }
            // 尝试 YYYYMM 格式 (6位数字)
            if (period.matches("\\d{6}")) {
                String formatted = period.substring(0, 4) + "-" + period.substring(4, 6) + "-01";
                return java.time.LocalDate.parse(formatted);
            }
            log.warn("无法解析期间格式: {}", period);
            return null;
        } catch (Exception e) {
            log.error("解析期间失败: {}, 错误: {}", period, e.getMessage());
            return null;
        }
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
        private String accbookCode;              // 单个组织代码 (兼容)
        private java.util.List<String> accbookCodes;  // 多个组织代码 (新增)
        private String periodStart;
        private String periodEnd;
        
        /**
         * 获取所有组织代码 (合并单值和数组)
         */
        public java.util.List<String> resolveAllCodes() {
            java.util.List<String> result = new java.util.ArrayList<>();
            if (accbookCodes != null && !accbookCodes.isEmpty()) {
                result.addAll(accbookCodes);
            } else if (accbookCode != null && !accbookCode.isEmpty()) {
                result.add(accbookCode);
            }
            return result;
        }
    }

    @Data
    public static class SyncByIdRequest {
        private String accessToken;
        private String voucherId;
    }
}
