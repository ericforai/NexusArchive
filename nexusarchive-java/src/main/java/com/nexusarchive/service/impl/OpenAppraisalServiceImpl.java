// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: OpenAppraisalServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.OpenAppraisal;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.OpenAppraisalMapper;
import com.nexusarchive.service.OpenAppraisalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 开放鉴定服务实现
 */
@Service
@RequiredArgsConstructor
public class OpenAppraisalServiceImpl implements OpenAppraisalService {

    private final OpenAppraisalMapper appraisalMapper;
    private final ArchiveMapper archiveMapper;

    @Override
    @Transactional
    public OpenAppraisal createAppraisal(OpenAppraisal appraisal) {
        // 验证档案是否存在
        Archive archive = archiveMapper.selectById(appraisal.getArchiveId());
        if (archive == null) {
            throw new RuntimeException("Archive not found: " + appraisal.getArchiveId());
        }

        // 设置冗余字段
        appraisal.setArchiveCode(archive.getArchiveCode());
        appraisal.setArchiveTitle(archive.getTitle());
        appraisal.setRetentionPeriod(archive.getRetentionPeriod());
        appraisal.setCurrentSecurityLevel(archive.getSecurityLevel() != null ? archive.getSecurityLevel() : "internal");
        appraisal.setStatus("PENDING");

        // 设置时间戳
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        appraisal.setCreatedTime(now);
        appraisal.setLastModifiedTime(now);

        appraisalMapper.insert(appraisal);
        return appraisal;
    }

    @Override
    @Transactional
    public void submitAppraisal(String id, String appraiserId, String appraiserName, 
                               String appraisalResult, String openLevel, String reason) {
        OpenAppraisal appraisal = appraisalMapper.selectById(id);
        if (appraisal == null) {
            throw new RuntimeException("Appraisal record not found");
        }

        if (!"PENDING".equals(appraisal.getStatus())) {
            throw new RuntimeException("Only pending appraisals can be submitted");
        }

        // 更新鉴定记录
        appraisal.setAppraiserId(appraiserId);
        appraisal.setAppraiserName(appraiserName);
        appraisal.setAppraisalResult(appraisalResult);
        appraisal.setOpenLevel(openLevel);
        appraisal.setReason(reason);
        appraisal.setAppraisalDate(LocalDate.now());
        appraisal.setStatus("COMPLETED");
        appraisalMapper.updateById(appraisal);

        // 根据鉴定结果更新档案
        Archive archive = archiveMapper.selectById(appraisal.getArchiveId());
        if (archive != null) {
            switch (appraisalResult) {
                case "OPEN":
                    // 开放：更新密级
                    archive.setSecurityLevel(openLevel);
                    break;
                case "CONTROLLED":
                    // 控制：保持原密级不变
                    break;
                case "EXTENDED":
                    // 延期：延长保管期限（这里简化处理，实际可能需要更复杂的逻辑）
                    String currentPeriod = archive.getRetentionPeriod();
                    if ("10Y".equals(currentPeriod)) {
                        archive.setRetentionPeriod("30Y");
                    } else if ("30Y".equals(currentPeriod)) {
                        archive.setRetentionPeriod("PERMANENT");
                    }
                    break;
            }
            archiveMapper.updateById(archive);
        }
    }

    @Override
    public Page<OpenAppraisal> getAppraisalList(int page, int limit, String status) {
        Page<OpenAppraisal> pageParam = new Page<>(page, limit);
        QueryWrapper<OpenAppraisal> queryWrapper = new QueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("created_at");
        return appraisalMapper.selectPage(pageParam, queryWrapper);
    }

    @Override
    public OpenAppraisal getAppraisalById(String id) {
        return appraisalMapper.selectById(id);
    }
}
