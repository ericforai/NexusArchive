// Input: MyBatis-Plus、Jackson、Lombok、Spring Framework、等
// Output: DestructionServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.constants.OperationResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.DestructionMapper;
import com.nexusarchive.service.DestructionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DestructionServiceImpl implements DestructionService {

    // ARCHITECTURE-NOTE: 销毁 → Archive 边界依赖
    // 直接依赖 ArchiveMapper 而非 ArchiveService 的原因：
    // 1. 需要读取 Archive.destructionHold 标志判断是否可销毁
    // 2. 需要执行 Archive 的逻辑删除（@TableLogic）
    // 3. 需要更新 Archive.destructionStatus 状态
    // 4. 销毁是特殊的业务操作，需要绕过常规的 CRUD 流程
    // 相关文档：docs/architecture/module-dependency-status.md#一、已确认的跨模块依赖
    private final DestructionMapper destructionMapper;
    private final ArchiveMapper archiveMapper;
    private final ObjectMapper objectMapper;

    // ARCHITECTURE-NOTE: 销毁模块直接查询 Archive 实体
    // 越过 ArchiveService，直接操作 ArchiveMapper 的原因：
    // 1. 需要访问 Archive.destructionHold 字段（业务规则：冻结档案不可销毁）
    // 2. 批量查询效率考虑（selectBatchIds）
    // 3. 销毁是特殊的生命周期操作，不应走常规更新流程
    @Override
    @Transactional
    public Destruction createDestruction(Destruction destruction) {
        // Validate archives for Destruction Hold
        try {
            List<String> archiveIds = objectMapper.readValue(destruction.getArchiveIds(), List.class);
            if (archiveIds != null && !archiveIds.isEmpty()) {
                List<Archive> archives = archiveMapper.selectBatchIds(archiveIds);
                for (Archive archive : archives) {
                    if (Boolean.TRUE.equals(archive.getDestructionHold())) {
                        throw new RuntimeException("Archive " + archive.getArchiveCode() + " is under Destruction Hold (Reason: " + archive.getHoldReason() + ") and cannot be destroyed.");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to validate destruction eligibility: " + e.getMessage(), e);
        }

        destruction.setStatus(OperationResult.PENDING);
        destructionMapper.insert(destruction);
        return destruction;
    }

    @Override
    public Page<Destruction> getDestructions(int page, int limit, String status) {
        Page<Destruction> pageParam = new Page<>(page, limit);
        LambdaQueryWrapper<Destruction> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(Destruction::getStatus, status);
        }
        queryWrapper.orderByDesc(Destruction::getCreatedTime);
        return destructionMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    @Transactional
    public void approveDestruction(String id, String approverId, String comment) {
        Destruction destruction = destructionMapper.selectById(id);
        if (destruction == null) {
            throw new RuntimeException("Destruction record not found");
        }
        destruction.setStatus("APPROVED");
        destruction.setApproverId(approverId);
        destruction.setApprovalComment(comment);
        destruction.setApprovalTime(LocalDateTime.now());
        destructionMapper.updateById(destruction);
    }

    // ARCHITECTURE-NOTE: 销毁执行 - 直接删除 Archive
    // 直接使用 archiveMapper.deleteById() 的原因：
    // 1. MyBatis-Plus @TableLogic 会自动处理逻辑删除
    // 2. 销毁是不可逆操作，需要显式控制
    // 3. 状态变更与物理删除需要原子性保证
    @Override
    @Transactional
    public void executeDestruction(String id) {
        Destruction destruction = destructionMapper.selectById(id);
        if (destruction == null) {
            throw new RuntimeException("Destruction record not found");
        }
        if (!"APPROVED".equals(destruction.getStatus())) {
            throw new RuntimeException("Only approved destruction requests can be executed");
        }

        try {
            // Parse archive IDs
            List<String> archiveIds = objectMapper.readValue(destruction.getArchiveIds(), List.class);

            // Logically delete archives
            for (String archiveId : archiveIds) {
                archiveMapper.deleteById(archiveId); // Logic delete if @TableLogic is set
            }

            destruction.setStatus("EXECUTED");
            destruction.setExecutionTime(LocalDateTime.now());
            destructionMapper.updateById(destruction);

        } catch (Exception e) {
            throw new RuntimeException("Failed to execute destruction: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void rejectDestruction(String id, String approverId, String comment) {
        Destruction destruction = destructionMapper.selectById(id);
        if (destruction == null) {
            throw new RuntimeException("Destruction record not found");
        }
        if (!OperationResult.PENDING.equals(destruction.getStatus())) {
            throw new RuntimeException("Only pending destruction requests can be rejected");
        }

        destruction.setStatus("REJECTED");
        destruction.setApproverId(approverId);
        destruction.setApprovalComment(comment);
        destruction.setApprovalTime(LocalDateTime.now());
        destructionMapper.updateById(destruction);
    }
}
