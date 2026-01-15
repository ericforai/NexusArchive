package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.CollectionBatchFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资料收集批次文件 Mapper
 */
@Mapper
public interface CollectionBatchFileMapper extends BaseMapper<CollectionBatchFile> {

    /**
     * 查询批次的文件列表
     */
    @Select("SELECT * FROM collection_batch_file " +
            "WHERE batch_id = #{batchId} " +
            "ORDER BY upload_order ASC")
    List<CollectionBatchFile> findByBatchId(@Param("batchId") Long batchId);

    /**
     * 查询待处理的文件
     */
    @Select("SELECT * FROM collection_batch_file " +
            "WHERE batch_id = #{batchId} " +
            "AND upload_status IN ('PENDING', 'UPLOADING') " +
            "ORDER BY upload_order ASC " +
            "LIMIT #{limit}")
    List<CollectionBatchFile> findPendingFiles(
        @Param("batchId") Long batchId,
        @Param("limit") int limit
    );

    /**
     * 统计批次各状态文件数
     */
    @Select("SELECT upload_status, COUNT(*) as count " +
            "FROM collection_batch_file " +
            "WHERE batch_id = #{batchId} " +
            "GROUP BY upload_status")
    List<java.util.Map<String, Object>> getStatusStats(@Param("batchId") Long batchId);

    /**
     * 检查文件哈希是否已存在 (幂等性)
     * NOTE: fondsId is String (VARCHAR(32)) to match V87 migration
     */
    @Select("SELECT cbf.* FROM collection_batch_file cbf " +
            "INNER JOIN collection_batch cb ON cbf.batch_id = cb.id " +
            "WHERE cbf.file_hash = #{fileHash} " +
            "AND cb.fonds_id = #{fondsId} " +
            "AND cb.fiscal_year = #{fiscalYear} " +
            "AND cbf.upload_status IN ('UPLOADED', 'VALIDATED') " +
            "LIMIT 1")
    CollectionBatchFile findDuplicateByHash(
        @Param("fileHash") String fileHash,
        @Param("fondsId") String fondsId,  // String, not Long!
        @Param("fiscalYear") String fiscalYear
    );

    /**
     * 根据文件ID查询批次文件记录
     * 用于获取关联的档案ID
     */
    @Select("SELECT * FROM collection_batch_file " +
            "WHERE file_id = #{fileId} " +
            "LIMIT 1")
    CollectionBatchFile selectByFileId(@Param("fileId") String fileId);
}
