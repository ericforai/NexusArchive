// Input: FondsHistoryService、FondsHistoryMapper、ArchiveMapper、BasFondsService、AuditLogService、ObjectMapper
// Output: FondsHistoryServiceImpl 类（含关键业务链路审计快照）
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.FondsHistoryDetail;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.BasFonds;
import com.nexusarchive.entity.FondsHistory;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.FondsHistoryMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.BasFondsService;
import com.nexusarchive.service.FondsHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全宗沿革服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FondsHistoryServiceImpl implements FondsHistoryService {
    
    private final FondsHistoryMapper fondsHistoryMapper;
    private final ArchiveMapper archiveMapper;
    private final BasFondsService basFondsService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String migrateFonds(String fromFondsNo, String toFondsNo, LocalDate effectiveDate, 
                              String reason, String approvalTicketId, String operatorId) {
        // 1. 验证源全宗和目标全宗
        BasFonds fromFonds = basFondsService.lambdaQuery()
            .eq(BasFonds::getFondsCode, fromFondsNo)
            .one();
        if (fromFonds == null) {
            throw new IllegalArgumentException("源全宗不存在: " + fromFondsNo);
        }
        
        BasFonds toFonds = basFondsService.lambdaQuery()
            .eq(BasFonds::getFondsCode, toFondsNo)
            .one();
        if (toFonds == null) {
            throw new IllegalArgumentException("目标全宗不存在: " + toFondsNo);
        }
        
        // 2. 生成快照信息
        Map<String, Object> snapshot = generateSnapshot(fromFonds);
        
        // 3. 创建沿革记录
        FondsHistory history = new FondsHistory();
        history.setFondsNo(toFondsNo);
        history.setEventType("MIGRATE");
        history.setFromFondsNo(fromFondsNo);
        history.setToFondsNo(toFondsNo);
        history.setEffectiveDate(effectiveDate);
        history.setReason(reason);
        history.setApprovalTicketId(approvalTicketId);
        history.setCreatedBy(operatorId);
        history.setCreatedAt(LocalDateTime.now());
        history.setDeleted(0);
        
        try {
            history.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            log.error("序列化快照信息失败", e);
            throw new RuntimeException("序列化快照信息失败: " + e.getMessage(), e);
        }
        
        fondsHistoryMapper.insert(history);
        
        // 4. 更新所有档案的全宗号（从源全宗迁移到目标全宗）
        LambdaUpdateWrapper<Archive> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Archive::getFondsNo, fromFondsNo)
            .set(Archive::getFondsNo, toFondsNo);
        archiveMapper.update(null, updateWrapper);
        
        log.info("全宗迁移完成: fromFondsNo={}, toFondsNo={}, historyId={}", 
                fromFondsNo, toFondsNo, history.getId());
        
        return history.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> mergeFonds(List<String> sourceFondsNos, String targetFondsNo, 
                                   LocalDate effectiveDate, String reason, 
                                   String approvalTicketId, String operatorId) {
        // 1. 验证目标全宗
        BasFonds targetFonds = basFondsService.lambdaQuery()
            .eq(BasFonds::getFondsCode, targetFondsNo)
            .one();
        if (targetFonds == null) {
            throw new IllegalArgumentException("目标全宗不存在: " + targetFondsNo);
        }
        
        List<String> historyIds = new ArrayList<>();
        
        // 2. 为每个源全宗创建沿革记录并迁移档案
        for (String sourceFondsNo : sourceFondsNos) {
            BasFonds sourceFonds = basFondsService.lambdaQuery()
                .eq(BasFonds::getFondsCode, sourceFondsNo)
                .one();
            if (sourceFonds == null) {
                log.warn("源全宗不存在，跳过: {}", sourceFondsNo);
                continue;
            }
            
            // 生成快照信息
            Map<String, Object> snapshot = generateSnapshot(sourceFonds);
            
            // 创建沿革记录
            FondsHistory history = new FondsHistory();
            history.setFondsNo(targetFondsNo);
            history.setEventType("MERGE");
            history.setFromFondsNo(sourceFondsNo);
            history.setToFondsNo(targetFondsNo);
            history.setEffectiveDate(effectiveDate);
            history.setReason(reason);
            history.setApprovalTicketId(approvalTicketId);
            history.setCreatedBy(operatorId);
            history.setCreatedAt(LocalDateTime.now());
            history.setDeleted(0);
            
            try {
                history.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
            } catch (Exception e) {
                log.error("序列化快照信息失败", e);
                continue;
            }
            
            fondsHistoryMapper.insert(history);
            historyIds.add(history.getId());
            
            // 更新档案的全宗号
            LambdaUpdateWrapper<Archive> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Archive::getFondsNo, sourceFondsNo)
                .set(Archive::getFondsNo, targetFondsNo);
            archiveMapper.update(null, updateWrapper);
        }
        
        log.info("全宗合并完成: sourceFondsNos={}, targetFondsNo={}, historyIds={}", 
                sourceFondsNos, targetFondsNo, historyIds);
        
        return historyIds;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> splitFonds(String sourceFondsNo, List<String> newFondsNos, 
                                  LocalDate effectiveDate, String reason, 
                                  String approvalTicketId, String operatorId) {
        // 1. 验证源全宗
        BasFonds sourceFonds = basFondsService.lambdaQuery()
            .eq(BasFonds::getFondsCode, sourceFondsNo)
            .one();
        if (sourceFonds == null) {
            throw new IllegalArgumentException("源全宗不存在: " + sourceFondsNo);
        }
        
        // 2. 验证新全宗
        for (String newFondsNo : newFondsNos) {
            BasFonds newFonds = basFondsService.lambdaQuery()
                .eq(BasFonds::getFondsCode, newFondsNo)
                .one();
            if (newFonds == null) {
                throw new IllegalArgumentException("新全宗不存在: " + newFondsNo);
            }
        }
        
        // 3. 生成快照信息
        Map<String, Object> snapshot = generateSnapshot(sourceFonds);
        
        List<String> historyIds = new ArrayList<>();
        
        // 4. 为每个新全宗创建沿革记录
        // 注意：实际档案分配逻辑需要根据业务规则实现（例如按年度、类型等分配）
        for (String newFondsNo : newFondsNos) {
            FondsHistory history = new FondsHistory();
            history.setFondsNo(newFondsNo);
            history.setEventType("SPLIT");
            history.setFromFondsNo(sourceFondsNo);
            history.setToFondsNo(newFondsNo);
            history.setEffectiveDate(effectiveDate);
            history.setReason(reason);
            history.setApprovalTicketId(approvalTicketId);
            history.setCreatedBy(operatorId);
            history.setCreatedAt(LocalDateTime.now());
            history.setDeleted(0);
            
            try {
                history.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
            } catch (Exception e) {
                log.error("序列化快照信息失败", e);
                continue;
            }
            
            fondsHistoryMapper.insert(history);
            historyIds.add(history.getId());
        }
        
        // TODO: 实现档案分配逻辑（根据业务规则将源全宗的档案分配到新全宗）
        // 这里需要根据实际业务需求实现，例如：
        // - 按年度分配
        // - 按类型分配
        // - 手动指定分配规则
        
        log.info("全宗分立完成: sourceFondsNo={}, newFondsNos={}, historyIds={}", 
                sourceFondsNo, newFondsNos, historyIds);
        
        return historyIds;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String renameFonds(String oldFondsNo, String newFondsNo, LocalDate effectiveDate, 
                             String reason, String approvalTicketId, String operatorId) {
        // 1. 验证旧全宗
        BasFonds oldFonds = basFondsService.lambdaQuery()
            .eq(BasFonds::getFondsCode, oldFondsNo)
            .one();
        if (oldFonds == null) {
            throw new IllegalArgumentException("旧全宗不存在: " + oldFondsNo);
        }
        
        // 2. 验证新全宗号是否已存在
        BasFonds existingFonds = basFondsService.lambdaQuery()
            .eq(BasFonds::getFondsCode, newFondsNo)
            .one();
        if (existingFonds != null) {
            throw new IllegalArgumentException("新全宗号已存在: " + newFondsNo);
        }
        
        // 3. 生成变更前快照
        Map<String, Object> beforeSnapshot = generateSnapshot(oldFonds);
        
        // 4. 创建沿革记录
        FondsHistory history = new FondsHistory();
        history.setFondsNo(newFondsNo);
        history.setEventType("RENAME");
        history.setFromFondsNo(oldFondsNo);
        history.setToFondsNo(newFondsNo);
        history.setEffectiveDate(effectiveDate);
        history.setReason(reason);
        history.setApprovalTicketId(approvalTicketId);
        history.setCreatedBy(operatorId);
        history.setCreatedAt(LocalDateTime.now());
        history.setDeleted(0);
        
        try {
            history.setSnapshotJson(objectMapper.writeValueAsString(beforeSnapshot));
        } catch (Exception e) {
            log.error("序列化快照信息失败", e);
            throw new RuntimeException("序列化快照信息失败: " + e.getMessage(), e);
        }
        
        fondsHistoryMapper.insert(history);
        
        // 5. 更新全宗表
        oldFonds.setFondsCode(newFondsNo);
        basFondsService.updateById(oldFonds);
        
        // 6. 更新所有档案的全宗号
        LambdaUpdateWrapper<Archive> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Archive::getFondsNo, oldFondsNo)
            .set(Archive::getFondsNo, newFondsNo);
        archiveMapper.update(null, updateWrapper);

        Map<String, Object> afterSnapshot = buildRenameAfterSnapshot(
                oldFonds,
                oldFondsNo,
                newFondsNo,
                effectiveDate,
                reason,
                approvalTicketId,
                history.getId()
        );
        recordRenameAudit(operatorId, oldFondsNo, newFondsNo, approvalTicketId, beforeSnapshot, afterSnapshot);
        
        log.info("全宗重命名完成: oldFondsNo={}, newFondsNo={}, historyId={}", 
                oldFondsNo, newFondsNo, history.getId());
        
        return history.getId();
    }
    
    @Override
    public List<FondsHistoryDetail> getFondsHistory(String fondsNo) {
        List<FondsHistory> histories = fondsHistoryMapper.findByFondsNo(fondsNo);
        
        return histories.stream().map(history -> {
            FondsHistoryDetail detail = new FondsHistoryDetail();
            detail.setId(history.getId());
            detail.setFondsNo(history.getFondsNo());
            detail.setEventType(history.getEventType());
            detail.setFromFondsNo(history.getFromFondsNo());
            detail.setToFondsNo(history.getToFondsNo());
            detail.setEffectiveDate(history.getEffectiveDate());
            detail.setReason(history.getReason());
            detail.setApprovalTicketId(history.getApprovalTicketId());
            detail.setCreatedBy(history.getCreatedBy());
            detail.setCreatedAt(history.getCreatedAt());
            
            // 解析快照信息
            if (history.getSnapshotJson() != null && !history.getSnapshotJson().isEmpty()) {
                try {
                    Map<String, Object> snapshot = objectMapper.readValue(
                        history.getSnapshotJson(), 
                        new TypeReference<Map<String, Object>>() {});
                    detail.setSnapshot(snapshot);
                } catch (Exception e) {
                    log.warn("反序列化快照信息失败: historyId={}", history.getId(), e);
                }
            }
            
            return detail;
        }).toList();
    }
    
    /**
     * 生成全宗快照信息
     */
    private Map<String, Object> generateSnapshot(BasFonds fonds) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("fondsNo", fonds.getFondsCode());
        snapshot.put("fondsName", fonds.getFondsName());
        snapshot.put("companyName", fonds.getCompanyName());
        snapshot.put("orgId", fonds.getOrgId());
        snapshot.put("description", fonds.getDescription());
        
        // 统计档案数量
        long archiveCount = archiveMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Archive>()
                .eq(Archive::getFondsNo, fonds.getFondsCode()));
        snapshot.put("archiveCount", archiveCount);
        
        snapshot.put("snapshotTime", LocalDateTime.now());
        
        return snapshot;
    }

    private Map<String, Object> buildRenameAfterSnapshot(BasFonds updatedFonds, String oldFondsNo, String newFondsNo,
                                                         LocalDate effectiveDate, String reason,
                                                         String approvalTicketId, String historyId) {
        Map<String, Object> afterSnapshot = generateSnapshot(updatedFonds);
        afterSnapshot.put("previousFondsNo", oldFondsNo);
        afterSnapshot.put("currentFondsNo", newFondsNo);
        afterSnapshot.put("effectiveDate", effectiveDate);
        afterSnapshot.put("reason", reason);
        afterSnapshot.put("approvalTicketId", approvalTicketId);
        afterSnapshot.put("historyId", historyId);
        return afterSnapshot;
    }

    private void recordRenameAudit(String operatorId, String oldFondsNo, String newFondsNo,
                                   String approvalTicketId, Map<String, Object> beforeSnapshot,
                                   Map<String, Object> afterSnapshot) {
        auditLogService.logBusinessSnapshot(
                operatorId,
                operatorId,
                "FONDS_RENAME",
                "FONDS_HISTORY",
                oldFondsNo,
                "SUCCESS",
                "关键业务链路审计：全宗重命名",
                "LOW",
                beforeSnapshot,
                afterSnapshot,
                oldFondsNo,
                newFondsNo,
                approvalTicketId
        );
    }
}
