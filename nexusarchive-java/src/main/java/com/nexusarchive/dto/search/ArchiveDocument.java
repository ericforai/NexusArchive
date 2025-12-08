package com.nexusarchive.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Elasticsearch 档案索引文档模型
 * 用于全文检索
 * 
 * @author Agent D (基础设施工程师)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveDocument {

    /**
     * 档案ID
     */
    private String id;

    /**
     * 档案标题
     */
    private String title;

    /**
     * 档号
     */
    private String archivalCode;

    /**
     * 摘要描述
     */
    private String summary;

    /**
     * 分类代码 (AC01=凭证, AC02=账簿, AC03=报告)
     */
    private String categoryCode;

    /**
     * 全宗号
     */
    private String fondsCode;

    /**
     * 部门/组织ID
     */
    private String departmentId;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 会计年度
     */
    private String fiscalYear;

    /**
     * 保管期限
     */
    private String retentionPeriod;

    /**
     * OCR 提取的全文内容
     */
    private String fullText;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 档案状态
     */
    private String status;

    // === 高亮返回字段 ===

    /**
     * 高亮后的标题 (搜索返回时设置)
     */
    private String highlightedTitle;

    /**
     * 高亮后的摘要 (搜索返回时设置)
     */
    private String highlightedSummary;

    /**
     * 高亮后的全文片段 (搜索返回时设置)
     */
    private String highlightedFullText;
}
