// Input: MyBatis-Plus、Apache、Java 标准库、本地模块
// Output: SysAuditLogMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

import java.time.LocalDate;
import java.util.List;

/**
 * 审计日志Mapper
 * 
 * 增强功能：
 * - 支持哈希链查询
 * - 支持日志链验证
 * 
 * @author Agent B - 合规开发工程师
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
    
    /**
     * 按时间范围查询日志（用于链验证）
     */
    @Select("SELECT * FROM sys_audit_log WHERE DATE(created_time) >= #{startDate} AND DATE(created_time) <= #{endDate} ORDER BY created_time ASC")
    List<SysAuditLog> findByDateRange(@Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);

    @SelectProvider(type = SysAuditLogSqlProvider.class, method = "buildFindByDateRangeAndFondsNo")
    List<SysAuditLog> findByDateRangeAndFondsNo(@Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate,
                                                @Param("fondsNo") String fondsNo);

    @SelectProvider(type = SysAuditLogSqlProvider.class, method = "buildFindByIdsInOrder")
    List<SysAuditLog> findByIdsInOrder(@Param("logIds") List<String> logIds);
    
    /**
     * 按用户ID查询日志
     */
    @Select("SELECT * FROM sys_audit_log WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<SysAuditLog> findByUserId(@Param("userId") String userId);
    
    /**
     * 获取最新日志的哈希值（用于哈希链）
     */
    @Select("SELECT log_hash FROM sys_audit_log ORDER BY created_time DESC LIMIT 1")
    String getLatestLogHash();

    class SysAuditLogSqlProvider {
        public String buildFindByDateRangeAndFondsNo() {
            return """
                    <script>
                    SELECT *
                    FROM sys_audit_log
                    WHERE DATE(created_time) &gt;= #{startDate}
                      AND DATE(created_time) &lt;= #{endDate}
                    <if test="fondsNo != null and fondsNo != ''">
                      AND (source_fonds = #{fondsNo} OR target_fonds = #{fondsNo})
                    </if>
                    ORDER BY created_time ASC, id ASC
                    </script>
                    """;
        }

        public String buildFindByIdsInOrder() {
            return """
                    <script>
                    SELECT *
                    FROM sys_audit_log
                    WHERE id IN
                    <foreach collection="logIds" item="logId" open="(" separator="," close=")">
                      #{logId}
                    </foreach>
                    ORDER BY created_time ASC, id ASC
                    </script>
                    """;
        }
    }
}
