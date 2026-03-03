// Input: ArchiveContainerVolume 实体类
// Output: ArchiveContainerVolumeMapper 接口
// Pos: src/main/java/com/nexusarchive/mapper/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.mapper.warehouse;

import com.nexusarchive.entity.warehouse.ArchiveContainerVolume;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 档案袋-案卷关联 Mapper 接口
 *
 * 提供档案袋与案卷关联的数据库操作方法
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Mapper
public interface ArchiveContainerVolumeMapper {

    /**
     * 插入关联记录
     *
     * @param entity 关联实体
     * @return 影响行数
     */
    int insert(ArchiveContainerVolume entity);

    /**
     * 批量插入关联记录
     *
     * @param entities 关联实体列表
     * @return 影响行数
     */
    int insertBatch(@Param("entities") List<ArchiveContainerVolume> entities);

    /**
     * 根据 ID 查询关联记录
     *
     * @param id 关联ID
     * @return 关联实体
     */
    ArchiveContainerVolume selectById(@Param("id") Long id);

    /**
     * 根据档案袋ID查询关联列表
     *
     * @param containerId 档案袋ID
     * @return 关联列表
     */
    List<ArchiveContainerVolume> selectByContainerId(@Param("containerId") Long containerId);

    /**
     * 根据案卷ID查询关联记录
     *
     * @param volumeId 案卷ID
     * @return 关联记录
     */
    ArchiveContainerVolume selectByVolumeId(@Param("volumeId") Long volumeId);

    /**
     * 删除指定档案袋的所有关联
     *
     * @param containerId 档案袋ID
     * @return 影响行数
     */
    int deleteByContainerId(@Param("containerId") Long containerId);

    /**
     * 删除指定档案袋的指定案卷关联
     *
     * @param containerId 档案袋ID
     * @param volumeIds 案卷ID列表
     * @return 影响行数
     */
    int deleteByContainerIdAndVolumeIds(
        @Param("containerId") Long containerId,
        @Param("volumeIds") List<Long> volumeIds
    );

    /**
     * 删除关联记录
     *
     * @param id 关联ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 更新显示顺序
     *
     * @param id 关联ID
     * @param displayOrder 显示顺序
     * @return 影响行数
     */
    int updateDisplayOrder(@Param("id") Long id, @Param("displayOrder") Integer displayOrder);

    /**
     * 统计档案袋的关联数量
     *
     * @param containerId 档案袋ID
     * @return 关联数量
     */
    int countByContainerId(@Param("containerId") Long containerId);
}
