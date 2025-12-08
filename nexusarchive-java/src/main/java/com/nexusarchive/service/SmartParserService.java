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
