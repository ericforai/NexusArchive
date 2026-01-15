// Input: YonSuite Client, EntityService, Lombok, Spring Framework
// Output: ErpOrgSyncService 类
// Pos: 业务服务层 - ERP 组织同步服务
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.SysEntity;
import com.nexusarchive.integration.yonsuite.client.YonSuiteOrgClient;
import com.nexusarchive.integration.yonsuite.dto.YonOrgTreeSyncResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ERP 组织同步服务
 * 从 YonSuite 同步组织架构数据到 sys_entity 表
 */
@Service
@RequiredArgsConstructor
public class ErpOrgSyncService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ErpOrgSyncService.class);

    private final YonSuiteOrgClient yonSuiteOrgClient;
    private final EntityService entityService;
    
    // ... inside syncFromYonSuite
    // ... no changes needed to method body if log is present
    
    // ... at line 169
    // ... duplicate class removed

    @Transactional
    @CacheEvict(value = "entityTree", allEntries = true)
    public SyncResult syncFromYonSuite() {
        return syncFromYonSuite("1970-01-01 00:00:00");
    }

    @Transactional
    @CacheEvict(value = "entityTree", allEntries = true)
    public SyncResult syncFromYonSuite(String lastSyncTime) {
        log.info("开始从 YonSuite 同步组织架构，起始时间: {}", lastSyncTime);

        SyncResult result = new SyncResult();
        result.setLastSyncTime(YonSuiteOrgClient.getCurrentTimestamp());
        List<String> errors = new ArrayList<>();
        result.setErrors(errors);

        try {
            List<YonOrgTreeSyncResponse.OrgRecord> orgRecords = yonSuiteOrgClient.queryOrgs(lastSyncTime);
            log.info("YonSuite API 返回组织记录数: {}", orgRecords.size());

            if (orgRecords.isEmpty()) {
                result.setSuccess(true);
                result.setMessage("YonSuite 无组织数据");
                return result;
            }

            // 获取现有法人列表，用于判断新增还是更新
            List<SysEntity> existingEntities = entityService.list();
            var existingMap = existingEntities.stream()
                    .collect(Collectors.toMap(SysEntity::getId, e -> e));

            int successCount = 0;
            int errorCount = 0;
            int createdCount = 0;
            int updatedCount = 0;

            for (YonOrgTreeSyncResponse.OrgRecord record : orgRecords) {
                try {
                    // 验证必要字段
                    if (!StringUtils.hasText(record.getId()) || !StringUtils.hasText(record.getName())) {
                        log.warn("跳过无效记录: id={}, name={}", record.getId(), record.getName());
                        errorCount++;
                        continue;
                    }

                    SysEntity entity = existingMap.get(record.getId());
                    boolean isUpdate = (entity != null);

                    if (isUpdate) {
                        // 更新现有法人
                        updateEntityFromRecord(entity, record);
                        entityService.updateById(entity);
                        updatedCount++;
                        log.debug("更新法人: id={}, name={}", record.getId(), record.getName());
                    } else {
                        // 创建新法人
                        entity = createEntityFromRecord(record);
                        entityService.save(entity);
                        createdCount++;
                        log.debug("创建法人: id={}, name={}", record.getId(), record.getName());
                    }
                    successCount++;

                } catch (Exception e) {
                    errorCount++;
                    String errorMsg = String.format("处理记录失败 [id=%s, name=%s]: %s",
                            record.getId(), record.getName(), e.getMessage());
                    log.error(errorMsg, e);
                    errors.add(errorMsg);
                }
            }

            result.setSuccess(true);
            result.setMessage(String.format("同步完成: 新增 %d 条，更新 %d 条，失败 %d 条",
                    createdCount, updatedCount, errorCount));
            result.setSuccessCount(successCount);
            result.setErrorCount(errorCount);

            log.info("YonSuite 组织同步完成: 新增={}, 更新={}, 失败={}", createdCount, updatedCount, errorCount);

        } catch (Exception e) {
            log.error("从 YonSuite 同步组织架构失败", e);
            result.setSuccess(false);
            result.setMessage("同步失败: " + e.getMessage());
            result.setErrorCount(1);
        }

        return result;
    }

    /**
     * 从 YonSuite 记录创建新的 SysEntity
     */
    private SysEntity createEntityFromRecord(YonOrgTreeSyncResponse.OrgRecord record) {
        SysEntity entity = new SysEntity();
        entity.setId(record.getId());
        entity.setName(record.getName());
        entity.setParentId(record.getParentId());
        entity.setOrderNum(record.getOrderNum() != null ? record.getOrderNum() : 0);
        entity.setStatus(mapStatus(record.getEnableStatus()));
        entity.setDescription(String.format("从 YonSuite 同步 (code=%s)", record.getCode()));
        return entity;
    }

    /**
     * 用 YonSuite 记录更新现有 SysEntity
     */
    private void updateEntityFromRecord(SysEntity entity, YonOrgTreeSyncResponse.OrgRecord record) {
        entity.setName(record.getName());
        entity.setParentId(record.getParentId());
        if (record.getOrderNum() != null) {
            entity.setOrderNum(record.getOrderNum());
        }
        entity.setStatus(mapStatus(record.getEnableStatus()));
        // 保留手动设置的 taxId、address、contactPerson 等字段，不覆盖
        String currentDesc = entity.getDescription();
        if (currentDesc == null || !currentDesc.contains("从 YonSuite 同步")) {
            entity.setDescription(String.format("从 YonSuite 同步 (code=%s)", record.getCode()));
        }
    }

    /**
     * 映射 YonSuite 状态到本地状态
     * YonSuite: enable=1 启用，enable=0 禁用
     * 本地: ACTIVE, INACTIVE
     */
    private String mapStatus(Integer enableStatus) {
        if (enableStatus == null || enableStatus == 1) {
            return "ACTIVE";
        }
        return "INACTIVE";
    }

    @Data
    public static class SyncResult {
        private boolean success;
        private String message;
        private int successCount;
        private int errorCount;
        private String lastSyncTime;
        private List<String> errors;

        // Manual Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        
        public String getLastSyncTime() { return lastSyncTime; }
        public void setLastSyncTime(String lastSyncTime) { this.lastSyncTime = lastSyncTime; }
        
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
}
