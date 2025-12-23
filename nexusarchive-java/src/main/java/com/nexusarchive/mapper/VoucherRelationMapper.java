// Input: MyBatis-Plus
// Output: VoucherRelationMapper 接口
// Pos: 数据访问层

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.VoucherRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 原始凭证与记账凭证关联 Mapper
 */
@Mapper
public interface VoucherRelationMapper extends BaseMapper<VoucherRelation> {

    /**
     * 按原始凭证ID查询关联的记账凭证
     */
    @Select("SELECT * FROM arc_voucher_relation WHERE original_voucher_id = #{originalVoucherId} AND deleted = 0")
    List<VoucherRelation> findByOriginalVoucherId(@Param("originalVoucherId") String originalVoucherId);

    /**
     * 按记账凭证ID查询关联的原始凭证
     */
    @Select("SELECT * FROM arc_voucher_relation WHERE accounting_voucher_id = #{accountingVoucherId} AND deleted = 0")
    List<VoucherRelation> findByAccountingVoucherId(@Param("accountingVoucherId") String accountingVoucherId);

    /**
     * 检查关联是否已存在
     */
    @Select("SELECT COUNT(*) FROM arc_voucher_relation " +
            "WHERE original_voucher_id = #{originalId} AND accounting_voucher_id = #{accountingId} AND deleted = 0")
    int countRelation(@Param("originalId") String originalId, @Param("accountingId") String accountingId);
}
