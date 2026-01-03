// Input: Archive Entity, LocalDate
// Output: ArchiveFreezeService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import java.time.LocalDate;

/**
 * 档案冻结/保全服务
 * 
 * 功能：
 * 1. 冻结档案（审计/诉讼冻结）
 * 2. 解除冻结
 * 3. 检查档案是否被冻结
 * 
 * PRD 要求：
 * - 冻结/保全期间禁止销毁
 * - 解除冻结需审批
 * - 状态流转记录在审计日志
 */
public interface ArchiveFreezeService {
    
    /**
     * 冻结档案
     * 
     * @param archiveId 档案ID
     * @param reason 冻结原因
     * @param operatorId 操作人ID
     * @param expireDate 冻结到期日期（可选，null 表示永久冻结）
     */
    void freezeArchive(String archiveId, String reason, String operatorId, LocalDate expireDate);
    
    /**
     * 解除冻结
     * 
     * @param archiveId 档案ID
     * @param reason 解除原因
     * @param operatorId 操作人ID
     */
    void unfreezeArchive(String archiveId, String reason, String operatorId);
    
    /**
     * 检查档案是否被冻结
     * 
     * @param archiveId 档案ID
     * @return 是否被冻结
     */
    boolean isFrozen(String archiveId);
    
    /**
     * 批量冻结档案
     * 
     * @param archiveIds 档案ID列表
     * @param reason 冻结原因
     * @param operatorId 操作人ID
     * @param expireDate 冻结到期日期（可选）
     */
    void freezeArchives(java.util.List<String> archiveIds, String reason, String operatorId, LocalDate expireDate);
}


