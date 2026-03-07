package com.nexusarchive.modules.signature.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.modules.signature.infra.SignatureVerificationRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SignatureVerificationRecordMapper extends BaseMapper<SignatureVerificationRecordEntity> {

    @Select("""
            SELECT *
            FROM arc_signature_verification
            WHERE archive_id = #{archiveId}
            ORDER BY verified_at DESC, created_time DESC
            """)
    List<SignatureVerificationRecordEntity> findByArchiveId(@Param("archiveId") String archiveId);

    @Select("""
            SELECT *
            FROM arc_signature_verification
            WHERE file_id = #{fileId}
            ORDER BY verified_at DESC, created_time DESC
            """)
    List<SignatureVerificationRecordEntity> findByFileId(@Param("fileId") String fileId);
}
