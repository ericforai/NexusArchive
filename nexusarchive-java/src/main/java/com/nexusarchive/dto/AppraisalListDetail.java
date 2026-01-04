// Input: AppraisalList, Archive
// Output: AppraisalListDetail DTO
// Pos: DTO层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 鉴定清单详情DTO
 * 用于返回给前端展示
 */
@Data
public class AppraisalListDetail {
    
    /**
     * 鉴定清单ID
     */
    private String appraisalListId;
    
    /**
     * 全宗号
     */
    private String fondsNo;
    
    /**
     * 归档年度
     */
    private Integer archiveYear;
    
    /**
     * 鉴定人信息
     */
    private String appraiserId;
    private String appraiserName;
    private LocalDate appraisalDate;
    
    /**
     * 鉴定结论
     */
    private String conclusion; // APPROVED, REJECTED, DEFERRED
    
    /**
     * 鉴定意见
     */
    private String comment;
    
    /**
     * 状态
     */
    private String status;
    
    /**
     * 档案列表（包含完整元数据）
     */
    private List<ArchiveAppraisalItem> archives;
    
    /**
     * 档案鉴定项
     */
    @Data
    public static class ArchiveAppraisalItem {
        private String archiveId;
        private String archiveCode;
        private String title;
        private LocalDate docDate;
        private LocalDate archivedAt;
        private LocalDate retentionStartDate;
        private String retentionPeriod;
        private LocalDate expirationDate;
        private String orgName;
        private String creator;
    }
}



