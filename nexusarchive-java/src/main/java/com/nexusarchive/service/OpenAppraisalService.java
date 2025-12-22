// Input: MyBatis-Plus、Java 标准库、本地模块
// Output: OpenAppraisalService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.OpenAppraisal;

/**
 * 开放鉴定服务接口
 */
public interface OpenAppraisalService {

    /**
     * 创建鉴定任务
     * 
     * @param appraisal 鉴定任务信息
     * @return 创建的鉴定记录
     */
    OpenAppraisal createAppraisal(OpenAppraisal appraisal);

    /**
     * 提交鉴定结果
     * 
     * @param id 鉴定记录ID
     * @param appraiserId 鉴定人ID
     * @param appraiserName 鉴定人姓名
     * @param appraisalResult 鉴定结果 (OPEN, CONTROLLED, EXTENDED)
     * @param openLevel 开放等级 (PUBLIC, INTERNAL, RESTRICTED)
     * @param reason 鉴定理由
     */
    void submitAppraisal(String id, String appraiserId, String appraiserName, 
                        String appraisalResult, String openLevel, String reason);

    /**
     * 查询鉴定任务列表（分页）
     * 
     * @param page 页码
     * @param limit 每页数量
     * @param status 状态筛选（可选）
     * @return 分页结果
     */
    Page<OpenAppraisal> getAppraisalList(int page, int limit, String status);

    /**
     * 根据ID查询鉴定详情
     * 
     * @param id 鉴定记录ID
     * @return 鉴定记录
     */
    OpenAppraisal getAppraisalById(String id);
}
