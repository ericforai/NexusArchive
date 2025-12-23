// Input: MyBatis-Plus
// Output: OriginalVoucherTypeMapper 接口
// Pos: 数据访问层

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.OriginalVoucherType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 原始凭证类型字典 Mapper
 */
@Mapper
public interface OriginalVoucherTypeMapper extends BaseMapper<OriginalVoucherType> {

    /**
     * 按一级类型查询所有二级类型
     */
    @Select("SELECT * FROM sys_original_voucher_type WHERE category_code = #{categoryCode} AND enabled = TRUE ORDER BY sort_order")
    List<OriginalVoucherType> findByCategory(@Param("categoryCode") String categoryCode);

    /**
     * 查询所有启用的类型
     */
    @Select("SELECT * FROM sys_original_voucher_type WHERE enabled = TRUE ORDER BY sort_order")
    List<OriginalVoucherType> findAllEnabled();

    /**
     * 按类型代码查询
     */
    @Select("SELECT * FROM sys_original_voucher_type WHERE type_code = #{typeCode}")
    OriginalVoucherType findByTypeCode(@Param("typeCode") String typeCode);
}
