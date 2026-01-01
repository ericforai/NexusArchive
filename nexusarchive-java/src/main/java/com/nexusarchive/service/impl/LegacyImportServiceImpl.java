// Input: Spring Framework、MyBatis-Plus
// Output: LegacyImportServiceImpl 实现类 (Facade协调器)
// Pos: 业务服务实现层 - 历史数据导入服务 Facade
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.request.FieldMappingConfig;
import com.nexusarchive.dto.request.ImportPreviewResult;
import com.nexusarchive.dto.request.ImportResult;
import com.nexusarchive.entity.LegacyImportTask;
import com.nexusarchive.mapper.LegacyImportTaskMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.LegacyImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 历史数据导入服务实现 (Facade 协调器)
 * <p>
 * 本服务已模块化拆分，委托给专用模块处理：
 * <ul>
 * <li>LegacyFileParser - 文件解析（CSV/Excel）</li>
 * <li>LegacyDataConverter - 数据转换</li>
 * <li>LegacyImportOrchestrator - 导入流程编排</li>
 * <li>LegacyImportUtils - 工具方法</li>
 * </ul>
 * </p>
 *
 * @see com.nexusarchive.service.impl.legacy.LegacyFileParser
 * @see com.nexusarchive.service.impl.legacy.LegacyDataConverter
 * @see com.nexusarchive.service.impl.legacy.LegacyImportOrchestrator
 * @see com.nexusarchive.service.impl.legacy.LegacyImportUtils
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyImportServiceImpl implements LegacyImportService {

    private final LegacyImportTaskMapper importTaskMapper;
    private final AuditLogService auditLogService;

    // 注入专用模块
    private final com.nexusarchive.service.impl.legacy.LegacyImportOrchestrator importOrchestrator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResult importLegacyData(MultipartFile file,
                                         FieldMappingConfig mappingConfig,
                                         String operatorId,
                                         String fondsNo) {
        return importOrchestrator.executeImport(file, mappingConfig, operatorId, fondsNo);
    }

    @Override
    public ImportPreviewResult previewImport(MultipartFile file, FieldMappingConfig mappingConfig) {
        return importOrchestrator.previewImport(file, mappingConfig);
    }

    @Override
    public Page<LegacyImportTask> getImportTasks(int page, int size, String status) {
        Page<LegacyImportTask> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<LegacyImportTask> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(status)) {
            wrapper.eq(LegacyImportTask::getStatus, status);
        }

        wrapper.orderByDesc(LegacyImportTask::getCreatedAt);

        return importTaskMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public LegacyImportTask getImportTaskDetail(String importId) {
        LegacyImportTask task = importTaskMapper.selectById(importId);
        if (task == null) {
            throw new BusinessException("导入任务不存在");
        }
        return task;
    }

    @Override
    public byte[] downloadErrorReport(String importId) {
        LegacyImportTask task = getImportTaskDetail(importId);
        if (!StringUtils.hasText(task.getErrorReportPath())) {
            throw new BusinessException("错误报告不存在");
        }

        // TODO: 从文件系统读取错误报告文件
        return new byte[0];
    }
}
