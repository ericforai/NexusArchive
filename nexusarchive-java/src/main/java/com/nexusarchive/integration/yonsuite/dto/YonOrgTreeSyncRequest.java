// Input: Lombok、Java 标准库
// Output: YonOrgTreeSyncRequest 类
// Pos: 数据传输对象 - YonSuite 组织树同步请求

package com.nexusarchive.integration.yonsuite.dto;

import lombok.Data;

/**
 * YonSuite 组织树版本同步请求 DTO
 * API: POST /yonbip/digitalModel/openapi/treedatasync/treeversionsync
 */
@Data
public class YonOrgTreeSyncRequest {

    /**
     * 时间戳（用于增量同步）
     * 格式: yyyy-MM-dd HH:mm:ss
     * 示例: 2000-01-01 00:00:00 (全量同步)
     */
    private String pubts;

    /**
     * 功能代码
     * adminorg - 行政组织
     * orgunit - 组织单元
     */
    private String funcCode;

    /**
     * 分页，从1开始
     */
    private Integer pageIndex;

    /**
     * 每页大小
     */
    private Integer pageSize;

    /**
     * 日期（可选）
     */
    private String timelineDate;
}
