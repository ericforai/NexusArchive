// Input: Approval Information
// Output: ApprovalChain DTO
// Pos: DTO层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批链DTO
 * 用于存储和展示审批链信息
 */
@Data
public class ApprovalChain {
    
    /**
     * 初审审批信息
     */
    private ApprovalInfo firstApproval;
    
    /**
     * 复核审批信息
     */
    private ApprovalInfo secondApproval;
    
    /**
     * 审批信息
     */
    @Data
    public static class ApprovalInfo {
        /**
         * 审批人ID
         */
        private String approverId;
        
        /**
         * 审批人姓名
         */
        private String approverName;
        
        /**
         * 审批意见
         */
        private String comment;
        
        /**
         * 是否同意
         */
        private Boolean approved;
        
        /**
         * 审批时间
         */
        private LocalDateTime timestamp;
    }
}


