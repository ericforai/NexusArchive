// Input: MyBatis-Plus、Apache Ibatis、Java 标准库
// Output: IntegrityCheckMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.IntegrityCheck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 四性检测结果 Mapper
 */
@Mapper
public interface IntegrityCheckMapper extends BaseMapper<IntegrityCheck> {

    /**
     * 按目标查询所有检测结果
     */
    @Select("""
        SELECT * FROM integrity_check
        WHERE target_type = #{targetType}
        AND target_id = #{targetId}
        ORDER BY checked_at DESC
    """)
    List<IntegrityCheck> findByTarget(
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId
    );

    /**
     * 查询最新一次检测结果
     */
    @Select("""
        SELECT * FROM integrity_check
        WHERE target_type = #{targetType}
        AND target_id = #{targetId}
        ORDER BY checked_at DESC
        LIMIT 1
    """)
    IntegrityCheck findLatestByTarget(
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId
    );

    /**
     * 按类型查询最新检测结果
     */
    @Select("""
        SELECT * FROM integrity_check
        WHERE target_type = #{targetType}
        AND target_id = #{targetId}
        AND check_type = #{checkType}
        ORDER BY checked_at DESC
        LIMIT 1
    """)
    IntegrityCheck findLatestByTargetAndType(
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId,
        @Param("checkType") String checkType
    );

    /**
     * 统计批次的四性检测结果
     */
    @Select("""
        SELECT check_type, result, COUNT(*) as count
        FROM integrity_check
        WHERE target_type = 'BATCH'
        AND target_id = #{batchId}
        GROUP BY check_type, result
    """)
    List<java.util.Map<String, Object>> summarizeByBatch(@Param("batchId") Long batchId);

    /**
     * 查询失败的检测记录
     */
    @Select("""
        SELECT * FROM integrity_check
        WHERE target_type = #{targetType}
        AND target_id = #{targetId}
        AND result = 'FAIL'
        ORDER BY checked_at DESC
    """)
    List<IntegrityCheck> findFailedChecks(
        @Param("targetType") String targetType,
        @Param("targetId") Long targetId
    );

    /**
     * 检查批次是否全部通过四性检测
     */
    @Select("""
        SELECT COUNT(*) = 0 FROM integrity_check
        WHERE target_type = 'BATCH'
        AND target_id = #{batchId}
        AND result = 'FAIL'
    """)
    boolean isAllPassed(@Param("batchId") Long batchId);
}
