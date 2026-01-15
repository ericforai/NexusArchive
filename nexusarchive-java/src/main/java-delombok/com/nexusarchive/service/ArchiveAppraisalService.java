// Input: Archive Entity, Appraisal List
// Output: ArchiveAppraisalService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import java.time.LocalDate;
import java.util.List;

/**
 * 档案鉴定清单服务
 * 
 * 功能：
 * 1. 生成鉴定清单（包含档案元数据快照）
 * 2. 提交鉴定结论（同意销毁/不同意销毁/延期保管）
 * 3. 导出鉴定清单（Excel/PDF）
 * 4. 更新档案状态为 APPRAISING
 * 
 * 法规依据：《会计档案管理办法》要求销毁前需进行鉴定
 */
public interface ArchiveAppraisalService {
    
    /**
     * 生成鉴定清单
     * 
     * @param archiveIds 待鉴定档案ID列表
     * @param fondsNo 全宗号（从登录态获取）
     * @param appraiserId 鉴定人ID
     * @param appraisalDate 鉴定日期
     * @return 鉴定清单ID
     */
    String createAppraisalList(List<String> archiveIds, String fondsNo, 
                                String appraiserId, LocalDate appraisalDate);
    
    /**
     * 提交鉴定结论
     * 
     * @param appraisalListId 鉴定清单ID
     * @param conclusion 鉴定结论：APPROVED（同意销毁）/ REJECTED（不同意销毁）/ DEFERRED（延期保管）
     * @param comment 鉴定意见
     */
    void submitAppraisalConclusion(String appraisalListId, String conclusion, String comment);
    
    /**
     * 导出鉴定清单
     * 
     * @param appraisalListId 鉴定清单ID
     * @param format 导出格式：EXCEL 或 PDF
     * @return 文件字节数组
     */
    byte[] exportAppraisalList(String appraisalListId, ExportFormat format);
    
    /**
     * 获取鉴定清单详情
     * 
     * @param appraisalListId 鉴定清单ID
     * @return 鉴定清单详情DTO
     */
    com.nexusarchive.dto.AppraisalListDetail getAppraisalListDetail(String appraisalListId);
    
    enum ExportFormat {
        EXCEL, PDF
    }
}

