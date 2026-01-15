// Input: MyBatis、Apache、Java 标准库、本地模块
// Output: StatsMapper 接口
// Pos: 数据访问层 - 统计专用 Mapper
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计专用 Mapper，使用优化的 SQL 聚合查询
 * 所有查询都考虑了索引使用和查询效率
 */
@Mapper
public interface StatsMapper {

    /**
     * 获取借阅统计（单次查询替代 N+1）
     * 使用条件聚合避免多次查询
     */
    @Select("SELECT " +
            "COUNT(*) FILTER (WHERE status = 'PENDING') AS pending_count, " +
            "COUNT(*) FILTER (WHERE status = 'APPROVED') AS approved_count, " +
            "COUNT(*) FILTER (WHERE status = 'BORROWED') AS borrowed_count, " +
            "COUNT(*) FILTER (WHERE status = 'OVERDUE') AS overdue_count, " +
            "COUNT(*) FILTER (WHERE status IN ('PENDING', 'APPROVED', 'BORROWED')) AS total_active " +
            "FROM borrowing")
    Map<String, Object> getBorrowingStats();

    /**
     * 获取仪表盘统计数据（单次查询）
     * 使用 CTE 和条件聚合优化
     */
    @Select("WITH archive_stats AS ( " +
            "  SELECT " +
            "    COUNT(*) FILTER (WHERE created_time >= CURRENT_DATE) AS today_count, " +
            "    COUNT(*) AS total_count " +
            "  FROM acc_archive " +
            "  WHERE deleted = 0 " +
            "), task_stats AS ( " +
            "  SELECT COUNT(*) AS pending_count " +
            "  FROM sys_ingest_request_status " +
            "  WHERE status NOT IN ('COMPLETED', 'FAILED') " +
            ") " +
            "SELECT " +
            "  a.total_count AS total_archives, " +
            "  a.today_count AS today_ingest, " +
            "  COALESCE(t.pending_count, 0) AS pending_tasks " +
            "FROM archive_stats a, task_stats t")
    Map<String, Object> getDashboardStats();

    /**
     * 获取存储统计（优化版本）
     */
    @Select("SELECT COALESCE(SUM(file_size), 0) AS used_bytes " +
            "FROM arc_file_content " +
            "WHERE deleted = 0")
    Long getStorageUsedBytes();

    /**
     * 获取归档趋势（优化版本）
     * 使用 date_trunc 替代 to_char 以便利用索引
     */
    @Select("SELECT " +
            "  date_trunc('day', created_time)::date AS date, " +
            "  COUNT(*) AS count " +
            "FROM acc_archive " +
            "WHERE created_time >= #{startDate} " +
            "  AND deleted = 0 " +
            "GROUP BY date_trunc('day', created_time) " +
            "ORDER BY date")
    @Results({
            @Result(property = "date", column = "date"),
            @Result(property = "count", column = "count")
    })
    List<Map<String, Object>> getArchivalTrend(@Param("startDate") LocalDate startDate);

    /**
     * 获取任务状态统计（优化版本）
     */
    @Select("SELECT " +
            "  status, " +
            "  COUNT(*) AS cnt " +
            "FROM sys_ingest_request_status " +
            "GROUP BY status")
    @Results({
            @Result(property = "status", column = "status"),
            @Result(property = "cnt", column = "cnt")
    })
    List<Map<String, Object>> getTaskStatusStats();

    /**
     * 获取按全宗分组的归档统计
     * 用于全宗维度的统计分析
     */
    @Select("SELECT " +
            "  fonds_no, " +
            "  COUNT(*) AS count, " +
            "  SUM(CASE WHEN created_time >= CURRENT_DATE THEN 1 ELSE 0 END) AS today_count " +
            "FROM acc_archive " +
            "WHERE deleted = 0 " +
            "GROUP BY fonds_no " +
            "ORDER BY count DESC")
    List<Map<String, Object>> getArchiveStatsByFonds();

    /**
     * 获取按年度分组的归档统计
     */
    @Select("SELECT " +
            "  fiscal_year, " +
            "  COUNT(*) AS count " +
            "FROM acc_archive " +
            "WHERE deleted = 0 " +
            "  AND fiscal_year IS NOT NULL " +
            "GROUP BY fiscal_year " +
            "ORDER BY fiscal_year DESC")
    List<Map<String, Object>> getArchiveStatsByYear();
}
