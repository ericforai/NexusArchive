// Input: Spring Framework、本地模块
// Output: ErpFeedbackService 接口
// Pos: 业务服务层 - ERP 反馈服务

package com.nexusarchive.service;

import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.integration.erp.dto.FeedbackResult;

/**
 * ERP 反馈服务
 * <p>
 * 负责将归档状态反馈回 ERP 系统（存证溯源）
 * </p>
 */
public interface ErpFeedbackService {

    /**
     * 触发 ERP 系统反馈
     *
     * @param file        文件记录
     * @param archivalCode 档号
     * @return 反馈结果
     */
    FeedbackResult triggerFeedback(ArcFileContent file, String archivalCode);
}
