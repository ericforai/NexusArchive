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
@Slf4j
@RequiredArgsConstructor
public class ErpOrgSyncService {

    private final YonSuiteOrgClient yonSuiteOrgClient;
    private final EntityService entityService;

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

                    // 过滤部门数据：只同步法人实体（orgType 为空或为法人类型）
                    // 电子会计档案系统没有部门概念，只管理法人实体
                    String orgType = record.getOrgType();
                    if (StringUtils.hasText(orgType) && isDepartmentType(orgType)) {
                        log.debug("跳过部门记录（电子会计档案不管理部门）: id={}, name={}, orgType={}",
                                record.getId(), record.getName(), orgType);
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
     * 判断是否为部门类型
     * 电子会计档案系统只管理法人实体，不管理部门
     */
    private boolean isDepartmentType(String orgType) {
        if (orgType == null) {
            return false;
        }
        String lowerType = orgType.toLowerCase();
        // 常见的部门类型标识
        return lowerType.contains("department") ||
               lowerType.contains("部门") ||
               lowerType.contains("dept") ||
               lowerType.equals("2"); // 某些系统中部门类型编码为 2
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
    }
}
