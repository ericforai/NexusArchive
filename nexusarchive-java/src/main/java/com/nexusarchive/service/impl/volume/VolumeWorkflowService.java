// Input: MyBatis-Plus、Lombok、Spring Framework
// Output: VolumeWorkflowService 类
// Pos: 案卷服务 - 审核流程层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl.volume;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VolumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 案卷工作流服务
 * <p>
 * 负责案卷的审核流程：提交审核、审批通过、审批驳回、移交
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolumeWorkflowService {

    private final VolumeMapper volumeMapper;
    private final ArchiveMapper archiveMapper;

    /**
     * 提交案卷审核
     */
    @Transactional
    public void submitForReview(String volumeId) {
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }
        if (!"draft".equals(volume.getStatus())) {
            throw new BusinessException("只有草稿状态的案卷可以提交审核");
        }

        volume.setStatus("pending");
        volume.setLastModifiedTime(LocalDateTime.now());
        volumeMapper.updateById(volume);
        log.info("案卷已提交审核: {}", volume.getVolumeCode());
    }

    /**
     * 审核通过并归档
     * 规范: "对整理阶段划定的保管期限、分类结果及排序等内容进行审核和确认"
     */
    @Transactional
    public void approveArchival(String volumeId, String reviewerId) {
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }
        if (!"pending".equals(volume.getStatus())) {
            throw new BusinessException("只有待审核状态的案卷可以审批");
        }

        LocalDateTime now = LocalDateTime.now();
        volume.setStatus("archived");
        volume.setReviewedBy(reviewerId);
        volume.setReviewedAt(now);
        volume.setArchivedAt(now);
        volume.setLastModifiedTime(now);
        volumeMapper.updateById(volume);

        // 更新卷内凭证状态为已归档
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getVolumeId, volumeId);
        List<Archive> archives = archiveMapper.selectList(wrapper);
        for (Archive archive : archives) {
            archive.setStatus("archived");
            archive.setLastModifiedTime(now);
            archiveMapper.updateById(archive);
        }

        log.info("案卷归档完成: {}", volume.getVolumeCode());
    }

    /**
     * 审核驳回
     */
    @Transactional
    public void rejectArchival(String volumeId, String reviewerId, String reason) {
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }
        if (!"pending".equals(volume.getStatus())) {
            throw new BusinessException("只有待审核状态的案卷可以驳回");
        }

        volume.setStatus("draft");
        volume.setReviewedBy(reviewerId);
        volume.setReviewedAt(LocalDateTime.now());
        volume.setLastModifiedTime(LocalDateTime.now());
        volumeMapper.updateById(volume);

        log.info("案卷审核驳回: {}, 原因: {}", volume.getVolumeCode(), reason);
    }

    /**
     * 移交档案管理部门
     * 规范: "会计年度终了后...移交单位档案管理机构保管"
     */
    @Transactional
    public void handoverToArchives(String volumeId) {
        log.info("开始移交案卷至档案部门: {}", volumeId);

        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }

        if (!"archived".equals(volume.getStatus())) {
            throw new BusinessException("只有已归档的案卷可以移交");
        }

        if ("ARCHIVES".equals(volume.getCustodianDept())) {
            throw new BusinessException("案卷已在档案部门保管中");
        }

        volume.setCustodianDept("ARCHIVES");
        volume.setLastModifiedTime(LocalDateTime.now());
        volumeMapper.updateById(volume);

        log.info("案卷移交完成: {}", volume.getVolumeCode());
    }
}
