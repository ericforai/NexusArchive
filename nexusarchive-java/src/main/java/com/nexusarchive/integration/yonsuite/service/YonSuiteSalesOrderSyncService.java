// Input: Spring、Lombok、MyBatis-Plus
// Output: YonSuiteSalesOrderSyncService
// Pos: 业务服务层

package com.nexusarchive.integration.yonsuite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.SalesOrder;
import com.nexusarchive.entity.SalesOrderDetail;
import com.nexusarchive.integration.yonsuite.client.YonSuiteSalesOrderClient;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderDetailResponse;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListRequest;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListResponse;
import com.nexusarchive.integration.yonsuite.mapper.SalesOrderDataMapper;
import com.nexusarchive.mapper.SdSalesOrderDetailMapper;
import com.nexusarchive.mapper.SdSalesOrderMapper;
import com.nexusarchive.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * YonSuite 销售订单同步服务
 * <p>
 * 负责从 YonSuite 同步销售订单数据：
 * <ul>
 *   <li>两步 API 调用（列表 → 详情）</li>
 *   <li>增量同步（基于 pubts 时间戳）</li>
 *   <li>关联匹配（销售出库单、记账凭证）</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class YonSuiteSalesOrderSyncService {

    private final YonSuiteSalesOrderClient salesOrderClient;
    private final SdSalesOrderMapper salesOrderMapper;
    private final SdSalesOrderDetailMapper salesOrderDetailMapper;
    private final SalesOrderDataMapper dataMapper;
    private final DataScopeService dataScopeService;

    /**
     * 同步销售订单
     */
    public SyncResult syncSalesOrders(String accessToken, SalesOrderListRequest request) {
        log.info("开始同步销售订单: vouchdateBegin={}, vouchdateEnd={}",
                request.getVouchdateBegin(), request.getVouchdateEnd());

        SyncResult result = new SyncResult();
        int pageIndex = 1;
        boolean hasMore = true;

        while (hasMore) {
            request.setPageIndex(pageIndex);

            SalesOrderListResponse response = salesOrderClient.querySalesOrders(accessToken, request);

            if (response.getData() == null || response.getData().getRecordList() == null) {
                break;
            }

            List<SalesOrderListResponse.SalesOrderRecord> records = response.getData().getRecordList();

            for (SalesOrderListResponse.SalesOrderRecord record : records) {
                try {
                    processOrderRecord(accessToken, record, result);
                } catch (Exception e) {
                    log.error("处理订单失败: {}", record.getId(), e);
                    result.addError(record.getCode(), e.getMessage());
                }
            }

            int totalPages = (int) Math.ceil((double) response.getData().getRecordCount() / request.getPageSize());
            hasMore = pageIndex < totalPages;
            pageIndex++;
        }

        log.info("销售订单同步完成: total={}, success={}, failed={}",
                result.total, result.success, result.failed);

        return result;
    }

    /**
     * 处理单条订单记录
     */
    @Transactional
    private void processOrderRecord(String accessToken, SalesOrderListResponse.SalesOrderRecord record, SyncResult result) {
        result.total++;

        // 1. 检查是否已存在
        SalesOrder existing = salesOrderMapper.selectByOrderId(record.getId());
        if (existing != null) {
            // 检查 pubts 是否变化
            if (record.getPubts() != null && record.getPubts().equals(existing.getPubts())) {
                result.skipped++;
                return;
            }
        }

        // 2. 获取详情
        SalesOrderDetailResponse detailResponse = salesOrderClient.querySalesOrderById(accessToken, record.getId());
        if (detailResponse == null || detailResponse.getData() == null) {
            result.addError(record.getCode(), "详情API返回空");
            return;
        }

        // 3. 映射数据
        SalesOrder order = dataMapper.toSalesOrderFromDetail(detailResponse.getData());

        // 设置全宗（从当前用户的全宗上下文获取第一个）
        DataScopeService.DataScopeContext scope = dataScopeService.resolve();
        Set<String> allowedFonds = scope.allowedFonds();
        if (allowedFonds != null && !allowedFonds.isEmpty()) {
            order.setFondsCode(allowedFonds.iterator().next());
        }

        // 4. 保存或更新
        if (existing != null) {
            order.setId(existing.getId());
            salesOrderMapper.updateById(order);
        } else {
            salesOrderMapper.insert(order);
        }

        // 5. 保存明细（先删除旧明细）
        salesOrderDetailMapper.delete(
                new LambdaQueryWrapper<SalesOrderDetail>()
                        .eq(SalesOrderDetail::getOrderId, order.getId())
        );

        List<SalesOrderDetail> details = dataMapper.toSalesOrderDetails(
                order.getId().toString(),
                detailResponse.getData().getOrderDetails()
        );

        for (SalesOrderDetail detail : details) {
            salesOrderDetailMapper.insert(detail);
        }

        result.success++;
    }

    /**
     * 同步结果
     */
    public static class SyncResult {
        public int total = 0;
        public int success = 0;
        public int failed = 0;
        public int skipped = 0;
        public List<ErrorItem> errors = new ArrayList<>();

        public void addError(String code, String reason) {
            failed++;
            errors.add(new ErrorItem(code, reason));
        }

        public static class ErrorItem {
            public String orderCode;
            public String reason;

            public ErrorItem(String orderCode, String reason) {
                this.orderCode = orderCode;
                this.reason = reason;
            }
        }
    }
}
