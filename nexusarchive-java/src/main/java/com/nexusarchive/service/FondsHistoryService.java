// Input: FondsHistory Entity
// Output: FondsHistoryService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.FondsHistoryDetail;
import com.nexusarchive.entity.FondsHistory;

import java.time.LocalDate;
import java.util.List;

/**
 * 全宗沿革服务
 * 
 * 功能：
 * 1. 全宗迁移（MIGRATE）
 * 2. 全宗合并（MERGE）
 * 3. 全宗分立（SPLIT）
 * 4. 全宗重命名（RENAME）
 * 5. 查询沿革历史
 * 
 * PRD 来源: Section 1.1 - 全宗沿革可追溯
 */
public interface FondsHistoryService {
    
    /**
     * 全宗迁移
     * 
     * @param fromFondsNo 源全宗号
     * @param toFondsNo 目标全宗号
     * @param effectiveDate 生效日期
     * @param reason 迁移原因
     * @param approvalTicketId 审批票据ID
     * @param operatorId 操作人ID
     * @return 沿革记录ID
     */
    String migrateFonds(String fromFondsNo, String toFondsNo, LocalDate effectiveDate, 
                       String reason, String approvalTicketId, String operatorId);
    
    /**
     * 全宗合并
     * 
     * @param sourceFondsNos 源全宗号列表（将被合并的全宗）
     * @param targetFondsNo 目标全宗号（合并后的全宗）
     * @param effectiveDate 生效日期
     * @param reason 合并原因
     * @param approvalTicketId 审批票据ID
     * @param operatorId 操作人ID
     * @return 沿革记录ID列表
     */
    List<String> mergeFonds(List<String> sourceFondsNos, String targetFondsNo, 
                            LocalDate effectiveDate, String reason, 
                            String approvalTicketId, String operatorId);
    
    /**
     * 全宗分立
     * 
     * @param sourceFondsNo 源全宗号（将被分立的全宗）
     * @param newFondsNos 新全宗号列表（分立后的全宗）
     * @param effectiveDate 生效日期
     * @param reason 分立原因
     * @param approvalTicketId 审批票据ID
     * @param operatorId 操作人ID
     * @return 沿革记录ID列表
     */
    List<String> splitFonds(String sourceFondsNo, List<String> newFondsNos, 
                           LocalDate effectiveDate, String reason, 
                           String approvalTicketId, String operatorId);
    
    /**
     * 全宗重命名
     * 
     * @param oldFondsNo 旧全宗号
     * @param newFondsNo 新全宗号
     * @param effectiveDate 生效日期
     * @param reason 重命名原因
     * @param approvalTicketId 审批票据ID
     * @param operatorId 操作人ID
     * @return 沿革记录ID
     */
    String renameFonds(String oldFondsNo, String newFondsNo, LocalDate effectiveDate, 
                      String reason, String approvalTicketId, String operatorId);
    
    /**
     * 查询全宗沿革历史
     * 
     * @param fondsNo 全宗号
     * @return 沿革历史列表
     */
    List<FondsHistoryDetail> getFondsHistory(String fondsNo);
}

