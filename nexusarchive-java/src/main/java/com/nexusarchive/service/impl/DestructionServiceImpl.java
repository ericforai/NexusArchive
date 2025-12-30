// Input: MyBatis-Plus、Jackson、Lombok、Spring Framework、等
// Output: DestructionServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    private final DestructionMapper destructionMapper;
    private final ArchiveMapper archiveMapper;
    private final ObjectMapper objectMapper;

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

        destruction.setStatus("PENDING");
        destructionMapper.insert(destruction);
        return destruction;
    }

    @Override
    public Page<Destruction> getDestructions(int page, int limit, String status) {
        Page<Destruction> pageParam = new Page<>(page, limit);
        QueryWrapper<Destruction> queryWrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        queryWrapper.orderByDesc("created_time");
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
}
