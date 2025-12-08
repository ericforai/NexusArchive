package com.nexusarchive.service.strategy;

import com.nexusarchive.entity.Archive;

/**
 * 自动关联匹配策略接口
 */
public interface MatchingStrategy {

    /**
     * 判断是否匹配
     * @param voucher 凭证档案 (AC01)
     * @param otherFile 其他档案 (AC02/AC03/AC04)
     * @return 匹配置信度 (0-100), 0表示不匹配
     */
    int match(Archive voucher, Archive otherFile);

    /**
     * 获取策略名称
     */
    String getName();
}
