// Input: DestructionLogService, DestructionLogMapper, ArchiveMapper, SM3Utils, ObjectMapper
// Output: DestructionLogServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.DestructionLog;
import com.nexusarchive.mapper.DestructionLogMapper;
import com.nexusarchive.service.DestructionLogService;
import com.nexusarchive.util.SM3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 销毁清册服务实现
 * 
 * 实现要点：
 * 1. 生成完整元数据快照（包含所有时间戳字段）
 * 2. 计算哈希链（prev_hash/curr_hash）
 * 3. 写入销毁清册记录（永久只读）
 * 4. 防止重复记录（唯一性约束）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DestructionLogServiceImpl implements DestructionLogService {
    
    private final DestructionLogMapper destructionLogMapper;
    private final SM3Utils sm3Utils;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logDestruction(Archive archive, String destructionId, String executorId, String traceId) {
        // 1. 检查是否已存在记录（防止重复记录）
        Integer archiveYear = archive.getFiscalYear() != null ? 
            Integer.parseInt(archive.getFiscalYear()) : null;
        
        int existingCount = destructionLogMapper.countByArchive(
            archive.getId(), archive.getFondsNo(), archiveYear);
        
        if (existingCount > 0) {
            log.warn("销毁清册记录已存在，跳过写入: archiveId={}, fondsNo={}, archiveYear={}", 
                    archive.getId(), archive.getFondsNo(), archiveYear);
            return;
        }
        
        // 2. 获取前一条记录的哈希值（用于哈希链）
        String prevHash = destructionLogMapper.getLastHash(archive.getFondsNo(), archiveYear);
        
        // 3. 生成完整元数据快照
        String snapshot = generateCompleteSnapshot(archive, destructionId);
        
        // 4. 计算当前记录的哈希值
        String currHash = calculateHashChain(prevHash, snapshot);
        
        // 5. 创建销毁清册记录
        DestructionLog destructionLog = new DestructionLog();
        destructionLog.setFondsNo(archive.getFondsNo());
        destructionLog.setArchiveYear(archiveYear);
        destructionLog.setArchiveObjectId(archive.getId());
        destructionLog.setRetentionPolicyId(archive.getRetentionPeriod()); // 使用 retentionPeriod 作为 policyId
        destructionLog.setApprovalTicketId(destructionId);
        destructionLog.setDestroyedBy(executorId);
        destructionLog.setDestroyedAt(LocalDateTime.now());
        destructionLog.setTraceId(traceId);
        destructionLog.setSnapshot(snapshot);
        destructionLog.setPrevHash(prevHash);
        destructionLog.setCurrHash(currHash);
        destructionLog.setCreatedAt(LocalDateTime.now());
        
        // 6. 写入数据库
        destructionLogMapper.insert(destructionLog);
        
        log.info("销毁清册记录写入成功: archiveId={}, fondsNo={}, archiveYear={}, traceId={}", 
                archive.getId(), archive.getFondsNo(), archiveYear, traceId);
    }
    
    @Override
    public byte[] exportDestructionLog(String fondsNo, Integer archiveYear, 
                                      LocalDate fromDate, LocalDate toDate) {
        // TODO: 实现 Excel/PDF 导出
        // 可以使用 Apache POI 导出 Excel，使用 iText 或 PDFBox 导出 PDF
        throw new UnsupportedOperationException("导出功能待实现");
    }
    
    @Override
    public String calculateHashChain(String prevHash, Object logData) {
        // 哈希链计算：curr_hash = SM3(prev_hash + logData)
        String dataString;
        if (logData instanceof String) {
            dataString = (String) logData;
        } else {
            try {
                dataString = objectMapper.writeValueAsString(logData);
            } catch (Exception e) {
                log.error("序列化日志数据失败", e);
                throw new RuntimeException("计算哈希链失败: " + e.getMessage(), e);
            }
        }
        
        // 如果 prevHash 为空，表示第一条记录
        String input = prevHash != null ? prevHash + dataString : dataString;
        
        return sm3Utils.hash(input);
    }
    
    /**
     * 生成完整元数据快照（包含所有时间戳字段）
     */
    private String generateCompleteSnapshot(Archive archive, String destructionId) {
        Map<String, Object> snapshot = new HashMap<>();
        
        // 档案基本信息
        snapshot.put("archiveObjectId", archive.getId());
        snapshot.put("archiveCode", archive.getArchiveCode());
        snapshot.put("fondsNo", archive.getFondsNo());
        snapshot.put("archiveYear", archive.getFiscalYear());
        snapshot.put("title", archive.getTitle());
        snapshot.put("docDate", archive.getDocDate());
        snapshot.put("amount", archive.getAmount());
        snapshot.put("orgName", archive.getOrgName());
        snapshot.put("uniqueBizId", archive.getUniqueBizId());
        snapshot.put("creator", archive.getCreator());
        snapshot.put("categoryCode", archive.getCategoryCode());
        snapshot.put("fiscalPeriod", archive.getFiscalPeriod());
        
        // 保管期限信息
        snapshot.put("retentionPeriod", archive.getRetentionPeriod());
        snapshot.put("retentionStartDate", archive.getRetentionStartDate());
        
        // 时间戳字段（关键追溯信息）
        snapshot.put("archivedAt", archive.getCreatedTime()); // 归档时间
        snapshot.put("retentionStartDate", archive.getRetentionStartDate()); // 保管期限起算日期
        snapshot.put("originalFormationDate", archive.getDocDate()); // 原始形成时间（使用 docDate）
        snapshot.put("expiredDate", calculateExpirationDate(archive)); // 到期日期
        
        // 销毁信息
        snapshot.put("destructionId", destructionId);
        snapshot.put("snapshotTime", LocalDateTime.now());
        
        // 扩展元数据
        snapshot.put("standardMetadata", archive.getStandardMetadata());
        snapshot.put("customMetadata", archive.getCustomMetadata());
        snapshot.put("summary", archive.getSummary());
        
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.error("生成销毁清册快照失败", e);
            throw new RuntimeException("生成销毁清册快照失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 计算到期日期
     */
    private LocalDate calculateExpirationDate(Archive archive) {
        if (archive.getRetentionStartDate() == null || archive.getRetentionPeriod() == null) {
            return null;
        }
        
        // 永久保管
        if ("PERMANENT".equalsIgnoreCase(archive.getRetentionPeriod()) || 
            "永久".equals(archive.getRetentionPeriod())) {
            return null;
        }
        
        // 解析年数
        int years = parseRetentionYears(archive.getRetentionPeriod());
        if (years <= 0) {
            return null;
        }
        
        return archive.getRetentionStartDate().plusYears(years);
    }
    
    /**
     * 解析保管期限年数
     */
    private int parseRetentionYears(String retentionPeriod) {
        if (retentionPeriod == null || retentionPeriod.isEmpty()) {
            return 0;
        }
        
        String period = retentionPeriod.trim().toUpperCase();
        if (period.contains("PERMANENT") || period.contains("永久")) {
            return Integer.MAX_VALUE;
        }
        
        try {
            if (period.endsWith("Y")) {
                period = period.substring(0, period.length() - 1);
            }
            return Integer.parseInt(period);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

