// Input: JUnit 5、Spring Boot Test
// Output: SalesOrderSyncTest 测试类
// Pos: 后端测试用例

package com.nexusarchive.integration.yonsuite;

import com.nexusarchive.entity.SalesOrder;
import com.nexusarchive.integration.yonsuite.dto.SalesOrderListResponse;
import com.nexusarchive.integration.yonsuite.mapper.SalesOrderDataMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 销售订单同步测试
 */
@SpringBootTest
@ActiveProfiles("test")
public class SalesOrderSyncTest {

    @Autowired
    private SalesOrderDataMapper dataMapper;

    /**
     * 测试数据映射器 - 列表记录转实体
     */
    @Test
    public void testMapperToSalesOrder() {
        // Given
        var record = new SalesOrderListResponse.SalesOrderRecord();
        record.setId("test-order-123");
        record.setCode("UO-2024-001");
        record.setAgentId("customer-001");
        record.setAgentName("测试客户");
        record.setVouchdate("2024-01-15");
        record.setTotalMoney(10000.0);
        record.setPayMoney(10000.0);
        record.setRealMoney(10000.0);
        record.setNextStatusName("ENDORDER");
        record.setPubts("20240115120000");

        // When
        SalesOrder order = dataMapper.toSalesOrder(record);

        // Then
        assertNotNull(order);
        assertEquals("test-order-123", order.getOrderId());
        assertEquals("UO-2024-001", order.getOrderCode());
        assertEquals("测试客户", order.getAgentName());
        assertEquals(BigDecimal.valueOf(10000.0), order.getPayMoney());
        assertEquals("ENDORDER", order.getNextStatusName());
    }

    /**
     * 测试数据映射器 - 空记录处理
     */
    @Test
    public void testMapperToSalesOrderWithNull() {
        // When
        SalesOrder order = dataMapper.toSalesOrder(null);

        // Then
        assertNull(order);
    }

    /**
     * 测试数据映射器 - BigDecimal 转换
     */
    @Test
    public void testBigDecimalConversion() {
        // Given
        var record = new SalesOrderListResponse.SalesOrderRecord();
        record.setId("test-001");
        record.setCode("TEST-001");
        record.setTotalMoney(null);  // 测试 null 值

        // When
        SalesOrder order = dataMapper.toSalesOrder(record);

        // Then
        assertNotNull(order);
        assertEquals(BigDecimal.ZERO, order.getTotalMoney());
    }
}
