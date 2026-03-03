// Input: ArchiveInventoryDetail 实体类
// Output: ArchiveInventoryDetailMapper 接口
// Pos: src/main/java/com/nexusarchive/mapper/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveInventoryDetail;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 盘点明细 Mapper 接口
 *
 * 提供盘点明细的数据库操作方法
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Mapper
public interface ArchiveInventoryDetailMapper {

    /**
     * 插入盘点明细
     *
     * @param entity 盘点明细实体
     * @return 影响行数
     */
    int insert(ArchiveInventoryDetail entity);

    /**
     * 批量插入盘点明细
     *
     * @param entities 盘点明细列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<ArchiveInventoryDetail> entities);

    /**
     * 根据 ID 查询盘点明细
     *
     * @param id 盘点明细ID
     * @return 盘点明细实体
     */
    ArchiveInventoryDetail selectById(@Param("id") Long id);

    /**
     * 根据盘点任务ID查询明细列表
     *
     * @param inventoryId 盘点任务ID
     * @return 明细列表
     */
    List<ArchiveInventoryDetail> selectByInventoryId(@Param("inventoryId") Long inventoryId);

    /**
     * 根据档案袋ID查询盘点明细
     *
     * @param containerId 档案袋ID
     * @return 明细列表
     */
    List<ArchiveInventoryDetail> selectByContainerId(@Param("containerId") Long containerId);

    /**
     * 删除盘点任务的所有明细
     *
     * @param inventoryId 盘点任务ID
     * @return 影响行数
     */
    int deleteByInventoryId(@Param("inventoryId") Long inventoryId);

    /**
     * 删除盘点明细
     *
     * @param id 盘点明细ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计盘点任务的明细数量
     *
     * @param inventoryId 盘点任务ID
     * @return 明细数量
     */
    int countByInventoryId(@Param("inventoryId") Long inventoryId);

    /**
     * 统计异常数量
     *
     * @param inventoryId 盘点任务ID
     * @return 异常数
     */
    int countAbnormalByInventoryId(@Param("inventoryId") Long inventoryId);
}
