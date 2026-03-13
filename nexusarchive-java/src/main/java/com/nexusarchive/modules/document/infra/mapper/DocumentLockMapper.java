package com.nexusarchive.modules.document.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.modules.document.domain.DocumentLockEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentLockMapper extends BaseMapper<DocumentLockEntity> {

    @Select("""
            SELECT *
            FROM document_locks
            WHERE project_id = #{projectId}
            ORDER BY created_at DESC
            """)
    List<DocumentLockEntity> findByProjectId(@Param("projectId") String projectId);

    @Select("""
            SELECT *
            FROM document_locks
            WHERE project_id = #{projectId}
              AND section_id = #{sectionId}
            ORDER BY created_at DESC
            LIMIT 1
            """)
    DocumentLockEntity findLatestBySectionId(@Param("projectId") String projectId,
                                             @Param("sectionId") String sectionId);

    @Delete("""
            DELETE FROM document_locks
            WHERE project_id = #{projectId}
            """)
    void deleteByProjectId(@Param("projectId") String projectId);
}
