// Input: Java 标准库、本地模块
// Output: ArchivalCodeGenerator 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

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
