// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: ArchiveMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
            SELECT to_char(date(created_time), 'YYYY-MM-DD') AS date,
                   COUNT(*) AS count
            FROM acc_archive
            WHERE created_time >= CURRENT_DATE - INTERVAL '29 days'
            GROUP BY date(created_time)
            ORDER BY date(created_time)
            """)
    List<Map<String, Object>> selectRecentTrend();

    @Select("<script>SELECT id FROM acc_archive WHERE deleted=0 AND department_id IN "
            + "<foreach collection='departmentIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<String> selectIdsByDepartmentIds(@Param("departmentIds") Collection<String> departmentIds);

    @Select("SELECT DISTINCT custom_metadata->>'bookType' FROM acc_archive WHERE category_code = 'AC02' AND fonds_no = #{fondsNo} AND custom_metadata->>'bookType' IS NOT NULL")
    List<String> selectDistinctBookTypes(@Param("fondsNo") String fondsNo);

    /**
     * 查询到期档案
     */
    @Select("""
        <script>
        SELECT * FROM acc_archive
        <where>
            deleted = 0
            AND status = 'archived'
            AND retention_period != 'PERMANENT'
            AND (
                (retention_period = '10Y' AND (retention_start_date + INTERVAL '10 years') &lt; CURRENT_DATE)
                OR (retention_period = '30Y' AND (retention_start_date + INTERVAL '30 years') &lt; CURRENT_DATE)
            )
            <if test="fondsNo != null and fondsNo != ''">
                AND fonds_no = #{fondsNo}
            </if>
        </where>
        ORDER BY retention_start_date ASC
        </script>
    """)
    com.baomidou.mybatisplus.core.metadata.IPage<Archive> selectExpired(
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Archive> page,
        @Param("fondsNo") String fondsNo
    );
}
