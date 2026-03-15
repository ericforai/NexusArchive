// Input: ArchiveAppraisalService, AppraisalListMapper, ArchiveMapper, ObjectMapper
// Output: ArchiveAppraisalServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.constants.OperationResult;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.AppraisalListDetail;
import com.nexusarchive.entity.AppraisalList;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.AppraisalListMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.ArchiveAppraisalService;
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
import java.util.stream.Collectors;

/**
 * 档案鉴定清单服务实现
 * 
 * 实现要点：
 * 1. 生成鉴定清单时，创建档案元数据快照（JSON格式）
 * 2. 更新档案状态为 APPRAISING
 * 3. 提交鉴定结论后，更新档案状态和鉴定清单状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveAppraisalServiceImpl implements ArchiveAppraisalService {
    
    private final AppraisalListMapper appraisalListMapper;
    private final ArchiveMapper archiveMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createAppraisalList(List<String> archiveIds, String fondsNo, 
                                       String appraiserId, LocalDate appraisalDate) {
        if (archiveIds == null || archiveIds.isEmpty()) {
            throw new IllegalArgumentException("待鉴定档案ID列表不能为空");
        }
        
        // 1. 查询档案列表，验证状态和全宗号
        List<Archive> archives = archiveMapper.selectBatchIds(archiveIds);
        if (archives.size() != archiveIds.size()) {
            throw new IllegalArgumentException("部分档案不存在");
        }
        
        // 验证所有档案都是 EXPIRED 状态，且属于同一全宗
        for (Archive archive : archives) {
            if (!"EXPIRED".equals(archive.getDestructionStatus())) {
                throw new IllegalArgumentException(
                    String.format("档案 %s 状态不是 EXPIRED，无法生成鉴定清单", archive.getArchiveCode()));
            }
            if (!fondsNo.equals(archive.getFondsNo())) {
                throw new IllegalArgumentException(
                    String.format("档案 %s 不属于全宗 %s", archive.getArchiveCode(), fondsNo));
            }
        }
        
        // 2. 生成档案元数据快照
        String archiveSnapshot = generateArchiveSnapshot(archives);
        
        // 3. 创建鉴定清单记录
        AppraisalList appraisalList = new AppraisalList();
        appraisalList.setFondsNo(fondsNo);
        appraisalList.setArchiveYear(archives.get(0).getFiscalYear() != null ? 
            Integer.parseInt(archives.get(0).getFiscalYear()) : null);
        appraisalList.setAppraiserId(appraiserId);
        appraisalList.setAppraiserName(""); // 需要从用户服务获取
        appraisalList.setAppraisalDate(appraisalDate);
        appraisalList.setArchiveIds(objectMapper.valueToTree(archiveIds).toString());
        appraisalList.setArchiveSnapshot(archiveSnapshot);
        appraisalList.setStatus(OperationResult.PENDING);
        appraisalList.setCreatedTime(LocalDateTime.now());
        appraisalList.setLastModifiedTime(LocalDateTime.now());
        
        appraisalListMapper.insert(appraisalList);
        
        // 4. 更新档案状态为 APPRAISING
        for (String archiveId : archiveIds) {
            LambdaUpdateWrapper<Archive> updateWrapper = new LambdaUpdateWrapper<Archive>()
                    .eq(Archive::getId, archiveId)
                    .eq(Archive::getDestructionStatus, "EXPIRED")
                    .set(Archive::getDestructionStatus, "APPRAISING");
            
            archiveMapper.update(null, updateWrapper);
        }
        
        log.info("创建鉴定清单成功，清单ID: {}, 档案数量: {}", appraisalList.getId(), archiveIds.size());
        
        return appraisalList.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitAppraisalConclusion(String appraisalListId, String conclusion, String comment) {
        AppraisalList appraisalList = appraisalListMapper.selectById(appraisalListId);
        if (appraisalList == null) {
            throw new IllegalArgumentException("鉴定清单不存在: " + appraisalListId);
        }
        
        if (!OperationResult.PENDING.equals(appraisalList.getStatus())) {
            throw new IllegalStateException("鉴定清单状态不是 PENDING，无法提交结论");
        }
        
        // 验证结论值
        if (!"APPROVED".equals(conclusion) && 
            !"REJECTED".equals(conclusion) && 
            !"DEFERRED".equals(conclusion)) {
            throw new IllegalArgumentException("无效的鉴定结论: " + conclusion);
        }
        
        // 更新鉴定清单状态
        appraisalList.setStatus("SUBMITTED");
        appraisalList.setLastModifiedTime(LocalDateTime.now());
        appraisalListMapper.updateById(appraisalList);
        
        // 解析档案ID列表
        try {
            List<String> archiveIds = objectMapper.readValue(
                appraisalList.getArchiveIds(), 
                new TypeReference<List<String>>() {}
            );
            
            // 根据鉴定结论更新档案状态
            String newStatus;
            if ("APPROVED".equals(conclusion)) {
                // 同意销毁，状态保持 APPRAISING，等待审批
                newStatus = "APPRAISING";
            } else if ("REJECTED".equals(conclusion)) {
                // 不同意销毁，回退到 EXPIRED
                newStatus = "EXPIRED";
            } else {
                // 延期保管，回退到 NORMAL
                newStatus = "NORMAL";
            }
            
            for (String archiveId : archiveIds) {
                LambdaUpdateWrapper<Archive> updateWrapper = new LambdaUpdateWrapper<Archive>()
                        .eq(Archive::getId, archiveId)
                        .eq(Archive::getDestructionStatus, "APPRAISING")
                        .set(Archive::getDestructionStatus, newStatus);
                
                archiveMapper.update(null, updateWrapper);
            }
            
            log.info("提交鉴定结论成功，清单ID: {}, 结论: {}, 档案数量: {}", 
                    appraisalListId, conclusion, archiveIds.size());
            
        } catch (Exception e) {
            log.error("解析档案ID列表失败", e);
            throw new RuntimeException("提交鉴定结论失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public byte[] exportAppraisalList(String appraisalListId, ExportFormat format) {
        // TODO: 实现 Excel/PDF 导出
        // 可以使用 Apache POI 导出 Excel，使用 iText 或 PDFBox 导出 PDF
        throw new UnsupportedOperationException("导出功能待实现");
    }
    
    @Override
    public com.nexusarchive.dto.AppraisalListDetail getAppraisalListDetail(String appraisalListId) {
        AppraisalList appraisalList = appraisalListMapper.selectById(appraisalListId);
        if (appraisalList == null) {
            throw new IllegalArgumentException("鉴定清单不存在: " + appraisalListId);
        }
        
        AppraisalListDetail detail = new AppraisalListDetail();
        detail.setAppraisalListId(appraisalList.getId());
        detail.setFondsNo(appraisalList.getFondsNo());
        detail.setArchiveYear(appraisalList.getArchiveYear());
        detail.setAppraiserId(appraisalList.getAppraiserId());
        detail.setAppraiserName(appraisalList.getAppraiserName());
        detail.setAppraisalDate(appraisalList.getAppraisalDate());
        detail.setStatus(appraisalList.getStatus());
        
        // 解析档案ID列表并查询档案详情
        try {
            List<String> archiveIds = objectMapper.readValue(
                appraisalList.getArchiveIds(), 
                new TypeReference<List<String>>() {}
            );
            
            List<Archive> archives = archiveMapper.selectBatchIds(archiveIds);
            List<AppraisalListDetail.ArchiveAppraisalItem> items = archives.stream()
                    .map(this::convertToAppraisalItem)
                    .collect(Collectors.toList());
            
            detail.setArchives(items);
            
        } catch (Exception e) {
            log.error("解析档案列表失败", e);
            throw new RuntimeException("获取鉴定清单详情失败: " + e.getMessage(), e);
        }
        
        return detail;
    }
    
    /**
     * 生成档案元数据快照（JSON格式）
     */
    private String generateArchiveSnapshot(List<Archive> archives) {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        
        for (Archive archive : archives) {
            Map<String, Object> item = new HashMap<>();
            item.put("archiveId", archive.getId());
            item.put("archiveCode", archive.getArchiveCode());
            item.put("title", archive.getTitle());
            item.put("docDate", archive.getDocDate());
            item.put("archivedAt", archive.getCreatedTime()); // 归档时间
            item.put("retentionStartDate", archive.getRetentionStartDate());
            item.put("retentionPeriod", archive.getRetentionPeriod());
            item.put("expirationDate", calculateExpirationDate(archive));
            item.put("orgName", archive.getOrgName());
            item.put("creator", archive.getCreator());
            item.put("fiscalYear", archive.getFiscalYear());
            item.put("fiscalPeriod", archive.getFiscalPeriod());
            item.put("amount", archive.getAmount());
            item.put("snapshotTime", LocalDateTime.now());
            
            snapshot.add(item);
        }
        
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.error("生成档案快照失败", e);
            throw new RuntimeException("生成档案快照失败: " + e.getMessage(), e);
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
    
    /**
     * 转换为鉴定项
     */
    private AppraisalListDetail.ArchiveAppraisalItem convertToAppraisalItem(Archive archive) {
        AppraisalListDetail.ArchiveAppraisalItem item = new AppraisalListDetail.ArchiveAppraisalItem();
        item.setArchiveId(archive.getId());
        item.setArchiveCode(archive.getArchiveCode());
        item.setTitle(archive.getTitle());
        item.setDocDate(archive.getDocDate());
        item.setArchivedAt(archive.getCreatedTime() != null ? 
            archive.getCreatedTime().toLocalDate() : null);
        item.setRetentionStartDate(archive.getRetentionStartDate());
        item.setRetentionPeriod(archive.getRetentionPeriod());
        item.setExpirationDate(calculateExpirationDate(archive));
        item.setOrgName(archive.getOrgName());
        item.setCreator(archive.getCreator());
        return item;
    }
}

