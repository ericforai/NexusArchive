// Input: Java 标准库、本地模块
// Output: SmartParserService 接口
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.entity.ArcFileContent;
import java.util.List;

/**
 * 智能解析服务
 * 负责从归档文件中提取结构化元数据
 */
public interface SmartParserService {

    /**
     * 异步解析并建立索引
     * 
     * @param files 待解析的文件列表
     */
    void parseAndIndex(List<ArcFileContent> files);
}
