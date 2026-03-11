package com.nexusarchive.modules.document.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.modules.document.domain.DocumentReminderEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DocumentReminderMapper extends BaseMapper<DocumentReminderEntity> {

    @Select("""
            SELECT *
            FROM document_reminders
            WHERE project_id = #{projectId}
              AND section_id = #{sectionId}
            ORDER BY created_at DESC
            """)
    List<DocumentReminderEntity> findBySectionId(@Param("projectId") String projectId,
                                                 @Param("sectionId") String sectionId);

    @Select("""
            SELECT *
            FROM document_reminders
            WHERE project_id = #{projectId}
            ORDER BY created_at DESC
            """)
    List<DocumentReminderEntity> findByProjectId(@Param("projectId") String projectId);

    @Delete("""
            DELETE FROM document_reminders
            WHERE project_id = #{projectId}
            """)
    void deleteByProjectId(@Param("projectId") String projectId);
}
