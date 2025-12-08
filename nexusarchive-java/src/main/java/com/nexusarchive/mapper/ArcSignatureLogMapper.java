package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.ArcSignatureLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 签章日志 Mapper
 * 
 * @author Agent B - 合规开发工程师
 */
@Mapper
public interface ArcSignatureLogMapper extends BaseMapper<ArcSignatureLog> {
    
    /**
     * 根据档案ID查询签章日志
     */
    @Select("SELECT * FROM arc_signature_log WHERE archive_id = #{archiveId} ORDER BY created_time DESC")
    List<ArcSignatureLog> findByArchiveId(@Param("archiveId") String archiveId);
    
    /**
     * 根据文件ID查询签章日志
     */
    @Select("SELECT * FROM arc_signature_log WHERE file_id = #{fileId} ORDER BY created_time DESC")
    List<ArcSignatureLog> findByFileId(@Param("fileId") String fileId);
    
    /**
     * 查询验证失败的签章日志
     */
    @Select("SELECT * FROM arc_signature_log WHERE verify_result = 'INVALID' ORDER BY verify_time DESC")
    List<ArcSignatureLog> findInvalidSignatures();
}
