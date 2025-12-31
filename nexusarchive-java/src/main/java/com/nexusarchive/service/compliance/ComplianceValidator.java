// Input: Spring Framework
// Output: ComplianceValidator 接口
// Pos: 服务层 - 合规验证器接口

package com.nexusarchive.service.compliance;

import com.nexusarchive.entity.Archive;

/**
 * 合规验证器接口
 * <p>
 * 所有验证器必须实现此接口，确保统一的验证流程
 * </p>
 */
public interface ComplianceValidator {

    /**
     * 验证档案是否符合特定规则
     *
     * @param archive 待验证档案
     * @return 验证结果
     */
    ComplianceResult validate(Archive archive);

    /**
     * 获取验证器名称
     */
    String getName();

    /**
     * 获取验证优先级（数字越小优先级越高）
     */
    int getPriority();
}
