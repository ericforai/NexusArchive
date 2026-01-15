// Input: Jackson、Lombok、Java 标准库
// Output: YonOrgTreeSyncResponse 类
// Pos: 数据传输对象 - YonSuite 组织树版本同步响应

package com.nexusarchive.integration.yonsuite.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * YonSuite 组织树版本同步响应 DTO
 * API: POST /yonbip/digitalModel/openapi/treedatasync/treeversionsync
 * 返回树版本列表，需要再用 treesync 接口获取实际组织数据
 */
@Data
public class YonOrgTreeSyncResponse {

    private String code;
    private String message;
    private SyncData data;

    @Data
    public static class SyncData {
        private Integer pageIndex;
        private Integer pageSize;
        private Integer recordCount;
        private List<TreeVersion> recordList;
        private Integer pageCount;
    }

    /**
     * 树版本信息
     */
    @Data
    public static class TreeVersion {
        private String id;
        private String treeId;
        private String name;
        private String startTime;
        private String endTime;
        private Integer syncStatus;
        private String description;
        private String creator;
        private String creationtime;
        private String modifier;
        private String modifiedtime;
        private String tenantId;
        private String versionNo;

        // Manual Getters
        public String getId() { return id; }
        public String getTreeId() { return treeId; }
        public String getName() { return name; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public Integer getSyncStatus() { return syncStatus; }
        public String getDescription() { return description; }
        public String getCreator() { return creator; }
        public String getCreationtime() { return creationtime; }
        public String getModifier() { return modifier; }
        public String getModifiedtime() { return modifiedtime; }
        public String getTenantId() { return tenantId; }
        public String getVersionNo() { return versionNo; }
    }

    /**
     * 组织节点数据（来自 treesync 接口）
     */
    @Data
    public static class OrgRecord {
        private String id;
        private String code;
        private String name;
        @JsonProperty("parent_id")
        private String parentId;
        private String orgType;
        private Integer level;
        @JsonProperty("order_num")
        private Integer orderNum;
        @JsonProperty("enable_status")
        private Integer enableStatus;
        private String creationtime;
        private String modifiedtime;
        @JsonProperty("tenant_id")
        private String tenantId;

        // Manual Getters
        public String getId() { return id; }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getParentId() { return parentId; }
        public String getOrgType() { return orgType; }
        public Integer getLevel() { return level; }
        public Integer getOrderNum() { return orderNum; }
        public Integer getEnableStatus() { return enableStatus; }
        public String getCreationtime() { return creationtime; }
        public String getModifiedtime() { return modifiedtime; }
        public String getTenantId() { return tenantId; }

        public void setId(String id) { this.id = id; }
        public void setCode(String code) { this.code = code; }
        public void setName(String name) { this.name = name; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }
        public void setEnableStatus(Integer enableStatus) { this.enableStatus = enableStatus; }
        
        // Missing setters that caused compilation error
        public void setOrgType(String orgType) { this.orgType = orgType; }
        public void setLevel(Integer level) { this.level = level; }
        public void setCreationtime(String creationtime) { this.creationtime = creationtime; }
        public void setModifiedtime(String modifiedtime) { this.modifiedtime = modifiedtime; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }
}
