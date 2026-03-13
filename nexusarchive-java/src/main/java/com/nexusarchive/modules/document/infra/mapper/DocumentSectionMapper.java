package com.nexusarchive.modules.document.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.modules.document.domain.DocumentSectionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentSectionMapper extends BaseMapper<DocumentSectionEntity> {

    @Select("""
            SELECT *
            FROM document_sections
            WHERE project_id = #{projectId}
            ORDER BY sort_order ASC, created_at ASC
            """)
    List<DocumentSectionEntity> findByProjectId(@Param("projectId") String projectId);

    @Delete("""
            DELETE FROM document_sections
            WHERE project_id = #{projectId}
            """)
    void deleteByProjectId(@Param("projectId") String projectId);
}
