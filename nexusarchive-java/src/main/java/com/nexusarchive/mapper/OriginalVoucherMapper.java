// Input: MyBatis-Plus
// Output: OriginalVoucherMapper 接口
// Pos: 数据访问层

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.OriginalVoucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 原始凭证 Mapper
 */
@Mapper
public interface OriginalVoucherMapper extends BaseMapper<OriginalVoucher> {

    /**
     * 获取下一个凭证序号
     */
    @Select("SELECT COALESCE(MAX(current_seq), 0) + 1 FROM arc_original_voucher_sequence " +
            "WHERE fonds_code = #{fondsCode} AND fiscal_year = #{fiscalYear} AND voucher_category = #{category}")
    Long getNextSequence(@Param("fondsCode") String fondsCode,
            @Param("fiscalYear") String fiscalYear,
            @Param("category") String category);

    /**
     * 更新序列号
     */
    @Update("INSERT INTO arc_original_voucher_sequence (id, fonds_code, fiscal_year, voucher_category, current_seq) " +
            "VALUES (#{id}, #{fondsCode}, #{fiscalYear}, #{category}, #{seq}) " +
            "ON CONFLICT (fonds_code, fiscal_year, voucher_category) " +
            "DO UPDATE SET current_seq = #{seq}, last_updated = CURRENT_TIMESTAMP")
    void updateSequence(@Param("id") String id,
            @Param("fondsCode") String fondsCode,
            @Param("fiscalYear") String fiscalYear,
            @Param("category") String category,
            @Param("seq") Long seq);

    /**
     * 查询版本历史
     */
    @Select("WITH RECURSIVE version_chain AS (" +
            "  SELECT * FROM arc_original_voucher WHERE id = #{id} AND deleted = 0 " +
            "  UNION ALL " +
            "  SELECT ov.* FROM arc_original_voucher ov " +
            "  INNER JOIN version_chain vc ON ov.id = vc.parent_version_id " +
            "  WHERE ov.deleted = 0" +
            ") SELECT * FROM version_chain ORDER BY version DESC")
    List<OriginalVoucher> findVersionHistory(@Param("id") String id);
}
