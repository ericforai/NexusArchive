// Input: ArchiveContainerMapper 接口
// Output: ArchiveContainerService 业务逻辑层
// Pos: src/main/java/com/nexusarchive/service/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.warehouse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import lombok.RequiredArgsConstructor;

import com.nexusarchive.entity.warehouse.ArchiveContainer;
import com.nexusarchive.entity.warehouse.ArchiveCabinet;
import com.nexusarchive.entity.warehouse.ArchiveContainerVolume;
import com.nexusarchive.mapper.warehouse.ArchiveContainerMapper;
import com.nexusarchive.dto.warehouse.ContainerDTO;
import com.nexusarchive.dto.warehouse.ContainerDetailVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * � 档案袋 Service 层
 *
 * 业务逻辑：
 * 1. 袋号生成规则：CN-{YYYY}-{4位流水号}
 * 2. 状态流转管理
 * 3. 案卷关联管理
 * 4. RFID/二维码定位
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ArchiveContainerService extends ServiceImpl<ArchiveContainerMapper, ArchiveContainer> {

    private final ArchiveContainerMapper containerMapper;
    private final ArchiveCabinetService cabinetService;
    private final ArchiveContainerVolumeService volumeService;

    // =====================================================
    // 1. 基础 CRUD 操作
    // =====================================================

    /**
     * 分页查询档案袋列表
     *
     * @param dto 查询条件
     * @return 分页结果
     */
    public List<ArchiveContainer> page(ContainerDTO dto) {
        LambdaQueryWrapper<ArchiveContainer> queryWrapper = new LambdaQueryWrapper<>();

        // 档案柜过滤
        if (dto.getCabinetId() != null) {
            queryWrapper.eq(ArchiveContainer::getCabinetId, dto.getCabinetId());
        }

        // 状态过滤
        if (dto.getStatus() != null && !dto.getStatus().isEmpty()) {
            queryWrapper.eq(ArchiveContainer::getStatus, dto.getStatus());
        }

        // 关键字搜索（袋号）
        if (dto.getKeyword() != null && !dto.getKeyword().trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                .like(ArchiveContainer::getContainerNo, dto.getKeyword())
                .or()
                .like(ArchiveContainer::getPhysicalLocation, dto.getKeyword())
            );
        }

        // 排序：按创建时间倒序
        queryWrapper.orderByDesc(ArchiveContainer::getCreatedAt);

        return containerMapper.selectList(queryWrapper);
    }

    /**
     * 根据ID获取档案袋详情
     *
     * @param id 档案袋ID
     * @return 详情信息
     */
    public ContainerDetailVO getDetail(Long id) {
        ArchiveContainer entity = containerMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("档案袋不存在: " + id);
        }

        ContainerDetailVO vo = convertToDetailVO(entity);

        // 获取关联的案卷列表
        List<ArchiveContainerVolume> volumes = volumeService.listByContainerId(id);
        // TODO: 转换为 VO 列表
        // vo.setVolumes(volumes);

        // 获取柜信息
        ArchiveCabinet cabinet = cabinetService.getById(entity.getCabinetId());
        vo.setCabinet(ArchiveCabinet.toVO(cabinet));

        // 计算使用率
        if (cabinet.getTotalCapacity() != null && cabinet.getTotalCapacity() > 0) {
            int usageRate = (entity.getCurrentCount() * 100) / cabinet.getTotalCapacity();
            vo.setUsageRate(usageRate);
        }

        return vo;
    }

    /**
     * 创建档案袋
     *
     * @param dto 创建DTO
     * @return 创建的档案袋
     */
    @Transactional(rollbackFor = ClassNotFoundException.class)
    public ArchiveContainer create(ContainerDTO dto) {
        // 1. 参数验证
        validateForCreate(dto);

        // 2. 生成袋号
        String containerNo = generateContainerNo(dto.getFondsId());
        dto.setContainerNo(containerNo);

        // 3. 创建实体
        ArchiveContainer entity = ArchiveContainer.builder()
            .fondsId(dto.getFondsId())
            .cabinetId(dto.getCabinetId())
            .cabinetPosition(dto.getCabinetPosition())
            .containerNo(containerNo)
            .status("empty")
            .checkStatus("pending")
            .capacity(dto.getCapacity())
            .build();

        // 4. 保存到数据库
        containerMapper.insert(entity);

        // 5. 更新档案柜当前数量
        cabinetService.incrementCurrentCount(entity.getCabinetId());

        return entity;
    }

    /**
     * 更新档案袋
     *
     * @param id 档案袋ID
     * @param dto 更新DTO
     * @return 更新后的档案袋
     */
    @Transactional(rollbackFor = ClassNotFoundException.class)
    public ArchiveContainer update(Long id, ContainerDTO dto) {
        ArchiveContainer entity = containerMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("档案袋不存在: " + id);
        }

        // 1. 状态校验
        validateForUpdate(dto, entity);

        // 2. 更新实体
        updateEntityFromDTO(entity, dto);

        // 3. 保存到数据库
        containerMapper.updateById(entity);

        return entity;
    }

    /**
     * 删除档案袋
     *
     * @param id 档案袋ID
     */
    @Transactional(rollbackFor = ClassNotFoundException.class)
    public void delete(Long id) {
        ArchiveContainer entity = containerMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("档案袋不存在: " + id);
        }

        // 检查是否可以删除
        validateForDelete(entity);

        // 逻辑删除（更新deleted标记）
        entity.setDeleted(true);
        containerMapper.updateById(entity);
    }

    // =====================================================
    // 2. 案卷关联管理
    // =====================================================

    /**
     * 关联案卷到档案袋
     *
     * @param containerId 档案袋ID
     * @param volumeIds 案卷ID列表
     */
    @Transactional(rollbackFor = ClassNotFoundException.class)
    public void linkVolumes(Long containerId, List<Long> volumeIds, boolean isPrimary) {
        // 1. 参数验证
        if (containerId == null || volumeIds == null || volumeIds.isEmpty()) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 2. 检查档案袋状态
        ArchiveContainer container = containerMapper.selectById(containerId);
        if (container == null) {
            throw new RuntimeException("档案袋不存在: " + containerId);
        }
        if (!"empty".equals(container.getStatus())) {
            throw new IllegalArgumentException("只有空袋可以关联案卷");
        }

        // 3. 删除旧的关联关系
        volumeService.deleteByContainerId(containerId);

        // 4. 创建新的关联关系
        List<ArchiveContainerVolume> volumeEntities = volumeIds.stream()
            .map(volumeId -> ArchiveContainerVolume.builder()
                    .containerId(containerId)
                    .volumeId(volumeId)
                    .isPrimary(isPrimary)
                    .displayOrder(0) // 主卷排第一
                    .build())
            .collect(Collectors.toList());

        volumeService.saveBatch(volumeEntities);

        // 5. 更新档案袋状态
        containerMapper.updateStatus(containerId, "linked");
        container.setArchiveCount(volumeIds.size());

        // 6. 更新档案柜当前数量
        cabinetService.incrementCurrentCountByCount(containerId, volumeIds.size());
    }

    /**
     * 解除案卷关联
     *
     * @param containerId 档案袋ID
     * @param volumeIds 要删除的案卷ID列表
     */
    @Transactional(rollbackFor = ClassNotFoundException.class)
    public void unlinkVolumes(Long containerId, List<Long> volumeIds) {
        // 1. 参数验证
        if (containerId == null || volumeIds == null || volumeIds.isEmpty()) {
            throw new IllegalArgumentException("参数不能为空");
        }

        // 2. 删除关联关系
        volumeService.deleteByContainerId(containerId, volumeIds);

        // 3. 更新档案袋状态和数量
        ArchiveContainer container = containerMapper.selectById(containerId);
        if (container == null) {
            throw new RuntimeException("档案袋不存在: " + containerId);
        }

        // 重新计算档案数量
        int newCount = (int) (container.getArchiveCount() - volumeIds.size());
        container.setArchiveCount(newCount);
        containerMapper.updateById(container);

        // 4. 如果所有关联已解，恢复为空袋状态
        if (newCount == 0) {
            containerMapper.updateStatus(containerId, "empty");
        }

        // 5. 更新档案柜当前数量（减少）
        cabinetService.incrementCurrentCountByCount(containerId, -volumeIds.size());
    }

    // =====================================================
    // 3. 辅助方法
    // =====================================================

    /**
     * 生成袋号
     * 规则：CN-{YYYY}-{4位流水号}
     */
    private String generateContainerNo(String fondsId) {
        String year = LocalDate.now().format("yyyy");
        String prefix = fondsId; // 可加入全宗代码前缀

        // 查询当前最大袋号
        String maxNo = containerMapper.selectMaxNo(fondsId);

        // 提取序号部分
        int nextNum = 1;
        if (maxNo != null && maxNo.startsWith(prefix)) {
            try {
                String numStr = maxNo.substring(prefix.length() + 4); // CN- 后4位
                nextNum = Integer.parseInt(numStr) + 1;
            } catch (NumberFormatException e) {
                // 解析失败，使用默认值
                nextNum = 1;
            }
        }

        return String.format("%s-%s-%03d", prefix, year, nextNum);
    }

    /**
     * 验证创建参数
     */
    private void validateForCreate(ContainerDTO dto) {
        // 柜柜必填
        if (dto.getCabinetId() == null) {
            throw new IllegalArgumentException("档案柜不能为空");
        }

        // 容量必须为正数
        if (dto.getCapacity() == null || dto.getCapacity() <= 0) {
            throw new IllegalArgumentException("容量必须大于0");
        }
    }

    /**
     * 验证更新参数
     */
    private void validateForUpdate(ContainerDTO dto, ArchiveContainer existing) {
        // 状态校验
        String oldStatus = existing.getStatus();
        String newStatus = dto.getStatus();

        // 不能从借出状态直接改为满袋状态
        if ("borrowed".equals(oldStatus) && "full".equals(newStatus)) {
            throw new IllegalArgumentException("借出的档案袋必须先归还");
        }

        // 满袋不能再添加档案
        if ("full".equals(newStatus)) {
            int currentCount = existing.getArchiveCount();
            Integer capacity = existing.getCapacity();
            if (currentCount >= capacity) {
                throw new IllegalArgumentException("已满的档案袋不能再添加档案");
            }
        }
    }

    /**
     * 验证删除条件
     */
    private void validateForDelete(ArchiveContainer entity) {
        if ("borrowed".equals(entity.getStatus())) {
            throw new IllegalArgumentException("借出的档案袋不能删除");
        }

        if ("boxed".equals(entity.getCheckStatus())) {
            throw new IllegalArgumentException("已装盒的档案袋不能删除");
        }
    }

    /**
     * 转换为详情 VO
     */
    private ContainerDetailVO convertToDetailVO(ArchiveContainer entity) {
        ContainerDetailVO vo = new ContainerDetailVO();
        vo.setId(entity.getId());
        vo.setContainerNo(entity.getContainerNo());
        vo.setCabinetId(entity.getCabinetId());
        vo.setCabinetPosition(entity.getCabinetPosition());
        vo.setPhysicalLocation(entity.getPhysicalLocation());
        vo.setVolumeId(entity.getVolumeId());
        vo.setCapacity(entity.getCapacity());
        vo.setArchiveCount(entity.getArchiveCount());
        vo.setStatus(entity.getStatus());
        vo.setCheckStatus(entity.getCheckStatus());

        // 柜信息
        ArchiveCabinet cabinet = cabinetService.getById(entity.getCabinetId());
        CabinetDetailVO cabinetVO = CabinetDetailVO.fromEntity(cabinet);
        vo.setCabinet(cabinetVO);

        // 容量统计
        if (cabinet.getTotalCapacity() != null) {
            int usageRate = (entity.getCurrentCount() * 100) / cabinet.getTotalCapacity();
            vo.setUsageRate(usageRate);
        }

        return vo;
    }

    /**
     * 获取档案袋的案卷列表
     *
     * @param containerId 档案袋ID
     * @return 案卷列表
     */
    public List<ArchiveContainerVolume> getVolumesByContainerId(Long containerId) {
        return volumeService.listByContainerId(containerId);
    }

    /**
     * 更新档案袋状态
     *
     * @param id 档案袋ID
     * @param status 新状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        ArchiveContainer container = containerMapper.selectById(id);
        if (container == null) {
            throw new RuntimeException("档案袋不存在: " + id);
        }
        containerMapper.updateStatus(id, status);
    }

    /**
     * 从 DTO 更新实体
     */
    private void updateEntityFromDTO(ArchiveContainer entity, ContainerDTO dto) {
        if (dto.getCabinetPosition() != null) {
            entity.setCabinetPosition(dto.getCabinetPosition());
        }
        if (dto.getPhysicalLocation() != null) {
            entity.setPhysicalLocation(dto.getPhysicalLocation());
        }
        if (dto.getCapacity() != null) {
            entity.setCapacity(dto.getCapacity());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        if (dto.getRemark() != null) {
            entity.setRemark(dto.getRemark());
        }
    }
}
