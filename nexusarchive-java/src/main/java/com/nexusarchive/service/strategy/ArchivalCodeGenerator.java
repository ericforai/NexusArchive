package com.nexusarchive.service.strategy;

import com.nexusarchive.entity.Archive;

/**
 * 档号生成策略接口
 * 负责生成符合 DA/T 94-2022 标准的唯一档号
 */
public interface ArchivalCodeGenerator {

    /**
     * 生成下一个档号
     * @param archive 待归档的档案元数据 (必须包含 fondsCode, fiscalYear, retentionPeriod, categoryCode 等)
     * @return 标准档号 String
     */
    String generateNextCode(Archive archive);
}
