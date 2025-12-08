package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.Archive;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 档案Mapper
 */
@Mapper
public interface ArchiveMapper extends BaseMapper<Archive> {

    @Select("""
            SELECT to_char(date(created_at), 'YYYY-MM-DD') AS date,
                   COUNT(*) AS count
            FROM acc_archive
            WHERE created_at >= CURRENT_DATE - INTERVAL '29 days'
            GROUP BY date(created_at)
            ORDER BY date(created_at)
            """)
    List<Map<String, Object>> selectRecentTrend();

    @Select("<script>SELECT id FROM acc_archive WHERE deleted=0 AND department_id IN "
            + "<foreach collection='departmentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<String> selectIdsByDepartmentIds(@Param("departmentIds") Collection<String> departmentIds);
}
