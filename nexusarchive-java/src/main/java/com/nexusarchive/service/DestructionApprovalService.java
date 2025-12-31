// Input: Destruction Entity, Approval Chain
// Output: DestructionApprovalService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

/**
 * 销毁审批服务
 * 
 * 功能：
 * 1. 初审审批
 * 2. 复核审批（双人复核）
 * 3. 获取审批链快照
 * 4. 状态流转：APPRAISING -> FIRST_APPROVED -> DESTRUCTION_APPROVED
 */
public interface DestructionApprovalService {
    
    /**
     * 初审审批
     * 
     * @param destructionId 销毁申请ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     * @param approved 是否同意（true=同意，false=拒绝）
     */
    void firstApproval(String destructionId, String approverId, String approverName, 
                       String comment, boolean approved);
    
    /**
     * 复核审批（双人复核）
     * 仅当初审通过后才能进行复核
     * 
     * @param destructionId 销毁申请ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     * @param approved 是否同意（true=同意，false=拒绝）
     */
    void secondApproval(String destructionId, String approverId, String approverName, 
                        String comment, boolean approved);
    
    /**
     * 获取审批链快照
     * 
     * @param destructionId 销毁申请ID
     * @return 审批链对象
     */
    com.nexusarchive.dto.ApprovalChain getApprovalChain(String destructionId);
}

