// Input: DestructionApprovalService, DestructionMapper, ArchiveMapper, ObjectMapper
// Output: DestructionApprovalServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.constants.OperationResult;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.ApprovalChain;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Destruction;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.DestructionMapper;
import com.nexusarchive.service.DestructionApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 销毁审批服务实现
 * 
 * 实现要点：
 * 1. 双人审批：初审 + 复核
 * 2. 审批链记录在 approval_snapshot (JSON) 字段
 * 3. 状态流转：APPRAISING -> FIRST_APPROVED -> DESTRUCTION_APPROVED
 * 4. 审批拒绝可回退到 APPRAISING 或 EXPIRED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DestructionApprovalServiceImpl implements DestructionApprovalService {

    // ARCHITECTURE-NOTE: 销毁审批 → Archive 状态变更边界
    // 直接依赖 ArchiveMapper 更新 Archive.destructionStatus 的原因：
    // 1. 需要精确控制状态机（PENDING → APPRAISING → FIRST_APPROVED → DESTRUCTION_APPROVED）
    // 2. 双人复核流程需要对 Archive 状态进行原子性更新
    // 3. 状态回退逻辑需要查询 Archive 当前状态
    // 相关文档：docs/architecture/module-dependency-status.md#一、已确认的跨模块依赖
    private final DestructionMapper destructionMapper;
    private final ArchiveMapper archiveMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void firstApproval(String destructionId, String approverId, String approverName, 
                              String comment, boolean approved) {
        Destruction destruction = destructionMapper.selectById(destructionId);
        if (destruction == null) {
            throw new IllegalArgumentException("销毁申请不存在: " + destructionId);
        }
        
        // 验证状态：必须是 PENDING 或 APPRAISING 状态才能进行初审
        if (!OperationResult.PENDING.equals(destruction.getStatus()) &&
            !"APPRAISING".equals(destruction.getStatus())) {
            throw new IllegalStateException(
                String.format("销毁申请状态为 %s，无法进行初审", destruction.getStatus()));
        }
        
        // 创建或更新审批链
        ApprovalChain approvalChain = getOrCreateApprovalChain(destruction);
        
        // 设置初审信息
        ApprovalChain.ApprovalInfo firstApproval = new ApprovalChain.ApprovalInfo();
        firstApproval.setApproverId(approverId);
        firstApproval.setApproverName(approverName);
        firstApproval.setComment(comment);
        firstApproval.setApproved(approved);
        firstApproval.setTimestamp(LocalDateTime.now());
        
        approvalChain.setFirstApproval(firstApproval);
        
        // 更新销毁申请
        String newStatus;
        if (approved) {
            newStatus = "FIRST_APPROVED"; // 初审通过，等待复核
        } else {
            // 初审拒绝，回退到 APPRAISING 或 EXPIRED
            // 根据档案状态决定回退到哪个状态
            newStatus = determineRejectedStatus(destruction);
        }
        
        destruction.setStatus(newStatus);
        destruction.setApproverId(approverId);
        destruction.setApproverName(approverName);
        destruction.setApprovalComment(comment);
        destruction.setApprovalTime(LocalDateTime.now());
        
        // 保存审批链快照
        try {
            String approvalSnapshot = objectMapper.writeValueAsString(approvalChain);
            destruction.setApprovalSnapshot(approvalSnapshot);
        } catch (Exception e) {
            log.error("保存审批链快照失败", e);
            throw new RuntimeException("保存审批链快照失败: " + e.getMessage(), e);
        }
        
        destructionMapper.updateById(destruction);
        
        // 如果初审通过，更新档案状态（保持 APPRAISING，等待复核）
        if (approved) {
            updateArchiveStatus(destruction, "APPRAISING");
        } else {
            // 初审拒绝，回退档案状态
            updateArchiveStatus(destruction, newStatus.equals("APPRAISING") ? "APPRAISING" : "EXPIRED");
        }
        
        log.info("初审审批完成，销毁申请ID: {}, 审批人: {}, 结果: {}", 
                destructionId, approverName, approved ? "通过" : "拒绝");
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void secondApproval(String destructionId, String approverId, String approverName, 
                               String comment, boolean approved) {
        Destruction destruction = destructionMapper.selectById(destructionId);
        if (destruction == null) {
            throw new IllegalArgumentException("销毁申请不存在: " + destructionId);
        }
        
        // 验证状态：必须是 FIRST_APPROVED 状态才能进行复核
        if (!"FIRST_APPROVED".equals(destruction.getStatus())) {
            throw new IllegalStateException(
                String.format("销毁申请状态为 %s，无法进行复核。必须先通过初审。", destruction.getStatus()));
        }
        
        // 获取审批链
        ApprovalChain approvalChain = getOrCreateApprovalChain(destruction);
        
        // 验证初审是否通过
        if (approvalChain.getFirstApproval() == null || 
            !Boolean.TRUE.equals(approvalChain.getFirstApproval().getApproved())) {
            throw new IllegalStateException("初审未通过，无法进行复核");
        }
        
        // 设置复核信息
        ApprovalChain.ApprovalInfo secondApproval = new ApprovalChain.ApprovalInfo();
        secondApproval.setApproverId(approverId);
        secondApproval.setApproverName(approverName);
        secondApproval.setComment(comment);
        secondApproval.setApproved(approved);
        secondApproval.setTimestamp(LocalDateTime.now());
        
        approvalChain.setSecondApproval(secondApproval);
        
        // 更新销毁申请
        String newStatus;
        if (approved) {
            newStatus = "DESTRUCTION_APPROVED"; // 复核通过，可以执行销毁
        } else {
            // 复核拒绝，回退到 APPRAISING
            newStatus = "APPRAISING";
        }
        
        destruction.setStatus(newStatus);
        
        // 更新审批信息（复核时更新）
        destruction.setApproverId(approverId);
        destruction.setApproverName(approverName);
        destruction.setApprovalComment(comment);
        destruction.setApprovalTime(LocalDateTime.now());
        
        // 保存审批链快照
        try {
            String approvalSnapshot = objectMapper.writeValueAsString(approvalChain);
            destruction.setApprovalSnapshot(approvalSnapshot);
        } catch (Exception e) {
            log.error("保存审批链快照失败", e);
            throw new RuntimeException("保存审批链快照失败: " + e.getMessage(), e);
        }
        
        destructionMapper.updateById(destruction);
        
        // 更新档案状态
        if (approved) {
            // 复核通过，更新档案状态为 DESTRUCTION_APPROVED
            updateArchiveStatus(destruction, "DESTRUCTION_APPROVED");
        } else {
            // 复核拒绝，回退到 APPRAISING
            updateArchiveStatus(destruction, "APPRAISING");
        }
        
        log.info("复核审批完成，销毁申请ID: {}, 审批人: {}, 结果: {}", 
                destructionId, approverName, approved ? "通过" : "拒绝");
    }
    
    @Override
    public com.nexusarchive.dto.ApprovalChain getApprovalChain(String destructionId) {
        Destruction destruction = destructionMapper.selectById(destructionId);
        if (destruction == null) {
            throw new IllegalArgumentException("销毁申请不存在: " + destructionId);
        }
        
        return getOrCreateApprovalChain(destruction);
    }
    
    /**
     * 获取或创建审批链
     */
    private ApprovalChain getOrCreateApprovalChain(Destruction destruction) {
        if (destruction.getApprovalSnapshot() != null && 
            !destruction.getApprovalSnapshot().isEmpty()) {
            try {
                return objectMapper.readValue(
                    destruction.getApprovalSnapshot(), 
                    ApprovalChain.class
                );
            } catch (Exception e) {
                log.warn("解析审批链快照失败，将创建新的审批链", e);
            }
        }
        
        return new ApprovalChain();
    }
    
    /**
     * 确定拒绝后的状态
     * 根据档案当前状态决定回退到哪个状态
     */
    private String determineRejectedStatus(Destruction destruction) {
        try {
            List<String> archiveIds = objectMapper.readValue(
                destruction.getArchiveIds(), 
                new TypeReference<List<String>>() {}
            );
            
            if (archiveIds != null && !archiveIds.isEmpty()) {
                List<Archive> archives = archiveMapper.selectBatchIds(archiveIds);
                // 如果所有档案都是 APPRAISING 状态，回退到 APPRAISING
                // 否则回退到 EXPIRED
                boolean allAppraising = archives.stream()
                    .allMatch(a -> "APPRAISING".equals(a.getDestructionStatus()));
                
                return allAppraising ? "APPRAISING" : "EXPIRED";
            }
        } catch (Exception e) {
            log.warn("确定拒绝状态失败，默认回退到 EXPIRED", e);
        }
        
        return "EXPIRED";
    }
    
    /**
     * 更新档案状态
     */
    private void updateArchiveStatus(Destruction destruction, String newStatus) {
        try {
            List<String> archiveIds = objectMapper.readValue(
                destruction.getArchiveIds(), 
                new TypeReference<List<String>>() {}
            );
            
            if (archiveIds != null && !archiveIds.isEmpty()) {
                for (String archiveId : archiveIds) {
                    LambdaUpdateWrapper<Archive> updateWrapper = 
                        new LambdaUpdateWrapper<Archive>()
                            .eq(Archive::getId, archiveId)
                            .set(Archive::getDestructionStatus, newStatus);
                    
                    archiveMapper.update(null, updateWrapper);
                }
            }
        } catch (Exception e) {
            log.error("更新档案状态失败", e);
            throw new RuntimeException("更新档案状态失败: " + e.getMessage(), e);
        }
    }
}
