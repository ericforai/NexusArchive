// Input: ArchiveInventory 实体类
// Output: ArchiveInventoryMapper 接口
// Pos: src/main/java/com/nexusarchive/mapper/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveInventory;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 盘点任务 Mapper 接口
 *
 * 提供盘点任务的数据库操作方法
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Mapper
public interface ArchiveInventoryMapper {

    /**
     * 插入盘点任务
     *
     * @param entity 盘点任务实体
     * @return 影响行数
     */
    int insert(ArchiveInventory entity);

    /**
     * 更新盘点任务
     *
     * @param entity 盘点任务实体
     * @return 影响行数
     */
    int update(ArchiveInventory entity);

    /**
     * 根据 ID 查询盘点任务
     *
     * @param id 盘点任务ID
     * @return 盘点任务实体
     */
    ArchiveInventory selectById(@Param("id") Long id);

    /**
     * 根据任务号查询
     *
     * @param taskNo 任务号
     * @return 盘点任务实体
     */
    ArchiveInventory selectByTaskNo(@Param("taskNo") String taskNo);

    /**
     * 条件查询盘点任务列表
     *
     * @param cabinetId 档案柜ID（可选）
     * @param status 状态（可选）
     * @param fondsId 全宗ID
     * @return 盘点任务列表
     */
    List<ArchiveInventory> selectList(
        @Param("cabinetId") Long cabinetId,
        @Param("status") String status,
        @Param("fondsId") Long fondsId
    );

    /**
     * 更新任务状态
     *
     * @param id 盘点任务ID
     * @param status 新状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 更新盘点进度
     *
     * @param id 盘点任务ID
     * @param totalContainers 总数
     * @param checkedContainers 已盘点数
     * @param abnormalContainers 异常数
     * @return 影响行数
     */
    int updateProgress(
        @Param("id") Long id,
        @Param("totalContainers") Integer totalContainers,
        @Param("checkedContainers") Integer checkedContainers,
        @Param("abnormalContainers") Integer abnormalContainers
    );

    /**
     * 删除盘点任务
     *
     * @param id 盘点任务ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 获取下一个任务号
     *
     * @param fondsId 全宗ID
     * @return 下一个任务号
     */
    String getNextTaskNo(@Param("fondsId") Long fondsId);
}
