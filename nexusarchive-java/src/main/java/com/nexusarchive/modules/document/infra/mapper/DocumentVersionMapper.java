package com.nexusarchive.modules.document.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.modules.document.domain.DocumentVersionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersionEntity> {

    @Select("""
            SELECT *
            FROM document_versions
            WHERE project_id = #{projectId}
            ORDER BY created_at DESC
            """)
    List<DocumentVersionEntity> findByProjectId(@Param("projectId") String projectId);
}
