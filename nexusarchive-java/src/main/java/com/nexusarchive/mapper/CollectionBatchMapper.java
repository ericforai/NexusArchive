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
        @Param("userId") Long userId,
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
}
