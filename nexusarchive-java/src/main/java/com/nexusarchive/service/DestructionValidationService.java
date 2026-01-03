// Input: Archive Entity, Borrowing Record
// Output: DestructionValidationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import java.util.List;

/**
 * 销毁校验服务
 * 
 * 功能：
 * 1. 校验档案是否可销毁（检查在借记录、冻结状态等）
 * 2. 检查档案是否在借
 * 3. 批量校验档案列表
 * 
 * PRD 要求：执行销毁前，必须检查所有档案是否存在在借记录
 * 仅允许 RETURNED 或无借阅记录的档案进入销毁
 */
public interface DestructionValidationService {
    
    /**
     * 校验档案是否可销毁
     * 
     * @param archiveIds 待销毁档案ID列表
     * @param fondsNo 全宗号（用于权限校验）
     * @throws DestructionNotAllowedException 若存在在借记录或冻结状态
     */
    void validateDestructionEligibility(List<String> archiveIds, String fondsNo);
    
    /**
     * 检查档案是否在借
     * 
     * @param archiveId 档案ID
     * @param fondsNo 全宗号
     * @param archiveYear 归档年度（可选，用于优化查询）
     * @return true 如果档案在借
     */
    boolean isBorrowed(String archiveId, String fondsNo, Integer archiveYear);
    
    /**
     * 批量检查档案是否在借
     * 
     * @param archiveIds 档案ID列表
     * @return 在借的档案ID列表
     */
    List<String> findBorrowedArchives(List<String> archiveIds);
}


