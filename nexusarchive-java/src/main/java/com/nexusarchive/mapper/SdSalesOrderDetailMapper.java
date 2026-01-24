// Input: MyBatis-Plus
// Output: SdSalesOrderDetailMapper
// Pos: MyBatis Mapper

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.SalesOrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 销售订单明细 MyBatis Mapper
 */
@Mapper
public interface SdSalesOrderDetailMapper extends BaseMapper<SalesOrderDetail> {
}
