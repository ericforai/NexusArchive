// Input: MyBatis-Plus
// Output: OriginalVoucherFileMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.OriginalVoucherFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 原始凭证文件 Mapper
 */
@Mapper
public interface OriginalVoucherFileMapper extends BaseMapper<OriginalVoucherFile> {

    /**
     * 按凭证ID查询所有文件
     */
    @Select("SELECT * FROM arc_original_voucher_file WHERE voucher_id = #{voucherId} AND deleted = 0 ORDER BY sequence_no")
    List<OriginalVoucherFile> findByVoucherId(@Param("voucherId") String voucherId);

    /**
     * 按哈希值查询文件（用于完整性校验）
     */
    @Select("SELECT * FROM arc_original_voucher_file WHERE file_hash = #{hash} AND deleted = 0")
    List<OriginalVoucherFile> findByHash(@Param("hash") String hash);
}
