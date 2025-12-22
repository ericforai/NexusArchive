// Input: MyBatis-Plus、Java 标准库、本地模块
// Output: ArchiveApprovalService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.ArchiveApproval;

/**
 * 档案审批服务接口
 */
public interface ArchiveApprovalService {

    /**
     * 创建审批申请
     * 
     * @param approval 审批申请信息
     * @return 创建的审批记录
     */
    ArchiveApproval createApproval(ArchiveApproval approval);

    /**
     * 批准归档
     * 
     * @param id 审批记录ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     */
    void approveArchive(String id, String approverId, String approverName, String comment);

    /**
     * 拒绝归档
     * 
     * @param id 审批记录ID
     * @param approverId 审批人ID
     * @param approverName 审批人姓名
     * @param comment 审批意见
     */
    void rejectArchive(String id, String approverId, String approverName, String comment);

    /**
     * 查询审批列表（分页）
     * 
     * @param page 页码
     * @param limit 每页数量
     * @param status 状态筛选（可选）
     * @return 分页结果
     */
    Page<ArchiveApproval> getApprovalList(int page, int limit, String status);

    /**
     * 根据ID查询审批详情
     * 
     * @param id 审批记录ID
     * @return 审批记录
     */
    ArchiveApproval getApprovalById(String id);
}
