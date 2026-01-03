// Input: ArchiveFreezeService, ArchiveMapper, AuditLogService
// Output: ArchiveFreezeServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.ArchiveFreezeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 档案冻结/保全服务实现
 * 
 * 实现要点：
 * 1. 设置 destructionHold = true 和 destructionStatus = FROZEN/HOLD
 * 2. 记录 holdReason 和操作人信息
 * 3. 记录审计日志
 * 4. 解除冻结时恢复状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveFreezeServiceImpl implements ArchiveFreezeService {
    
    private final ArchiveMapper archiveMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeArchive(String archiveId, String reason, String operatorId, LocalDate expireDate) {
        // 1. 查询档案
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new IllegalArgumentException("档案不存在: " + archiveId);
        }
        
        // 2. 检查是否已被冻结
        if (Boolean.TRUE.equals(archive.getDestructionHold()) || 
            "FROZEN".equals(archive.getDestructionStatus()) || 
            "HOLD".equals(archive.getDestructionStatus())) {
            log.warn("档案已被冻结，跳过: archiveId={}, currentStatus={}", 
                    archiveId, archive.getDestructionStatus());
            return;
        }
        
        // 3. 设置冻结状态
        archive.setDestructionHold(true);
        archive.setDestructionStatus("FROZEN"); // 默认使用 FROZEN，可根据 reason 判断使用 HOLD
        archive.setHoldReason(reason);
        
        // 4. 更新数据库
        archiveMapper.updateById(archive);
        
        // 5. 记录审计日志
        log.info("档案冻结成功: archiveId={}, archiveCode={}, reason={}, operatorId={}, expireDate={}", 
                archiveId, archive.getArchiveCode(), reason, operatorId, expireDate);
        
        // TODO: 记录到审计日志表（AuditLogService）
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreezeArchive(String archiveId, String reason, String operatorId) {
        // 1. 查询档案
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            throw new IllegalArgumentException("档案不存在: " + archiveId);
        }
        
        // 2. 检查是否被冻结
        if (!Boolean.TRUE.equals(archive.getDestructionHold()) && 
            !"FROZEN".equals(archive.getDestructionStatus()) && 
            !"HOLD".equals(archive.getDestructionStatus())) {
            log.warn("档案未被冻结，跳过解除: archiveId={}", archiveId);
            return;
        }
        
        // 3. 解除冻结状态
        archive.setDestructionHold(false);
        // 如果原状态是 FROZEN/HOLD，恢复为 NORMAL 或 EXPIRED（根据实际情况）
        if ("FROZEN".equals(archive.getDestructionStatus()) || 
            "HOLD".equals(archive.getDestructionStatus())) {
            // 检查是否到期，如果到期则设置为 EXPIRED，否则设置为 NORMAL
            // 这里简化处理，统一设置为 NORMAL，实际应该根据 retentionStartDate 判断
            archive.setDestructionStatus("NORMAL");
        }
        archive.setHoldReason(null);
        
        // 4. 更新数据库
        archiveMapper.updateById(archive);
        
        // 5. 记录审计日志
        log.info("档案解除冻结成功: archiveId={}, archiveCode={}, reason={}, operatorId={}", 
                archiveId, archive.getArchiveCode(), reason, operatorId);
        
        // TODO: 记录到审计日志表（AuditLogService）
    }
    
    @Override
    public boolean isFrozen(String archiveId) {
        Archive archive = archiveMapper.selectById(archiveId);
        if (archive == null) {
            return false;
        }
        
        return Boolean.TRUE.equals(archive.getDestructionHold()) || 
               "FROZEN".equals(archive.getDestructionStatus()) || 
               "HOLD".equals(archive.getDestructionStatus());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freezeArchives(List<String> archiveIds, String reason, String operatorId, LocalDate expireDate) {
        if (archiveIds == null || archiveIds.isEmpty()) {
            throw new IllegalArgumentException("档案ID列表不能为空");
        }
        
        int successCount = 0;
        List<String> failedIds = new ArrayList<>();
        List<String> failedReasons = new ArrayList<>();
        
        for (String archiveId : archiveIds) {
            try {
                freezeArchive(archiveId, reason, operatorId, expireDate);
                successCount++;
            } catch (Exception e) {
                log.warn("冻结档案失败: archiveId={}, error={}", archiveId, e.getMessage());
                failedIds.add(archiveId);
                failedReasons.add(e.getMessage());
            }
        }
        
        log.info("批量冻结完成: 总数={}, 成功={}, 失败={}", archiveIds.size(), successCount, failedIds.size());
        
        // [P1-FIX] 如有失败项，抛出部分成功异常，让调用方知晓
        if (!failedIds.isEmpty()) {
            throw new PartialSuccessException(
                    String.format("批量冻结部分失败: 成功 %d 个, 失败 %d 个, 失败ID: %s", 
                            successCount, failedIds.size(), String.join(",", failedIds)),
                    successCount, failedIds);
        }
    }
    
    /**
     * 部分成功异常
     * [P1-FIX] 用于批量操作部分成功的场景
     */
    public static class PartialSuccessException extends RuntimeException {
        private final int successCount;
        private final List<String> failedIds;
        
        public PartialSuccessException(String message, int successCount, List<String> failedIds) {
            super(message);
            this.successCount = successCount;
            this.failedIds = failedIds;
        }
        
        public int getSuccessCount() { return successCount; }
        public List<String> getFailedIds() { return failedIds; }
    }
}


