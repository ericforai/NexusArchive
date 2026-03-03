// Input: ArchiveInventoryMapper 接口
// Output: ArchiveInventoryService 业务逻辑层
// Pos: src/main/java/com/nexusarchive/service/warehouse
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.warehouse;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nexusarchive.entity.warehouse.ArchiveInventory;
import com.nexusarchive.mapper.warehouse.ArchiveInventoryMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 盘点任务 Service 层
 *
 * 业务逻辑：
 * 1. 盘点任务号生成：PD-{YYYY}-{3位流水号}
 * 2. 任务状态流转管理
 * 3. 盘点进度跟踪
 * 4. 异常统计
 *
 * @author Claude Code
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class ArchiveInventoryService extends ServiceImpl<ArchiveInventoryMapper, ArchiveInventory> {

    private final ArchiveInventoryMapper inventoryMapper;

    /**
     * 分页查询盘点任务列表
     *
     * @param cabinetId 档案柜ID（可选）
     * @param status 状态（可选）
     * @param fondsId 全宗ID
     * @return 盘点任务列表
     */
    public List<ArchiveInventory> list(Long cabinetId, String status, Long fondsId) {
        return inventoryMapper.selectList(cabinetId, status, fondsId);
    }

    /**
     * 创建盘点任务
     *
     * @param entity 盘点任务实体
     * @return 创建的任务
     */
    @Transactional(rollbackFor = Exception.class)
    public ArchiveInventory create(ArchiveInventory entity) {
        // 生成任务号
        String taskNo = generateTaskNo(entity.getFondsId());
        entity.setTaskNo(taskNo);
        entity.setStatus("pending");

        inventoryMapper.insert(entity);
        return entity;
    }

    /**
     * 开始盘点任务
     *
     * @param id 盘点任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        ArchiveInventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) {
            throw new RuntimeException("盘点任务不存在: " + id);
        }
        if (!"pending".equals(inventory.getStatus())) {
            throw new RuntimeException("只有待开始的任务才能开始");
        }

        inventoryMapper.updateStatus(id, "in_progress");

        // 更新开始时间
        ArchiveInventory updateEntity = new ArchiveInventory();
        updateEntity.setId(id);
        updateEntity.setStartTime(LocalDateTime.now());
        inventoryMapper.update(updateEntity);
    }

    /**
     * 完成盘点任务
     *
     * @param id 盘点任务ID
     * @param totalContainers 总数
     * @param checkedContainers 已盘点数
     * @param abnormalContainers 异常数
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id, Integer totalContainers, Integer checkedContainers, Integer abnormalContainers) {
        inventoryMapper.updateStatus(id, "completed");
        inventoryMapper.updateProgress(id, totalContainers, checkedContainers, abnormalContainers);

        // 更新结束时间
        ArchiveInventory updateEntity = new ArchiveInventory();
        updateEntity.setId(id);
        updateEntity.setEndTime(LocalDateTime.now());
        inventoryMapper.update(updateEntity);
    }

    /**
     * 取消盘点任务
     *
     * @param id 盘点任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        ArchiveInventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) {
            throw new RuntimeException("盘点任务不存在: " + id);
        }
        if ("completed".equals(inventory.getStatus())) {
            throw new RuntimeException("已完成的任务不能取消");
        }

        inventoryMapper.updateStatus(id, "cancelled");
    }

    /**
     * 生成盘点任务号
     * 规则：PD-{YYYY}-{3位流水号}
     */
    private String generateTaskNo(Long fondsId) {
        String year = LocalDate.now().toString().substring(0, 4);
        String maxNo = inventoryMapper.getNextTaskNo(fondsId);

        int nextNum = 1;
        if (maxNo != null && maxNo.startsWith("PD-" + year)) {
            try {
                String numStr = maxNo.substring(8); // PD-YYYY- 后的数字
                nextNum = Integer.parseInt(numStr) + 1;
            } catch (NumberFormatException e) {
                nextNum = 1;
            }
        }

        return String.format("PD-%s-%03d", year, nextNum);
    }

    /**
     * 获取下一个任务号
     *
     * @param fondsId 全宗ID
     * @return 下一个任务号
     */
    public String getNextTaskNo(Long fondsId) {
        String year = LocalDate.now().toString().substring(0, 4);
        String maxNo = inventoryMapper.getNextTaskNo(fondsId);

        int nextNum = 1;
        if (maxNo != null && maxNo.startsWith("PD-" + year)) {
            try {
                String numStr = maxNo.substring(8);
                nextNum = Integer.parseInt(numStr) + 1;
            } catch (NumberFormatException e) {
                nextNum = 1;
            }
        }

        return String.format("PD-%s-%03d", year, nextNum);
    }
}
