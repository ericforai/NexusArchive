// Input: MyBatis-Plus、Apache Ibatis、Java 标准库
// Output: ArchiveSubmitBatchMapper 接口
// Pos: 数据访问层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.ArchiveSubmitBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 归档提交批次 Mapper
 */
@Mapper
public interface ArchiveSubmitBatchMapper extends BaseMapper<ArchiveSubmitBatch> {

    /**
     * 生成下一个批次编号
     */
    @Select("SELECT 'AB-' || TO_CHAR(NOW(), 'YYYY') || '-' || LPAD(NEXTVAL('archive_batch_seq')::TEXT, 4, '0')")
    String generateBatchNo();

    /**
     * 按全宗和状态查询批次
     */
    @Select("""
        SELECT * FROM archive_batch
        WHERE fonds_id = #{fondsId}
        AND (#{status} IS NULL OR status = #{status})
        ORDER BY created_time DESC
    """)
    List<ArchiveSubmitBatch> findByFondsAndStatus(
        @Param("fondsId") Long fondsId,
        @Param("status") String status
    );

    /**
     * 分页查询批次
     */
    @Select("""
        <script>
        SELECT * FROM archive_batch
        <where>
            <if test="fondsId != null">
                fonds_id = #{fondsId}
            </if>
            <if test="status != null and status != ''">
                AND status = #{status}
            </if>
        </where>
        ORDER BY created_time DESC
        </script>
    """)
    IPage<ArchiveSubmitBatch> findPage(
        Page<ArchiveSubmitBatch> page,
        @Param("fondsId") Long fondsId,
        @Param("status") String status
    );

    /**
     * 检查期间是否有未完成的批次
     * periodStart/periodEnd 格式: "YYYY-MM" (如 "2025-01")
     */
    @Select("""
        SELECT COUNT(*) FROM archive_batch
        WHERE fonds_id = #{fondsId}
        AND period_start <= CAST(#{periodEnd} || '-01' AS DATE)
        AND period_end >= CAST(#{periodStart} || '-01' AS DATE)
        AND status NOT IN ('ARCHIVED', 'REJECTED', 'FAILED')
    """)
    int countPendingBatchesInPeriod(
        @Param("fondsId") Long fondsId,
        @Param("periodStart") String periodStart,
        @Param("periodEnd") String periodEnd
    );

    /**
     * 更新批次状态
     */
    @Update("""
        UPDATE archive_batch
        SET status = #{status}, updated_at = NOW()
        WHERE id = #{batchId}
    """)
    int updateStatus(@Param("batchId") Long batchId, @Param("status") String status);

    /**
     * 统计各状态的批次数量
     */
    @Select("""
        <script>
        SELECT status, COUNT(*) as count
        FROM archive_batch
        <where>
            <if test="fondsId != null">
                fonds_id = #{fondsId}
            </if>
        </where>
        GROUP BY status
        </script>
    """)
    List<java.util.Map<String, Object>> countByStatus(@Param("fondsId") Long fondsId);
}
