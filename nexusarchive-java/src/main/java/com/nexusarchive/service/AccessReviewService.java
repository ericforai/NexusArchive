// Input: AccessReview Entity
// Output: AccessReviewService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.dto.request.AccessReviewRequest;
import com.nexusarchive.entity.AccessReview;

import java.time.LocalDate;
import java.util.List;

/**
 * 访问权限复核服务
 * 
 * 功能：
 * 1. 定期复核（Periodic Review）
 * 2. 临时复核（Ad Hoc Review）
 * 3. 按需复核（On Demand Review）
 * 4. 自动生成复核任务
 * 
 * PRD 来源: Section 7.1 - 身份与账号生命周期
 */
public interface AccessReviewService {
    
    /**
     * 创建复核任务
     * 
     * @param request 复核请求
     * @return 复核记录ID
     */
    String createReview(AccessReviewRequest request);
    
    /**
     * 执行复核
     * 
     * @param reviewId 复核记录ID
     * @param reviewerId 复核人ID
     * @param approved 是否批准
     * @param reviewResult 复核结果说明
     * @param actionTaken 采取的行动
     */
    void executeReview(String reviewId, String reviewerId, boolean approved, 
                      String reviewResult, String actionTaken);
    
    /**
     * 查询用户的复核记录
     * 
     * @param userId 用户ID
     * @return 复核记录列表
     */
    List<AccessReview> getUserReviews(String userId);
    
    /**
     * 生成定期复核任务
     * 定时任务调用
     */
    void generatePeriodicReviews();
    
    /**
     * 查询待复核的任务
     * 
     * @param reviewerId 复核人ID（可选）
     * @return 待复核任务列表
     */
    List<AccessReview> getPendingReviews(String reviewerId);
}

