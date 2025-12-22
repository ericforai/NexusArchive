// Input: Java 标准库、本地模块
// Output: ReconciliationService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.ReconciliationRecord;
import java.time.LocalDate;
import java.util.List;

/**
 * 账、凭、证三位一体核对服务接口
 */
public interface ReconciliationService {

    /**
     * 执行核对
     * 
     * @param configId ERP配置ID
     * @param subjectCode 科目代码(可选；为空时执行凭证级核对)
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param operatorId 操作人ID
     * @return 核对结果记录
     */
    ReconciliationRecord performReconciliation(Long configId, String subjectCode, 
                                              LocalDate startDate, LocalDate endDate, 
                                              String operatorId);

    /**
     * 获取历史核对记录
     * 
     * @param configId ERP配置ID
     * @return 记录列表
     */
    List<ReconciliationRecord> getHistory(Long configId);
}
