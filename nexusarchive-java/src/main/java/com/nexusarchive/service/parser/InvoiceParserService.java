package com.nexusarchive.service.parser;

import java.io.File;
import java.util.Map;

/**
 * 电子发票版式解析服务接口
 * 用于从 PDF/OFD 文件中原样提取文本坐标和元数据
 */
public interface InvoiceParserService {

    /**
     * 解析发票文件获取高亮元数据
     *
     * @param file 发票文件
     * @return 包含坐标信息的元数据 Map (JSON结构)
     */
    Map<String, Object> parse(File file);

    /**
     * 是否支持该文件类型
     *
     * @param fileType 文件扩展名 (pdf/ofd)
     * @return true if supported
     */
    boolean supports(String fileType);
}
