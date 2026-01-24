// Input: Spring Web、Lombok
// Output: SalesOrderController
// Pos: 控制器层

package com.nexusarchive.controller;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListRequest;
import com.nexusarchive.integration.yonsuite.service.YonAuthService;
import com.nexusarchive.integration.yonsuite.service.YonSuiteSalesOrderSyncService;
import com.nexusarchive.service.DataScopeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 销售订单同步控制器
 */
@RestController
@RequestMapping("/sales-order")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "销售订单同步")
public class SalesOrderController {

    private final YonSuiteSalesOrderSyncService syncService;
    private final YonAuthService yonAuthService;
    private final DataScopeService dataScopeService;

    /**
     * 同步销售订单
     * POST /sales-order/sync
     */
    @PostMapping("/sync")
    @Operation(summary = "同步销售订单")
    public Result<YonSuiteSalesOrderSyncService.SyncResult> syncSalesOrders(
            @RequestBody SalesOrderListRequest request
    ) {
        // 从 YonAuthService 获取 access_token (自动刷新)
        String accessToken = yonAuthService.getAccessToken();

        log.info("收到销售订单同步请求: dateBegin={}, dateEnd={}",
                request.getVouchdateBegin(), request.getVouchdateEnd());

        YonSuiteSalesOrderSyncService.SyncResult result = syncService.syncSalesOrders(accessToken, request);

        return Result.success(result);
    }

    /**
     * 查询订单详情
     * GET /sales-order/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询订单详情")
    public Result<Object> getSalesOrder(@PathVariable Long id) {
        // TODO: 实现查询逻辑
        return Result.success(null);
    }

    /**
     * 查询关联单据链路
     * GET /sales-order/{id}/relations
     */
    @GetMapping("/{id}/relations")
    @Operation(summary = "查询关联单据链路")
    public Result<Object> getRelations(@PathVariable Long id) {
        // TODO: 实现关联查询
        return Result.success(null);
    }
}
