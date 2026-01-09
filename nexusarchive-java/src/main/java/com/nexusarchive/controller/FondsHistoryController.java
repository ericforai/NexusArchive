// Input: Spring Web、FondsHistoryService
// Output: FondsHistoryController 类
// Pos: Web 控制器层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.controller;

import com.nexusarchive.common.Result;
import com.nexusarchive.dto.FondsHistoryDetail;
import com.nexusarchive.service.FondsHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 全宗沿革控制器
 * 
 * PRD 来源: Section 1.1 - 全宗沿革可追溯
 */
@Slf4j
@RestController
@RequestMapping("/fonds-history")
@RequiredArgsConstructor
@Tag(name = "全宗沿革管理", description = "全宗迁移、合并、分立、重命名接口")
public class FondsHistoryController {
    
    private final FondsHistoryService fondsHistoryService;
    
    @PostMapping("/migrate")
    @Operation(summary = "全宗迁移")
    @PreAuthorize("hasAnyAuthority('fonds:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, String>> migrateFonds(
            @RequestParam String fromFondsNo,
            @RequestParam String toFondsNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam String reason,
            @RequestParam(required = false) String approvalTicketId,
            @RequestHeader("X-User-Id") String operatorId) {
        
        try {
            String historyId = fondsHistoryService.migrateFonds(
                fromFondsNo, toFondsNo, effectiveDate, reason, approvalTicketId, operatorId);
            return Result.success(Map.of("historyId", historyId));
        } catch (Exception e) {
            log.error("全宗迁移失败: fromFondsNo={}, toFondsNo={}", fromFondsNo, toFondsNo, e);
            return Result.fail("全宗迁移失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/merge")
    @Operation(summary = "全宗合并")
    @PreAuthorize("hasAnyAuthority('fonds:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, Object>> mergeFonds(
            @RequestParam List<String> sourceFondsNos,
            @RequestParam String targetFondsNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam String reason,
            @RequestParam(required = false) String approvalTicketId,
            @RequestHeader("X-User-Id") String operatorId) {
        
        try {
            List<String> historyIds = fondsHistoryService.mergeFonds(
                sourceFondsNos, targetFondsNo, effectiveDate, reason, approvalTicketId, operatorId);
            return Result.success(Map.of("historyIds", historyIds));
        } catch (Exception e) {
            log.error("全宗合并失败: sourceFondsNos={}, targetFondsNo={}", sourceFondsNos, targetFondsNo, e);
            return Result.fail("全宗合并失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/split")
    @Operation(summary = "全宗分立")
    @PreAuthorize("hasAnyAuthority('fonds:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, Object>> splitFonds(
            @RequestParam String sourceFondsNo,
            @RequestParam List<String> newFondsNos,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam String reason,
            @RequestParam(required = false) String approvalTicketId,
            @RequestHeader("X-User-Id") String operatorId) {
        
        try {
            List<String> historyIds = fondsHistoryService.splitFonds(
                sourceFondsNo, newFondsNos, effectiveDate, reason, approvalTicketId, operatorId);
            return Result.success(Map.of("historyIds", historyIds));
        } catch (Exception e) {
            log.error("全宗分立失败: sourceFondsNo={}, newFondsNos={}", sourceFondsNo, newFondsNos, e);
            return Result.fail("全宗分立失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/rename")
    @Operation(summary = "全宗重命名")
    @PreAuthorize("hasAnyAuthority('fonds:manage') or hasRole('SYS_ADMIN')")
    public Result<Map<String, String>> renameFonds(
            @RequestParam String oldFondsNo,
            @RequestParam String newFondsNo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            @RequestParam String reason,
            @RequestParam(required = false) String approvalTicketId,
            @RequestHeader("X-User-Id") String operatorId) {
        
        try {
            String historyId = fondsHistoryService.renameFonds(
                oldFondsNo, newFondsNo, effectiveDate, reason, approvalTicketId, operatorId);
            return Result.success(Map.of("historyId", historyId));
        } catch (Exception e) {
            log.error("全宗重命名失败: oldFondsNo={}, newFondsNo={}", oldFondsNo, newFondsNo, e);
            return Result.fail("全宗重命名失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{fondsNo}")
    @Operation(summary = "查询全宗沿革历史")
    @PreAuthorize("hasAnyAuthority('fonds:view', 'fonds:manage')")
    public Result<List<FondsHistoryDetail>> getFondsHistory(@PathVariable String fondsNo) {
        try {
            List<FondsHistoryDetail> history = fondsHistoryService.getFondsHistory(fondsNo);
            return Result.success(history);
        } catch (Exception e) {
            log.error("查询全宗沿革历史失败: fondsNo={}", fondsNo, e);
            return Result.fail("查询失败: " + e.getMessage());
        }
    }
}





