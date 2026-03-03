// Input: ArchiveContainerVolumeMapper 接口
// Output: ArchiveContainerVolumeService 业务逻辑层
// Pos: src/main/java/com/nexusarchive/service/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.warehouse;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusarchive.entity.warehouse.ArchiveContainerVolume;
import com.nexusarchive.mapper.warehouse.ArchiveContainerVolumeMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 档案袋-案卷关联 Service 层
 *
 * 业务逻辑：
 * 1. 案卷关联管理
 * 2. 装盒时间记录
 * 3. 主卷排序
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ArchiveContainerVolumeService extends ServiceImpl<ArchiveContainerVolumeMapper, ArchiveContainerVolume> {

    private final ArchiveContainerVolumeMapper volumeMapper;

    /**
     * 根据档案袋ID获取关联列表
     *
     * @param containerId 档案袋ID
     * @return 关联列表
     */
    public List<ArchiveContainerVolume> listByContainerId(Long containerId) {
        return volumeMapper.selectByContainerId(containerId);
    }

    /**
     * 根据案卷ID获取关联记录
     *
     * @param volumeId 案卷ID
     * @return 关联记录
     */
    public ArchiveContainerVolume getByVolumeId(Long volumeId) {
        return volumeMapper.selectByVolumeId(volumeId);
    }

    /**
     * 删除档案袋的所有关联
     *
     * @param containerId 档案袋ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByContainerId(Long containerId) {
        volumeMapper.deleteByContainerId(containerId);
    }

    /**
     * 批量保存关联记录
     *
     * @param volumes 关联记录列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveBatch(List<ArchiveContainerVolume> volumes) {
        if (volumes != null && !volumes.isEmpty()) {
            // 设置装盒时间
            LocalDateTime now = LocalDateTime.now();
            volumes.forEach(v -> {
                if (v.getBoxedAt() == null) {
                    v.setBoxedAt(now);
                }
            });
            volumeMapper.insertBatch(volumes);
        }
    }

    /**
     * 统计档案袋的关联数量
     *
     * @param containerId 档案袋ID
     * @return 关联数量
     */
    public int countByContainerId(Long containerId) {
        return volumeMapper.countByContainerId(containerId);
    }
}
