// Input: MyBatis-Plus、Apache Ibatis、Java 标准库
// Output: PeriodLockMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.PeriodLock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 期间锁定 Mapper
 */
@Mapper
public interface PeriodLockMapper extends BaseMapper<PeriodLock> {

    /**
     * 检查期间是否被锁定
     */
    @Select("""
        SELECT * FROM period_lock
        WHERE fonds_id = #{fondsId}
        AND period = #{period}
        AND unlock_at IS NULL
        LIMIT 1
    """)
    PeriodLock findActiveLock(
        @Param("fondsId") String fondsId,
        @Param("period") String period
    );

    /**
     * 检查期间是否被指定类型锁定
     */
    @Select("""
        SELECT * FROM period_lock
        WHERE fonds_id = #{fondsId}
        AND period = #{period}
        AND lock_type = #{lockType}
        AND unlock_at IS NULL
    """)
    PeriodLock findActiveLockByType(
        @Param("fondsId") String fondsId,
        @Param("period") String period,
        @Param("lockType") String lockType
    );

    /**
     * 获取全宗的所有已锁定期间
     */
    @Select("""
        SELECT * FROM period_lock
        WHERE fonds_id = #{fondsId}
        AND unlock_at IS NULL
        ORDER BY period DESC
    """)
    List<PeriodLock> findAllActiveLocks(@Param("fondsId") String fondsId);

    /**
     * 获取期间范围内的锁定记录
     */
    @Select("""
        SELECT * FROM period_lock
        WHERE fonds_id = #{fondsId}
        AND period >= #{periodStart}
        AND period <= #{periodEnd}
        AND unlock_at IS NULL
        ORDER BY period
    """)
    List<PeriodLock> findLocksInRange(
        @Param("fondsId") String fondsId,
        @Param("periodStart") String periodStart,
        @Param("periodEnd") String periodEnd
    );

    /**
     * 检查期间是否已归档
     */
    @Select("""
        SELECT COUNT(*) FROM period_lock
        WHERE fonds_id = #{fondsId}
        AND period = #{period}
        AND lock_type = 'ARCHIVED'
        AND unlock_at IS NULL
    """)
    int countArchivedLock(
        @Param("fondsId") String fondsId,
        @Param("period") String period
    );
}
