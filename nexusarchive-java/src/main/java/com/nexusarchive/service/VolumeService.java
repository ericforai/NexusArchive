// Input: MyBatis-Plus、Lombok、Apache、Spring Framework
// Output: VolumeService 类
// Pos: 业务服务层 - 案卷服务 Facade
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VolumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 组卷服务 (Facade 协调器)
 * <p>
 * 符合 DA/T 104-2024 第7.4节组卷规范
 * </p>
 * <p>
 * 本服务已模块化拆分，委托给专用模块处理：
 * <ul>
 * <li>VolumeAssembler - 组卷逻辑</li>
 * <li>VolumeWorkflowService - 审核流程</li>
 * <li>AipPackageExporter - AIP包导出</li>
 * <li>VolumeQuery - 查询操作</li>
 * <li>VolumePdfGenerator - PDF生成</li>
 * <li>VolumeUtils - 工具方法</li>
 * </ul>
 * </p>
 *
 * @see com.nexusarchive.service.impl.volume.VolumeAssembler
 * @see com.nexusarchive.service.impl.volume.VolumeWorkflowService
 * @see com.nexusarchive.service.impl.volume.AipPackageExporter
 * @see com.nexusarchive.service.impl.volume.VolumeQuery
 * @see com.nexusarchive.service.impl.volume.VolumePdfGenerator
 * @see com.nexusarchive.service.impl.volume.VolumeUtils
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VolumeService {

    private final VolumeMapper volumeMapper;
    private final ArchiveMapper archiveMapper;

    // 注入专用模块
    private final com.nexusarchive.service.impl.volume.VolumeAssembler volumeAssembler;
    private final com.nexusarchive.service.impl.volume.VolumeWorkflowService volumeWorkflowService;
    private final com.nexusarchive.service.impl.volume.AipPackageExporter aipPackageExporter;

    /**
     * 按月自动组卷
     * 规范: "业务单据一般按月进行组卷"
     *
     * @param fiscalPeriod 会计期间 (YYYY-MM)
     * @return 创建的案卷
     */
    public Volume assembleByMonth(String fiscalPeriod) {
        return volumeAssembler.assembleByMonth(fiscalPeriod);
    }

    /**
     * 获取案卷列表
     */
    public Page<Volume> getVolumeList(int page, int limit, String status) {
        return com.nexusarchive.service.impl.volume.VolumeQuery.getVolumeList(volumeMapper, page, limit, status);
    }

    /**
     * 获取案卷详情
     */
    public Volume getVolumeById(String volumeId) {
        return com.nexusarchive.service.impl.volume.VolumeQuery.getVolumeById(volumeMapper, volumeId);
    }

    /**
     * 获取卷内文件列表
     * 规范: "卷内文件一般按照形成时间顺序排列"
     */
    public List<Archive> getVolumeFiles(String volumeId) {
        return com.nexusarchive.service.impl.volume.VolumeQuery.getVolumeFiles(archiveMapper, volumeId);
    }

    /**
     * 提交案卷审核
     */
    public void submitForReview(String volumeId) {
        volumeWorkflowService.submitForReview(volumeId);
    }

    /**
     * 审核通过并归档
     * 规范: "对整理阶段划定的保管期限、分类结果及排序等内容进行审核和确认"
     */
    public void approveArchival(String volumeId, String reviewerId) {
        volumeWorkflowService.approveArchival(volumeId, reviewerId);
    }

    /**
     * 审核驳回
     */
    public void rejectArchival(String volumeId, String reviewerId, String reason) {
        volumeWorkflowService.rejectArchival(volumeId, reviewerId, reason);
    }

    /**
     * 生成归档登记表数据
     * 参照 GB/T 18894-2016 附录 A 的表 A.1
     */
    public Map<String, Object> generateRegistrationForm(String volumeId) {
        return com.nexusarchive.service.impl.volume.VolumeQuery.generateRegistrationForm(volumeMapper, archiveMapper, volumeId);
    }

    /**
     * 移交档案管理部门
     * 规范: "会计年度终了后...移交单位档案管理机构保管"
     */
    public void handoverToArchives(String volumeId) {
        volumeWorkflowService.handoverToArchives(volumeId);
    }

    /**
     * 导出案卷 AIP 包
     * 符合 DA/T 94-2022 和 GB/T 39674 标准
     *
     * @return AIP 包 ZIP 文件
     */
    public File exportAipPackage(String volumeId) throws IOException {
        return aipPackageExporter.exportAipPackage(volumeId);
    }
}
