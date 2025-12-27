// Input: MyBatis-Plus、Apache Ibatis、Java 标准库
// Output: ArchiveBatchItemMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ArchiveBatchItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 归档批次条目 Mapper
 */
@Mapper
public interface ArchiveBatchItemMapper extends BaseMapper<ArchiveBatchItem> {

    /**
     * 按批次查询所有条目
     */
    @Select("SELECT * FROM archive_batch_item WHERE batch_id = #{batchId} ORDER BY id")
    List<ArchiveBatchItem> findByBatchId(@Param("batchId") Long batchId);

    /**
     * 按批次和类型查询条目
     */
    @Select("""
        SELECT * FROM archive_batch_item
        WHERE batch_id = #{batchId}
        AND item_type = #{itemType}
        ORDER BY id
    """)
    List<ArchiveBatchItem> findByBatchIdAndType(
        @Param("batchId") Long batchId,
        @Param("itemType") String itemType
    );

    /**
     * 统计批次中各类型的数量
     */
    @Select("""
        SELECT item_type, COUNT(*) as count
        FROM archive_batch_item
        WHERE batch_id = #{batchId}
        GROUP BY item_type
    """)
    List<java.util.Map<String, Object>> countByType(@Param("batchId") Long batchId);

    /**
     * 统计批次中失败的条目数
     */
    @Select("""
        SELECT COUNT(*) FROM archive_batch_item
        WHERE batch_id = #{batchId}
        AND status = 'FAILED'
    """)
    int countFailedItems(@Param("batchId") Long batchId);

    /**
     * 批量更新条目状态
     */
    @Update("""
        UPDATE archive_batch_item
        SET status = #{status}
        WHERE batch_id = #{batchId}
    """)
    int updateStatusByBatchId(@Param("batchId") Long batchId, @Param("status") String status);

    /**
     * 检查凭证是否已在其他批次中
     */
    @Select("""
        SELECT COUNT(*) FROM archive_batch_item bi
        JOIN archive_batch b ON bi.batch_id = b.id
        WHERE bi.item_type = 'VOUCHER'
        AND bi.ref_id = #{refId}
        AND b.status NOT IN ('REJECTED', 'FAILED')
    """)
    int countVoucherInOtherBatches(@Param("refId") Long refId);
}
