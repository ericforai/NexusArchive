// Input: MyBatis-Plus
// Output: SdSalesOrderMapper
// Pos: MyBatis Mapper

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 销售订单 MyBatis Mapper
 */
@Mapper
public interface SdSalesOrderMapper extends BaseMapper<SalesOrder> {

    /**
     * 根据 YonSuite 订单ID查询
     */
    @Select("SELECT * FROM sd_sales_order WHERE order_id = #{orderId}")
    SalesOrder selectByOrderId(@Param("orderId") String orderId);

    /**
     * 查询需要关联的订单（用于关联匹配）
     */
    @Select("SELECT * FROM sd_sales_order WHERE agent_id = #{agentId} " +
            "AND vouchdate = #{vouchdate} AND sales_out_id IS NULL LIMIT 100")
    List<SalesOrder> selectForRelation(@Param("agentId") String agentId,
                                      @Param("vouchdate") String vouchdate);
}
