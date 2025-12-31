// Input: MultipartFile、FieldMappingConfig、Java 标准库
// Output: LegacyImportService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.request.FieldMappingConfig;
import com.nexusarchive.dto.request.ImportPreviewResult;
import com.nexusarchive.dto.request.ImportResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 历史数据导入服务
 * 
 * OpenSpec 来源: openspec-legacy-data-import.md
 */
public interface LegacyImportService {
    
    /**
     * 导入历史数据
     * 
     * @param file 上传的文件（CSV 或 Excel）
     * @param mappingConfig 字段映射配置（可选，如不提供则使用默认映射）
     * @param operatorId 操作人ID
     * @param fondsNo 当前操作人的全宗号（用于权限校验）
     * @return 导入结果
     */
    ImportResult importLegacyData(MultipartFile file, 
                                   FieldMappingConfig mappingConfig,
                                   String operatorId, 
                                   String fondsNo);
    
    /**
     * 预览导入数据（不执行导入，仅解析和验证）
     * 
     * @param file 上传的文件
     * @param mappingConfig 字段映射配置
     * @return 预览结果（包含解析的数据、验证错误、统计信息）
     */
    ImportPreviewResult previewImport(MultipartFile file, 
                                       FieldMappingConfig mappingConfig);
    
    /**
     * 获取导入任务列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param status 状态筛选（可选）
     * @return 导入任务列表
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.nexusarchive.entity.LegacyImportTask> 
        getImportTasks(int page, int size, String status);
    
    /**
     * 获取导入任务详情
     * 
     * @param importId 导入任务ID
     * @return 导入任务详情
     */
    com.nexusarchive.entity.LegacyImportTask getImportTaskDetail(String importId);
    
    /**
     * 下载错误报告
     * 
     * @param importId 导入任务ID
     * @return 错误报告文件（Excel格式）
     */
    byte[] downloadErrorReport(String importId);
}

