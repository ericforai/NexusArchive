// Input: ArchiveBorrowing 实体类
// Output: ArchiveBorrowingMapper 接口
// Pos: src/main/java/com/nexusarchive/mapper/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper.warehouse;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusarchive.entity.warehouse.ArchiveBorrowing;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 实物借阅 Mapper 接口
 *
 * 提供实物借阅的数据库操作方法
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Mapper
public interface ArchiveBorrowingMapper extends BaseMapper<ArchiveBorrowing> {

    /**
     * 根据借阅单号查询
     *
     * @param borrowNo 借阅单号
     * @return 借阅实体
     */
    ArchiveBorrowing selectByBorrowNo(@Param("borrowNo") String borrowNo);

    /**
     * 条件查询借阅记录列表
     *
     * @param containerId 档案袋ID（可选）
     * @param status 状态（可选）
     * @param borrower 借阅人（可选）
     * @param fondsId 全宗ID
     * @return 借阅记录列表
     */
    List<ArchiveBorrowing> selectList(
        @Param("containerId") Long containerId,
        @Param("status") String status,
        @Param("borrower") String borrower,
        @Param("fondsId") Long fondsId
    );

    /**
     * 更新借阅状态
     *
     * @param id 借阅ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 确认归还
     *
     * @param id 借阅ID
     * @param returnDate 归还日期
     * @return 影响行数
     */
    int confirmReturn(@Param("id") Long id, @Param("returnDate") LocalDate returnDate);

    /**
     * 获取下一个借阅单号
     *
     * @param fondsId 全宗ID
     * @return 下一个借阅单号
     */
    String getNextBorrowNo(@Param("fondsId") Long fondsId);

    /**
     * 统计逾期借阅数量
     *
     * @param fondsId 全宗ID
     * @return 逾期数量
     */
    int countOverdue(@Param("fondsId") Long fondsId);

    /**
     * 查询逾期借阅列表
     *
     * @param fondsId 全宗ID
     * @return 逾期借阅列表
     */
    List<ArchiveBorrowing> selectOverdueList(@Param("fondsId") Long fondsId);
}
