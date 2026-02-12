// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: CollectionBatchMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.CollectionBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 资料收集批次 Mapper
 */
@Mapper
public interface CollectionBatchMapper extends BaseMapper<CollectionBatch> {

    /**
     * 查询用户的批次列表
     */
    @Select("SELECT * FROM collection_batch " +
            "WHERE created_by = #{userId} " +
            "ORDER BY created_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<CollectionBatch> findByUserId(
        @Param("userId") String userId,
        @Param("limit") int limit,
        @Param("offset") int offset
    );

    /**
     * 更新批次统计信息
     */
    @Update("UPDATE collection_batch " +
            "SET uploaded_files = (" +
            "   SELECT COUNT(*) FROM collection_batch_file " +
            "   WHERE batch_id = #{batchId} AND upload_status IN ('UPLOADED', 'VALIDATED')" +
            "), " +
            "failed_files = (" +
            "   SELECT COUNT(*) FROM collection_batch_file " +
            "   WHERE batch_id = #{batchId} AND upload_status IN ('FAILED', 'DUPLICATE', 'CHECK_FAILED')" +
            "), " +
            "total_size_bytes = (" +
            "   SELECT COALESCE(SUM(file_size_bytes), 0) FROM collection_batch_file " +
            "   WHERE batch_id = #{batchId}" +
            "), " +
            "last_modified_time = CURRENT_TIMESTAMP " +
            "WHERE id = #{batchId}")
    int updateStatistics(@Param("batchId") Long batchId);

    /**
     * 更新批次状态
     */
    @Update("UPDATE collection_batch " +
            "SET status = #{status}, " +
            "completed_time = CASE " +
            "   WHEN #{status} IN ('VALIDATED', 'ARCHIVED') THEN CURRENT_TIMESTAMP " +
            "   ELSE completed_time " +
            "END, " +
            "last_modified_time = CURRENT_TIMESTAMP, " +
            "error_message = #{errorMessage} " +
            "WHERE id = #{batchId}")
    int updateStatus(
        @Param("batchId") Long batchId,
        @Param("status") String status,
        @Param("errorMessage") String errorMessage
    );

    /**
     * 查询指定日期的最大批次号
     * @param datePart 日期部分 (yyyyMMdd 格式)
     * @return 最大批次号，不存在返回 null
     */
    @Select("SELECT batch_no FROM collection_batch " +
            "WHERE batch_no LIKE 'COL-' || #{datePart} || '-%' " +
            "ORDER BY batch_no DESC " +
            "LIMIT 1")
    String selectMaxBatchNoByDate(@Param("datePart") String datePart);

    /**
     * 通过 PostgreSQL 函数生成下一个批次号（并发安全）
     * 内部使用 pg_advisory_xact_lock 确保同日期序号串行化
     * @param datePart 日期部分 (yyyyMMdd 格式)
     * @return 下一个批次编号，格式: COL-YYYYMMDD-NNNNN
     */
    @Select("SELECT next_batch_no(#{datePart})")
    String nextBatchNo(@Param("datePart") String datePart);

    /**
     * 查询指定日期的所有批次号（用于悲观锁）
     * FOR UPDATE 锁定这些行，防止并发修改
     * @param datePart 日期部分 (yyyyMMdd 格式)
     * @return 所有匹配的批次号
     * @deprecated 已被 {@link #nextBatchNo(String)} 替代，保留供回退使用
     */
    @Deprecated
    @Select("SELECT batch_no FROM collection_batch " +
            "WHERE batch_no LIKE 'COL-' || #{datePart} || '-%' " +
            "FOR UPDATE")
    List<String> selectBatchNosForUpdate(@Param("datePart") String datePart);
}
